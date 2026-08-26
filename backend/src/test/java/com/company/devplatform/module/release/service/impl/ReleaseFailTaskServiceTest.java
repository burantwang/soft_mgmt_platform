package com.company.devplatform.module.release.service.impl;

import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.release.dto.FailCaseHandleDTO;
import com.company.devplatform.module.release.dto.FailTaskAssignDTO;
import com.company.devplatform.module.release.dto.FailTaskCreateDTO;
import com.company.devplatform.module.release.dto.FailTaskStatusDTO;
import com.company.devplatform.module.release.entity.ReleaseFailCase;
import com.company.devplatform.module.release.entity.ReleaseFailTask;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import com.company.devplatform.module.release.enums.FailCaseStatus;
import com.company.devplatform.module.release.enums.FailTaskStatus;
import com.company.devplatform.module.release.mapper.ReleaseFailCaseMapper;
import com.company.devplatform.module.release.mapper.ReleaseFailTaskMapper;
import com.company.devplatform.module.release.mapper.ReleaseProjectMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordProjectMapper;
import com.company.devplatform.module.release.util.HtmlReportParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 失败聚合任务核心服务单元测试（创建/指派/流转/联动/越权）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReleaseFailTaskServiceTest {

    @Mock
    private ReleaseFailTaskMapper taskMapper;
    @Mock
    private ReleaseFailCaseMapper caseMapper;
    @Mock
    private ReleaseRecordMapper recordMapper;
    @Mock
    private ReleaseRecordProjectMapper recordProjectMapper;
    @Mock
    private ReleaseProjectMapper projectMapper;
    @Mock
    private SysUserMapper userMapper;

    @InjectMocks
    private ReleaseFailTaskServiceImpl service;

    /** 创建 spy：固定当前用户与超管标记（避免依赖 Sa-Token 上下文） */
    private ReleaseFailTaskServiceImpl spyAs(long userId) {
        ReleaseFailTaskServiceImpl spy = spy(service);
        doReturn(userId).when(spy).currentUserId();
        doReturn(false).when(spy).isSuperAdmin();
        return spy;
    }

    /* ==================== 创建 ==================== */

    @Test
    void createTaskForRecord_shouldCreateTaskWithCasesAndPendingStatus() {
        ReleaseRecord record = new ReleaseRecord();
        record.setId(10L);
        record.setBranch("UXOS_23");
        record.setVersion("v1.0.0");
        record.setFailedCount(2);
        record.setErrorCount(0);
        record.setPublisherId(5L);

        HtmlReportParser.FailCase fc1 = new HtmlReportParser.FailCase();
        fc1.setName("用例A");
        fc1.setLog("断言失败");
        HtmlReportParser.FailCase fc2 = new HtmlReportParser.FailCase();
        fc2.setName("用例B");
        fc2.setLog("超时");

        doAnswer(inv -> {
            ReleaseFailTask t = inv.getArgument(0);
            t.setId(1L);
            return 1;
        }).when(taskMapper).insert(any(ReleaseFailTask.class));
        doAnswer(inv -> {
            ReleaseFailCase c = inv.getArgument(0);
            c.setId(100L);
            return 1;
        }).when(caseMapper).insert(any(ReleaseFailCase.class));
        when(taskMapper.selectLatestTaskNo(any())).thenReturn(null);

        ReleaseFailTask task = service.createTaskForRecord(record, Arrays.asList(fc1, fc2));

        assertNotNull(task.getId());
        assertTrue(task.getTaskNo().startsWith("FT"));
        assertEquals(FailTaskStatus.PENDING.getCode(), task.getStatus());
        assertEquals(record.getId(), task.getRecordId());
        assertEquals(record.getPublisherId(), task.getCreatorId());
        verify(caseMapper, times(2)).insert(any(ReleaseFailCase.class));
    }

    @Test
    void genTaskNo_shouldIncrementLatestSeq() {
        String prefix = "FT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(taskMapper.selectLatestTaskNo(prefix)).thenReturn(prefix + "002");
        assertEquals(prefix + "003", service.genTaskNo());
    }

    @Test
    void create_manual_withRecordAlreadyLinked_shouldThrow() {
        ReleaseRecord record = new ReleaseRecord();
        record.setId(10L);
        record.setResult(2); // 失败状态

        when(recordMapper.selectById(10L)).thenReturn(record);
        when(taskMapper.selectOne(any())).thenReturn(new ReleaseFailTask()); // 已存在关联任务

        ReleaseFailTaskServiceImpl spy = spyAs(5L);
        FailTaskCreateDTO dto = new FailTaskCreateDTO();
        dto.setRecordId(10L);
        dto.setSummary("失败任务");

        BusinessException ex = assertThrows(BusinessException.class, () -> spy.create(dto));
        assertEquals(ErrorCode.DATA_EXIST.getCode(), ex.getCode());
    }

    @Test
    void create_manual_success_shouldInsertTaskAndCases() {
        doAnswer(inv -> {
            ReleaseFailTask t = inv.getArgument(0);
            t.setId(1L);
            return 1;
        }).when(taskMapper).insert(any(ReleaseFailTask.class));
        doAnswer(inv -> 1).when(caseMapper).insert(any(ReleaseFailCase.class));
        when(taskMapper.selectLatestTaskNo(any())).thenReturn(null);

        ReleaseFailTaskServiceImpl spy = spyAs(5L);
        FailTaskCreateDTO dto = new FailTaskCreateDTO();
        dto.setSummary("手动任务");
        dto.setFailReason("原因");
        FailTaskCreateDTO.FailCaseItemDTO item = new FailTaskCreateDTO.FailCaseItemDTO();
        item.setCaseName("用例1");
        dto.setCases(Collections.singletonList(item));

        ReleaseFailTask task = spy.create(dto);

        assertNotNull(task.getId());
        assertEquals(FailTaskStatus.PENDING.getCode(), task.getStatus());
        assertEquals(5L, task.getCreatorId());
        verify(caseMapper, times(1)).insert(any(ReleaseFailCase.class));
    }

    /* ==================== 指派 ==================== */

    @Test
    void assign_shouldUpdateAssigneeAndInheritToPendingCases() {
        ReleaseFailTask task = newTask(1L, 5L, null, FailTaskStatus.PENDING.getCode());
        ReleaseFailCase pending = newCase(11L, 1L, FailCaseStatus.PENDING.getCode(), null);
        ReleaseFailCase fixed = newCase(12L, 1L, FailCaseStatus.FIXED.getCode(), null);

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(caseMapper.selectList(any())).thenReturn(Arrays.asList(pending, fixed));
        when(userMapper.selectById(99L)).thenReturn(new com.company.devplatform.module.auth.entity.SysUser());

        ReleaseFailTaskServiceImpl spy = spyAs(5L);
        FailTaskAssignDTO dto = new FailTaskAssignDTO();
        dto.setId(1L);
        dto.setAssigneeId(99L);
        spy.assign(dto);

        assertEquals(99L, task.getAssigneeId());
        assertEquals(99L, pending.getAssigneeId());
        assertNull(fixed.getAssigneeId());
        verify(taskMapper).updateById(task);
        verify(caseMapper).updateById(pending);
        verify(caseMapper, never()).updateById(fixed);
    }

    @Test
    void assign_byNonCreator_shouldThrowNoPermission() {
        ReleaseFailTask task = newTask(1L, 5L, null, FailTaskStatus.PENDING.getCode());
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReleaseFailTaskServiceImpl spy = spyAs(99L); // 非创建人
        FailTaskAssignDTO dto = new FailTaskAssignDTO();
        dto.setId(1L);
        dto.setAssigneeId(99L);

        BusinessException ex = assertThrows(BusinessException.class, () -> spy.assign(dto));
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    void assign_onClosedTask_shouldThrow() {
        ReleaseFailTask task = newTask(1L, 5L, null, FailTaskStatus.CLOSED.getCode());
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReleaseFailTaskServiceImpl spy = spyAs(5L);
        FailTaskAssignDTO dto = new FailTaskAssignDTO();
        dto.setId(1L);
        dto.setAssigneeId(99L);

        assertThrows(BusinessException.class, () -> spy.assign(dto));
    }

    /* ==================== 处理用例 + 状态联动 ==================== */

    @Test
    void handleCase_whenAllCasesDone_shouldCompleteTask() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.PENDING.getCode());
        ReleaseFailCase c = newCase(11L, 1L, FailCaseStatus.PENDING.getCode(), 9L);

        when(caseMapper.selectById(11L)).thenReturn(c);
        when(taskMapper.selectById(1L)).thenReturn(task);
        // handleCase 与 refreshTaskStatus 的 selectList 返回同一对象引用，状态修改后联动可见
        when(caseMapper.selectList(any())).thenReturn(Collections.singletonList(c));

        ReleaseFailTaskServiceImpl spy = spyAs(9L);
        FailCaseHandleDTO dto = new FailCaseHandleDTO();
        dto.setCaseId(11L);
        dto.setStatus(FailCaseStatus.FIXED.getCode());
        dto.setFailReason("定位到原因");
        dto.setFixPlan("修改方案");
        spy.handleCase(dto);

        assertEquals(FailCaseStatus.FIXED.getCode(), c.getStatus());
        assertNotNull(c.getHandleTime());
        assertEquals(9L, c.getAssigneeId());
        verify(taskMapper).updateById(argThat((ReleaseFailTask t) ->
                FailTaskStatus.COMPLETED.getCode() == t.getStatus() && t.getHandleTime() != null));
    }

    @Test
    void handleCase_partial_shouldSetTaskProcessing() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.PENDING.getCode());
        ReleaseFailCase c1 = newCase(11L, 1L, FailCaseStatus.PENDING.getCode(), 9L);
        ReleaseFailCase c2 = newCase(12L, 1L, FailCaseStatus.PENDING.getCode(), 9L);

        when(caseMapper.selectById(11L)).thenReturn(c1);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(caseMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2));

        ReleaseFailTaskServiceImpl spy = spyAs(9L);
        FailCaseHandleDTO dto = new FailCaseHandleDTO();
        dto.setCaseId(11L);
        dto.setStatus(FailCaseStatus.PROCESSING.getCode());
        spy.handleCase(dto);

        assertEquals(FailCaseStatus.PROCESSING.getCode(), c1.getStatus());
        verify(taskMapper).updateById(argThat((ReleaseFailTask t) ->
                FailTaskStatus.PROCESSING.getCode() == t.getStatus()));
    }

    @Test
    void handleCase_byNonAssignee_shouldThrowNoPermission() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.PENDING.getCode());
        ReleaseFailCase c = newCase(11L, 1L, FailCaseStatus.PENDING.getCode(), 9L);

        when(caseMapper.selectById(11L)).thenReturn(c);
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReleaseFailTaskServiceImpl spy = spyAs(88L); // 非责任人
        FailCaseHandleDTO dto = new FailCaseHandleDTO();
        dto.setCaseId(11L);
        dto.setStatus(FailCaseStatus.FIXED.getCode());
        dto.setFixPlan("方案");

        BusinessException ex = assertThrows(BusinessException.class, () -> spy.handleCase(dto));
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    void handleCase_completeWithoutReason_shouldThrow() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.PROCESSING.getCode());
        ReleaseFailCase c = newCase(11L, 1L, FailCaseStatus.PROCESSING.getCode(), 9L);

        when(caseMapper.selectById(11L)).thenReturn(c);
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReleaseFailTaskServiceImpl spy = spyAs(9L);
        FailCaseHandleDTO dto = new FailCaseHandleDTO();
        dto.setCaseId(11L);
        dto.setStatus(FailCaseStatus.FIXED.getCode());
        // 未填写原因/方案

        BusinessException ex = assertThrows(BusinessException.class, () -> spy.handleCase(dto));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void handleCase_finishedCaseBackToPending_shouldThrow() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.PROCESSING.getCode());
        ReleaseFailCase c = newCase(11L, 1L, FailCaseStatus.FIXED.getCode(), 9L);

        when(caseMapper.selectById(11L)).thenReturn(c);
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReleaseFailTaskServiceImpl spy = spyAs(9L);
        FailCaseHandleDTO dto = new FailCaseHandleDTO();
        dto.setCaseId(11L);
        dto.setStatus(FailCaseStatus.PENDING.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () -> spy.handleCase(dto));
        assertEquals(ErrorCode.BUSINESS_ERROR.getCode(), ex.getCode());
    }

    /* ==================== 流转 ==================== */

    @Test
    void changeStatus_completeWithPendingCase_shouldThrow() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.PROCESSING.getCode());
        ReleaseFailCase pending = newCase(11L, 1L, FailCaseStatus.PENDING.getCode(), 9L);

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(caseMapper.selectList(any())).thenReturn(Collections.singletonList(pending));

        ReleaseFailTaskServiceImpl spy = spyAs(9L);
        FailTaskStatusDTO dto = new FailTaskStatusDTO();
        dto.setId(1L);
        dto.setStatus(FailTaskStatus.COMPLETED.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () -> spy.changeStatus(dto));
        assertEquals(ErrorCode.BUSINESS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void changeStatus_completeWhenAllCasesDone_shouldSucceed() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.PROCESSING.getCode());
        ReleaseFailCase done = newCase(11L, 1L, FailCaseStatus.FIXED.getCode(), 9L);

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(caseMapper.selectList(any())).thenReturn(Collections.singletonList(done));

        ReleaseFailTaskServiceImpl spy = spyAs(9L);
        FailTaskStatusDTO dto = new FailTaskStatusDTO();
        dto.setId(1L);
        dto.setStatus(FailTaskStatus.COMPLETED.getCode());
        spy.changeStatus(dto);

        assertEquals(FailTaskStatus.COMPLETED.getCode(), task.getStatus());
        assertNotNull(task.getHandleTime());
        verify(taskMapper).updateById(task);
    }

    @Test
    void changeStatus_onClosedTask_shouldThrow() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.CLOSED.getCode());
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReleaseFailTaskServiceImpl spy = spyAs(9L);
        FailTaskStatusDTO dto = new FailTaskStatusDTO();
        dto.setId(1L);
        dto.setStatus(FailTaskStatus.PROCESSING.getCode());

        assertThrows(BusinessException.class, () -> spy.changeStatus(dto));
    }

    @Test
    void changeStatus_byNonAssignee_shouldThrowNoPermission() {
        ReleaseFailTask task = newTask(1L, 5L, 9L, FailTaskStatus.PENDING.getCode());
        when(taskMapper.selectById(1L)).thenReturn(task);

        ReleaseFailTaskServiceImpl spy = spyAs(88L); // 非创建人非被指派人
        FailTaskStatusDTO dto = new FailTaskStatusDTO();
        dto.setId(1L);
        dto.setStatus(FailTaskStatus.PROCESSING.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () -> spy.changeStatus(dto));
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
    }

    /* ==================== 工具方法 ==================== */

    private ReleaseFailTask newTask(Long id, Long creatorId, Long assigneeId, Integer status) {
        ReleaseFailTask task = new ReleaseFailTask();
        task.setId(id);
        task.setTaskNo("FT20260826001");
        task.setRecordId(10L);
        task.setCreatorId(creatorId);
        task.setAssigneeId(assigneeId);
        task.setStatus(status);
        task.setSummary("测试任务");
        return task;
    }

    private ReleaseFailCase newCase(Long id, Long taskId, Integer status, Long assigneeId) {
        ReleaseFailCase c = new ReleaseFailCase();
        c.setId(id);
        c.setTaskId(taskId);
        c.setCaseName("用例" + id);
        c.setStatus(status);
        c.setAssigneeId(assigneeId);
        return c;
    }
}
