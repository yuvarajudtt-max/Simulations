package com.dtt.simulations.service.Impl;


import com.dtt.simulations.constants.AppConstants;
import com.dtt.simulations.dto.NotificationContextDTO;
import com.dtt.simulations.dto.NotificationDTO;
import com.dtt.simulations.dto.NotificationDataDTO;
import com.dtt.simulations.enums.ServiceNames;
import com.dtt.simulations.model.MoneyTransfer;
import com.dtt.simulations.model.Subscriber;
import com.dtt.simulations.model.SubscriberFCMToken;
import com.dtt.simulations.repo.MoneyTransferRepo;
import com.dtt.simulations.repo.SubscriberFcmTokenRepoIface;
import com.dtt.simulations.repo.SubscriberRepoIface;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;
import com.dtt.simulations.service.Iface.MoneyTransferIface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;



@Service

public class MoneyTransferImpl implements MoneyTransferIface {

    private final MoneyTransferRepo moneyTransferRepo;
    private final LogModelServiceImpl logModelService;
    private final SubscriberRepoIface subscriberRepoIface;
    private final SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface;
    private final MessageSource messageSource;

    public MoneyTransferImpl(
            MoneyTransferRepo moneyTransferRepo,
            LogModelServiceImpl logModelService,
            SubscriberRepoIface subscriberRepoIface,
            SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface,
        MessageSource messageSource){

        this.moneyTransferRepo = moneyTransferRepo;
        this.logModelService = logModelService;
        this.subscriberRepoIface = subscriberRepoIface;
        this.subscriberFcmTokenRepoIface = subscriberFcmTokenRepoIface;
        this.messageSource = messageSource;
    }

    @Value("${notify.url}")
    public String notifyUrl;

    Logger logger= LoggerFactory.getLogger(MoneyTransferImpl.class);
    Locale locale = LocaleContextHolder.getLocale();
    RestTemplate restTemplate = new RestTemplate();

