package com.dtt.simulations.responseentity;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppUtil {

    public static Date getCurrentDate() {
        return new Date();
    }

    private AppUtil(){
        throw new AssertionError("Utility class - do not instantiate");
    }


    public static ApiResponse createApiResponse(boolean success, String msg, Object object) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage(msg);
        apiResponse.setResult(object);
        apiResponse.setSuccess(success);
        return apiResponse;

    }

    public static Date getTimeStamp() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.parse(new Timestamp(System.currentTimeMillis()).toString());
    }



    public static String getTimeStampString(Date date) {
        Optional<Date> oDate = Optional.ofNullable(date);
        Date d = oDate.isPresent() ? oDate.get() : new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return f.format(d);
    }






    public static String getUUId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }


    public static String getDate(){
        SimpleDateFormat smpdate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return smpdate.format(date);
    }



}

