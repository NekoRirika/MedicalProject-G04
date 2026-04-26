package com.xycy.chestimaging.dto.cases;

import com.xycy.chestimaging.dto.detection.DetectionResponse;
import com.xycy.chestimaging.model.MedicalCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CaseResponse {
    private Long id;
    private String caseId;
    private String patientName;
    private String patientGender;
    private String patientIdCard;
    private String patientPhone;
    private LocalDate patientBirthday;
    private String caseDescription;
    private LocalDate checkDate;
    private String checkNote;
    private String status;
    private LocalDateTime createdAt;
    private String createdBy;

    private List<DetectionResponse> detections;//新增字段 用于在病例详细界面展示已经检测完毕的结果

    public CaseResponse() {
    }

    public CaseResponse(MedicalCase caseEntity) {
        this.id = caseEntity.getId();
        this.caseId = caseEntity.getCaseId();
        this.patientName = caseEntity.getPatientName();
        this.patientGender = caseEntity.getPatientGender();
        this.patientIdCard = maskIdCard(caseEntity.getPatientIdCard());
        this.patientPhone = maskPhone(caseEntity.getPatientPhone());
        this.patientBirthday = caseEntity.getPatientBirthday();
        this.caseDescription = caseEntity.getCaseDescription();
        this.checkDate = caseEntity.getCheckDate();
        this.checkNote = caseEntity.getCheckNote();
        this.status = caseEntity.getStatus();
        this.createdAt = caseEntity.getCreatedAt();
        this.createdBy = caseEntity.getCreatedBy();
    }

    /**
     * 身份证脱敏：保留前 3 位和后 4 位，中间用*替代
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 7) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 手机号脱敏：保留前 3 位和后 4 位，中间用****替代
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getPatientIdCard() {
        return patientIdCard;
    }

    public void setPatientIdCard(String patientIdCard) {
        this.patientIdCard = patientIdCard;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public LocalDate getPatientBirthday() {
        return patientBirthday;
    }

    public void setPatientBirthday(LocalDate patientBirthday) {
        this.patientBirthday = patientBirthday;
    }

    public String getCaseDescription() {
        return caseDescription;
    }

    public void setCaseDescription(String caseDescription) {
        this.caseDescription = caseDescription;
    }

    public LocalDate getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDate checkDate) {
        this.checkDate = checkDate;
    }

    public String getCheckNote() {
        return checkNote;
    }

    public void setCheckNote(String checkNote) {
        this.checkNote = checkNote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<DetectionResponse> getDetections() {
        return detections;
    }

    public void setDetections(List<DetectionResponse> detections) {
        this.detections = detections;
    }
}