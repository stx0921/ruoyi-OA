package com.ruoyi.oa.member.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.oa.apply.domain.OaFile;
import com.ruoyi.oa.apply.service.IOaFileService;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.importx.MemberImportParser;
import com.ruoyi.oa.importx.MemberImportRow;
import com.ruoyi.oa.importx.MemberImportValidator;
import com.ruoyi.oa.importx.MemberTemplateWriter;
import com.ruoyi.oa.member.domain.OaImportBatch;
import com.ruoyi.oa.member.domain.OaImportRowError;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.dto.ImportConfirmDTO;
import com.ruoyi.oa.member.mapper.OaImportBatchMapper;
import com.ruoyi.oa.member.mapper.OaImportRowErrorMapper;
import com.ruoyi.oa.member.mapper.OaMemberMapper;
import com.ruoyi.oa.member.service.IOaMemberImportService;
import com.ruoyi.oa.member.vo.ImportConfirmVO;
import com.ruoyi.oa.member.vo.ImportRowErrorVO;
import com.ruoyi.oa.member.vo.ImportRowResultVO;
import com.ruoyi.oa.member.vo.ImportValidateResultVO;
import com.ruoyi.oa.support.MemberAccountService;
import com.ruoyi.oa.support.MemberNoGenerator;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.oa.support.OaFileUrlHelper;
import com.ruoyi.oa.support.OaLockService;
import com.ruoyi.oa.support.OaMemberLogService;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 成员 Excel 导入 Service 实现（POI 4.1.2，validate/confirm 分离，confirm 分批 100 行且单行失败不回滚整批）
 *
 * @author ruoyi-oa
 */
@Service
public class OaMemberImportServiceImpl implements IOaMemberImportService
{
    private static final Logger log = LoggerFactory.getLogger(OaMemberImportServiceImpl.class);

    /** xlsx 扩展名 */
    private static final String[] XLSX_EXTENSION = { "xlsx" };

    /** 批次号 Redis 序列前缀 */
    private static final String IMPORT_SEQ_KEY = "oa:seq:import:";

    /** 结果：全部可导入 */
    private static final String RESULT_ALL_VALID = "ALL_VALID";

    /** 结果：部分可导入 */
    private static final String RESULT_PARTIAL = "PARTIAL";

    /** 结果：全部错误 */
    private static final String RESULT_ALL_INVALID = "ALL_INVALID";

    @Autowired
    private OaImportBatchMapper batchMapper;

    @Autowired
    private OaImportRowErrorMapper rowErrorMapper;

    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private MemberImportValidator importValidator;

    @Autowired
    private IOaFileService fileService;

    @Autowired
    private MemberNoGenerator memberNoGenerator;

    @Autowired
    private MemberAccountService accountService;

    @Autowired
    private OaMemberLogService memberLogService;

    @Autowired
    private OaLockService lockService;

    @Autowired
    private OaCurrentUser currentUser;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** 单次最大数据行数 */
    @Value("${oa.import.max-rows:500}")
    private int maxRows;

    /** 文件最大体积（MB） */
    @Value("${oa.import.max-file-size-mb:10}")
    private int maxFileSizeMb;

    /** 确认导入分批大小 */
    @Value("${oa.import.batch-size:100}")
    private int batchSize;

    private final MemberImportParser parser = new MemberImportParser();

    private final MemberTemplateWriter templateWriter = new MemberTemplateWriter();

