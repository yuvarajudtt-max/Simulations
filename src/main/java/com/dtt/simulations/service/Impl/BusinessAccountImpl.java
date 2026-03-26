package com.dtt.simulations.service.Impl;


import com.dtt.simulations.constants.AppConstants;
import com.dtt.simulations.dto.*;
import com.dtt.simulations.enums.BuisinessAccountStatus;
import com.dtt.simulations.enums.ServiceNames;
import com.dtt.simulations.model.BusinessAccountModel;
import com.dtt.simulations.model.Subscriber;
import com.dtt.simulations.model.SubscriberFCMToken;
import com.dtt.simulations.repo.*;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;

import com.dtt.simulations.service.Iface.BusinessAccountIface;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;



@Service
public class BusinessAccountImpl implements BusinessAccountIface {

    private static final Logger logger = LoggerFactory.getLogger(BusinessAccountImpl.class);

    RestTemplate restTemplate = new RestTemplate();

    private final BuisinessAccountRepo buisinessAccountRepo;
    private final SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface;
    private final LogModelServiceImpl logModelService;
    private final SubscriberRepoIface subscriberRepoIface;
    private final MessageSource messageSource;


    public BusinessAccountImpl(
            BuisinessAccountRepo buisinessAccountRepo,
            SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface,
            LogModelServiceImpl logModelService,
            SubscriberRepoIface subscriberRepoIface,
            MessageSource messageSource
          ) {

        this.buisinessAccountRepo = buisinessAccountRepo;
        this.subscriberFcmTokenRepoIface = subscriberFcmTokenRepoIface;
        this.logModelService = logModelService;
        this.subscriberRepoIface = subscriberRepoIface;
        this.messageSource = messageSource;
    }





    @Value("${notify.url}")
    public String notifyUrl;

    @Value("${userprofile.url}")
    public String userProfileUrl;

    @Value("${sign.x-coordinate}")
    String xcoordinate;

    @Value("${sign.y-coordinate}")
    String ycoordinate;


    @Value("${pdf.file.path}")
    private String pdfBasePath;

    @Value("${pdf.file.name}")
    private String pdfFileName;

    @Value("${file.sign.url}")
    String filesign;



    @Value("${bank.client.id}")
    public String clientId;

    @Value("${bank.secret}")
    public String clientSecret;


    @Value("${bank.access.token.url}")
    public String accessTokenUrl;






    private static final String MSG = "Your Bank account opening form is submitted successfully";
    private static final String FALSE = "false";
    private static final String APPROVED ="Your Bank account is Approved";
    private static final String REJECTED ="Your Bank account is rejected";
    private static final String DOCUMENT_TYPE ="documentType";
    private static final String ACCEPT_LANGUAGE ="Accept-Language";


    @Override
    public ApiResponse saveBuissinessBankAccount(String businessAccountDto) {
        Locale locale = LocaleContextHolder.getLocale();
        try{

            logger.info("saveBusinessBankAccount() Impl  >> Inside saveVisa implementation businessAccountDto {}",businessAccountDto);

            Date startTime = AppUtil.getTimeStamp();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(businessAccountDto);


            String model=jsonNode.get("model").asText();

            JsonNode modelJson = objectMapper.readTree(model);


            String base64=jsonNode.get("base64").asText();
            String documentType =jsonNode.path(DOCUMENT_TYPE).asText(null);


            if(base64==null){
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_BANK_FORM_STATUS_NULL,null,locale),null);
            }
            String passportNumber=modelJson.get("id_document_number").asText();
            String applicantName=modelJson.get("given_name").asText();


            if(passportNumber == null || passportNumber.isEmpty()){
                logger.info("saveBuissinessBankAccount() Impl >> false >> passport number cannot be  cannot be null");
                return AppUtil.createApiResponse(false , messageSource.getMessage(AppConstants.API_ERROR_ID_DOC_NULL, null, locale),null);
            }


            BusinessAccountModel businessAccountModel = new BusinessAccountModel();

            Subscriber subscriber = new Subscriber();


