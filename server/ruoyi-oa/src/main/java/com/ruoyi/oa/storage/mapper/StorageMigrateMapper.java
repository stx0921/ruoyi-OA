package com.ruoyi.oa.storage.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.storage.domain.StorageRef;

/**
 * 存储迁移 Mapper：只做存量扫描与 CAS 路径更新（oa_file + sys_user）。
 *
 * <p>迁移与用户上传可并行：只处理 {@code /profile/%} 老记录，新上传写 OSS，互不干扰；
 * 跳过 {@code biz_type='import_error'}（导入错误明细属本地临时文件）。</p>
 *
 * @author ruoyi-oa
 */
@Mapper
public interface StorageMigrateMapper
{
    /** 按 file_id 游标扫描待迁移 OA 附件（/profile/% 且非 import_error） */
    List<StorageRef> selectLegacyOaFilesAfter(@Param("afterId") Long afterId, @Param("limit") int limit);

    /** 按 user_id 游标扫描待迁移头像（/profile/%） */
    List<StorageRef> selectLegacyAvatarsAfter(@Param("afterId") Long afterId, @Param("limit") int limit);

    /** 按 file_id 游标扫描已迁移 OA 附件（裸 key，回滚用） */
    List<StorageRef> selectOssOaFilesAfter(@Param("afterId") Long afterId, @Param("limit") int limit);

    /** 按 user_id 游标扫描已迁移头像（/common/attach/%） */
    List<StorageRef> selectOssAvatarsAfter(@Param("afterId") Long afterId, @Param("limit") int limit);

    /** CAS 更新 oa_file.file_path（防并发覆盖） */
    int updateOaFilePath(@Param("id") Long id, @Param("oldPath") String oldPath,
            @Param("newPath") String newPath);

    /** CAS 更新 sys_user.avatar（防并发覆盖） */
    int updateAvatarPath(@Param("id") Long id, @Param("oldPath") String oldPath,
            @Param("newPath") String newPath);

    long countLegacyOaFiles();

    long countLegacyAvatars();

    long countOssOaFiles();

    long countOssAvatars();
}
