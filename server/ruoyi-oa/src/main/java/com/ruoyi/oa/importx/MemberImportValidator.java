package com.ruoyi.oa.importx;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.service.IOaDeptService;
import com.ruoyi.oa.member.mapper.OaMemberMapper;
import com.ruoyi.oa.member.vo.DeptOptionVO;
import com.ruoyi.oa.member.vo.ImportRowErrorVO;
import com.ruoyi.oa.member.vo.ImportRowResultVO;
import com.ruoyi.system.service.ISysDictTypeService;

/**
 * 导入逐行校验器（字典/部门名内存缓存，按序收集全部错误）
 *
 * @author ruoyi-oa
 */
@Component
public class MemberImportValidator
{
    /** 姓名正则 */
    private static final String NAME_REGEX = "^[\\u4e00-\\u9fa5A-Za-z·]{2,20}$";

    /** 学号正则 */
    private static final String STUDENT_NO_REGEX = "^\\d{8,12}$";

    /** 手机号正则 */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /** 日期格式 */
    private static final String[] DATE_PATTERNS = { "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd" };

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private IOaDeptService deptService;

    /**
     * 逐行校验
     *
     * @param rows             数据行
     * @param updateSupport    true=更新已有档案，false=跳过重复行
     * @param restrictDeptId   部长限制部门ID（null=不限制）
     * @param restrictDeptName 部长限制部门名称
     * @return 行级结果
     */
    public List<ImportRowResultVO> validate(List<MemberImportRow> rows, boolean updateSupport, Long restrictDeptId,
            String restrictDeptName)
    {
        Map<String, Long> deptMap = loadDeptMap();
        DictCache gradeDict = loadDict(OaMemberConstants.DICT_GRADE);
        DictCache cohortDict = loadDict(OaMemberConstants.DICT_COHORT);
        DictCache positionDict = loadDict(OaMemberConstants.DICT_POSITION);
        String currentYear = new SimpleDateFormat("yyyy").format(new Date());
        Date today = truncateDay(new Date());

        Map<String, Integer> fileStudentRows = new HashMap<>();
        List<ImportRowResultVO> results = new ArrayList<>(rows.size());
        for (MemberImportRow row : rows)
        {
            List<ImportRowErrorVO> errors = new ArrayList<>();
            row.setErrors(errors);
            row.setResult(null);
            validateRow(row, errors, updateSupport, restrictDeptId, restrictDeptName, deptMap, gradeDict,
                    cohortDict, positionDict, currentYear, today, fileStudentRows);

            ImportRowResultVO vo = new ImportRowResultVO();
            vo.setRowNum(row.getRowNum());
            vo.setStudentNo(trim(row.getStudentNo()));
            vo.setName(trim(row.getName()));
            vo.setErrors(errors);
            vo.setResult(row.getResult());
            results.add(vo);
        }
        return results;
    }