            if ("passport".equals(documentType))
            {
                subscriber=subscriberRepoIface.findByPassportNumber(passportNumber);
            }
            else if ("emirates_id".equals(documentType)) {
                subscriber=subscriberRepoIface.findByNationalIdNumber(passportNumber);

            }else if("suid".equals(documentType)){
                subscriber=subscriberRepoIface.findBySubscriberUid(passportNumber);
            }
            businessAccountModel.setSubscriberUId(subscriber.getSubscriberUid());
            businessAccountModel.setApplicantName(applicantName);

            businessAccountModel.setPassportNumber(passportNumber);

            businessAccountModel.setBankAccountOpeningForm(base64);

            businessAccountModel.setApplicationStatus(BuisinessAccountStatus.APPLIED.toString());
            businessAccountModel.setBankAccountHolder(applicantName);
            businessAccountModel.setCreatedOn(AppUtil.getDate());

            buisinessAccountRepo.save(businessAccountModel);


            sendNotification(subscriber.getSubscriberUid(),applicantName,MSG);

            Date endTime = AppUtil.getTimeStamp();
            logModelService.setLogModelDTO(true,subscriber.getSubscriberUid(),null, ServiceNames.OTHER.toString(),AppUtil.getUUId(),MSG,startTime,endTime,FALSE);


