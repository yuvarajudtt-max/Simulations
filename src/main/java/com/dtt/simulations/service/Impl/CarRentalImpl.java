package com.dtt.simulations.service.Impl;


import com.dtt.simulations.constants.AppConstants;
import com.dtt.simulations.dto.CarRentalDto;
import com.dtt.simulations.dto.NotificationContextDTO;
import com.dtt.simulations.dto.NotificationDTO;
import com.dtt.simulations.dto.NotificationDataDTO;
import com.dtt.simulations.enums.ServiceNames;
import com.dtt.simulations.model.CarRental;
import com.dtt.simulations.model.Subscriber;

import com.dtt.simulations.model.SubscriberFCMToken;

import com.dtt.simulations.repo.*;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;
import com.dtt.simulations.service.Iface.CarRentalIface;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;



@Service

public class CarRentalImpl implements CarRentalIface {

    Logger logger = LoggerFactory.getLogger(CarRentalImpl.class);

    private final CarRentalRepo carRentalRepo;
    private final LogModelServiceImpl logModelService;
    private final SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface;
    private final SubscriberRepoIface subscriberRepoIface;
    private final MessageSource messageSource;

    public CarRentalImpl(
            CarRentalRepo carRentalRepo,
            LogModelServiceImpl logModelService,
            SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface,
            SubscriberRepoIface subscriberRepoIface,
            MessageSource messageSource) {

        this.carRentalRepo = carRentalRepo;
        this.logModelService = logModelService;
        this.subscriberFcmTokenRepoIface = subscriberFcmTokenRepoIface;
        this.subscriberRepoIface = subscriberRepoIface;
        this.messageSource = messageSource;
    }

    @Value("${notify.url}")
    public String notifyUrl;
    Locale locale = LocaleContextHolder.getLocale();
    Random random = new Random();
    RestTemplate restTemplate = new RestTemplate();

    @Override
    public ApiResponse saveCarRentalData(String carRentalJson) {
        try{

            Date startTime= AppUtil.getCurrentDate();

            if(carRentalJson.equals("null") || carRentalJson.isEmpty()){
                logger.info("saveCarRentalData() Impl >> false >> Car Rental Details cannot be null");
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_DTO_NULL,null,locale) , null);
            }

            CarRental carRental = new CarRental();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(carRentalJson);



            String name =  jsonNode.path("fullName").asText(null);
            String passportNo =  jsonNode.path("idDocNumber").asText(null);
            String drivingNo =  jsonNode.path("drivingLicenseNumber").asText(null);

            String internationPermit =  jsonNode.path("internationalPermit").asText(null);
            String country =  jsonNode.path("nationality").asText(null);
            String pickup =  jsonNode.path("pick_up_date").asText(null);
            String rentalDays =  jsonNode.path("rental_days").asText(null);
            String pidDocument =  jsonNode.path("pidDocument").asText(null);
            String mdlDocument =  jsonNode.path("mdlDocument").asText(null);
            String photo =  jsonNode.path("photo").asText(null);


