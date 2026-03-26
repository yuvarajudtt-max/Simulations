package com.dtt.simulations.POA.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Coordinates {

    private String fieldName;

    private double posX;

    private double posY;

    @JsonProperty("PageNumber")
    private int pageNumber;

    private int width;

    private int height;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "Coordinates{" +
                "fieldName='" + fieldName + '\'' +
                ", posX=" + posX +
                ", posY=" + posY +
                ", pageNumber=" + pageNumber +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
