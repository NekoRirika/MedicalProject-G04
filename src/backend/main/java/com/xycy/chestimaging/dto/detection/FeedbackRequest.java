package com.xycy.chestimaging.dto.detection;

public class FeedbackRequest {
    private String evaluation;
    private String feedback;

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}