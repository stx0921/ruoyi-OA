package com.ruoyi.oa.storage.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.storage.FileStorageRouter;
import com.ruoyi.common.storage.LocalFileStorage;
import com.ruoyi.common.storage.OssFileStorage;
import com.ruoyi.common.storage.StorageException;
import com.ruoyi.common.storage.StorageKeyUtils;
import com.ruoyi.common.storage.StorageObject;
import com.ruoyi.common.storage.StorageProperties;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.oa.storage.domain.StorageMigrationResult;
import com.ruoyi.oa.storage.domain.StorageRef;
import com.ruoyi.oa.storage.domain.StorageStatusVO;
import com.ruoyi.oa.storage.mapper.StorageMigrateMapper;
import com.ruoyi.oa.storage.support.StoragePathUtils;

/**
 * 存量存储迁移：本地磁盘 /profile 存量 → 阿里云 OSS（含头像），支持 dry-run、分批、CAS 幂等与回滚。
 *
 * <p>设计约束：</p>
 * <ul>
 *   <li>只读本地文件，不删除；迁移失败保持原路径不变，下次重跑自动重试；</li>
 *   <li>跳过 {@code biz_type='import_error'} 与 {@code /profile/import/}（本地临时文件）；</li>
 *   <li>多实例用 Redis SETNX 锁（10 分钟）防重复执行；</li>
 *   <li>CAS 更新（where 旧值）防并发覆盖；</li>
 *   <li>分批限速（默认每批 200 条，每条间隔 50ms），避免占满带宽。</li>
 * </ul>
 *
 * @author ruoyi-oa
 */
@Service
public class StorageMigrationService
{
    private static final Logger log = LoggerFactory.getLogger(StorageMigrationService.class);

    /** 分布式迁移锁 key */
    private static final String LOCK_KEY = "storage:migrate:lock";

    /** 锁超时（分钟），超时自动释放，避免实例崩溃后永久锁死 */
    private static final int LOCK_MINUTES = 10;

    private static final int MAX_LIMIT = 2000;

    private static final int DEFAULT_LIMIT = 200;

    /** 单次响应最多返回的失败明细条数 */
    private static final int MAX_FAILURES = 20;

    private static final String TYPE_OA_FILE = "oa_file";

    private static final String TYPE_AVATAR = "avatar";

    private static final String TYPE_ALL = "all";

    private static final String MODE_MIGRATE = "migrate";

    private static final String MODE_ROLLBACK = "rollback";

    /** 每条记录之间的限速间隔（毫秒），避免迁移占满带宽 */
    private static final long RATE_LIMIT_SLEEP_MS = 50L;

    @Autowired
    private StorageMigrateMapper migrateMapper;

    @Autowired
    private FileStorageRouter storageRouter;

    @Autowired
    private LocalFileStorage localFileStorage;

    @Autowired
    private OssFileStorage ossFileStorage;

    @Autowired
    private RedisCache redisCache;

    /**
     * 存储状态 + 待迁移/已迁移统计。
     */
    public StorageStatusVO status()
    {
        StorageProperties properties = storageRouter.getProperties();
        StorageProperties.Oss oss = properties.getOss();
        StorageStatusVO vo = new StorageStatusVO();
        vo.setType(properties.getType());
        vo.setReadFallback(properties.isReadFallback());
        vo.setOssEnabled(storageRouter.isOssReady());
        vo.setBucket(oss.getBucket());
        vo.setEndpoint(oss.getEndpoint());
        vo.setKeyPrefix(oss.getKeyPrefix());
        vo.setPrivateBucket(oss.isPrivate());
        vo.setPublicDomain(oss.getPublicDomain());
        vo.setSignExpireSeconds(oss.getSignExpireSeconds());
        vo.setInstanceRole(oss.isUseInstanceRole());
        vo.setPendingOaFiles(migrateMapper.countLegacyOaFiles());
        vo.setPendingAvatars(migrateMapper.countLegacyAvatars());
        vo.setOssOaFiles(migrateMapper.countOssOaFiles());
        vo.setOssAvatars(migrateMapper.countOssAvatars());
        return vo;
    }

