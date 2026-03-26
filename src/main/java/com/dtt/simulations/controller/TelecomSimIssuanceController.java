package com.dtt.simulations.controller;

import com.dtt.simulations.dto.SimIssuanceDto;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.service.Iface.TelecomSimIssuanceIface;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class TelecomSimIssuanceController {


    private final TelecomSimIssuanceIface telecomSimIssuanceIface;

    public TelecomSimIssuanceController(TelecomSimIssuanceIface telecomSimIssuanceIface) {
        this.telecomSimIssuanceIface = telecomSimIssuanceIface;
    }
    @PostMapping("/api/post/save/sim/data")
    public ApiResponse saveSimData(@Valid @RequestBody SimIssuanceDto dto, BindingResult bindingResult){
        return telecomSimIssuanceIface.saveSimData(dto);
    }

}
