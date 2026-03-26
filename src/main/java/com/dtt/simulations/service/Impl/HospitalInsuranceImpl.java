


package com.dtt.simulations.service.Impl;

import com.dtt.simulations.constants.AppConstants;
import com.dtt.simulations.dto.HospitalInsuranceDTO;
import com.dtt.simulations.dto.NotificationContextDTO;
import com.dtt.simulations.dto.NotificationDTO;
import com.dtt.simulations.dto.NotificationDataDTO;
import com.dtt.simulations.enums.ServiceNames;
import com.dtt.simulations.model.HospitalInsurance;
import com.dtt.simulations.model.Subscriber;
import com.dtt.simulations.model.SubscriberFCMToken;
import com.dtt.simulations.repo.*;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;
import com.dtt.simulations.service.Iface.HospitalInsuranceIface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class HospitalInsuranceImpl implements HospitalInsuranceIface {

    Logger logger = LoggerFactory.getLogger(HospitalInsuranceImpl.class);
    private final HospitalInsuranceRepo hospitalInsuranceRepo;
    private final LogModelServiceImpl logModelService;
    private final SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface;
    private final SubscriberRepoIface subscriberRepoIface;
    private final MessageSource messageSource;

    public HospitalInsuranceImpl(
            HospitalInsuranceRepo hospitalInsuranceRepo,
            LogModelServiceImpl logModelService,
            SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface,
            SubscriberRepoIface subscriberRepoIface,
            MessageSource messageSource) {

        this.hospitalInsuranceRepo = hospitalInsuranceRepo;
        this.logModelService = logModelService;
        this.subscriberFcmTokenRepoIface = subscriberFcmTokenRepoIface;
        this.subscriberRepoIface = subscriberRepoIface;
        this.messageSource = messageSource;
    }
    @Value("${notify.url}")
    public String notifyUrl;

    RestTemplate restTemplate = new RestTemplate();


    @Override
    public ApiResponse savedata(String jsonData) {

        Locale locale = LocaleContextHolder.getLocale();
        try {
            if (jsonData.equals("null") || jsonData.isEmpty()) {
                logger.info("savedata() → false → Hospital insurance details cannot be null");
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_DTO_NULL, null, locale),
                        null);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonData);

            HospitalInsurance hospitalInsurance = new HospitalInsurance();

            String insuredMember = jsonNode.get("insured_member").asText();
            String phoneNumber = jsonNode.get("phone_number").asText();
            String gender = jsonNode.get("gender").asText();
            String photo = jsonNode.get("portrait").asText();

            hospitalInsurance.setName(insuredMember);
            hospitalInsurance.setPhoneNumber(phoneNumber);
            hospitalInsurance.setGender(gender);
            hospitalInsurance.setPhoto(photo);
            hospitalInsurance.setPolicyName(jsonNode.get("policy_name").asText());
            hospitalInsurance.setPolicyNumber(jsonNode.get("policy_number").asText());
            hospitalInsurance.setPolicyStartDate(jsonNode.get("issue_date").asText());
            hospitalInsurance.setPolicyEndDate(jsonNode.get("expiry_date").asText());
            hospitalInsurance.setPolicyStatus(jsonNode.get("policy_status").asText());
            hospitalInsurance.setPolicyDocument(jsonNode.get("health_insurance_document").asText());
            hospitalInsurance.setJsonData(jsonData);
            hospitalInsurance.setCreatedOn(AppUtil.getDate());
            hospitalInsurance.setUpdatedOn(AppUtil.getDate());

            String passportNumber = jsonNode.path("idDocNumber").asText(null);
            String documentType = jsonNode.path("documentType").asText(null);

            if (passportNumber == null || passportNumber.isEmpty()) {
                logger.info("Passport number is missing");
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_ID_DOC_NULL, null, locale),
                        null);
            }

            Subscriber subscriber;

            if ("passport".equals(documentType)) {
                subscriber = subscriberRepoIface.findByPassportNumber(passportNumber);
            } else if ("emirates_id".equals(documentType)) {
                subscriber = subscriberRepoIface.findByNationalIdNumber(passportNumber);
            } else {
                subscriber = subscriberRepoIface.findBySubscriberUid(passportNumber);
            }

            hospitalInsuranceRepo.save(hospitalInsurance);

            sendNotification(subscriber.getSubscriberUid(),subscriber.getFullName(),AppConstants.API_RESPONSE_HEALTH_INSURANCE_SUCCESS);

            Date start = AppUtil.getCurrentDate();
            Date end = AppUtil.getCurrentDate();

            logModelService.setLogModelDTO(true,
                    subscriber.getSubscriberUid(),
                    null,
                    ServiceNames.OTHER.toString(),
                    AppUtil.getUUId(),
                    AppConstants.API_RESPONSE_HEALTH_INSURANCE_SUCCESS,
                    start, end, "false");

            return AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_HEALTH_INSURANCE_SUCCESS, null, locale),
                    null);

        } catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }
    }

    @Override
    public ApiResponse getHospitalInsuranceDetails() {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            List<HospitalInsurance> list = hospitalInsuranceRepo.getHospitalInsuranceDetails();

            if (list == null || list.isEmpty()) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND, null, locale),
                        null);
            }

            List<HospitalInsuranceDTO> dtoList = new ArrayList<>();
            for (HospitalInsurance i : list) {
                HospitalInsuranceDTO dto = getHospitalInsuranceDTO(i);

                dtoList.add(dto);
            }

            return AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS, null, locale),
                    dtoList);

        } catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }
    }

    private static HospitalInsuranceDTO getHospitalInsuranceDTO(HospitalInsurance i) {
        HospitalInsuranceDTO dto = new HospitalInsuranceDTO();
        dto.setId(i.getId());
        dto.setName(i.getName());
        dto.setGender(i.getGender());
        dto.setPhoneNumber(i.getPhoneNumber());
        dto.setPolicyNumber(i.getPolicyNumber());
        dto.setPolicyName(i.getPolicyName());
        dto.setPolicyStartDate(i.getPolicyStartDate());
        dto.setPolicyEndDate(i.getPolicyEndDate());
        dto.setPolicyStatus(i.getPolicyStatus());
        return dto;
    }

    @Override
    public ApiResponse getHospitalInsuranceDetailsById(int id) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            if (id == 0) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_ID_EMPTY, null, locale),
                        null);
            }

            HospitalInsurance insurance = hospitalInsuranceRepo.getHospitalInsuranceDetailsById(id);

            if (insurance == null) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND, null, locale),
                        null);
            }

            return AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS, null, locale),
                    insurance);

        } catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }
    }


    public void sendNotification(String subscriberUUID,String applicantName,String message){
        try{
            if(subscriberUUID != null){

                SubscriberFCMToken subscriberFCMToken = subscriberFcmTokenRepoIface.findBysuid(subscriberUUID);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                NotificationDTO notificationBody = getNotificationDTO(applicantName, message, subscriberFCMToken);
                HttpEntity<Object> requestEntity = new HttpEntity<>(notificationBody, headers);


                ResponseEntity<String> res = restTemplate.exchange(notifyUrl, HttpMethod.POST, requestEntity,
                        String.class);
                if (res.getStatusCode().is2xxSuccessful()) {
                    logger.info("Notification sent");
                } else {
                    logger.info("Notification failed");
                }
            }
        }catch (Exception e){
            logger.error("{}", e.getMessage());
        }

    }

    public NotificationDTO getNotificationDTO(String applicantName, String message, SubscriberFCMToken subscriberFCMToken) {

        Locale locale = LocaleContextHolder.getLocale();
        NotificationDataDTO dataDTO = new NotificationDataDTO();
        NotificationContextDTO contextDTO = new NotificationContextDTO();
        NotificationDTO notificationBody = new NotificationDTO();

        notificationBody.setTo(subscriberFCMToken.getFcmToken());
        notificationBody.setPriority("high");

        String title = messageSource.getMessage(
                "api.response.notification.title",
                new Object[]{applicantName},
                locale
        );

        String body = messageSource.getMessage(
                message,
                null,
                locale
        );

        dataDTO.setTitle(title);
        dataDTO.setBody(body);

        Map<String, String> immigrationNotification = new HashMap<>();
        immigrationNotification.put("health", "insurance");

        contextDTO.setPrefImmigrationAuthority(immigrationNotification);
        dataDTO.setNotificationContext(contextDTO);
        notificationBody.setData(dataDTO);
        return notificationBody;
    }
}