    private void validateRow(MemberImportRow row, List<ImportRowErrorVO> errors, boolean updateSupport,
            Long restrictDeptId, String restrictDeptName, Map<String, Long> deptMap, DictCache gradeDict,
            DictCache cohortDict, DictCache positionDict, String currentYear, Date today,
            Map<String, Integer> fileStudentRows)
    {
        String name = trim(row.getName());
        String studentNo = trim(row.getStudentNo());
        String phone = trim(row.getPhone());

        // 1) 姓名
        if (StringUtils.isEmpty(name))
        {
            addError(errors, "姓名", "姓名不能为空", name, "请填写真实姓名（2–20 字）");
        }
        else if (!name.matches(NAME_REGEX))
        {
            addError(errors, "姓名", "姓名需为 2–20 个中英文字符", name, "请填写中文姓名或英文名，不含数字");
        }

        // 2) 性别
        String gender = trim(row.getGender());
        if (StringUtils.isEmpty(gender))
        {
            addError(errors, "性别", "性别不能为空", gender, "填写 男 / 女");
        }
        else if ("男".equals(gender) || "0".equals(gender))
        {
            row.setGenderCode("0");
        }
        else if ("女".equals(gender) || "1".equals(gender))
        {
            row.setGenderCode("1");
        }
        else
        {
            addError(errors, "性别", "性别只能填男或女", gender, "填写 男 / 女");
        }

        // 3) 学号
        boolean studentNoValid = false;
        if (StringUtils.isEmpty(studentNo))
        {
            addError(errors, "学号", "学号不能为空", studentNo, "请填写 8–12 位数字学号");
        }
        else if (!studentNo.matches(STUDENT_NO_REGEX))
        {
            addError(errors, "学号", "学号必须是 8–12 位数字", studentNo, "请填写 8–12 位纯数字学号");
        }
        else
        {
            studentNoValid = true;
        }

        // 4) 手机号
        boolean phoneValid = false;
        if (StringUtils.isEmpty(phone))
        {
            addError(errors, "手机号", "手机号不能为空", phone, "请填写 11 位手机号");
        }
        else if (!phone.matches(PHONE_REGEX))
        {
            addError(errors, "手机号", "手机号格式不正确", phone, "请填写 11 位手机号");
        }
        else
        {
            phoneValid = true;
        }

        // 5) 文件内学号重复
        if (studentNoValid)
        {
            Integer firstRow = fileStudentRows.get(studentNo);
            if (firstRow != null)
            {
                addError(errors, "学号", "学号在文件内重复（另见第 " + firstRow + " 行）", studentNo,
                        "请删除或修改重复行");
                studentNoValid = false;
            }
            else
            {
                fileStudentRows.put(studentNo, row.getRowNum());
            }
        }

        // 6) 学院 / 专业 / 班级
        String college = trim(row.getCollege());
        if (StringUtils.isEmpty(college))
        {
            addError(errors, "学院", "学院不能为空", college, "请填写学院名称");
        }
        else if (college.length() > 100)
        {
            addError(errors, "学院", "学院最多 100 字", college, "请精简学院名称");
        }
        String major = trim(row.getMajor());
        if (StringUtils.isEmpty(major))
        {
            addError(errors, "专业", "专业不能为空", major, "请填写专业名称");
        }
        else if (major.length() > 100)
        {
            addError(errors, "专业", "专业最多 100 字", major, "请精简专业名称");
        }
        String className = trim(row.getClassName());
        if (StringUtils.isEmpty(className))
        {
            addError(errors, "班级", "班级不能为空", className, "请填写班级");
        }
        else if (className.length() > 50)
        {
            addError(errors, "班级", "班级最多 50 字", className, "请精简班级名称");
        }

        // 7) 年级
        String grade = trim(row.getGrade());
        if (StringUtils.isEmpty(grade))
        {
            addError(errors, "年级", "年级不能为空", grade, "填写字典 oa_grade 中的标签，如 大二");
        }
        else
        {
            String gradeValue = gradeDict.resolve(grade);
            if (gradeValue == null)
            {
                addError(errors, "年级", "年级不在字典范围", grade, "填写 大一/大二/大三/大四/研一/研二/研三");
            }
            else
            {
                row.setGradeValue(gradeValue);
            }
        }

        // 8) 届次（可空，缺省当年）
        String cohort = trim(row.getCohort());
        String cohortValue;
        if (StringUtils.isEmpty(cohort))
        {
            cohortValue = currentYear;
        }
        else
        {
            cohortValue = cohortDict.resolve(cohort);
            if (cohortValue == null && cohort.matches("^\\d{4}届$"))
            {
                cohortValue = cohort.substring(0, 4);
            }
            if (cohortValue == null || !cohortDict.values.contains(cohortValue))
            {
                addError(errors, "届次", "届次不在字典范围", cohort, "填写如 2026届，或留空默认当年");
                cohortValue = null;
            }
        }
        row.setCohortValue(cohortValue);

        // 9) 部门
        String deptName = trim(row.getDeptName());
        if (StringUtils.isEmpty(deptName))
        {
            addError(errors, "部门", "部门不能为空", deptName, "填写组织根节点下的部门名称");
        }
        else
        {
            Long deptId = deptMap.get(deptName);
            if (deptId == null)
            {
                addError(errors, "部门", "部门「" + deptName + "」不存在或已停用", deptName, "填写组织根节点下的部门名称");
            }
            else
            {
                row.setDeptId(deptId);
                if (restrictDeptId != null && !restrictDeptId.equals(deptId))
                {
                    addError(errors, "部门", "无权导入到该部门", deptName,
                            "仅可导入本部门「" + StringUtils.nvl(restrictDeptName, "") + "」");
                }
            }
        }

        // 10) 职务（可空，缺省干事）
        String position = trim(row.getPosition());
        if (StringUtils.isEmpty(position))
        {
            row.setPositionCode(OaMemberConstants.POSITION_STAFF);
        }
        else
        {
            String positionCode = positionDict.resolve(position);
            if (positionCode == null)
            {
                addError(errors, "职务", "职务不在字典范围", position, "填写 会长/副会长/部长/副部长/干事，或留空默认干事");
            }
            else
            {
                row.setPositionCode(positionCode);
            }
        }

        // 11) 入会日期（可空，缺省今天，不能晚于今天）
        String joinDateText = trim(row.getJoinDateText());
        if (StringUtils.isEmpty(joinDateText))
        {
            row.setJoinDate(new Date());
        }
        else
        {
            Date joinDate = parseDate(joinDateText);
            if (joinDate == null)
            {
                addError(errors, "入会日期", "入会日期格式应为 yyyy-MM-dd", joinDateText, "使用 yyyy-MM-dd 格式，如 2026-09-01");
            }
            else if (joinDate.after(today))
            {
                addError(errors, "入会日期", "入会日期不能晚于今天", joinDateText, "请填写今天或更早的日期");
            }
            else
            {
                row.setJoinDate(joinDate);
            }
        }

        // 12) 库里唯一性校验（R9 / R14）
        if (studentNoValid && phoneValid)
        {
            OaMember byPhone = memberMapper.selectByPhone(phone);
            if (byPhone != null && !studentNo.equals(byPhone.getStudentNo()))
            {
                addError(errors, "手机号", "该手机号已关联学号 " + byPhone.getStudentNo() + "，请人工核对", phone,
                        "请核对手机号，或联系管理员解绑");
            }
            OaMember existing = memberMapper.selectByStudentNo(studentNo);
            if (existing != null)
            {
                if (!phone.equals(existing.getPhone()))
                {
                    addError(errors, "学号", "学号 " + studentNo + " 与已存在档案不匹配（R14）", studentNo,
                            "请人工核对学号与手机号，避免误改账号密码来源");
                }
                else if (!updateSupport)
                {
                    // 学号命中且手机号相同：跳过重复行（再无其它错误时不阻断整批）
                    row.setResult(errors.isEmpty() ? "SKIP" : "ERROR");
                    return;
                }
                else
                {
                    row.setResult("UPDATE");
                }
            }
            else
            {
                row.setResult("NEW");
            }
        }

        if (!errors.isEmpty())
        {
            row.setResult("ERROR");
        }
        else if (row.getResult() == null)
        {
            row.setResult("NEW");
        }
    }