    /**
     * 对账：迁移后两条对账 SQL 的计数（都应为 0），以及已在 OSS 的条数。
     */
    public Map<String, Object> reconcile()
    {
        long pendingOaFiles = migrateMapper.countLegacyOaFiles();
        long pendingAvatars = migrateMapper.countLegacyAvatars();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pendingOaFiles", pendingOaFiles);
        map.put("pendingAvatars", pendingAvatars);
        map.put("ossOaFiles", migrateMapper.countOssOaFiles());
        map.put("ossAvatars", migrateMapper.countOssAvatars());
        map.put("passed", pendingOaFiles == 0L && pendingAvatars == 0L);
        map.put("note", "迁移完成后 pendingOaFiles / pendingAvatars 均应为 0；import_error 与 /profile/import/ 不参与迁移");
        return map;
    }

    /**
     * 执行迁移 / 回滚。
     *
     * @param mode migrate（本地→OSS）| rollback（OSS→本地，危险）
     * @param type oa_file | avatar | all
     * @param dryRun true 只统计不上传不写库
     * @param limit 单次处理条数（1-2000，默认 200）
     */
    public StorageMigrationResult migrate(String mode, String type, boolean dryRun, int limit)
    {
        String normalizedMode = normalizeMode(mode);
        String normalizedType = normalizeType(type);
        int normalizedLimit = normalizeLimit(limit);

        StorageMigrationResult result = new StorageMigrationResult();
        result.setMode(normalizedMode);
        result.setType(normalizedType);
        result.setDryRun(dryRun);
        result.setLimit(normalizedLimit);
        long start = System.currentTimeMillis();

        String lockToken = null;
        if (!dryRun)
        {
            lockToken = tryLock();
            if (lockToken == null)
            {
                throw new ServiceException("已有存储迁移任务正在执行，请稍后再试", 429);
            }
        }
        try
        {
            if (MODE_ROLLBACK.equals(normalizedMode))
            {
                doRollback(result);
            }
            else
            {
                doMigrate(result);
            }
        }
        finally
        {
            if (lockToken != null)
            {
                releaseLock(lockToken);
            }
            result.setPendingAfter(pendingCount(result.getType(), normalizedMode));
            result.setDurationMs(System.currentTimeMillis() - start);
        }
        log.info("存储{}完成 mode={} type={} dryRun={} scanned={} migrated={} skipped={} missing={} failed={} bytes={}",
                MODE_ROLLBACK.equals(normalizedMode) ? "回滚" : "迁移", normalizedMode, normalizedType, dryRun,
                result.getScanned(), result.getMigrated(), result.getSkipped(), result.getMissing(), result.getFailed(),
                result.getBytes());
        return result;
    }

    // ------------------------------------------------------------------ 正向迁移

    private void doMigrate(StorageMigrationResult result)
    {
        result.setPendingBefore(countLegacy(result.getType()));
        if (TYPE_OA_FILE.equals(result.getType()) || TYPE_ALL.equals(result.getType()))
        {
            migrateOaFiles(result);
        }
        if (TYPE_AVATAR.equals(result.getType()) || TYPE_ALL.equals(result.getType()))
        {
            migrateAvatars(result);
        }
    }

    private void migrateOaFiles(StorageMigrationResult result)
    {
        Long afterId = 0L;
        int processed = 0;
        while (processed < result.getLimit())
        {
            int pageSize = Math.min(result.getLimit() - processed, 200);
            List<StorageRef> rows = migrateMapper.selectLegacyOaFilesAfter(afterId, pageSize);
            if (rows == null || rows.isEmpty())
            {
                break;
            }
            for (StorageRef row : rows)
            {
                afterId = row.getId();
                processed++;
                result.setScanned(result.getScanned() + 1);
                migrateOneAttachment(row, result);
            }
            if (rows.size() < pageSize)
            {
                break;
            }
        }
    }

