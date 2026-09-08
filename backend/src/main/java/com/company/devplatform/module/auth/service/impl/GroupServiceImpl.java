package com.company.devplatform.module.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.dto.GroupDTO;
import com.company.devplatform.module.auth.entity.SysGroup;
import com.company.devplatform.module.auth.entity.SysGroupUser;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysGroupMapper;
import com.company.devplatform.module.auth.mapper.SysGroupUserMapper;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.auth.service.GroupService;
import com.company.devplatform.module.auth.vo.GroupDetailVO;
import com.company.devplatform.module.auth.vo.GroupVO;
import com.company.devplatform.module.auth.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户组管理服务实现
 */
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final SysGroupMapper groupMapper;
    private final SysGroupUserMapper groupUserMapper;
    private final SysUserMapper userMapper;

    @Override
    public Page<GroupVO> page(String keyword, Integer status, int page, int size) {
        Page<SysGroup> p = groupMapper.selectPage(new Page<>(page, size),
                Wrappers.<SysGroup>lambdaQuery()
                        .eq(status != null, SysGroup::getStatus, status)
                        .like(StringUtils.hasText(keyword), SysGroup::getGroupName, keyword)
                        .orderByAsc(SysGroup::getId));
        Page<GroupVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toGroupVO).toList());
        return result;
    }

    @Override
    public List<GroupVO> all() {
        List<SysGroup> groups = groupMapper.selectList(Wrappers.<SysGroup>lambdaQuery()
                .eq(SysGroup::getStatus, 1)
                .orderByAsc(SysGroup::getId));
        return groups.stream().map(this::toGroupVO).toList();
    }

    @Override
    public GroupDetailVO detail(Long id) {
        SysGroup group = getGroupOrThrow(id);
        GroupDetailVO vo = new GroupDetailVO();
        BeanUtils.copyProperties(group, vo);
        vo.setMemberIds(getMemberIds(id));
        vo.setMemberCount((long) vo.getMemberIds().size());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(GroupDTO dto) {
        checkNameUnique(dto.getGroupName(), null);
        SysGroup group = new SysGroup();
        group.setGroupName(dto.getGroupName());
        group.setRemark(dto.getRemark());
        group.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        groupMapper.insert(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(GroupDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "组ID不能为空");
        }
        SysGroup group = getGroupOrThrow(dto.getId());
        checkNameUnique(dto.getGroupName(), dto.getId());
        group.setGroupName(dto.getGroupName());
        group.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            group.setStatus(dto.getStatus());
        }
        groupMapper.updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysGroup group = getGroupOrThrow(id);
        // 解散组:解除全部成员关联(不影响用户本身与用户角色)
        groupUserMapper.delete(Wrappers.<SysGroupUser>lambdaQuery()
                .eq(SysGroupUser::getGroupId, id));
        groupMapper.deleteById(group.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMembers(Long groupId, List<Long> userIds) {
        getGroupOrThrow(groupId);
        groupUserMapper.delete(Wrappers.<SysGroupUser>lambdaQuery()
                .eq(SysGroupUser::getGroupId, groupId));
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<Long> distinctIds = userIds.stream().distinct().toList();
        if (userMapper.selectBatchIds(distinctIds).size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在无效的用户");
        }
        for (Long userId : distinctIds) {
            SysGroupUser relation = new SysGroupUser();
            relation.setGroupId(groupId);
            relation.setUserId(userId);
            groupUserMapper.insert(relation);
        }
    }

    @Override
    public List<Long> getMemberIds(Long groupId) {
        getGroupOrThrow(groupId);
        return groupUserMapper.selectList(Wrappers.<SysGroupUser>lambdaQuery()
                        .eq(SysGroupUser::getGroupId, groupId))
                .stream().map(SysGroupUser::getUserId).toList();
    }

    @Override
    public Page<MemberVO> memberPage(Long groupId, String keyword, Integer status, int page, int size) {
        getGroupOrThrow(groupId);
        List<Long> memberIds = groupUserMapper.selectList(Wrappers.<SysGroupUser>lambdaQuery()
                        .eq(SysGroupUser::getGroupId, groupId))
                .stream().map(SysGroupUser::getUserId).toList();
        Page<MemberVO> result = new Page<>(page, size, 0);
        if (memberIds.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size),
                Wrappers.<SysUser>lambdaQuery()
                        .in(SysUser::getId, memberIds)
                        .and(StringUtils.hasText(keyword),
                                w -> w.like(SysUser::getUsername, keyword)
                                        .or().like(SysUser::getNickname, keyword))
                        .eq(status != null, SysUser::getStatus, status)
                        .orderByDesc(SysUser::getCreateTime));
        result.setRecords(p.getRecords().stream().map(this::toMemberVO).toList());
        result.setTotal(p.getTotal());
        return result;
    }

    private GroupVO toGroupVO(SysGroup group) {
        GroupVO vo = new GroupVO();
        BeanUtils.copyProperties(group, vo);
        vo.setMemberCount(groupUserMapper.selectCount(Wrappers.<SysGroupUser>lambdaQuery()
                .eq(SysGroupUser::getGroupId, group.getId())));
        return vo;
    }

    private MemberVO toMemberVO(SysUser user) {
        MemberVO vo = new MemberVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    private SysGroup getGroupOrThrow(Long id) {
        SysGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "组不存在");
        }
        return group;
    }

    private void checkNameUnique(String groupName, Long excludeId) {
        long cnt = groupMapper.selectCount(Wrappers.<SysGroup>lambdaQuery()
                .eq(SysGroup::getGroupName, groupName)
                .ne(excludeId != null, SysGroup::getId, excludeId));
        if (cnt > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "组名已存在");
        }
    }
}
