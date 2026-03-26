package com.dtt.simulations.service.Iface;


import com.dtt.simulations.responseentity.ApiResponse;

public interface HospitalInsuranceIface {

    ApiResponse savedata(String jsonData);

    ApiResponse getHospitalInsuranceDetails();

    ApiResponse getHospitalInsuranceDetailsById(int id);
}
