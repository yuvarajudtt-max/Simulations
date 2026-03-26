package com.dtt.simulations.POA.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DocDetails {

    @JsonProperty("ownerName")
    private String ownerName;

    @JsonProperty("receps")
    private List<Receps> receps;

    @JsonProperty("tempname")
    private String tempName;

    @JsonProperty("daysToComplete")
    private String daysToComplete;

    @JsonProperty("signaturesRequiredCount")
    private int signaturesRequiredCount;

    @JsonProperty("autoReminders")
    private String autoReminders;

    @JsonProperty("remindEvery")
    private String remindEvery;

    @JsonProperty("annotations")
    private String annotations;

    @JsonProperty("noteToAll")
    private String noteToAll;

    @JsonProperty("orgn_name")
    private String orgName;

    @JsonProperty("watermark")
    private String watermark;

    @JsonProperty("expiredate")
    private String expireDate;


    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public List<Receps> getReceps() {
        return receps;
    }

    public void setReceps(List<Receps> receps) {
        this.receps = receps;
    }

    public String getTempName() {
        return tempName;
    }

    public void setTempName(String tempName) {
        this.tempName = tempName;
    }

    public String getDaysToComplete() {
        return daysToComplete;
    }

    public void setDaysToComplete(String daysToComplete) {
        this.daysToComplete = daysToComplete;
    }

    public int getSignaturesRequiredCount() {
        return signaturesRequiredCount;
    }

    public void setSignaturesRequiredCount(int signaturesRequiredCount) {
        this.signaturesRequiredCount = signaturesRequiredCount;
    }

    public String getAutoReminders() {
        return autoReminders;
    }

    public void setAutoReminders(String autoReminders) {
        this.autoReminders = autoReminders;
    }

    public String getRemindEvery() {
        return remindEvery;
    }

    public void setRemindEvery(String remindEvery) {
        this.remindEvery = remindEvery;
    }

    public String getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    public String getNoteToAll() {
        return noteToAll;
    }

    public void setNoteToAll(String noteToAll) {
        this.noteToAll = noteToAll;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    @Override
    public String toString() {
        return "DocDetails{" +
                "ownerName='" + ownerName + '\'' +
                ", receps=" + receps +
                ", tempName='" + tempName + '\'' +
                ", daysToComplete='" + daysToComplete + '\'' +
                ", signaturesRequiredCount=" + signaturesRequiredCount +
                ", autoReminders='" + autoReminders + '\'' +
                ", remindEvery='" + remindEvery + '\'' +
                ", annotations='" + annotations + '\'' +
                ", noteToAll='" + noteToAll + '\'' +
                ", orgName='" + orgName + '\'' +
                ", watermark='" + watermark + '\'' +
                ", expireDate='" + expireDate + '\'' +
                '}';
    }
}
