package com.ruoyi.oa.storage.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储迁移/回滚结果（dry-run 与执行共用）。
 *
 * @author ruoyi-oa
 */
public class StorageMigrationResult
{
    /** migrate | rollback */
    private String mode;

    /** oa_file | avatar | all */
    private String type;

    /** 是否试运行（true 只统计，不上传不写库） */
    private boolean dryRun;

    /** 单次处理上限 */
    private int limit;

    /** 处理前待迁移/待回滚条数 */
    private long pendingBefore;

    /** 处理后待迁移/待回滚条数 */
    private long pendingAfter;

    /** 扫描条数 */
    private long scanned;

    /** 成功迁移条数（dry-run 时为“可迁移条数”） */
    private long migrated;

    /** 跳过条数（已迁移/并发修改/非目标路径等） */
    private long skipped;

    /** 本地/远端文件缺失条数 */
    private long missing;

    /** 失败条数（异常） */
    private long failed;

    /** 传输字节数（dry-run 为预计字节数） */
    private long bytes;

    /** 耗时（毫秒） */
    private long durationMs;

    /** 失败/缺失明细（最多 20 条，避免响应过大） */
    private List<Failure> failures = new ArrayList<>();

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public void setDryRun(boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    public int getLimit()
    {
        return limit;
    }

    public void setLimit(int limit)
    {
        this.limit = limit;
    }

    public long getPendingBefore()
    {
        return pendingBefore;
    }

    public void setPendingBefore(long pendingBefore)
    {
        this.pendingBefore = pendingBefore;
    }

    public long getPendingAfter()
    {
        return pendingAfter;
    }

    public void setPendingAfter(long pendingAfter)
    {
        this.pendingAfter = pendingAfter;
    }

    public long getScanned()
    {
        return scanned;
    }

    public void setScanned(long scanned)
    {
        this.scanned = scanned;
    }

    public long getMigrated()
    {
        return migrated;
    }

    public void setMigrated(long migrated)
    {
        this.migrated = migrated;
    }

    public long getSkipped()
    {
        return skipped;
    }

    public void setSkipped(long skipped)
    {
        this.skipped = skipped;
    }

    public long getMissing()
    {
        return missing;
    }

    public void setMissing(long missing)
    {
        this.missing = missing;
    }

    public long getFailed()
    {
        return failed;
    }

    public void setFailed(long failed)
    {
        this.failed = failed;
    }

    public long getBytes()
    {
        return bytes;
    }

    public void setBytes(long bytes)
    {
        this.bytes = bytes;
    }

    public long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(long durationMs)
    {
        this.durationMs = durationMs;
    }

    public List<Failure> getFailures()
    {
        return failures;
    }

    public void setFailures(List<Failure> failures)
    {
        this.failures = failures;
    }

    /**
     * 单条失败明细
     */
    public static class Failure
    {
        private Long id;

        private String path;

        private String error;

        public Failure()
        {
        }

        public Failure(Long id, String path, String error)
        {
            this.id = id;
            this.path = path;
            this.error = error;
        }

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath(String path)
        {
            this.path = path;
        }

        public String getError()
        {
            return error;
        }

        public void setError(String error)
        {
            this.error = error;
        }
    }
}