    private void migrateOneAttachment(StorageRef row, StorageMigrationResult result)
    {
        String oldPath = row.getPath();
        if (!StoragePathUtils.isProfileRef(oldPath))
        {
            result.setSkipped(result.getSkipped() + 1);
            return;
        }
        String key = StoragePathUtils.localKeyOf(oldPath);
        if (StringUtils.isEmpty(key) || !StorageKeyUtils.isSafeKey(key))
        {
            result.setFailed(result.getFailed() + 1);
            addFailure(result, row.getId(), oldPath, "非法历史路径，未迁移");
            return;
        }
        File localFile = resolveLocalFile(key);
        if (localFile == null || !localFile.isFile())
        {
            result.setMissing(result.getMissing() + 1);
            addFailure(result, row.getId(), oldPath, "本地文件不存在，未迁移（库不改，可重试）");
            return;
        }
        long size = localFile.length();
        if (result.isDryRun())
        {
            result.setBytes(result.getBytes() + size);
            result.setMigrated(result.getMigrated() + 1);
            return;
        }
        try
        {
            uploadLocalIfNeeded(key, localFile, size);
            // 业务附件 OSS 模式写裸 key（与 FileStorageRouter.storeRef 规则一致）
            String newPath = key;
            int updated = migrateMapper.updateOaFilePath(row.getId(), oldPath, newPath);
            if (updated > 0)
            {
                result.setMigrated(result.getMigrated() + 1);
                result.setBytes(result.getBytes() + size);
            }
            else
            {
                // 并发修改：保持库中现值不变
                result.setSkipped(result.getSkipped() + 1);
            }
            sleepQuietly();
        }
        catch (Exception e)
        {
            result.setFailed(result.getFailed() + 1);
            addFailure(result, row.getId(), oldPath, friendlyError(e));
            log.warn("OA 附件迁移失败 fileId={} path={}", row.getId(), oldPath, e);
        }
    }

    private void migrateAvatars(StorageMigrationResult result)
    {
        Long afterId = 0L;
        int processed = 0;
        while (processed < result.getLimit())
        {
            int pageSize = Math.min(result.getLimit() - processed, 200);
            List<StorageRef> rows = migrateMapper.selectLegacyAvatarsAfter(afterId, pageSize);
            if (rows == null || rows.isEmpty())
            {
                break;
            }
            for (StorageRef row : rows)
            {
                afterId = row.getId();
                processed++;
                result.setScanned(result.getScanned() + 1);
                migrateOneAvatar(row, result);
            }
            if (rows.size() < pageSize)
            {
                break;
            }
        }
    }

    private void migrateOneAvatar(StorageRef row, StorageMigrationResult result)
    {
        String oldPath = row.getPath();
        if (!StoragePathUtils.isProfileRef(oldPath))
        {
            result.setSkipped(result.getSkipped() + 1);
            return;
        }
        String key = StoragePathUtils.localKeyOf(oldPath);
        if (StringUtils.isEmpty(key) || !StorageKeyUtils.isSafeKey(key))
        {
            result.setFailed(result.getFailed() + 1);
            addFailure(result, row.getId(), oldPath, "非法历史路径，未迁移");
            return;
        }
        File localFile = resolveLocalFile(key);
        if (localFile == null || !localFile.isFile())
        {
            result.setMissing(result.getMissing() + 1);
            addFailure(result, row.getId(), oldPath, "本地头像文件不存在，未迁移（库不改，可重试）");
            return;
        }
        long size = localFile.length();
        if (result.isDryRun())
        {
            result.setBytes(result.getBytes() + size);
            result.setMigrated(result.getMigrated() + 1);
            return;
        }
        try
        {
            uploadLocalIfNeeded(key, localFile, size);
            String newPath = StoragePathUtils.toAttachRef(key);
            int updated = migrateMapper.updateAvatarPath(row.getId(), oldPath, newPath);
            if (updated > 0)
            {
                result.setMigrated(result.getMigrated() + 1);
                result.setBytes(result.getBytes() + size);
            }
            else
            {
                result.setSkipped(result.getSkipped() + 1);
            }
            sleepQuietly();
        }
        catch (Exception e)
        {
            result.setFailed(result.getFailed() + 1);
            addFailure(result, row.getId(), oldPath, friendlyError(e));
            log.warn("头像迁移失败 userId={} path={}", row.getId(), oldPath, e);
        }
    }

