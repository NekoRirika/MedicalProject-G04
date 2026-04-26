package com.xycy.chestimaging.dto.detection;

import java.time.LocalDateTime;

public class FeedbackListItem {
    private Long id;                        // 反馈ID（主键）
    private String caseNo;                  // 病例号（格式: CASE-{caseId}）
    private String patientName;             // 患者姓名
    private LocalDateTime detectTime;       // 检测时间
    private String evaluation;              // 反馈评价内容
    private LocalDateTime feedbackTime;     // 反馈时间
    private String operator;                // 操作者（谁提交的反馈）
    private String feedback;                // 反馈备注

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public void setCaseNo(String caseNo) {
        this.caseNo = caseNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public LocalDateTime getDetectTime() {
        return detectTime;
    }

    public void setDetectTime(LocalDateTime detectTime) {
        this.detectTime = detectTime;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public LocalDateTime getFeedbackTime() {
        return feedbackTime;
    }

    public void setFeedbackTime(LocalDateTime feedbackTime) {
        this.feedbackTime = feedbackTime;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