    private Map<String, Long> loadDeptMap()
    {
        Map<String, Long> deptMap = new HashMap<>();
        try
        {
            List<DeptOptionVO> options = deptService.selectAllDeptOptions();
            if (options != null)
            {
                for (DeptOptionVO option : options)
                {
                    deptMap.put(StringUtils.trim(option.getDeptName()), option.getDeptId());
                }
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("查询组织部门失败，请稍后再试", 500);
        }
        return deptMap;
    }

    private DictCache loadDict(String dictType)
    {
        DictCache cache = new DictCache();
        try
        {
            List<SysDictData> dataList = dictTypeService.selectDictDataByType(dictType);
            if (dataList != null)
            {
                for (SysDictData data : dataList)
                {
                    cache.labels.put(StringUtils.trim(data.getDictLabel()), data.getDictValue());
                    cache.values.add(data.getDictValue());
                }
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("字典 " + dictType + " 加载失败，请确认脚本已执行", 500);
        }
        return cache;
    }

    private void addError(List<ImportRowErrorVO> errors, String column, String msg, String value,
            String suggestion)
    {
        errors.add(new ImportRowErrorVO(column, msg, value == null ? "" : value, suggestion));
    }

    private Date parseDate(String text)
    {
        for (String pattern : DATE_PATTERNS)
        {
            try
            {
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false);
                return format.parse(text);
            }
            catch (Exception ignored)
            {
                // 尝试下一个格式
            }
        }
        return null;
    }

    private Date truncateDay(Date date)
    {
        try
        {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            return format.parse(format.format(date));
        }
        catch (Exception e)
        {
            return DateUtils.getNowDate();
        }
    }

    private String trim(String value)
    {
        return value == null ? "" : value.trim();
    }

    /**
     * 字典缓存（标签→值 + 值集合，允许直接填字典值）
     */
    private static class DictCache
    {
        private final Map<String, String> labels = new HashMap<>();

        private final Set<String> values = new HashSet<>();

        private String resolve(String text)
        {
            if (text == null)
            {
                return null;
            }
            String label = labels.get(text);
            if (label != null)
            {
                return label;
            }
            return values.contains(text) ? text : null;
        }
    }

}