    /**
     * OSS 已有同 key 对象且大小一致时跳过上传（幂等），否则覆盖上传。
     */
    private void uploadLocalIfNeeded(String key, File localFile, long size)
    {
        if (!ossFileStorage.isReady())
        {
            throw new StorageException("OSS 存储未就绪，请先完成 ruoyi.storage.oss 配置");
        }
        if (ossFileStorage.exists(key))
        {
            long remoteSize = remoteSize(key);
            if (remoteSize == size)
            {
                return;
            }
            log.warn("OSS 对象已存在但大小不一致，将覆盖上传 key={} local={} remote={}", key, size, remoteSize);
        }
        try (InputStream in = new FileInputStream(localFile))
        {
            ossFileStorage.put(key, in, size);
        }
        catch (StorageException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new StorageException("本地文件读取失败：" + key, e);
        }
    }

    private long remoteSize(String key)
    {
        try (StorageObject object = ossFileStorage.open(key))
        {
            return object == null ? -1L : object.getSize();
        }
        catch (Exception e)
        {
            log.warn("读取 OSS 对象大小失败 key={}", key, e);
            return -1L;
        }
    }

    // ------------------------------------------------------------------ 反向回滚

    private void doRollback(StorageMigrationResult result)
    {
        if (!ossFileStorage.isReady())
        {
            throw new ServiceException("OSS 存储未就绪，无法执行回滚", 503);
        }
        result.setPendingBefore(countOss(result.getType()));
        if (TYPE_OA_FILE.equals(result.getType()) || TYPE_ALL.equals(result.getType()))
        {
            rollbackOaFiles(result);
        }
        if (TYPE_AVATAR.equals(result.getType()) || TYPE_ALL.equals(result.getType()))
        {
            rollbackAvatars(result);
        }
    }

    private void rollbackOaFiles(StorageMigrationResult result)
    {
        Long afterId = 0L;
        int processed = 0;
        while (processed < result.getLimit())
        {
            int pageSize = Math.min(result.getLimit() - processed, 200);
            List<StorageRef> rows = migrateMapper.selectOssOaFilesAfter(afterId, pageSize);
            if (rows == null || rows.isEmpty())
            {
                break;
            }
            for (StorageRef row : rows)
            {
                afterId = row.getId();
                processed++;
                result.setScanned(result.getScanned() + 1);
                rollbackOneAttachment(row, result);
            }
            if (rows.size() < pageSize)
            {
                break;
            }
        }
    }

    private void rollbackOneAttachment(StorageRef row, StorageMigrationResult result)
    {
        String oldPath = row.getPath();
        if (!StoragePathUtils.isOssKeyRef(oldPath) || !StorageKeyUtils.isSafeKey(oldPath))
        {
            result.setSkipped(result.getSkipped() + 1);
            return;
        }
        if (result.isDryRun())
        {
            long size = remoteSize(oldPath);
            if (size < 0)
            {
                result.setMissing(result.getMissing() + 1);
                addFailure(result, row.getId(), oldPath, "OSS 对象不存在，无法回滚");
            }
            else
            {
                result.setBytes(result.getBytes() + size);
                result.setMigrated(result.getMigrated() + 1);
            }
            return;
        }
        try (StorageObject object = storageRouter.openByPath(oldPath))
        {
            if (object == null)
            {
                result.setMissing(result.getMissing() + 1);
                addFailure(result, row.getId(), oldPath, "OSS 对象不存在，无法回滚");
                return;
            }
            // 防止回滚覆盖本地已有且不一致的文件
            if (!canWriteLocal(oldPath, object, result, row.getId()))
            {
                return;
            }
            writeLocalIfAbsent(oldPath, object);
            String newPath = StoragePathUtils.toProfileRef(oldPath);
            int updated = migrateMapper.updateOaFilePath(row.getId(), oldPath, newPath);
            if (updated > 0)
            {
                result.setMigrated(result.getMigrated() + 1);
                result.setBytes(result.getBytes() + Math.max(0L, object.getSize()));
            }
            else
            {
                result.setSkipped(result.getSkipped() + 1);
            }
            sleepQuietly();
        }
        catch (Exception e)
        {
            result.setFailed(result.getFailed() + 1);
            addFailure(result, row.getId(), oldPath, friendlyError(e));
            log.warn("OA 附件回滚失败 fileId={} path={}", row.getId(), oldPath, e);
        }
    }