    @Override
    public void downloadTemplate(HttpServletResponse response)
    {
        try
        {
            byte[] data = templateWriter.writeTemplate();
            String fileName = URLEncoder.encode("成员导入模板_v1.xlsx", "UTF-8").replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + "; filename*=utf-8''" + fileName);
            response.getOutputStream().write(data);
            response.flushBuffer();
        }
        catch (Exception e)
        {
            log.error("导入模板下载失败", e);
            throw new ServiceException("导入模板下载失败，请稍后再试", 500);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportValidateResultVO validate(MultipartFile file, boolean updateSupport)
    {
        assertFile(file);
        List<MemberImportRow> rows = parseInputStream(file);
        Long restrictDeptId = currentRestrictDeptId();
        String restrictDeptName = restrictDeptId == null ? null : getDeptName(restrictDeptId);

        List<ImportRowResultVO> results = importValidator.validate(rows, updateSupport, restrictDeptId,
                restrictDeptName);
        int newRows = 0;
        int updateRows = 0;
        int skipRows = 0;
        int errorRows = 0;
        for (ImportRowResultVO result : results)
        {
            if ("ERROR".equals(result.getResult()))
            {
                errorRows++;
            }
            else if ("UPDATE".equals(result.getResult()))
            {
                updateRows++;
            }
            else if ("SKIP".equals(result.getResult()))
            {
                skipRows++;
            }
            else
            {
                newRows++;
            }
        }
        int totalRows = results.size();
        int validRows = totalRows - errorRows;
        // success_rows 口径：可成功导入的行（预计新增 + 预计更新）
        int importableRows = newRows + updateRows;

        // 1) 落批次（状态待确认）
        String filePath = saveImportFile(file);
        OaImportBatch batch = new OaImportBatch();
        batch.setBatchNo(nextBatchNo());
        batch.setFileName(file.getOriginalFilename());
        batch.setFilePath(filePath);
        batch.setTemplateVersion(MemberImportParser.TEMPLATE_VERSION);
        batch.setTotalRows(totalRows);
        batch.setSuccessRows(importableRows);
        batch.setFailRows(errorRows);
        batch.setUpdateRows(updateRows);
        batch.setSkipRows(skipRows);
        batch.setStatus("0");
        batch.setErrorFileUrl("");
        batch.setOperatorId(currentUser.userId());
        batch.setOperatorName(currentUser.nickName());
        batch.setDeptId(currentUser.user() == null ? null : currentUser.user().getDeptId());
        batchMapper.insertBatch(batch);

        // 2) 落行级错误
        saveRowErrors(batch.getBatchId(), results);

        // 3) 生成错误明细 xlsx（仅当存在错误行）
        String errorFileUrl = "";
        if (errorRows > 0)
        {
            byte[] data = templateWriter.writeErrors(results);
            OaFile errorFile = fileService.saveBytes("导入错误明细.xlsx",
                    "导入错误明细_" + batch.getBatchNo() + ".xlsx", data, "import_error", batch.getBatchId(),
                    currentUser.userId());
            errorFileUrl = OaFileUrlHelper.url(errorFile.getFileId());
        }
        batchMapper.updateBatchValidate(batch.getBatchId(), importableRows, errorRows, updateRows, skipRows,
                errorFileUrl);

        ImportValidateResultVO vo = new ImportValidateResultVO();
        vo.setBatchId(batch.getBatchId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setTotalRows(totalRows);
        vo.setValidRows(validRows);
        vo.setInvalidRows(errorRows);
        vo.setNewRows(newRows);
        vo.setUpdateRows(updateRows);
        vo.setSkipRows(skipRows);
        vo.setErrorFileUrl(errorFileUrl);
        if (errorRows == 0)
        {
            vo.setResult(RESULT_ALL_VALID);
        }
        else if (validRows == 0)
        {
            vo.setResult(RESULT_ALL_INVALID);
        }
        else
        {
            vo.setResult(RESULT_PARTIAL);
        }
        vo.setRows(results);
        log.info("导入校验完成 batchNo={} total={} valid={} invalid={} new={} update={} skip={}", batch.getBatchNo(),
                totalRows, validRows, errorRows, newRows, updateRows, skipRows);
        return vo;
    }

    @Override
    public ImportConfirmVO confirm(ImportConfirmDTO dto)
    {
        if (dto == null || dto.getBatchId() == null)
        {
            throw new ServiceException("批次ID不能为空", 400);
        }
        OaImportBatch batch = batchMapper.selectBatchById(dto.getBatchId());
        if (batch == null)
        {
            throw new ServiceException("导入批次不存在", 404);
        }
        if (!"0".equals(batch.getStatus()))
        {
            throw new ServiceException("该批次已确认或已作废，请勿重复导入", 409);
        }
        boolean admin = currentUser.isAdmin();
        boolean president = currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT);
        if (!admin && !president && !currentUser.userId().equals(batch.getOperatorId()))
        {
            throw new ServiceException("无权确认该导入批次", 403);
        }
        boolean updateSupport = !Boolean.FALSE.equals(dto.getUpdateSupport());

        String lockKey = OaMemberConstants.LOCK_IMPORT + batch.getBatchNo();
        if (!lockService.tryLock(lockKey, 300L))
        {
            throw new ServiceException("该批次正在导入中，请稍后重试", 409);
        }
        try
        {
            List<MemberImportRow> rows = parseSavedFile(batch.getFilePath());
            Long restrictDeptId = currentRestrictDeptId();
            String restrictDeptName = restrictDeptId == null ? null : getDeptName(restrictDeptId);
            List<ImportRowResultVO> results = importValidator.validate(rows, updateSupport, restrictDeptId,
                    restrictDeptName);
            Map<Integer, MemberImportRow> rowMap = new HashMap<>();
            for (MemberImportRow row : rows)
            {
                rowMap.put(row.getRowNum(), row);
            }

            int newRows = 0;
            int updateRows = 0;
            int skipRows = 0;
            int failRows = 0;
            List<ImportRowResultVO> failedResults = new ArrayList<>();
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            int chunkSize = batchSize <= 0 ? 100 : batchSize;

            // 分批处理，每行独立事务：单行失败不回滚整批
            for (int start = 0; start < results.size(); start += chunkSize)
            {
                int end = Math.min(start + chunkSize, results.size());
                List<ImportRowResultVO> chunk = results.subList(start, end);
                for (ImportRowResultVO result : chunk)
                {
                    if ("ERROR".equals(result.getResult()))
                    {
                        failRows++;
                        failedResults.add(result);
                        continue;
                    }
                    if ("SKIP".equals(result.getResult()))
                    {
                        skipRows++;
                        continue;
                    }
                    MemberImportRow row = rowMap.get(result.getRowNum());
                    if (row == null)
                    {
                        failRows++;
                        addRuntimeError(result, "行数据缺失，请重新上传文件");
                        failedResults.add(result);
                        continue;
                    }
                    try
                    {
                        String mode = result.getResult();
                        String saved = template.execute(status -> processRow(row, mode));
                        if ("UPDATE".equals(saved))
                        {
                            updateRows++;
                        }
                        else
                        {
                            newRows++;
                        }
                    }
                    catch (ServiceException e)
                    {
                        failRows++;
                        addRuntimeError(result, e.getMessage());
                        failedResults.add(result);
                        log.warn("导入第 {} 行失败：{}", result.getRowNum(), e.getMessage());
                    }
                    catch (Exception e)
                    {
                        failRows++;
                        addRuntimeError(result, "入库失败，请稍后重试");
                        failedResults.add(result);
                        log.error("导入第 " + result.getRowNum() + " 行异常", e);
                    }
                }
            }

            // 回写行级错误与错误明细
            rowErrorMapper.deleteByBatchId(batch.getBatchId());
            String errorFileUrl = "";
            if (failRows > 0)
            {
                saveRowErrors(batch.getBatchId(), failedResults);
                byte[] data = templateWriter.writeErrors(failedResults);
                OaFile errorFile = fileService.saveBytes("导入错误明细.xlsx",
                        "导入错误明细_" + batch.getBatchNo() + ".xlsx", data, "import_error",
                        batch.getBatchId(), currentUser.userId());
                errorFileUrl = OaFileUrlHelper.url(errorFile.getFileId());
            }
            batchMapper.updateBatchConfirm(batch.getBatchId(), newRows + updateRows, failRows, updateRows, skipRows,
                    errorFileUrl);

            ImportConfirmVO vo = new ImportConfirmVO();
            vo.setBatchId(batch.getBatchId());
            vo.setBatchNo(batch.getBatchNo());
            vo.setTotalRows(batch.getTotalRows());
            // success_rows 口径：成功导入行（新增 + 更新），updateRows 为其中的更新子集
            vo.setSuccessRows(newRows + updateRows);
            vo.setUpdateRows(updateRows);
            vo.setSkipRows(skipRows);
            vo.setFailRows(failRows);
            vo.setErrorFileUrl(errorFileUrl);
            log.info("导入确认完成 batchNo={} new={} update={} skip={} fail={}", batch.getBatchNo(), newRows,
                    updateRows, skipRows, failRows);
            return vo;
        }
        finally
        {
            lockService.unlock(lockKey);
        }
    }

    // ============================ 内部方法 ============================

    /**
     * 单行入库（独立事务）：新增或更新（更新时不动 member_no/status/user_id）
     *
     * @return NEW / UPDATE
     */
    private String processRow(MemberImportRow row, String mode)
    {
        if ("UPDATE".equals(mode))
        {
            OaMember existing = memberMapper.selectByStudentNo(row.getStudentNo());
            if (existing == null)
            {
                insertNewMember(row);
                return "NEW";
            }
            Map<String, Object> before = memberLogService.snapshot(existing);
            existing.setCollege(row.getCollege().trim());
            existing.setMajor(row.getMajor().trim());
            existing.setClassName(row.getClassName().trim());
            existing.setGrade(row.getGradeValue());
            existing.setCohort(row.getCohortValue());
            existing.setDeptId(row.getDeptId());
            existing.setPosition(row.getPositionCode() == null ? OaMemberConstants.POSITION_STAFF
                    : row.getPositionCode());
            if (row.getJoinDate() != null)
            {
                existing.setJoinDate(row.getJoinDate());
            }
            existing.setUpdateBy(currentUser.username());
            memberMapper.updateMember(existing);
            memberLogService.record(existing, "编辑", before, memberLogService.snapshot(existing));
            return "UPDATE";
        }
        insertNewMember(row);
        return "NEW";
    }

    /**
     * 新增成员并开通账号（source=3 Excel 导入，免审）
     */
    private void insertNewMember(MemberImportRow row)
    {
        Date joinDate = row.getJoinDate() == null ? DateUtils.getNowDate() : row.getJoinDate();
        OaMember member = new OaMember();
        member.setMemberNo(memberNoGenerator.nextMemberNo(joinDate));
        member.setName(row.getName().trim());
        member.setGender(row.getGenderCode());
        member.setStudentNo(row.getStudentNo().trim());
        member.setPhone(row.getPhone().trim());
        member.setCollege(row.getCollege().trim());
        member.setMajor(row.getMajor().trim());
        member.setClassName(row.getClassName().trim());
        member.setGrade(row.getGradeValue());
        member.setCohort(StringUtils.isEmpty(row.getCohortValue())
                ? new java.text.SimpleDateFormat("yyyy").format(joinDate) : row.getCohortValue());
        member.setDeptId(row.getDeptId());
        member.setPosition(row.getPositionCode() == null ? OaMemberConstants.POSITION_STAFF : row.getPositionCode());
        member.setStatus(OaMemberConstants.MEMBER_STATUS_ACTIVE);
        member.setSource(OaMemberConstants.SOURCE_IMPORT);
        member.setJoinDate(joinDate);
        member.setEmail("");
        member.setWechat("");
        member.setQq("");
        member.setObeyAdjust("0");
        member.setPhotoUrl("");
        member.setCardUrl("");
        member.setRemark("Excel 导入");
        member.setCreateBy(currentUser.username());
        memberMapper.insertMember(member);

        SysUser account = accountService.ensureAccount(member);
        if (account == null)
        {
            throw new ServiceException("账号开通失败，请稍后重试", 500);
        }
        member.setUserId(account.getUserId());
        memberMapper.updateMemberUserId(member.getMemberId(), account.getUserId());
        memberLogService.record(member, "新增", null, memberLogService.snapshot(member));
    }

    /**
     * 落行级错误（oa_import_row_error）
     */
    private void saveRowErrors(Long batchId, List<ImportRowResultVO> results)
    {
        List<OaImportRowError> list = new ArrayList<>();
        if (results != null)
        {
            for (ImportRowResultVO result : results)
            {
                if (result.getErrors() == null || result.getErrors().isEmpty())
                {
                    continue;
                }
                for (ImportRowErrorVO error : result.getErrors())
                {
                    OaImportRowError rowError = new OaImportRowError();
                    rowError.setBatchId(batchId);
                    rowError.setRowNum(result.getRowNum() == null ? 0 : result.getRowNum());
                    rowError.setStudentNo(StringUtils.nvl(result.getStudentNo(), ""));
                    rowError.setName(StringUtils.nvl(result.getName(), ""));
                    rowError.setColumnName(StringUtils.nvl(error.getColumn(), ""));
                    rowError.setErrorMsg(StringUtils.nvl(error.getMsg(), ""));
                    Map<String, Object> raw = new HashMap<>(4);
                    raw.put("rowNum", result.getRowNum());
                    raw.put("studentNo", result.getStudentNo());
                    raw.put("name", result.getName());
                    raw.put("column", error.getColumn());
                    raw.put("value", error.getValue());
                    rowError.setRawJson(JSON.toJSONString(raw));
                    list.add(rowError);
                }
            }
        }
        if (!list.isEmpty())
        {
            rowErrorMapper.batchInsert(list);
        }
    }

    private void addRuntimeError(ImportRowResultVO result, String message)
    {
        if (result.getErrors() == null)
        {
            result.setErrors(new ArrayList<>());
        }
        result.getErrors().add(new ImportRowErrorVO("", StringUtils.nvl(message, "入库失败"), "",
                "请修正后重新导入该行"));
    }

    private void assertFile(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请选择要导入的 Excel 文件", 400);
        }
        String originalName = file.getOriginalFilename();
        if (StringUtils.isEmpty(originalName) || !originalName.toLowerCase().endsWith(".xlsx"))
        {
            throw new ServiceException("仅支持 .xlsx 文件，请使用模板填写后上传", 400);
        }
        long maxBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes)
        {
            throw new ServiceException("文件大小超过 " + maxFileSizeMb + "MB，请压缩后重新上传", 400);
        }
    }

    private List<MemberImportRow> parseInputStream(MultipartFile file)
    {
        try (InputStream inputStream = file.getInputStream())
        {
            return parser.parse(inputStream, maxRows);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("导入文件解析失败", e);
            throw new ServiceException("导入文件解析失败，请确认文件为 .xlsx 且未损坏", 400);
        }
    }

    private List<MemberImportRow> parseSavedFile(String filePath)
    {
        String absolutePath = toAbsolutePath(filePath);
        File savedFile = new File(absolutePath);
        if (!savedFile.isFile())
        {
            throw new ServiceException("导入文件已丢失，请重新上传校验", 400);
        }
        try (InputStream inputStream = new FileInputStream(savedFile))
        {
            return parser.parse(inputStream, maxRows);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("导入文件重新解析失败 path={}", filePath, e);
            throw new ServiceException("导入文件解析失败，请重新上传", 400);
        }
    }

    /**
     * 上传时保存原始文件（confirm 阶段重新解析）
     */
    private String saveImportFile(MultipartFile file)
    {
        try
        {
            return FileUploadUtils.upload(RuoYiConfig.getImportPath(), file, XLSX_EXTENSION, true);
        }
        catch (Exception e)
        {
            log.error("导入文件暂存失败", e);
            throw new ServiceException("导入文件暂存失败，请稍后再试", 500);
        }
    }

    private String toAbsolutePath(String filePath)
    {
        if (StringUtils.isEmpty(filePath))
        {
            return filePath;
        }
        if (filePath.startsWith(Constants.RESOURCE_PREFIX))
        {
            return RuoYiConfig.getProfile() + filePath.substring(Constants.RESOURCE_PREFIX.length());
        }
        return filePath;
    }

    /**
     * 部长仅可导入本部门（admin / 主席团不限）
     */
    private Long currentRestrictDeptId()
    {
        if (currentUser.isAdmin() || currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT))
        {
            return null;
        }
        SysUser user = currentUser.user();
        return user == null ? null : user.getDeptId();
    }

    private String getDeptName(Long deptId)
    {
        if (deptId == null)
        {
            return "";
        }
        try
        {
            SysDept dept = deptService.selectDeptById(deptId);
            return dept == null ? "" : dept.getDeptName();
        }
        catch (Exception e)
        {
            log.warn("查询部门名称失败 deptId={}", deptId, e);
            return "";
        }
    }

    /**
     * 批次号：IMP + yyyyMMddHHmmss + 3 位序号（Redis INCR，DB 兜底）
     */
    private String nextBatchNo()
    {
        String stamp = DateUtils.dateTimeNow("yyyyMMddHHmmss");
        String prefix = "IMP" + stamp;
        String key = IMPORT_SEQ_KEY + stamp;
        try
        {
            Long seq = redisCache.redisTemplate.opsForValue().increment(key);
            if (seq != null && seq == 1L)
            {
                redisCache.expire(key, 5, TimeUnit.MINUTES);
            }
            if (seq != null && seq > 0)
            {
                return prefix + String.format("%03d", seq);
            }
        }
        catch (Exception e)
        {
            log.warn("批次号 Redis 生成失败，回退数据库", e);
        }
        int next = 1;
        try
        {
            String max = batchMapper.selectMaxBatchNo(prefix);
            if (StringUtils.isNotEmpty(max) && max.length() > prefix.length())
            {
                next = Integer.parseInt(max.substring(prefix.length())) + 1;
            }
        }
        catch (Exception e)
        {
            log.warn("批次号数据库兜底失败", e);
        }
        return prefix + String.format("%03d", next);
    }

}
