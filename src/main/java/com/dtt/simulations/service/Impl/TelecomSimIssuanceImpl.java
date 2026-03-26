package com.dtt.simulations.service.Impl;


import com.dtt.simulations.constants.AppConstants;
import com.dtt.simulations.dto.NotificationContextDTO;
import com.dtt.simulations.dto.NotificationDTO;
import com.dtt.simulations.dto.NotificationDataDTO;
import com.dtt.simulations.dto.SimIssuanceDto;
import com.dtt.simulations.enums.ServiceNames;
import com.dtt.simulations.model.Subscriber;
import com.dtt.simulations.model.SubscriberFCMToken;
import com.dtt.simulations.model.TelecomSimIssuance;
import com.dtt.simulations.repo.SubscriberFcmTokenRepoIface;
import com.dtt.simulations.repo.SubscriberRepoIface;
import com.dtt.simulations.repo.TelecomSimIssuanceRepo;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;
import com.dtt.simulations.service.Iface.TelecomSimIssuanceIface;

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
public class TelecomSimIssuanceImpl implements TelecomSimIssuanceIface {


    private final TelecomSimIssuanceRepo telecomSimIssuanceRepo;
    private final LogModelServiceImpl logModelService;
    private final MessageSource messageSource;
    private final SubscriberRepoIface subscriberRepoIface;
    private final SubscriberFcmTokenRepoIface subscriberFcmTokenRepository;

    public TelecomSimIssuanceImpl(
            TelecomSimIssuanceRepo telecomSimIssuanceRepo,
            LogModelServiceImpl logModelService,
            MessageSource messageSource,
            SubscriberRepoIface subscriberRepoIface,
            SubscriberFcmTokenRepoIface subscriberFcmTokenRepository) {

        this.telecomSimIssuanceRepo = telecomSimIssuanceRepo;
        this.logModelService = logModelService;
        this.messageSource = messageSource;
        this.subscriberRepoIface = subscriberRepoIface;
        this.subscriberFcmTokenRepository = subscriberFcmTokenRepository;
    }


    @Value("${notification.url}")
    String sendNotificationURL;


    private static final Logger logger = LoggerFactory.getLogger(TelecomSimIssuanceImpl.class);

    Random random = new Random();
    RestTemplate restTemplate = new RestTemplate();


    @Override
    public ApiResponse saveSimData(SimIssuanceDto dto) {
        Locale locale = LocaleContextHolder.getLocale();
        try {


            Date startTime = AppUtil.getCurrentDate();

            Subscriber subscriber = new Subscriber();

            if ("passport".equals(dto.getDocumentType())) {
                subscriber = subscriberRepoIface.findByPassportNumber(dto.getIdDocNumber());
            } else if ("emirates_id".equals(dto.getDocumentType())) {
                subscriber = subscriberRepoIface.findByNationalIdNumber(dto.getIdDocNumber());

            } else if ("suid".equals(dto.getDocumentType())) {
                subscriber = subscriberRepoIface.findBySubscriberUid(dto.getIdDocNumber());
            }
            if (subscriber == null) {
                logger.info(" saveSimData() Impl >> false >> Subscriber details not found");
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_SUBSCRIBER_DETAILS_NOT_FOUND, null, locale),
                        null);
            }

            logger.info("saveSimData() Impl >> false >> Inside saveSimData implementation");

            TelecomSimIssuance telecomSimIssuance = new TelecomSimIssuance();
            telecomSimIssuance.setFullName(dto.getFullName());
            telecomSimIssuance.setIdDocNumber(dto.getIdDocNumber());
            telecomSimIssuance.setMobileNumber(dto.getMobileNumber());
            if (dto.getAdditionalData() != null) {
                telecomSimIssuance.setAdditionalData(dto.getAdditionalData().toString());
            }


            int simNumber = 100000 + random.nextInt(900000);
            telecomSimIssuance.setSimNumber(simNumber);
            telecomSimIssuanceRepo.save(telecomSimIssuance);


            Date endTime = AppUtil.getCurrentDate();

            sendNotificationToActivateCertificate(subscriber);
            logModelService.setLogModelDTO(true, subscriber.getSubscriberUid(), null, ServiceNames.SIM.toString(), AppUtil.getUUId(), AppConstants.API_RESPONSE_SIM_ACTIVATED, startTime, endTime, "false");

            return AppUtil.createApiResponse(true, messageSource.getMessage(AppConstants.API_RESPONSE_SIM_ACTIVATED, null, locale), null);


        } catch (Exception e) {
            logger.info("{}", e.getMessage());
            return AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, null, locale),
                    null);
        }


    }


    public void sendNotificationToActivateCertificate(Subscriber subscriber) {
        try {

            Locale locale = LocaleContextHolder.getLocale();

            SubscriberFCMToken subscriberFCMToken = subscriberFcmTokenRepository.findBysuid(subscriber.getSubscriberUid());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            NotificationDTO notificationBody = new NotificationDTO();
            NotificationDataDTO dataDTO = new NotificationDataDTO();
            NotificationContextDTO contextDTO = new NotificationContextDTO();
            notificationBody.setTo(subscriberFCMToken.getFcmToken());
            notificationBody.setPriority("high");
            Map<String, String> simActivation = new HashMap<>();

            String title = messageSource.getMessage(
                    "api.response.notification.title",
                    new Object[]{subscriber.getFullName()},
                    locale
            );

            String body = messageSource.getMessage(
                    "api.response.notification.sim.activated",
                    null,
                    locale
            );

            dataDTO.setTitle(title);
            dataDTO.setBody(body);


            simActivation.put("SimActivation", "Success");

            contextDTO.setPrefImmigrationAuthority(simActivation);
            dataDTO.setNotificationContext(contextDTO);
            notificationBody.setData(dataDTO);
            HttpEntity<Object> requestEntity = new HttpEntity<>(notificationBody, headers);

            ResponseEntity<String> res = restTemplate.exchange(sendNotificationURL, HttpMethod.POST, requestEntity,
                    String.class);
            if (res.getStatusCode().is2xxSuccessful()) {
                logger.info("Notification sent");
            } else {
                logger.info("Notification failed");
            }


        } catch (Exception e) {
            logger.info("{}", e.getMessage());

        }

    }


}
