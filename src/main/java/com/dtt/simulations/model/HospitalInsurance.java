package com.dtt.simulations.model;


import jakarta.persistence.*;

@Entity
@Table(name = "health_care_registration")
public class HospitalInsurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "insured_name")
    private String name;

    @Column(name = "gender")
    private String gender;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "policy_number")
    private String policyNumber;

    @Column(name = "policy_name")
    private String policyName;

    @Column(name = "photo")
    private String photo;

    @Column(name = "policy_start_date")
    private String policyStartDate;

    @Column(name = "policy_end_date")
    private String policyEndDate;

    @Column(name = "policy_status")
    private String policyStatus;

    @Column(name = "json_data")
    private String jsonData;

    @Column(name = "created_on")
    private String createdOn;

    @Column(name = "updated_on")
    private String updatedOn;

    @Column(name = "policy_document")
    private String policyDocument;

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

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
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

    public String getJsonData() {
        return jsonData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    public String getPolicyDocument() {
        return policyDocument;
    }

    public void setPolicyDocument(String policyDocument) {
        this.policyDocument = policyDocument;
    }

    @Override
    public String toString() {
        return "HospitalInsurance{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", policyNumber='" + policyNumber + '\'' +
                ", policyName='" + policyName + '\'' +
                ", photo='" + photo + '\'' +
                ", policyStartDate='" + policyStartDate + '\'' +
                ", policyEndDate='" + policyEndDate + '\'' +
                ", policyStatus='" + policyStatus + '\'' +
                ", jsonData='" + jsonData + '\'' +
                ", createdOn='" + createdOn + '\'' +
                ", updatedOn='" + updatedOn + '\'' +
                ", policyDocument='" + policyDocument + '\'' +
                '}';
    }
}

