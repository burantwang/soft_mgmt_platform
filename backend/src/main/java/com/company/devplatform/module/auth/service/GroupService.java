package com.company.devplatform.module.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.module.auth.dto.GroupDTO;
import com.company.devplatform.module.auth.vo.GroupDetailVO;
import com.company.devplatform.module.auth.vo.GroupVO;
import com.company.devplatform.module.auth.vo.MemberVO;

import java.util.List;

/**
 * 用户组管理服务接口
 * <p>组=组织/团队归属(如 软件研发一处/二处)，与角色权限解耦；一个人可加入多个组。</p>
 */
public interface GroupService {

    /** 分页查询组 */
    Page<GroupVO> page(String keyword, Integer status, int page, int size);

    /** 全部启用组(带人数,下拉用) */
    List<GroupVO> all();

    /** 组详情(含成员ID,用于编辑回填) */
    GroupDetailVO detail(Long id);

    /** 新增组 */
    void create(GroupDTO dto);

    /** 编辑组 */
    void update(GroupDTO dto);

    /** 删除组(解散:同时解除成员关联,不影响用户本身) */
    void delete(Long id);

    /** 整体替换组成员 */
    void saveMembers(Long groupId, List<Long> userIds);

    /** 查询组内已选成员ID */
    List<Long> getMemberIds(Long groupId);

    /** 分页查询组内成员(用户基础信息) */
    Page<MemberVO> memberPage(Long groupId, String keyword, Integer status, int page, int size);
}
