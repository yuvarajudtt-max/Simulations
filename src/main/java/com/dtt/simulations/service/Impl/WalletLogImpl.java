package com.dtt.simulations.service.Impl;

import com.dtt.simulations.constants.AppConstants;
import com.dtt.simulations.dto.WalletTranscationDto;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;
import com.dtt.simulations.service.Iface.WalletLogIface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;

@Service
public class WalletLogImpl implements WalletLogIface {

    @Value("${wallet.transaction.log}")
    public String walletTranscationLog;


    private final MessageSource messageSource;

    public WalletLogImpl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    Logger logger= LoggerFactory.getLogger(WalletLogImpl.class);
    Locale locale = LocaleContextHolder.getLocale();
    RestTemplate restTemplate = new RestTemplate();

    @Override
    public ApiResponse savewalletLog(WalletTranscationDto walletTranscationDto) {
        try {
            if (walletTranscationDto == null) {
                return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale), null);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> reqEntity1 = new HttpEntity<>(walletTranscationDto, headers);

            ResponseEntity<ApiResponse> res1 = restTemplate.exchange(walletTranscationLog, HttpMethod.POST, reqEntity1, ApiResponse.class);
            logger.info("Wallet log API called. URL: {}, Status: {}", walletTranscationLog, res1.getStatusCode());
            if (!res1.getStatusCode().is2xxSuccessful()) {
                logger.error("Wallet log API failed with status: {}", res1.getStatusCode());
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
            }

            ApiResponse body = res1.getBody();

            if (body == null) {
                logger.error("Wallet log API response body is null");
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_RESPONSE_BODY_EMPTY,null,locale), null);
            }

            if (!body.isSuccess()) {
                return AppUtil.createApiResponse(
                        false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),body.getMessage());
            }

            return AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_LOG_SAVED,null,locale),
                    body.getMessage());

        }catch (Exception e){
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }
    }
}