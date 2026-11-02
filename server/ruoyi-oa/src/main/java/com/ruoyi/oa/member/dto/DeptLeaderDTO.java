package com.ruoyi.oa.member.dto;

import java.util.List;

/**
 * 部门负责人保存参数
 *
 * @author ruoyi-oa
 */
public class DeptLeaderDTO
{
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    private Long deptId;

    /** 负责人列表 */
    private List<LeaderItem> leaders;

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public List<LeaderItem> getLeaders()
    {
        return leaders;
    }

    public void setLeaders(List<LeaderItem> leaders)
    {
        this.leaders = leaders;
    }

    /**
     * 负责人项
     */
    public static class LeaderItem
    {
        /** 负责人账号ID */
        private Long userId;

        /** 负责人类型（1正部长 2副部长） */
        private String leaderType;

        /** 展示顺序 */
        private Integer sort;

        public Long getUserId()
        {
            return userId;
        }

        public void setUserId(Long userId)
        {
            this.userId = userId;
        }

        public String getLeaderType()
        {
            return leaderType;
        }

        public void setLeaderType(String leaderType)
        {
            this.leaderType = leaderType;
        }

        public Integer getSort()
        {
            return sort;
        }

        public void setSort(Integer sort)
        {
            this.sort = sort;
        }
    }

}
