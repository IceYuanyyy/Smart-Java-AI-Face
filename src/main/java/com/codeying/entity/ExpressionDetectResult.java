package com.codeying.entity;

/**
 * 表情检测结果
 */
public class ExpressionDetectResult {

    /** 是否检测到人脸 */
    private boolean faceDetected;

    /** 表情标签（如 happy, sad） */
    private String expressionLabel;

    /** 表情描述（如 高兴, 悲伤） */
    private String expressionDescription;

    /** 置信度分数（0-1） */
    private float score;

    public ExpressionDetectResult() {
    }

    public ExpressionDetectResult(boolean faceDetected, String expressionLabel, String expressionDescription, float score) {
        this.faceDetected = faceDetected;
        this.expressionLabel = expressionLabel;
        this.expressionDescription = expressionDescription;
        this.score = score;
    }

    public boolean isFaceDetected() {
        return faceDetected;
    }

    public void setFaceDetected(boolean faceDetected) {
        this.faceDetected = faceDetected;
    }

    public String getExpressionLabel() {
        return expressionLabel;
    }

    public void setExpressionLabel(String expressionLabel) {
        this.expressionLabel = expressionLabel;
    }

    public String getExpressionDescription() {
        return expressionDescription;
    }

    public void setExpressionDescription(String expressionDescription) {
        this.expressionDescription = expressionDescription;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}
