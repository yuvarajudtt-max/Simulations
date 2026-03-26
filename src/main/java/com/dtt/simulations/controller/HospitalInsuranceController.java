package com.dtt.simulations.controller;


import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.service.Iface.HospitalInsuranceIface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class HospitalInsuranceController {

    private static final  String CLASS = "HospitalInsuranceController";
    private static Logger logger = LoggerFactory.getLogger(HospitalInsuranceController.class);


    private final HospitalInsuranceIface hospitalInsuranceIface;

    public HospitalInsuranceController(HospitalInsuranceIface hospitalInsuranceIface) {
        this.hospitalInsuranceIface = hospitalInsuranceIface;
    }

    @PostMapping("/api/post/save/hospitalInsurance")
    public ApiResponse savedata(@RequestBody String jsonData) {

        return hospitalInsuranceIface.savedata(jsonData);

    }

    @GetMapping("/api/get/HospitalInsurance/details/for/table")
    public ApiResponse getHospitalInsuranceDetails() {
        logger.info(CLASS + "getHospitalInsuranceDetails controller >> /api/get/HospitalInsurance/details/for/table}");
        return hospitalInsuranceIface.getHospitalInsuranceDetails();

    }

    @GetMapping("/api/get/health-insurance-data/by/id/{id}")
    public ApiResponse getHospitalInsuranceDetailsById(@PathVariable int id){

        return hospitalInsuranceIface.getHospitalInsuranceDetailsById(id);
    }

}
