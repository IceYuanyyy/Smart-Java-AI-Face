package com.codeying.entity;

/**
 * 活體檢測結果
 */
public class LivenessDetectResult {

    /** 是否為真人（活體） */
    private boolean live;

    /** 活體分數（0-1，越高越可能是真人） */
    private float score;

    /** 狀態描述（如：真人、非活體等） */
    private String statusDescription;

    /** 是否檢測到人臉 */
    private boolean faceDetected;

    public LivenessDetectResult() {
    }

    public LivenessDetectResult(boolean live, float score, String statusDescription, boolean faceDetected) {
        this.live = live;
        this.score = score;
        this.statusDescription = statusDescription;
        this.faceDetected = faceDetected;
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public boolean isFaceDetected() {
        return faceDetected;
    }

    public void setFaceDetected(boolean faceDetected) {
        this.faceDetected = faceDetected;
    }
}
