package com.dtt.simulations.POA.service.Iface;


import com.dtt.simulations.POA.dto.PowerOfAttorneyDto;
import com.dtt.simulations.POA.dto.SavePoaDto;
import com.dtt.simulations.POA.responseentity.ApiResponsePOA;

public interface PowerOfAttorneyIface {
    ApiResponsePOA savePoaInTemp(SavePoaDto powerOfAttorney);
    ApiResponsePOA getStatus(int id);
    ApiResponsePOA getNotaryAndScopeInformation();
    ApiResponsePOA transferPoa(String agentEmail);
    ApiResponsePOA getAllPoaCredentialRequests(String principalDocumentNumber);
    ApiResponsePOA getAllPoaCredentials(String suid);
    public String modifypoa(PowerOfAttorneyDto powerOfAttorneyDto)throws java.io.IOException;

    ApiResponsePOA savePoa(SavePoaDto powerOfAttorney);

}