    @Override
    public ApiResponse getAllMoneyTransfer() {
        try {
            List<MoneyTransfer> moneyTransfers = moneyTransferRepo.findAll();
            List<MoneyTransfer> responseForms = getMoneyTransfers(moneyTransfers);
            return AppUtil.createApiResponse(true, messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS,null,locale), responseForms);
        }catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }



    }

    private static List<MoneyTransfer> getMoneyTransfers(List<MoneyTransfer> moneyTransfers) {
        List<MoneyTransfer> responseForms = new ArrayList<>();
        for (MoneyTransfer i : moneyTransfers) {
            MoneyTransfer moneyTransferDto = new MoneyTransfer();
            moneyTransferDto.setId(i.getId());
            moneyTransferDto.setPrincipalName(i.getPrincipalName());
            moneyTransferDto.setAgentName(i.getAgentName());
            moneyTransferDto.setNotaryName(i.getNotaryName());
            responseForms.add(moneyTransferDto);

        }
        return responseForms;
    }

    @Override
    public ApiResponse getMoneyTransferById(int id) {
        try {

            MoneyTransfer moneyTransfer = moneyTransferRepo.findById(id);
            return AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS, null, locale),
                    moneyTransfer);

        }catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }
    }

    @Override
    public ApiResponse saveMoneyTransfer(MoneyTransfer moneyTransfer) {
        try{
            if(moneyTransfer == null){
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_DTO_NULL,null,locale),
                        null);
            }

            MoneyTransfer moneyTransfer1 = getMoneyTransfer(moneyTransfer);

            moneyTransferRepo.save(moneyTransfer1);

            Date start= AppUtil.getCurrentDate();

            Date end=AppUtil.getCurrentDate();

            Subscriber subscriberAgent =
                    subscriberRepoIface.findbyDocumentNumber(moneyTransfer.getAgentIdDocNumber());

            Subscriber subscriberPrincipal =
                    subscriberRepoIface.findbyDocumentNumber(moneyTransfer.getPrincipalIdDocNumber());

            if (subscriberAgent == null || subscriberPrincipal == null) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_SUBSCRIBER_DETAILS_NOT_FOUND,null,locale),
                        null);

            }

            SubscriberFCMToken agentToken =
                    subscriberFcmTokenRepoIface.findBysuid(subscriberAgent.getSubscriberUid());

            if (agentToken != null) {
                sendNotification(
                        agentToken.getFcmToken(),
                        "Hi " + subscriberAgent.getFullName(),
                        "Money transfer initiated successfully",
                        "Agent"
                );
            }

            SubscriberFCMToken principalToken =
                    subscriberFcmTokenRepoIface.findBysuid(subscriberPrincipal.getSubscriberUid());

            if (principalToken != null) {
                sendNotification(
                        principalToken.getFcmToken(),
                        "Hi " + subscriberPrincipal.getFullName(),
                        "Money transfer initiated by " + subscriberAgent.getFullName(),
                        "Principal"
                );
            }


            logModelService.setLogModelDTO(true,subscriberAgent.getSubscriberUid(),null, ServiceNames.OTHER.toString(),AppUtil.getUUId(),"Money transfer initiated by the agent",start,end,"false");
            return AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_MONEY_TRANSFER_SUCCESS,null,locale),
                    null);


        }catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }
    }

    private static MoneyTransfer getMoneyTransfer(MoneyTransfer moneyTransfer) {
        MoneyTransfer moneyTransfer1 = new MoneyTransfer();
        moneyTransfer1.setPrincipalName(moneyTransfer.getPrincipalName());
        moneyTransfer1.setPrincipalEmail(moneyTransfer.getPrincipalEmail());
        moneyTransfer1.setPrincipalIdDocNumber(moneyTransfer.getPrincipalIdDocNumber());
        moneyTransfer1.setAgentName(moneyTransfer.getAgentName());
        moneyTransfer1.setAgentEmail(moneyTransfer.getAgentEmail());
        moneyTransfer1.setAgentIdDocNumber(moneyTransfer.getAgentIdDocNumber());
        moneyTransfer1.setNotaryName(moneyTransfer.getNotaryName());
        moneyTransfer1.setNotaryEmail(moneyTransfer.getNotaryEmail());
        moneyTransfer1.setNotaryIdDocNumber(moneyTransfer.getNotaryIdDocNumber());
        moneyTransfer1.setPoaSignedDoc(moneyTransfer.getPoaSignedDoc());
        moneyTransfer1.setCreatedOn(AppUtil.getDate());
        moneyTransfer1.setUpdatedOn(AppUtil.getDate());
        moneyTransfer1.setValidUpto(moneyTransfer.getValidUpto());
        return moneyTransfer1;
    }


    private void sendNotification(String fcmToken,
                                  String title,
                                  String body,
                                  String logMessage) {

        NotificationDTO notificationBody = getNotificationDTO(fcmToken, title, body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> requestEntity = new HttpEntity<>(notificationBody, headers);

        try {
            ResponseEntity<Object> response =
                    restTemplate.exchange(notifyUrl, HttpMethod.POST, requestEntity, Object.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Notification sent successfully: {}", logMessage);
            } else {
                logger.warn("Notification failed: {}", logMessage);
            }

        } catch (Exception e) {
            logger.error("Notification exception: {}", logMessage, e);
        }
    }

    private static NotificationDTO getNotificationDTO(String fcmToken, String title, String body) {
        NotificationDataDTO dataDTO = new NotificationDataDTO();
        NotificationContextDTO contextDTO = new NotificationContextDTO();
        NotificationDTO notificationBody = new NotificationDTO();

        notificationBody.setTo(fcmToken);
        notificationBody.setPriority("high");

        dataDTO.setTitle(title);
        dataDTO.setBody(body);

        Map<String, String> notificationMap = new HashMap<>();
        notificationMap.put("Money", "Transfer");

        contextDTO.setPrefImmigrationAuthority(notificationMap);
        dataDTO.setNotificationContext(contextDTO);
        notificationBody.setData(dataDTO);
        return notificationBody;
    }
}
