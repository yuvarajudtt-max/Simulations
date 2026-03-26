


package com.dtt.simulations.POA.responseentity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResponseW3c {

    @JsonProperty("Success")
    private boolean success;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("ErrorCode")
    private int errorCode;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("Result")
    private Object result;


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ApiResponseW3c{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", result=" + result +
                '}';
    }
}

