package com.ruoyi.oa.constant;

/**
 * 人员录入模块常量
 *
 * @author ruoyi-oa
 */
public final class OaMemberConstants
{
    private OaMemberConstants()
    {
    }

    /** 成员状态：在籍 */
    public static final String MEMBER_STATUS_ACTIVE = "0";

    /** 成员状态：已退会 */
    public static final String MEMBER_STATUS_LEFT = "1";

    /** 成员状态：已毕业 */
    public static final String MEMBER_STATUS_GRADUATED = "2";

    /** 成员来源：自助申请 */
    public static final String SOURCE_APPLY = "1";

    /** 成员来源：手动录入 */
    public static final String SOURCE_MANUAL = "2";

    /** 成员来源：Excel 导入 */
    public static final String SOURCE_IMPORT = "3";

    /** 申请状态：待审批 */
    public static final String APPLY_STATUS_PENDING = "0";

    /** 申请状态：已通过 */
    public static final String APPLY_STATUS_APPROVED = "1";

    /** 申请状态：已驳回 */
    public static final String APPLY_STATUS_REJECTED = "2";

    /** 申请状态：已撤回 */
    public static final String APPLY_STATUS_WITHDRAWN = "3";

    /** 申请状态：已关闭 */
    public static final String APPLY_STATUS_CLOSED = "4";

    /** 角色：主席团 */
    public static final String ROLE_PRESIDENT = "oa_president";

    /** 角色：部长 */
    public static final String ROLE_MINISTER = "oa_minister";

    /** 角色：干事 */
    public static final String ROLE_STAFF = "oa_staff";

    /** 职务：会长 */
    public static final String POSITION_PRESIDENT = "1";

    /** 职务：副会长 */
    public static final String POSITION_VICE_PRESIDENT = "2";

    /** 职务：部长 */
    public static final String POSITION_MINISTER = "3";

    /** 职务：副部长 */
    public static final String POSITION_VICE_MINISTER = "4";

    /** 职务：干事 */
    public static final String POSITION_STAFF = "5";

    /** 字典：年级 */
    public static final String DICT_GRADE = "oa_grade";

    /** 字典：届次 */
    public static final String DICT_COHORT = "oa_cohort";

    /** 字典：职务 */
    public static final String DICT_POSITION = "oa_position";

    /** 字典：成员状态 */
    public static final String DICT_MEMBER_STATUS = "oa_member_status";

    /** 字典：申请状态 */
    public static final String DICT_APPLY_STATUS = "oa_apply_status";

    /** 字典：成员来源 */
    public static final String DICT_SOURCE = "oa_member_source";

    /** 图形验证码 Redis 前缀 */
    public static final String CAPTCHA_KEY = "oa:captcha:";

    /** 校验令牌 Redis 前缀（H5 上传/提交） */
    public static final String VERIFY_TOKEN_KEY = "oa:verify:token:";

    /** 进度查询令牌 Redis 前缀 */
    public static final String QUERY_TOKEN_KEY = "oa:query:token:";

    /** 验证码失败计数 Redis 前缀 */
    public static final String CAPTCHA_FAIL_KEY = "oa:captcha:fail:";

    /** 提交频控 Redis 前缀 */
    public static final String SUBMIT_LIMIT_KEY = "oa:limit:submit:24h:";

    /** 成员编号序号 Redis 前缀 */
    public static final String MEMBER_SEQ_KEY = "oa:seq:member:";

    /** 申请编号序号 Redis 前缀 */
    public static final String APPLY_SEQ_KEY = "oa:seq:apply:";

    /** 业务锁前缀 */
    public static final String LOCK_APPLY_SUBMIT = "oa:lock:apply:submit:";

    /** 业务锁前缀：审批 */
    public static final String LOCK_APPLY_AUDIT = "oa:lock:apply:audit:";

    /** 业务锁前缀：成员创建 */
    public static final String LOCK_MEMBER_CREATE = "oa:lock:member:create:";

    /** 业务锁前缀：成员调部门 */
    public static final String LOCK_MEMBER_TRANSFER = "oa:lock:member:transfer:";

    /** 业务锁前缀：导入确认 */
    public static final String LOCK_IMPORT = "oa:lock:import:";

    /** 组织根节点名称 */
    public static final String ORG_ROOT_NAME = "校学生会";
}
