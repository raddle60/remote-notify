package com.raddle.notify.remote.bean;

import java.awt.Color;

public class PositionColor {
    private boolean equal = false;
    private String postion;
    private String color;
    private Color pointColor;
    private Color curColor;
    private int maxNotMatchedTimes = 3;
    private int notMatchedTimes = 0;

    public boolean isEqual() {
        return equal;
    }

    public void setEqual(boolean equal) {
        this.equal = equal;
    }

    public String getPostion() {
        return postion;
    }

    public void setPostion(String postion) {
        this.postion = postion;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Color getCurColor() {
        return curColor;
    }

    public void setCurColor(Color curColor) {
        this.curColor = curColor;
    }

    public Color getPointColor() {
        return pointColor;
    }

    public void setPointColor(Color pointColor) {
        this.pointColor = pointColor;
    }

    public int getMaxNotMatchedTimes() {
        return maxNotMatchedTimes;
    }

    public void setMaxNotMatchedTimes(int maxNotMatchedTimes) {
        this.maxNotMatchedTimes = maxNotMatchedTimes;
    }

    public int getNotMatchedTimes() {
        return notMatchedTimes;
    }

    public void setNotMatchedTimes(int notMatchedTimes) {
        this.notMatchedTimes = notMatchedTimes;
    }
}
