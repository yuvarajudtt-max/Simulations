package com.dtt.simulations.controller;


import com.dtt.simulations.dto.BankApproveRejectDto;
import com.dtt.simulations.dto.BankRequestDto;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;

import com.dtt.simulations.service.Iface.BusinessAccountIface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class BusinessAccountController {

    private static final Logger logger = LoggerFactory.getLogger(BusinessAccountController.class);

   private static final  String CLASS = "BusinessAccountController";

    private final BusinessAccountIface businessAccountIface;

    public BusinessAccountController(BusinessAccountIface businessAccountIface) {
        this.businessAccountIface = businessAccountIface;
    }

    @GetMapping("/api/get/service/status")
    public ApiResponse getServiceStatus() {
       return AppUtil.createApiResponse(true,"Service is running ",false);
    }


    @PostMapping("/api/post/save/bankAccount")
    public ApiResponse saveBusinessBankAccount(@RequestBody String model) {

        return businessAccountIface.saveBuissinessBankAccount(model);

    }

    @PostMapping("/api/post/approve-reject/buisiness-account/{passport}/{status}")
    public ApiResponse approveOrRejectBusinessAccount(@PathVariable String passport,@PathVariable String status){
        return  businessAccountIface.approveOrRejectBuisinessAccount(passport,status);
    }

    @GetMapping("/api/get/business-account/{passportNumber}")
    public ApiResponse getBusinessAccount(@PathVariable String passportNumber) {
        return businessAccountIface.getBuisinessAccountByPassport(passportNumber);
    }

    @GetMapping("/api/get/bank-account/{id}")
    public ApiResponse getBusinessAccountbyId(@PathVariable int id) {
        return businessAccountIface.getBuisinessAccountById(id);
    }

    @GetMapping("/api/get/all/business-account")
    public ApiResponse getAllBusinessAccount() {
        return businessAccountIface.getAllBuisinessAccount();
    }



    @PostMapping("/api/get/userProfile")
    public ApiResponse getUserProfile(@RequestBody BankRequestDto bankRequestDto) {

        logger.info(CLASS + "getUserProfile >> Inside /api/get/userProfile");

        return businessAccountIface.getUserProfile(bankRequestDto);

    }

    @PostMapping("/api/approve-reject/bank/account")
    public ApiResponse approveBank(@RequestBody BankApproveRejectDto bankApproveRejectDto) {
        return businessAccountIface.approveRejectBankAccount(bankApproveRejectDto);
    }

    @GetMapping("/api/get/business-account/view/{id}")
    public ApiResponse getViewBusinessAccount(@PathVariable int id) {
        return businessAccountIface.viewforApproveReject(id);
    }

    @PostMapping("/api/post/save/web/bankAccount")
    public ApiResponse saveBusinessBankAccountWeb(@RequestBody String json) {

            return businessAccountIface.saveBankAccountOpeningWeb(json);

    }

}
