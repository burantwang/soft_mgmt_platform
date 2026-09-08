package com.company.devplatform.module.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.auth.dto.GroupDTO;
import com.company.devplatform.module.auth.dto.GroupMembersDTO;
import com.company.devplatform.module.auth.service.GroupService;
import com.company.devplatform.module.auth.vo.GroupDetailVO;
import com.company.devplatform.module.auth.vo.GroupVO;
import com.company.devplatform.module.auth.vo.MemberVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户组管理接口
 * <p>组=组织/团队归属(如 软件研发一处/二处)，用于把人分部门/团队，方便管理；权限仍按个人角色分配。</p>
 */
@RestController
@RequestMapping("/api/system/groups")
@RequiredArgsConstructor
@SaCheckPermission("system:manage")
public class GroupController {

    private final GroupService groupService;

    /** 分页查询组 */
    @GetMapping
    public Result<Page<GroupVO>> page(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(groupService.page(keyword, status, page, Math.min(Math.max(size, 1), 100)));
    }

    /** 全部启用组(下拉选项,含人数) */
    @GetMapping("/all")
    public Result<List<GroupVO>> all() {
        return Result.ok(groupService.all());
    }

    /** 组详情(含成员ID,编辑回填用) */
    @GetMapping("/{id}")
    public Result<GroupDetailVO> detail(@PathVariable Long id) {
        return Result.ok(groupService.detail(id));
    }

    /** 新增组 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody GroupDTO dto) {
        groupService.create(dto);
        return Result.ok();
    }

    /** 编辑组 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody GroupDTO dto) {
        groupService.update(dto);
        return Result.ok();
    }

    /** 删除组(解散,解除全部成员关联,不影响用户) */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return Result.ok();
    }

    /** 分页查询组内成员 */
    @GetMapping("/{id}/members")
    public Result<Page<MemberVO>> memberPage(@PathVariable Long id,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.ok(groupService.memberPage(id, keyword, status, page, Math.min(Math.max(size, 1), 100)));
    }

    /** 查询组内已选成员ID(编辑回填用) */
    @GetMapping("/{id}/member-ids")
    public Result<List<Long>> getMemberIds(@PathVariable Long id) {
        return Result.ok(groupService.getMemberIds(id));
    }

    /** 整体替换组成员 */
    @PutMapping("/{id}/members")
    public Result<Void> saveMembers(@PathVariable Long id, @Valid @RequestBody GroupMembersDTO dto) {
        groupService.saveMembers(id, dto.getUserIds());
        return Result.ok();
    }
}
