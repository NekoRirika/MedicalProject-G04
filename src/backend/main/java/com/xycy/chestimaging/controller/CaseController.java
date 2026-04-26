package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.annotation.AuditLog;
import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.dto.cases.CaseCreateRequest;
import com.xycy.chestimaging.dto.cases.CaseUpdateRequest;
import com.xycy.chestimaging.exception.AccessDeniedException;
import com.xycy.chestimaging.exception.BusinessException;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.CaseService;
import com.xycy.chestimaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/cases")
public class CaseController {
    @Autowired
    private CaseService caseService;
    @Autowired
    private UserService userService;

    private void checkDoctorRole() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user.getRole() != User.Role.doctor) {
            throw new AccessDeniedException("权限不足，只有医生可以操作");
        }
    }

    private void checkDoctorResearcherRole(){//检查权限：医生+科研人员
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user.getRole() != User.Role.researcher&&user.getRole() != User.Role.doctor) {
            throw new AccessDeniedException("权限不足，只有医生和科研人员可以操作");
        }
    }

    @GetMapping
    @AuditLog(operationType = "QUERY_CASE", operationContent = "查询病例列表")
    public Response<?> getCases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int page_size,
            @RequestParam(required = false) String case_id,
            @RequestParam(required = false) String patient_name) {
        checkDoctorResearcherRole();
        return Response.success("查询成功", caseService.getCases(page, page_size, case_id, patient_name));
    }

    @PostMapping
    @AuditLog(operationType = "CREATE_CASE", operationContent = "创建病例")
    public Response<?> createCase(@RequestBody CaseCreateRequest request) {
        checkDoctorRole();
        validateCaseCreateRequest(request);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Response.success("创建成功", caseService.createCase(request, username));
    }

    private void validateCaseCreateRequest(CaseCreateRequest request) {
        if (request.getPatientIdCard() != null && !request.getPatientIdCard().trim().isEmpty()) {
            String idCard = request.getPatientIdCard().trim();
            Pattern idCardPattern = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
            if (!idCardPattern.matcher(idCard).matches()) {
                throw new BusinessException("身份证号码格式不正确，请输入18位有效身份证号码");
            }
        }

        if (request.getPatientPhone() != null && !request.getPatientPhone().trim().isEmpty()) {
            String phone = request.getPatientPhone().trim();
            Pattern phonePattern = Pattern.compile("^1[3-9]\\d{9}$");
            if (!phonePattern.matcher(phone).matches()) {
                throw new BusinessException("手机号码格式不正确，请输入11位有效手机号码");
            }
        }

        if (request.getPatientBirthday() != null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (request.getPatientBirthday().after(cal.getTime())) {
                throw new BusinessException("出生日期不能晚于今天");
            }
        }
    }

    @GetMapping("/{id}")
    @AuditLog(operationType = "QUERY_CASE", operationContent = "查询病例详情")
    public Response<?> getCaseById(@PathVariable Long id) {
        checkDoctorResearcherRole();
        return Response.success("查询成功", caseService.getCaseById(id));
    }

    @PutMapping("/{id}")
    @AuditLog(operationType = "UPDATE_CASE", operationContent = "更新病例信息")
    public Response<?> updateCase(@PathVariable Long id, @RequestBody CaseUpdateRequest request) {
        checkDoctorRole();
        return Response.success("编辑成功", caseService.updateCase(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditLog(operationType = "DELETE_CASE", operationContent = "删除病例")
    public Response<?> deleteCase(@PathVariable Long id) {
        checkDoctorRole();
        caseService.deleteCase(id);
        return Response.success("删除成功");
    }
}