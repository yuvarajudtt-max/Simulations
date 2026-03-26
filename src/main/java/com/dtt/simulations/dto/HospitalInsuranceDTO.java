package com.dtt.simulations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HospitalInsuranceDTO {

    private int id;

    private String name;

    private String gender;



    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("policy_number")
    private String policyNumber;

    @JsonProperty("policy_name")
    private String policyName;

    @JsonProperty("policy_start_date")
    private String policyStartDate;

    @JsonProperty("policy_end_date")
    private String policyEndDate;

    @JsonProperty("policy_status")
    private String policyStatus;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyStartDate() {
        return policyStartDate;
    }

    public void setPolicyStartDate(String policyStartDate) {
        this.policyStartDate = policyStartDate;
    }

    public String getPolicyEndDate() {
        return policyEndDate;
    }

    public void setPolicyEndDate(String policyEndDate) {
        this.policyEndDate = policyEndDate;
    }

    public String getPolicyStatus() {
        return policyStatus;
    }

    public void setPolicyStatus(String policyStatus) {
        this.policyStatus = policyStatus;
    }

    @Override
    public String toString() {
        return "HospitalInsuranceDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", policyNumber='" + policyNumber + '\'' +
                ", policyName='" + policyName + '\'' +
                ", policyStartDate='" + policyStartDate + '\'' +
                ", policyEndDate='" + policyEndDate + '\'' +
                ", policyStatus='" + policyStatus + '\'' +
                '}';
    }
}
