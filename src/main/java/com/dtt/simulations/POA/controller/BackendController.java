package com.dtt.simulations.POA.controller;


import com.dtt.simulations.POA.dto.PowerOfAttorneyDto;
import com.dtt.simulations.POA.dto.SavePoaDto;
import com.dtt.simulations.POA.responseentity.ApiResponsePOA;
import com.dtt.simulations.POA.responseentity.AppUtilPOA;
import com.dtt.simulations.POA.service.Iface.PowerOfAttorneyIface;
import com.dtt.simulations.POA.service.Impl.PowerOfAttorneyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class BackendController {


    private static final Logger logger = LoggerFactory.getLogger(BackendController.class);


    static final  String CLASS = "Backend Controller";


    private final PowerOfAttorneyIface powerOfAttorneyIface;
    private final PowerOfAttorneyImpl powerOfAttorney;

    public BackendController(
            PowerOfAttorneyIface powerOfAttorneyIface,
            PowerOfAttorneyImpl powerOfAttorney) {
        this.powerOfAttorneyIface = powerOfAttorneyIface;
        this.powerOfAttorney = powerOfAttorney;
    }



    @PostMapping("/save/poa/request/v1")
    public ApiResponsePOA savePoaRequestV1(@RequestBody SavePoaDto savePoaDto){
        return powerOfAttorneyIface.savePoaInTemp(savePoaDto);
    }



    @PostMapping("/save/poa/request")
    public ApiResponsePOA savePoaRequest(@RequestBody SavePoaDto savePoaDto){
        return powerOfAttorneyIface.savePoa(savePoaDto);
    }


    @GetMapping("/get/poa/application/by/id/{id}")
    public ApiResponsePOA getPoaApplicationById(@PathVariable("id") int id){
        return powerOfAttorneyIface.getStatus(id);
    }

    @GetMapping("/get/notary-and-scope/information")
    public ApiResponsePOA getNotaryAndScopeInformation(){

        return powerOfAttorneyIface.getNotaryAndScopeInformation();
    }

    @PostMapping("/api/post/transfer-poa/{agentEmail}")
    public ApiResponsePOA transferPoa(@PathVariable("agentEmail") String agentEmail){
        logger.info(CLASS + "transfer POA");
        return powerOfAttorneyIface.transferPoa(agentEmail);
    }


    @GetMapping("/get/poa/service/status")
    public ApiResponsePOA getStatus(){

        return AppUtilPOA.createApiResponse(true,"POA up and running",null);
    }

    @PostMapping("/reset/poa")
    public ApiResponsePOA resetPoaData() {
        try {
            logger.info("Received request to reset POA data");

           return powerOfAttorney.resetPoaData();

        } catch (Exception e) {
            logger.error("Unexpected error in reset POA endpoint: {}", e.getMessage(), e);

            return AppUtilPOA.createApiResponse(false, "Unexpected error occurred", e.getMessage());
        }
    }


    @GetMapping("/getAll/power-of-attorney-credential-requests/by/principle-docNumber/{principalDocumentNumber}")
    public ApiResponsePOA getAllPoaCredentialRequests(@PathVariable String principalDocumentNumber){
        logger.info("{} credential requests by principle id Doc {}",CLASS,principalDocumentNumber);
        return powerOfAttorneyIface.getAllPoaCredentialRequests(principalDocumentNumber);
    }

    @GetMapping("/getAll/power-of-attorney-credentials/by/agent-docNumber/{agentDocumentNumber}")
    public ApiResponsePOA getAllPoaCredentials(@PathVariable String agentDocumentNumber){
        logger.info("{} credential by agent id Doc {}",CLASS,agentDocumentNumber);
        return powerOfAttorneyIface.getAllPoaCredentials(agentDocumentNumber);
    }


    @PostMapping("api/poa/modify-pdf")
    public ResponseEntity<String> modifyPoaPdf(@RequestBody PowerOfAttorneyDto powerOfAttorneyDto) throws IOException {
        String  modifiedPdfBase64 = powerOfAttorneyIface.modifypoa(powerOfAttorneyDto);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return ResponseEntity.ok().headers(headers).body(modifiedPdfBase64);
    }

}