    private void rollbackAvatars(StorageMigrationResult result)
    {
        Long afterId = 0L;
        int processed = 0;
        while (processed < result.getLimit())
        {
            int pageSize = Math.min(result.getLimit() - processed, 200);
            List<StorageRef> rows = migrateMapper.selectOssAvatarsAfter(afterId, pageSize);
            if (rows == null || rows.isEmpty())
            {
                break;
            }
            for (StorageRef row : rows)
            {
                afterId = row.getId();
                processed++;
                result.setScanned(result.getScanned() + 1);
                rollbackOneAvatar(row, result);
            }
            if (rows.size() < pageSize)
            {
                break;
            }
        }
    }

    private void rollbackOneAvatar(StorageRef row, StorageMigrationResult result)
    {
        String oldPath = row.getPath();
        String key = StoragePathUtils.attachKeyOf(oldPath);
        if (StringUtils.isEmpty(key) || !StorageKeyUtils.isSafeKey(key))
        {
            result.setSkipped(result.getSkipped() + 1);
            return;
        }
        if (result.isDryRun())
        {
            long size = remoteSize(key);
            if (size < 0)
            {
                result.setMissing(result.getMissing() + 1);
                addFailure(result, row.getId(), oldPath, "OSS 头像对象不存在，无法回滚");
            }
            else
            {
                result.setBytes(result.getBytes() + size);
                result.setMigrated(result.getMigrated() + 1);
            }
            return;
        }
        try (StorageObject object = storageRouter.openByPath(oldPath))
        {
            if (object == null)
            {
                result.setMissing(result.getMissing() + 1);
                addFailure(result, row.getId(), oldPath, "OSS 头像对象不存在，无法回滚");
                return;
            }
            // 防止回滚覆盖本地已有且不一致的文件
            if (!canWriteLocal(key, object, result, row.getId()))
            {
                return;
            }
            writeLocalIfAbsent(key, object);
            String newPath = StoragePathUtils.toProfileRef(key);
            int updated = migrateMapper.updateAvatarPath(row.getId(), oldPath, newPath);
            if (updated > 0)
            {
                result.setMigrated(result.getMigrated() + 1);
                result.setBytes(result.getBytes() + Math.max(0L, object.getSize()));
            }
            else
            {
                result.setSkipped(result.getSkipped() + 1);
            }
            sleepQuietly();
        }
        catch (Exception e)
        {
            result.setFailed(result.getFailed() + 1);
            addFailure(result, row.getId(), oldPath, friendlyError(e));
            log.warn("头像回滚失败 userId={} path={}", row.getId(), oldPath, e);
        }
    }

    // ------------------------------------------------------------------ 通用

    private File resolveLocalFile(String key)
    {
        String profile = RuoYiConfig.getProfile();
        if (StringUtils.isEmpty(profile))
        {
            return null;
        }
        return new File(profile, key);
    }

    /**
     * 回滚写入前检查：本地已存在同名文件且大小不同 → 记失败、不覆盖；大小一致 → 复用现有文件。
     *
     * @return true 表示可继续（不存在或大小一致）
     */
    private boolean canWriteLocal(String key, StorageObject object, StorageMigrationResult result, Long id)
    {
        File existing = resolveLocalFile(key);
        if (existing == null || !existing.isFile())
        {
            return true;
        }
        if (object.getSize() >= 0 && existing.length() == object.getSize())
        {
            return true;
        }
        result.setFailed(result.getFailed() + 1);
        addFailure(result, id, key, "本地已存在同名文件且大小不一致，未覆盖（请人工确认后重试）");
        return false;
    }