            return AppUtil.createApiResponse(true, messageSource.getMessage(AppConstants.API_RESPONSE_BANK_FORM_SUBMIT_SUCCESS, null, locale),null);

        } catch (Exception e) {
            logger.error("{}",e.getMessage());

            return  AppUtil.createApiResponse(false,  messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, null, locale), null);
        }

    }


    @Override
    public ApiResponse approveOrRejectBuisinessAccount(String passport, String status) {
        Locale locale = LocaleContextHolder.getLocale();
        try{
            Date startTime = AppUtil.getTimeStamp();
            if(passport.isEmpty()){
                return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_ID_DOC_NULL, null, locale),null);
            }
            if(status == null || status.isEmpty()){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_BANK_FORM_STATUS_NULL,null,locale),null);
            }

            BusinessAccountModel businessAccountModel = buisinessAccountRepo.getByPassportId(passport);
            String bankAccountNumber = getRandomNumber(11);
            if(businessAccountModel == null){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale),null);

            }
            if(status.equals("APPROVED")){
                businessAccountModel.setBankAccountNumber(bankAccountNumber);
                businessAccountModel.setUpdatedOn(AppUtil.getDate());
                businessAccountModel.setBankAccountStatus(BuisinessAccountStatus.ACTIVE.toString());
                businessAccountModel.setApplicationStatus(BuisinessAccountStatus.APPROVED.toString());
                buisinessAccountRepo.save(businessAccountModel);
                Date endTime = AppUtil.getTimeStamp();
                logModelService.setLogModelDTO(true,businessAccountModel.getSubscriberUId(),null, ServiceNames.OTHER.toString(),AppUtil.getUUId(),APPROVED,startTime,endTime,FALSE);

                return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_BANK_APPROVED,null,locale),null);

            } else if (status.equals("REJECTED")) {
                businessAccountModel.setUpdatedOn(AppUtil.getDate());
                businessAccountModel.setApplicationStatus(BuisinessAccountStatus.REJECTED.toString());
                buisinessAccountRepo.save(businessAccountModel);
                Date endTime = AppUtil.getTimeStamp();
                logModelService.setLogModelDTO(true,businessAccountModel.getSubscriberUId(),null, ServiceNames.OTHER.toString(),AppUtil.getUUId(),REJECTED,startTime,endTime,FALSE);

                return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_BANK_REJECTED,null,locale),null);
            }else{
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_INVALID_STATUS,null,locale),null);
            }

        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return  AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }

    @Override
    public ApiResponse getBuisinessAccountByPassport(String passportNumber) {
        Locale locale = LocaleContextHolder.getLocale();
        try{
            BusinessAccountModel businessAccountModel = buisinessAccountRepo.getByPassportId(passportNumber);
            if(businessAccountModel == null){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale),false);
            }
            return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS,null,locale),businessAccountModel);

        }catch (Exception e) {
            logger.error("{}", e.getMessage());
            return  AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }

    @Override
    public ApiResponse getAllBuisinessAccount() {
        Locale locale = LocaleContextHolder.getLocale();
        try{
            List<BusinessAccountModel> businessAccountModelList = buisinessAccountRepo.getAllData();
            List<BusinessAccountDto> businessAccountDtosList = new ArrayList<>();
            if(businessAccountModelList == null || businessAccountModelList.isEmpty()){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale),businessAccountModelList);
            }

            for(BusinessAccountModel i :businessAccountModelList ){
                BusinessAccountDto businessAccountDto = getBusinessAccountDto(i);
                businessAccountDtosList.add(businessAccountDto);

            }

            return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS,null,locale),businessAccountDtosList);

        }catch (Exception e) {
            logger.error("{}", e.getMessage());
            return  AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }

    private static BusinessAccountDto getBusinessAccountDto(BusinessAccountModel i) {
        BusinessAccountDto businessAccountDto = new BusinessAccountDto();
        businessAccountDto.setId(i.getId());
        businessAccountDto.setSubscriberUId(i.getSubscriberUId());
        businessAccountDto.setBankAccountHolder(i.getBankAccountHolder());
        businessAccountDto.setApplicantName(i.getApplicantName());
        businessAccountDto.setCompanyName(i.getCompanyName());
        businessAccountDto.setApplicantPhoto(i.getApplicantPhoto());
        businessAccountDto.setPassportNumber(i.getPassportNumber());
        businessAccountDto.setNationality(i.getNationality());
        businessAccountDto.setVisaNumber(i.getVisaNumber());
        businessAccountDto.setBankAccountOpeningJsonData(i.getBankAccountOpeningJsonData());
        businessAccountDto.setApplicationStatus(i.getApplicationStatus());
        businessAccountDto.setBankAccountNumber(i.getBankAccountNumber());
        businessAccountDto.setBankAccountStatus(i.getBankAccountStatus());
        businessAccountDto.setCreatedDate(i.getCreatedOn());
        return businessAccountDto;
    }

    @Override
    public ApiResponse getBuisinessAccountById(int id) {
        Locale locale = LocaleContextHolder.getLocale();
        try{
            BusinessAccountModel businessAccountModel = buisinessAccountRepo.getDetailsById(id);
            if(businessAccountModel == null){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale),false);
            }
            return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS,null,locale),businessAccountModel);

        }catch (Exception e) {
            logger.error("{}", e.getMessage());
            return  AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }



    public String getRandomNumber(int maxLength) {
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            StringBuilder otp = new StringBuilder(maxLength);

            for (int i = 0; i < maxLength; i++) {
                otp.append(secureRandom.nextInt(9));
            }
            return otp.toString();
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return null;
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
        NotificationDTO notificationBody = new NotificationDTO();
        NotificationDataDTO dataDTO = new NotificationDataDTO();
        NotificationContextDTO contextDTO = new NotificationContextDTO();
        notificationBody.setTo(subscriberFCMToken.getFcmToken());
        notificationBody.setPriority("high");

        Map<String, String> visaApprovedStatus = new HashMap<>();


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
        visaApprovedStatus.put("Bank", "Success");

        contextDTO.setPrefImmigrationAuthority(visaApprovedStatus);
        dataDTO.setNotificationContext(contextDTO);
        notificationBody.setData(dataDTO);
        return notificationBody;
    }


    @Override
    public ApiResponse approveRejectBankAccount(BankApproveRejectDto bankApproveRejectDto) {
        Locale locale = LocaleContextHolder.getLocale();
        try{
            BusinessAccountModel businessAccountModel = buisinessAccountRepo.getByPassportId(bankApproveRejectDto.getPassport());
            if(businessAccountModel == null){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale),null);
            }

            if(bankApproveRejectDto.getStatus().equals("APPROVED")) {

                businessAccountModel.setBankAccountNumber(getRandomNumber(11));
                businessAccountModel.setUpdatedOn(AppUtil.getDate());
                businessAccountModel.setBankAccountStatus(BuisinessAccountStatus.ACTIVE.toString());
                businessAccountModel.setApplicationStatus(BuisinessAccountStatus.APPROVED.toString());
                businessAccountModel.setExtendedEkyc(bankApproveRejectDto.getExtendedKyc().equals("yes"));
                businessAccountModel.setKycJson(bankApproveRejectDto.getKycJson());

                buisinessAccountRepo.save(businessAccountModel);

                Subscriber subscriber=subscriberRepoIface.findBySubscriberUid(bankApproveRejectDto.getPassport());

                Date end = AppUtil.getCurrentDate();
                Date start = AppUtil.getCurrentDate();
                logModelService.setLogModelDTO(true, subscriber.getSubscriberUid(), null, ServiceNames.OTHER.toString(), AppUtil.getUUId(), APPROVED, start, end, FALSE);
                sendNotification(subscriber.getSubscriberUid(),subscriber.getFullName(),APPROVED);
                return AppUtil.createApiResponse(true, messageSource.getMessage(AppConstants.API_RESPONSE_BANK_APPROVED,null,locale), null);
            }else{

                businessAccountModel.setUpdatedOn(AppUtil.getDate());
                businessAccountModel.setApplicationStatus(BuisinessAccountStatus.REJECTED.toString());
                businessAccountModel.setExtendedEkyc(bankApproveRejectDto.getExtendedKyc().equals("yes"));
                businessAccountModel.setKycJson(bankApproveRejectDto.getKycJson());
                buisinessAccountRepo.save(businessAccountModel);

                Subscriber subscriber=subscriberRepoIface.findBySubscriberUid(bankApproveRejectDto.getPassport());


                Date end = AppUtil.getCurrentDate();
                Date start = AppUtil.getCurrentDate();
                logModelService.setLogModelDTO(true, subscriber.getSubscriberUid(), null, ServiceNames.OTHER.toString(), AppUtil.getUUId(), REJECTED, start, end, FALSE);
                sendNotification(subscriber.getSubscriberUid(),subscriber.getFullName(),REJECTED);
                return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_BANK_REJECTED,null,locale),null);


            }
        }catch (Exception e){
            logger.error("Unexpected error occurred while processing request", e);
            return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),null);
        }
    }

    @Override
    public ApiResponse viewforApproveReject(int id) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            BusinessAccountModel businessAccountModel = buisinessAccountRepo.getDetailsById(id);
            if(businessAccountModel == null){
                return AppUtil.createApiResponse(false,messageSource.getMessage(AppConstants.API_ERROR_DATA_NOT_FOUND,null,locale),false);
            }
            ViewApproveRejectForBankDto viewApproveRejectForBankDto = new ViewApproveRejectForBankDto();


            if(businessAccountModel.isExtendedEkyc()) {

                String kycJsonString = businessAccountModel.getKycJson();

                ObjectMapper objectMapper = new ObjectMapper();

                JsonNode kycData = objectMapper.readTree(kycJsonString);

                viewApproveRejectForBankDto.setExtendedKyc(businessAccountModel.isExtendedEkyc());
                viewApproveRejectForBankDto.setKycReport(kycData.get("Ekyc_document").asText());
            }else{
                String kycJsonString = businessAccountModel.getKycJson();

                ObjectMapper objectMapper = new ObjectMapper();

                JsonNode kycData = objectMapper.readTree(kycJsonString);

                viewApproveRejectForBankDto.setExtendedKyc(businessAccountModel.isExtendedEkyc());
                viewApproveRejectForBankDto.setKycReport(kycData.get("kyc_document").asText());
            }


            return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS,null,locale),viewApproveRejectForBankDto);

        } catch (Exception e) {
            logger.info("{}",e.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);

        }
    }


    @Override
    public ApiResponse saveBankAccountOpeningWeb(String bankAccountDto) {
        Locale locale = LocaleContextHolder.getLocale();
        try{

            Date startTime = AppUtil.getTimeStamp();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode modelJson=objectMapper.readTree(bankAccountDto);



            String passportNumber= modelJson.path("idDocNumber").asText(null);
            String applicantName= modelJson.path("fullName").asText(null);

            String country = modelJson.path("nationality").asText(null);



            String photo =  modelJson.path("photo").asText(null);
            String pidDocument = modelJson.path("pidDocument").asText(null);


            String documentType =modelJson.path(DOCUMENT_TYPE).asText(null);

            Subscriber subscriber = new Subscriber();


            if ("passport".equals(documentType))
            {
                subscriber=subscriberRepoIface.findByPassportNumber(passportNumber);
            }
            else if ("emirates_id".equals(documentType)) {
                subscriber=subscriberRepoIface.findByNationalIdNumber(passportNumber);

            }else if("suid".equals(documentType)){
                subscriber=subscriberRepoIface.findBySubscriberUid(passportNumber);
            }


            if(passportNumber == null || passportNumber.isEmpty()){
                logger.info(" saveBuissinessBankAccount() Impl >> false >> passport number cannot be  cannot be null");
                return AppUtil.createApiResponse(false ,messageSource.getMessage(AppConstants.API_ERROR_ID_DOC_NULL,null,locale),null);
            }

            BusinessAccountModel businessAccountModel = new BusinessAccountModel();



            businessAccountModel.setSubscriberUId(subscriber.getSubscriberUid());
            businessAccountModel.setApplicantName(applicantName);


            businessAccountModel.setPassportNumber(passportNumber);
            businessAccountModel.setVisitorDocument(pidDocument);

            businessAccountModel.setNationality(country);
            businessAccountModel.setApplicantPhoto(photo);

            businessAccountModel.setApplicationStatus(BuisinessAccountStatus.APPLIED.toString());
            businessAccountModel.setBankAccountHolder(applicantName);
            businessAccountModel.setCreatedOn(AppUtil.getDate());

            FileSigningDto fileSigningDto = new FileSigningDto();
            fileSigningDto.setSuid(subscriber.getSubscriberUid());

            buisinessAccountRepo.save(businessAccountModel);


            sendNotification(subscriber.getSubscriberUid(),applicantName,AppConstants.API_RESPONSE_BANK_FORM_SUBMITTED);


            Date endTime = AppUtil.getTimeStamp();
            logModelService.setLogModelDTO(true,subscriber.getSubscriberUid(),null, ServiceNames.OTHER.toString(),AppUtil.getUUId(),MSG,startTime,endTime,FALSE);
            return AppUtil.createApiResponse(true,messageSource.getMessage(AppConstants.API_RESPONSE_BANK_FORM_SUBMIT_SUCCESS,null,locale),null);



        } catch (Exception e) {

            logger.error("{}",e.getMessage());

            return  AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }


    @Override
    public ApiResponse getUserProfile(BankRequestDto bankRequestDto) {
        Locale locale = LocaleContextHolder.getLocale();
        try{

            ApiResponse response1 = getAccessToken();
            if(!response1.isSuccess()){
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_ACCESS_TOKEN,null,locale),null);
            }

            String url1 = userProfileUrl;

            BankRequestDto bankRequestDto1 = new BankRequestDto();

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("UgPassAuthorization", "Bearer "+response1.getResult());



            bankRequestDto1.setUserId(bankRequestDto.getUserId());
            bankRequestDto1.setUserIdType(bankRequestDto.getUserIdType());
            bankRequestDto1.setProfileType(bankRequestDto.getProfileType());
            bankRequestDto1.setPurpose(bankRequestDto.getPurpose());

            HttpEntity<Object> reqEntity1 = new HttpEntity<>(bankRequestDto1, headers);

            String response = String.valueOf(restTemplate.exchange(url1, HttpMethod.POST, reqEntity1, String.class));
            logger.info("Response while calling URL :{} {}",url1,response);
            String jsonString = response.substring(response.indexOf("{"));
            ObjectMapper objectMapper = new ObjectMapper();
            ApiResponse apiResponse = objectMapper.readValue(jsonString,ApiResponse.class);
            if(!apiResponse.isSuccess()){
                return AppUtil.createApiResponse(false,apiResponse.getMessage(),null);
            }
            return AppUtil.createApiResponse(apiResponse.isSuccess(),apiResponse.getMessage(),apiResponse.getResult());

        } catch (Exception e) {
           logger.info("{}",e.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale) ,null);
        }

    }


    public ApiResponse getAccessToken(){
        Locale locale = LocaleContextHolder.getLocale();
        try{

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            String authHeaderVal=clientId+":"+clientSecret;
            String clientIdBase64= Base64.getEncoder().encodeToString(authHeaderVal.getBytes());
            headers.set("UgPassAuthorization", "Basic "+clientIdBase64);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "client_credentials");
            map.add("client_id", clientId);


            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.exchange(accessTokenUrl, HttpMethod.POST, entity,
                    String.class);
            logger.info("Response while calling  :{} {}",accessTokenUrl,response);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());


                if (root.has("error")) {

                    return AppUtil.createApiResponse(false, AppConstants.API_ERROR_ACCESS_TOKEN, null);
                }

                JsonNode accessToken = root.path("access_token");
                String parsedAccessToken = accessToken.asText();

                return AppUtil.createApiResponse(true, messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS,null,locale), parsedAccessToken);
            }
            else{
                return AppUtil.createApiResponse(false, AppConstants.API_ERROR_ACCESS_TOKEN, null);
            }

        }catch (Exception e){
            logger.info("{}",e.getMessage());
            return  AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }

    public ApiResponse fileSigning(FileSigningDto fileSigningDto) {
        Locale locale = LocaleContextHolder.getLocale();
        try{
            // Your existing map setup code remains the same...
            Map<String, Object> mapmodel = new HashMap<>();
            mapmodel.put("accountType" ,"self");
            mapmodel.put("accountId",fileSigningDto.getSuid());
            mapmodel.put(DOCUMENT_TYPE, "PADES");
            mapmodel.put("subscriberUniqueId" ,fileSigningDto.getSuid());
            mapmodel.put("organizationUid" ,null);
            mapmodel.put("qrCodeRequired" ,false);

            Map<String, Object> signPlaceHolderCoordinates = new HashMap<>();
            signPlaceHolderCoordinates.put("pageNumber", 1);
            signPlaceHolderCoordinates.put("signatureXaxis", xcoordinate);
            signPlaceHolderCoordinates.put("signatureYaxis", ycoordinate);
            signPlaceHolderCoordinates.put("imgWidth",126);
            signPlaceHolderCoordinates.put("imgHeight",30.09);

            mapmodel.put("placeHolderCoordinates",signPlaceHolderCoordinates);
            mapmodel.put("esealPlaceHolderCoordinates",null);
            mapmodel.put("deligationSign",null);
            mapmodel.put("recipientName",null);
            mapmodel.put("recipientEncryptedString",null);
            mapmodel.put("authPin",null);
            mapmodel.put("signPin",null);
            mapmodel.put("mobile",false);

            ObjectMapper objectMapper = new ObjectMapper();
            String model = objectMapper.writeValueAsString(mapmodel);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("model", model);

            String fullPath = pdfBasePath + pdfFileName;

            Path pdfPath = Paths.get(fullPath);

            if (!Files.exists(pdfPath)) {

                return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_FILE_NOT_FOUND,null,locale), null);
            }

            Resource resource = new FileSystemResource(pdfPath.toFile());

            body.add("file", resource);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<Object> reqEntity = new HttpEntity<>(body, headers);

            ResponseEntity<ApiResponse> response = restTemplate.exchange(filesign, HttpMethod.POST, reqEntity, ApiResponse.class);

            logger.info("FileSign API called. URL: {}, Status: {}", filesign, response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("FileSign API failed with HTTP status: {}", response.getStatusCode());
                return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
            }

            ApiResponse body1 = response.getBody();

            if (body1 == null) {
                logger.error("FileSign API returned null body");
                return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale) ,null
                );
            }

            if (!body1.isSuccess()) {
                return AppUtil.createApiResponse(false, body1.getMessage(), null);
            }

            return AppUtil.createApiResponse(
                    true, body1.getMessage(), body1.getResult()
            );

        } catch (Exception e) {
            logger.info("{}",e.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }

}

