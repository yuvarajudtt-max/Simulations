package com.dtt.simulations.POA.responseentity;


import java.text.SimpleDateFormat;
import java.util.*;



public class AppUtilPOA {




    private AppUtilPOA(){
        throw new AssertionError("Utility class - do not instantiate");
    }


    public static ApiResponsePOA createApiResponse(boolean success, String msg, Object object) {
        ApiResponsePOA apiResponse = new ApiResponsePOA();
        apiResponse.setMessage(msg);
        apiResponse.setResult(object);
        apiResponse.setSuccess(success);
        return apiResponse;

    }



    public static String getBase64FromByteArr(byte[] bytes){
        Base64.Encoder base64 = Base64.getEncoder();
        return base64.encodeToString(bytes);
    }



    public static String getDate(){
        SimpleDateFormat smpdate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return smpdate.format(date);
    }


}