            if(rentalDays.equals("null") || rentalDays.isEmpty() || pickup.equals("null")||pickup.isEmpty()){
                logger.info("saveCarRentalData() Impl >> false >> Car Rental Details cannot be null");
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_PICK_UP_REQUIRED,null,locale),
                        null);
            }

            if(!internationPermit.equals("Issued")){
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_INTERNATIONAL_PERMIT_REQUIRED,null,locale),
                        null);

            }


            carRental.setApplicantName(name);
            carRental.setPassportNumber(passportNo);

            carRental.setDrivingLicenseDocument(mdlDocument);
            carRental.setDrivingLicenseNumber(drivingNo);
            carRental.setPidDocumnet(pidDocument);

            carRental.setCountry(country);
            carRental.setPickUpDate(pickup);
            carRental.setStatus("APPROVED");
            carRental.setCreatedOn(startTime);
            carRental.setUpdatedOn(startTime);
            carRental.setPhoto(photo);


            carRental.setNoOfDays(rentalDays);
            String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            int maxDigits = 5;

            char letter = letters.charAt(random.nextInt(letters.length()));
            int number = random.nextInt((int) Math.pow(10, maxDigits));

            carRental.setCarNumber(String.format("%c%05d", letter, number));
            carRental.setInternationalPermit(internationPermit);
            carRental.setJsonData(carRentalJson);

            carRentalRepo.save(carRental);

            Subscriber subscriber=subscriberRepoIface.findbyDocumentNumber(passportNo);
            sendNotification(subscriber.getSubscriberUid(),subscriber.getFullName(),"Car rented successfully");


            Date start= AppUtil.getCurrentDate();
            Date end=AppUtil.getCurrentDate();
            logModelService.setLogModelDTO(true,subscriber.getSubscriberUid(),null, ServiceNames.OTHER.toString(),AppUtil.getUUId(),"Car rented successfully",start,end,"false");

            return  AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_CAR_RENTED,null,locale),
                    null);

        }catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }
    }
    @Override
    public ApiResponse getAllCarRentalData() {
        try{

            List<CarRental> carRentalList = carRentalRepo.getCarRentalDetails();
            List<CarRentalDto> responseForms = new ArrayList<>();
            if (carRentalList != null && !carRentalList.isEmpty()) {

                for (CarRental i : carRentalList) {

                    CarRentalDto carRentalDto = new CarRentalDto();
                    carRentalDto.setId(i.getId());
                    carRentalDto.setApplicantName(i.getApplicantName());
                    carRentalDto.setPassportNumber(i.getPassportNumber());
                    carRentalDto.setDrivingLicenseNumber(i.getDrivingLicenseNumber());
                    carRentalDto.setInternationalPermit(i.getInternationalPermit());
                    carRentalDto.setCreatedOn(i.getCreatedOn());
                    carRentalDto.setUpdatedOn(i.getUpdatedOn());
                    carRentalDto.setStatus(i.getStatus());
                    carRentalDto.setCountry(i.getCountry());
                    carRentalDto.setCarNumber(i.getCarNumber());
                    carRentalDto.setNoOfDays(i.getNoOfDays());
                    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


                    LocalDateTime dateTime = LocalDateTime.parse(i.getPickUpDate().trim(), inputFormatter);

                    carRentalDto.setPickupDate(dateTime.format(outputFormatter));

                    responseForms.add(carRentalDto);

                }




                return AppUtil.createApiResponse(true, messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS,null,locale), responseForms);
            }
            return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale), carRentalList);


        }catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),
                    null);
        }
    }

    @Override
    public ApiResponse getCarRentalDataById(int id) {
        try{
            if(id==0){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_ID_DOC_NULL,null,locale),null);
            }

            CarRental carRental = carRentalRepo.getCarRentalDetaildById(id);


            if(carRental==null){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale),null);
            }

            return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS,null,locale),carRental);

        }catch (Exception e){
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

    private static NotificationDTO getNotificationDTO(String applicantName, String message, SubscriberFCMToken subscriberFCMToken) {
        NotificationDTO notificationBody = new NotificationDTO();
        NotificationDataDTO dataDTO = new NotificationDataDTO();
        NotificationContextDTO contextDTO = new NotificationContextDTO();
        notificationBody.setTo(subscriberFCMToken.getFcmToken());
        notificationBody.setPriority("high");
        dataDTO.setTitle("Hi " + applicantName);
        Map<String, String> immigrationNotification = new HashMap<>();
        immigrationNotification.put("Car Rented", "success");
        dataDTO.setBody(message);
        contextDTO.setPrefImmigrationAuthority(immigrationNotification);
        dataDTO.setNotificationContext(contextDTO);
        notificationBody.setData(dataDTO);
        return notificationBody;
    }
}