    /**
     * 本地不存在同名文件时写入；大小一致时直接复用（不重复写盘）。
     */
    private void writeLocalIfAbsent(String key, StorageObject object) throws IOException
    {
        File existing = resolveLocalFile(key);
        if (existing != null && existing.isFile() && object.getSize() >= 0 && existing.length() == object.getSize())
        {
            return;
        }
        localFileStorage.put(key, object.getInputStream(), object.getSize());
    }

    private long countLegacy(String type)
    {
        if (TYPE_OA_FILE.equals(type))
        {
            return migrateMapper.countLegacyOaFiles();
        }
        if (TYPE_AVATAR.equals(type))
        {
            return migrateMapper.countLegacyAvatars();
        }
        return migrateMapper.countLegacyOaFiles() + migrateMapper.countLegacyAvatars();
    }

    private long countOss(String type)
    {
        if (TYPE_OA_FILE.equals(type))
        {
            return migrateMapper.countOssOaFiles();
        }
        if (TYPE_AVATAR.equals(type))
        {
            return migrateMapper.countOssAvatars();
        }
        return migrateMapper.countOssOaFiles() + migrateMapper.countOssAvatars();
    }

    private long pendingCount(String type, String mode)
    {
        return MODE_ROLLBACK.equals(mode) ? countOss(type) : countLegacy(type);
    }

    private String normalizeMode(String mode)
    {
        String value = StringUtils.isEmpty(mode) ? MODE_MIGRATE : mode.trim().toLowerCase();
        if (!MODE_MIGRATE.equals(value) && !MODE_ROLLBACK.equals(value))
        {
            throw new ServiceException("不支持的迁移模式：" + mode + "（仅支持 migrate / rollback）", 400);
        }
        return value;
    }

    private String normalizeType(String type)
    {
        String value = StringUtils.isEmpty(type) ? TYPE_ALL : type.trim().toLowerCase();
        if (!TYPE_OA_FILE.equals(value) && !TYPE_AVATAR.equals(value) && !TYPE_ALL.equals(value))
        {
            throw new ServiceException("不支持的迁移类型：" + type + "（仅支持 oa_file / avatar / all）", 400);
        }
        return value;
    }

    private int normalizeLimit(int limit)
    {
        if (limit <= 0)
        {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void addFailure(StorageMigrationResult result, Long id, String path, String error)
    {
        if (result.getFailures().size() < MAX_FAILURES)
        {
            result.getFailures().add(new StorageMigrationResult.Failure(id, path, error));
        }
    }

    private String friendlyError(Exception e)
    {
        if (e instanceof StorageException)
        {
            return e.getMessage();
        }
        return "迁移失败：" + e.getClass().getSimpleName();
    }

    private String tryLock()
    {
        String token = IdUtils.fastSimpleUUID();
        try
        {
            Boolean acquired = redisCache.redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, token, LOCK_MINUTES,
                    TimeUnit.MINUTES);
            return Boolean.TRUE.equals(acquired) ? token : null;
        }
        catch (Exception e)
        {
            log.error("获取存储迁移锁失败（Redis 不可用）", e);
            throw new ServiceException("迁移锁获取失败，请检查 Redis 服务", 500);
        }
    }

    private void releaseLock(String token)
    {
        try
        {
            Object current = redisCache.redisTemplate.opsForValue().get(LOCK_KEY);
            if (current != null && token.equals(String.valueOf(current)))
            {
                redisCache.redisTemplate.delete(LOCK_KEY);
            }
        }
        catch (Exception e)
        {
            log.warn("释放存储迁移锁失败（{} 分钟后自动过期）", LOCK_MINUTES, e);
        }
    }

    private void sleepQuietly()
    {
        try
        {
            Thread.sleep(RATE_LIMIT_SLEEP_MS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
