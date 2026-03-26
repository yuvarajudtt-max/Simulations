package com.dtt.simulations.POA.service.Impl;

import com.dtt.simulations.POA.dto.*;
import com.dtt.simulations.POA.model.*;
import com.dtt.simulations.POA.repo.*;
import com.dtt.simulations.POA.responseentity.ApiResponsePOA;
import com.dtt.simulations.POA.responseentity.ApiResponseW3c;
import com.dtt.simulations.POA.responseentity.AppUtilPOA;
import com.dtt.simulations.POA.service.Iface.PowerOfAttorneyIface;
import com.dtt.simulations.constants.AppConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.layout.element.Text;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import com.itextpdf.io.font.constants.StandardFonts;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;


@Service
@EnableScheduling
public class PowerOfAttorneyImpl implements PowerOfAttorneyIface {

    @Value("${poa.Coordinates}")
    public String poaCoordinates;

    private static final Logger logger = LoggerFactory.getLogger(PowerOfAttorneyImpl.class);

  private static final  String AGENT_NAME = "agentName";
  private static final   String SCOPE ="scope";


    @Value(value = "${create.request}")
    String createRequestUrl;

    @Value(value = "${fetch.verify}")
    String fetchVerifyUrl;
    @Value(value = "${notification}")
    String notificationUrl;

    @Value(value = "${api.access}")
    String accessApi;

    @Value(value = "${save.doc}")
    String saveDocUrl;

    @Value(value = "${doc.status}")
    String docStatus;

    @Value(value = "${edms.url}")
    String edmsUrl;

    @Value(value = "${org.id}")
    String orgId;

    @Value(value = "${org.name}")
    String orgName;

    @Value(value = "${pdf.embed}")
    String pdfEmbed;

    @Value(value = "${scope.list}")
    List<String> scopeList;



    private final PoaSubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface;
    private final PoaPowerOfAttorneyRepo powerOfAttorneyRepo;
    private final PoaSubscriberRepo subscriberRepo;
    private final PoaTemporaryPoaRepo temporaryPoaRepo;
    private final PoaOrgContactEmailRepo orgContactEmailRepo;
    private final PoaCredentialsRepo poaCredentialsRepo;
    private final PoaVisitorCompleteDetailsRepo visitorCompleteDetailsRepo;
    private final PoaRepositoryImpl poaRepository;
    private final MessageSource messageSource;




    public PowerOfAttorneyImpl(
            PoaSubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface,
            PoaPowerOfAttorneyRepo powerOfAttorneyRepo,
            PoaSubscriberRepo subscriberRepo,
            PoaTemporaryPoaRepo temporaryPoaRepo,
            PoaOrgContactEmailRepo orgContactEmailRepo,
            PoaCredentialsRepo poaCredentialsRepo,
            PoaVisitorCompleteDetailsRepo visitorCompleteDetailsRepo,
            PoaRepositoryImpl poaRepository,
            MessageSource messageSource) {

        this.subscriberFcmTokenRepoIface = subscriberFcmTokenRepoIface;
        this.powerOfAttorneyRepo = powerOfAttorneyRepo;
        this.subscriberRepo = subscriberRepo;
        this.temporaryPoaRepo = temporaryPoaRepo;
        this.orgContactEmailRepo = orgContactEmailRepo;
        this.poaCredentialsRepo = poaCredentialsRepo;
        this.visitorCompleteDetailsRepo = visitorCompleteDetailsRepo;
        this.poaRepository = poaRepository;
        this.messageSource = messageSource;
    }

    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();
    Locale locale = LocaleContextHolder.getLocale();
 

    @Override
    public ApiResponsePOA savePoaInTemp(SavePoaDto dto) {

        try {

            ApiResponsePOA validationResponse = validatePowerOfAttorney(dto, locale);
            if (!validationResponse.isSuccess()) {
                return validationResponse;
            }

            PoaSubscriber principal = subscriberRepo.fetchSubscriberByIDDocNumber(dto.getPrincipleIdDocNumber());
            PoaSubscriber agent = subscriberRepo.fetchSubscriberByIDDocNumber(dto.getAgentIdDocNumber());
            PoaSubscriber notary = subscriberRepo.fetchSubscriberByEmailId(dto.getNotaryEmail());

            PoaTemporary temporaryPoa = buildTemporaryEntity(dto, principal, agent, notary);

            String poaForm = generateAuthorizationForm(dto);
            temporaryPoa.setPoaRequestForm(poaForm);

            PoaTemporary savedPoa = temporaryPoaRepo.save(temporaryPoa);

            ApiResponsePOA signingResponse = initiateSigning(savedPoa.getId());
            if (!signingResponse.isSuccess()) {
                return signingResponse;
            }
            return successResponse(savedPoa);

        } catch (Exception e) {
            logger.error("savePoaInTemp Exception: {}", e.getMessage(), e);
            return buildError(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,locale);
        }
    }




    public ApiResponsePOA initiateSigning(int id) {
        try {

            PoaTemporary powerOfAttorney = temporaryPoaRepo.fetchById(id);
            if (powerOfAttorney.getStatus().equals("IN PROGRESS")) {
                return AppUtilPOA.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_SIGNING_ALREADY_INITIATED,null,locale), null);
            }
            powerOfAttorney.setStatus("IN PROGRESS");
            temporaryPoaRepo.save(powerOfAttorney);
            Thread thread = new Thread(() -> {

                try {
                    this.initiateSigningOriginal(id);
                } catch (Exception e) {
                    logger.error("{}",e.getMessage());
                }

            });
            thread.start();
            return AppUtilPOA.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_SIGNING_INITIATED_SUCCESS,null,locale), null);

        } catch (Exception e) {
            logger.error("{}",e.getMessage());
            return AppUtilPOA.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }



    public ApiResponsePOA initiateSigningOriginal(int id) {
        try {


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
            String oneYearLater = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1).format(formatter);

            PoaTemporary powerOfAttorney = temporaryPoaRepo.fetchById(id);
            Model model = new Model();
            model.setAction("send");
            model.setDisableOrder(false);
            model.setDocData("");
            model.setAllowToAssignSomeone(false);
            model.setWatermarkText("");
            model.setTempId("");
            model.setSignatureEnvironment("");
            model.setDocSerialNo("");
            ClassPathResource resource = new ClassPathResource("static/" + "MultiSign.pdf");
            File file = resource.getFile();

            model.setDocData("");
            DocDetails docDetails = getDocDetails(oneYearLater, powerOfAttorney);
            model.setDocDetails(docDetails);
            Map<String, EsealCordinates> esealCordinatesMap = getStringEsealCordinatesMap(powerOfAttorney);
            model.setEsealCords(esealCordinatesMap);
            Map<String, Coordinates> signCords = getStringCoordinatesMap(powerOfAttorney);
            model.setSignCords(signCords);
            model.setEntityName("");
            model.setMobile(false);
            model.setQrCords(null);
            model.setQrCodeRequired(false);
            model.setMultisign(true);
            model.setFileName(file.getName());

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            String base64 = powerOfAttorney.getPoaRequestForm();
            byte[] byteArray = Base64.getDecoder().decode(base64);

            Path testFile = Files.createTempFile("POA", ".pdf");

            Files.write(testFile, byteArray);

            model.setFileName("POA.pdf");

            String url = accessApi;
            GetAccessTokenDto getAccessTokenDto = new GetAccessTokenDto();
            getAccessTokenDto.setEmail(powerOfAttorney.getPrincipleEmail());
            getAccessTokenDto.setName(powerOfAttorney.getPrincipleName());
            getAccessTokenDto.setSuid(powerOfAttorney.getPrincipleSuid());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setBasicAuth("QT95Ix3sjFtY9jf6SnxnelXaqwUdJAXWoTI9RBEhVfk3oJdi", "sLHmG9J26omJmLfSzqWu8qByGi82nyaSIhPgs63gEU7h6D9TPDL3eYygHwPxjBmy");
            HttpEntity<Object> requestEntity = new HttpEntity<>(getAccessTokenDto, headers);

            ResponseEntity<ApiResponsePOA> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, ApiResponsePOA.class);

            ApiResponsePOA body = response.getBody();
            if (body == null || body.getResult() == null) {
                logger.error("API response or result is null. Cannot serialize.");
                return AppUtilPOA.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, null, locale), null);
            }


            String res = objectMapper.writeValueAsString(body.getResult());

            logger.info("Response from calling URL:::{} {}",accessApi,res);
            AccessTokenResponseDto accessTokenResponseDto = objectMapper.readValue(res, AccessTokenResponseDto.class);

            model.setActoken("");


            String mod = objectMapper.writeValueAsString(model);


            Files.write(testFile, byteArray);



            Resource resourcenew = new FileSystemResource(testFile.toFile());

            formData.add("file", resourcenew);
            formData.add("model", mod);
            String saveDoc = saveDocUrl;
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            httpHeaders.set("x-access-token", accessTokenResponseDto.getApiToken());
            HttpEntity<Object> requestEntityNew = new HttpEntity<>(formData, httpHeaders);
            ResponseEntity<ApiResponsePOA> responseNew = restTemplate.exchange(saveDoc, HttpMethod.POST, requestEntityNew, ApiResponsePOA.class);
            logger.info("Response from calling Document URL:::{} {}",accessApi,res);
            ApiResponsePOA body1 = responseNew.getBody();


            if (body1 == null || body1.getResult() == null) {
                logger.error("API Response or Result was null for URL: {}", url);
                return AppUtilPOA.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, null, locale), null);
            }

            String resNew = objectMapper.writeValueAsString(body.getResult());
            Result mappedResult = objectMapper.readValue(resNew, Result.class);



            powerOfAttorney.setDocId(mappedResult.getTempId());
            powerOfAttorney.setStatus("IN Progress");
            powerOfAttorney.setPoaId(id);
            temporaryPoaRepo.save(powerOfAttorney);


            NotificationDTOPOA notificationDTO = new NotificationDTOPOA();
            NotificationDataDTOPOA notificationDataDTO = getNotificationDataDTOPOA(powerOfAttorney);
            notificationDTO.setData(notificationDataDTO);
            notificationDTO.setPriority("high");
            notificationDTO.setTo(subscriberFcmTokenRepoIface.findBysubscriberUid(powerOfAttorney.getPrincipleSuid()).getFcmToken());
            HttpHeaders notifyHeaders = new HttpHeaders();
            notifyHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> requestNotify = new HttpEntity<>(notificationDTO, notifyHeaders);
            restTemplate.exchange(notificationUrl, HttpMethod.POST, requestNotify, Object.class);
            return AppUtilPOA.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_SIGNING_INITIATED_SUCCESS,null,locale), null);
        } catch (Exception e) {
            logger.info("{}",e.getMessage());
            return AppUtilPOA.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }

    private static NotificationDataDTOPOA getNotificationDataDTOPOA(PoaTemporary powerOfAttorney) {
        NotificationContextDTOPOA notificationContextDTO = new NotificationContextDTOPOA();
        Map<String, String> contextDto = new HashMap<>();
        contextDto.put("glitrchtip", "raised");
        notificationContextDTO.setPrefGlitchtip(contextDto);
        NotificationDataDTOPOA notificationDataDTO = new NotificationDataDTOPOA();
        notificationDataDTO.setBody("Your POA document is ready for signing. Please sign the document");
        notificationDataDTO.setTitle("Hi " + powerOfAttorney.getPrincipleName());
        notificationDataDTO.setNotificationContext(notificationContextDTO);
        return notificationDataDTO;
    }

    private Map<String, EsealCordinates> getStringEsealCordinatesMap(PoaTemporary powerOfAttorney) {
        Map<String, EsealCordinates> esealCordinatesMap = new HashMap<>();
        EsealCordinates esealCordinates = new EsealCordinates();
        esealCordinates.setFieldName("");
        esealCordinates.setOrganizationID(orgId);
        esealCordinates.setPosX(336.8);
        esealCordinates.setPosY(152.8);
        esealCordinates.setHeight(96);
        esealCordinates.setWidth(96);
        esealCordinates.setPageNumber(2);
        esealCordinatesMap.put(powerOfAttorney.getNotarySuid(), esealCordinates);
        return esealCordinatesMap;
    }

    private static Map<String, Coordinates> getStringCoordinatesMap(PoaTemporary powerOfAttorney) {
        Map<String, Coordinates> signCords = new HashMap<>();
        Coordinates principalCordinates = new Coordinates();
        principalCordinates.setFieldName("");
        principalCordinates.setHeight(45);
        principalCordinates.setWidth(174);
        principalCordinates.setPosX(72);
        principalCordinates.setPosY(540);
        principalCordinates.setPageNumber(1);
        signCords.put(powerOfAttorney.getPrincipleSuid(), principalCordinates);
        Coordinates agentCordinates = new Coordinates();
        agentCordinates.setFieldName("");
        agentCordinates.setHeight(45);
        agentCordinates.setWidth(169);
        agentCordinates.setPosX(74.4);
        agentCordinates.setPosY(104);

        agentCordinates.setPageNumber(2);
        signCords.put(powerOfAttorney.getAgentSuid(), agentCordinates);
        Coordinates notaryCordinates = new Coordinates();
        notaryCordinates.setFieldName("");
        notaryCordinates.setHeight(46);
        notaryCordinates.setWidth(169);
        notaryCordinates.setPosX(76.8);
        notaryCordinates.setPosY(199.2);
        notaryCordinates.setPageNumber(2);
        signCords.put(powerOfAttorney.getNotarySuid(), notaryCordinates);
        return signCords;
    }

    private DocDetails getDocDetails(String oneYearLater, PoaTemporary powerOfAttorney) {
        DocDetails docDetails = new DocDetails();
        docDetails.setAnnotations("");
        docDetails.setExpireDate(oneYearLater);
        docDetails.setOrgName("");
        docDetails.setAutoReminders("");
        docDetails.setDaysToComplete("2");
        docDetails.setOwnerName(powerOfAttorney.getPrincipleName());
        docDetails.setRemindEvery("");
        docDetails.setNoteToAll("");
        docDetails.setSignaturesRequiredCount(3);
        docDetails.setWatermark("");
        docDetails.setTempName("POA.pdf");
        Receps primary = new Receps();
        List<Receps> recepsList = new ArrayList<>();

        Receps agent = new Receps();
        Receps notary = new Receps();
        primary.setAllowComments(false);
        primary.setEmail(powerOfAttorney.getPrincipleEmail());
        primary.setSuid(powerOfAttorney.getPrincipleSuid());
        primary.setIndex("");
        primary.setOrder(1);
        primary.setAlternateSignatories("");
        List<User> alter = new ArrayList<>();
        primary.setAlternateSignatoriesList(alter);
        primary.setName(powerOfAttorney.getPrincipleName());
        primary.setEseal(false);
        primary.setSignedBy("");
        primary.setDelegationId("");
        primary.setOrgName("");
        primary.setSignatureMandatory(true);
        primary.setReferredTo("");
        primary.setReferredBy("");
        primary.setOrgUID("");
        primary.setHasDelegation(false);
        recepsList.add(primary);
        agent.setAllowComments(false);
        agent.setEmail(powerOfAttorney.getAgentEmail());
        agent.setSuid(powerOfAttorney.getAgentSuid());
        agent.setIndex("");
        agent.setOrder(2);
        agent.setAlternateSignatories("");
        agent.setAlternateSignatoriesList(alter);
        agent.setName(powerOfAttorney.getAgentName());
        agent.setEseal(false);
        agent.setSignedBy("");
        agent.setDelegationId("");
        agent.setOrgName("");
        agent.setSignatureMandatory(true);
        agent.setReferredTo("");
        agent.setReferredBy("");
        agent.setOrgUID("");
        agent.setHasDelegation(false);
        recepsList.add(agent);
        notary.setAllowComments(false);
        notary.setEmail(powerOfAttorney.getNotaryEmail());
        notary.setSuid(powerOfAttorney.getNotarySuid());
        notary.setIndex("");
        notary.setOrder(3);
        notary.setAlternateSignatories("");
        notary.setAlternateSignatoriesList(alter);
        notary.setName(powerOfAttorney.getNotaryName());
        notary.setEseal(true);
        notary.setSignedBy("");
        notary.setDelegationId("");
        notary.setOrgName(orgName);
        notary.setSignatureMandatory(true);
        notary.setReferredTo("");
        notary.setReferredBy("");
        notary.setOrgUID(orgId);
        notary.setHasDelegation(false);
        recepsList.add(notary);
        docDetails.setReceps(recepsList);
        return docDetails;
    }



    @Override
    public ApiResponsePOA getStatus(int id) {
        try{
            PoaTemporary temporaryPoa = temporaryPoaRepo.fetchById(id);
            if(temporaryPoa.getStatus().equals("Completed")){
                return AppUtilPOA.createApiResponse(true,"Notarization done",null);

            }
            else{
                return AppUtilPOA.createApiResponse(false,"Not Completed",null);
            }
        }catch (Exception e){
            logger.info("{}",e.getMessage());
            return AppUtilPOA.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),null);
        }
    }


    @Override
    public ApiResponsePOA getNotaryAndScopeInformation() {
        try {
            List<PoaOrgContactsEmail> orgContactsEmailList = orgContactEmailRepo.fetchAllEmployees(orgId);
            List<NotaryDto> notaryDtoList = new ArrayList<>();
            for (PoaOrgContactsEmail orgContactsEmail : orgContactsEmailList) {
                NotaryDto notaryDto = new NotaryDto();
                notaryDto.setNotaryEmployeeEmail(orgContactsEmail.getEmployeeEmail());
                notaryDto.setNotarySuid(orgContactsEmail.getSubscriberUid());
                notaryDto.setNotaryUaeIdEmail(orgContactsEmail.getUgpassEmail());

                notaryDtoList.add(notaryDto);
            }
            NotaryResponseDto notaryResponseDto = new NotaryResponseDto();
            notaryResponseDto.setNotaryDtoList(notaryDtoList);
            notaryResponseDto.setScope(scopeList);
            return AppUtilPOA.createApiResponse(true, "Fetched Notary Response Successfully", notaryResponseDto);

        } catch (Exception e) {
            logger.info("{}",e.getMessage());
            return AppUtilPOA.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }
    }


    @Override
    public ApiResponsePOA transferPoa(String agentEmail) {
        try {
            String url = createRequestUrl;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            POACredentialRequests poaCredentialRequests = new POACredentialRequests();
            poaCredentialRequests.setType("POACredentialRequest");
            poaCredentialRequests.setScope("POACredentialRequest");
            poaCredentialRequests.setClientId("");
            SelectedClaims selectedClaims = new SelectedClaims();
            selectedClaims.setDocument(Arrays.asList(
                    "principleEmail", "principleName", "principleIdDocNumber",
                    "principleSuid", "agentEmail", AGENT_NAME, "agentSuid",
                    "agentIdDocNumber", "notarySuid", "notaryIdDocNumber",
                    "notaryEmail", "poaDocSigned", SCOPE, "notaryName", "delegationUpto","photo"
            ));
            poaCredentialRequests.setSelectedClaims(selectedClaims);
            HttpEntity<Object> requestEntity = new HttpEntity<>(poaCredentialRequests, headers);
            ResponseEntity<ApiResponseW3c> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, ApiResponseW3c.class);

            ApiResponseW3c body = response.getBody();

            if (body == null) {
                logger.error("API Body is null for URL: {}", url);
                return AppUtilPOA.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, null, locale), null);
            }

            Object result = body.getResult();

            if (result == null || result.toString().isBlank()) {
                logger.error("API Result payload is null or blank for URL: {}", url);
                return AppUtilPOA.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_RESPONSE_BODY_EMPTY, null, locale), null);
            }



            List<String> parts = Arrays.asList(result.toString().split("/"));
            String transactionId = parts.getLast();
            PoaSubscriber subscriber = subscriberRepo.fetchSubscriberByEmailId(agentEmail);
            Thread thread = new Thread(() -> {

                try {
                    verifyCredentials(transactionId, agentEmail,
                            subscriber.getSubscriberUid(),
                            subscriber.getIdDocNumber(),
                            subscriber.getFullName());
                } catch (InterruptedException | JsonProcessingException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Thread interrupted during credential verification", e);
                }

            });
            thread.start();

            ApiResponseW3c body1 = response.getBody();
            Object resultData = (body1 != null) ? body1.getResult() : null;


            if (resultData == null) {
                logger.error("API body or result payload was null for URL: {}", url);
                return AppUtilPOA.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, null, locale), null);
            }


            return AppUtilPOA.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_TRANSFER_IN_PROGRESS, null, locale),
                    resultData.toString());
        } catch (Exception e) {
            logger.info("{}",e.getMessage());
            return AppUtilPOA.createApiResponse(false,
                  messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale), null);
        }

    }


    public void verifyCredentials(String transactionId, String email, String suid, String idDoc, String name) throws InterruptedException, JsonProcessingException {
        String url = fetchVerifyUrl + "/" + transactionId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String>response;
        boolean success = false;
        do {


            response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);


            String jsonResponse = response.getBody();

            ApiResponseW3c apiResponse = objectMapper.readValue(jsonResponse, ApiResponseW3c.class);

            if (apiResponse.isSuccess()) {

                success = true;


                String res = objectMapper.writeValueAsString(apiResponse.getResult());

                W3cResult w3cResult = objectMapper.readValue(res, W3cResult.class);

                PoaCredentials poaCredentialsDb = poaCredentialsRepo.fetchByEmail(email);
                if (poaCredentialsDb != null) {

                    PoaVisitorCompleteDetails visitorCompleteDetails = visitorCompleteDetailsRepo.fetchDetails(suid);


                    HttpHeaders head = new HttpHeaders();
                    HttpEntity<Object> request = new HttpEntity<>(head);
                    ResponseEntity<byte[]> resp = restTemplate.exchange(visitorCompleteDetails.getSelfieUri(), HttpMethod.GET, request, byte[].class);
                    String base64 = AppUtilPOA.getBase64FromByteArr(resp.getBody());
                    poaCredentialsDb.setAgentPhoto(base64);
                    poaCredentialsDb.setAgentIdDocNumber(idDoc);
                    poaCredentialsDb.setAgentName(name);
                    poaCredentialsDb.setAgentSuid(suid);
                    poaCredentialsDb.setPoaCredential(w3cResult.getVpToken());
                    poaCredentialsRepo.save(poaCredentialsDb);
                } else {
                    PoaCredentials poaCredentials = new PoaCredentials();

                    PoaVisitorCompleteDetails visitorCompleteDetails = visitorCompleteDetailsRepo.fetchDetails(suid);
                    HttpHeaders head = new HttpHeaders();
                    HttpEntity<Object> request1 = new HttpEntity<>(head);
                    ResponseEntity<byte[]> resp = restTemplate.exchange(visitorCompleteDetails.getSelfieUri(), HttpMethod.GET, request1, byte[].class);



                    String base64 = AppUtilPOA.getBase64FromByteArr(resp.getBody());



                    poaCredentials.setAgentPhoto(base64);

                    poaCredentials.setAgentSuid(suid);
                    poaCredentials.setPoaCredential(w3cResult.getVpToken());
                    poaCredentials.setAgentEmail(email);
                    poaCredentials.setAgentIdDocNumber(idDoc);
                    poaCredentials.setAgentName(name);
                    poaCredentials.setAgentSuid(suid);
                    poaCredentials.setCreatedOn(AppUtilPOA.getDate());
                    poaCredentials.setUpdatedOn(AppUtilPOA.getDate());
                    poaCredentials.setStatus("APPROVED");
                    poaCredentialsRepo.save(poaCredentials);
                }

                PoaSubscriberFcmToken subscriberFcmToken = subscriberFcmTokenRepoIface.findBysubscriberUid(suid);

                NotificationDTOPOA notificationDTO = getNotificationDTOPOA("Provision POACredential", name, subscriberFcmToken);
                HttpHeaders notifyHeaders = new HttpHeaders();
                notifyHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> requestNotify = new HttpEntity<>(notificationDTO, notifyHeaders);
                restTemplate.exchange(notificationUrl, HttpMethod.POST, requestNotify, Object.class);
            } else {

                Thread.sleep(2000);
            }
        } while (!success);
    }

    private static NotificationDTOPOA getNotificationDTOPOA(String provisionPoaCredential, String name, PoaSubscriberFcmToken subscriberFcmToken) {
        NotificationDTOPOA notificationDTO = new NotificationDTOPOA();
        NotificationContextDTOPOA notificationContextDTO = new NotificationContextDTOPOA();
        NotificationDataDTOPOA notificationDataDTO = new NotificationDataDTOPOA();
        notificationDataDTO.setBody(provisionPoaCredential);
        notificationDataDTO.setTitle("Hi " + name);
        notificationDataDTO.setNotificationContext(notificationContextDTO);
        Map<String, String> contextDto = new HashMap<>();
        contextDto.put("glitrchtip", "raised");
        notificationContextDTO.setPrefGlitchtip(contextDto);
        notificationDTO.setData(notificationDataDTO);
        notificationDTO.setPriority("high");
        notificationDTO.setTo(subscriberFcmToken.getFcmToken());
        return notificationDTO;
    }


    @Transactional
    public ApiResponsePOA resetPoaData() {
        try {
            logger.info("Starting POA data reset process...");

            String result = poaRepository.deleteFromPoaTables();

            logger.info("POA data reset completed successfully: {}", result);

            return AppUtilPOA.createApiResponse(true, result, null);

        } catch (Exception e) {
            logger.error("Error during POA data reset: {}", e.getMessage(), e);
            return AppUtilPOA.createApiResponse(false, "Error occurred during data reset", e.getMessage());
        }
    }


    @Override
    public ApiResponsePOA getAllPoaCredentialRequests(String principalDocumentNumber) {
        try {

            PowerOfAttorney powerOfAttorney = powerOfAttorneyRepo.fetchApprovedPoaRequests(principalDocumentNumber);

            if(powerOfAttorney == null){
                return AppUtilPOA.createApiResponse(false, "No user found",null);
            }


            PoaCredentialsRequestDto dto = new PoaCredentialsRequestDto();

            dto.setPrincipleEmail(powerOfAttorney.getPrincipleEmail());
            dto.setPrincipleName(powerOfAttorney.getPrincipleName());
            dto.setAgentEmail(powerOfAttorney.getAgentEmail());
            dto.setAgentName(powerOfAttorney.getAgentName());

            dto.setDelegationUpto(powerOfAttorney.getEffectiveFrom());
            dto.setStatus(powerOfAttorney.getStatus());
            dto.setPoaRequestForm(powerOfAttorney.getPoaRequestForm());
            dto.setPrincipleIdDocNumber(powerOfAttorney.getPrincipleIdDocNumber());
            dto.setPrincipleSuid(powerOfAttorney.getPrincipleSuid());
            dto.setAgentIdDocNumber(powerOfAttorney.getAgentIdDocNumber());
            dto.setAgentSuid(powerOfAttorney.getAgentSuid());
            dto.setNotaryName(powerOfAttorney.getNotaryName());
            dto.setNotaryIdDocNumber(powerOfAttorney.getNotaryIdDocNumber());
            dto.setNotaryEmail(powerOfAttorney.getNotaryEmail());
            dto.setNotarySuid(powerOfAttorney.getNotarySuid());
            dto.setScope(powerOfAttorney.getScope());
            dto.setPoaDocSigned(powerOfAttorney.getPoaDocSigned());
            dto.setCreatedOn(powerOfAttorney.getCreatedOn());
            dto.setUpdatedOn(powerOfAttorney.getUpdatedOn());
            dto.setPhoto(powerOfAttorney.getPrincipalPhoto());
            return AppUtilPOA.createApiResponse(true, "fetched successfully", dto);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            return AppUtilPOA.createApiResponse(false, "Something went wrong", null);
        }
    }


    @Override
    public ApiResponsePOA getAllPoaCredentials(String agentDocumentNumber) {
        try {
            PoaCredentials poaCredentials = poaCredentialsRepo.fetchByDocNumber(agentDocumentNumber);

            if(poaCredentials ==null){
                return AppUtilPOA.createApiResponse(false, "No details found", null);
            }

            PoaCredentialsDto dto = new PoaCredentialsDto();

            dto.setAgentSuid(poaCredentials.getAgentSuid());
            dto.setAgentName(poaCredentials.getAgentName());
            dto.setAgentIdDocNumber(poaCredentials.getAgentIdDocNumber());
            dto.setPoaCredential(poaCredentials.getPoaCredential());
            dto.setAgentEmail(poaCredentials.getAgentEmail());
            dto.setCreatedOn(poaCredentials.getCreatedOn());
            dto.setUpdatedOn(poaCredentials.getUpdatedOn());
            dto.setStatus(poaCredentials.getStatus());
            dto.setPhoto(poaCredentials.getAgentPhoto());

            return AppUtilPOA.createApiResponse(true, "fetched successfully", dto);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while processing request", e);
            return AppUtilPOA.createApiResponse(false, "Something went wrong", null);
        }
    }


    @Override
    public String modifypoa(PowerOfAttorneyDto dto) throws IOException {

        ClassPathResource resource = new ClassPathResource("poa.pdf");
        byte[] existingPdfBytes = Files.readAllBytes(Paths.get(resource.getURI()));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(existingPdfBytes));
        PdfWriter pdfWriter = new PdfWriter(outputStream);

        try (PdfDocument pdfDocument = new PdfDocument(pdfReader, pdfWriter)) {

            JsonNode jsonNode = jsonConversion(poaCoordinates);

            writeText(pdfDocument,
                    dto.getPrincipalName(),
                    getX(jsonNode, "principalName"),
                    getY(jsonNode, "principalName"));

            writeText(pdfDocument,
                    dto.getAgentName(),
                    getX(jsonNode, AGENT_NAME),
                    getY(jsonNode, AGENT_NAME));

            writeText(pdfDocument,
                    dto.getScope(),
                    getX(jsonNode, SCOPE),
                    getY(jsonNode, SCOPE));

            writeText(pdfDocument,
                    dto.getStartDate(),
                    getX(jsonNode, "startDate"),
                    getY(jsonNode, "startDate"));

            writeText(pdfDocument,
                    dto.getEndDate(),
                    getX(jsonNode, "endDate"),
                    getY(jsonNode, "endDate"));

            writeText(pdfDocument, dto.getAgentName(), 120, 93);

        }

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }


    public JsonNode jsonConversion(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);

        } catch (Exception e) {
            logger.info("{}",e.getMessage());
        }
        return null;
    }




    @Override
    public ApiResponsePOA savePoa(SavePoaDto savePoaDto) {
        try {

            ApiResponsePOA validationResponse = validatePowerOfAttorney(savePoaDto, locale);
            if (!validationResponse.isSuccess()) {
                return validationResponse;
            }

            PoaSubscriber principal = subscriberRepo
                    .fetchSubscriberByIDDocNumber(savePoaDto.getPrincipleIdDocNumber());

            PoaSubscriber agent = subscriberRepo
                    .fetchSubscriberByIDDocNumber(savePoaDto.getAgentIdDocNumber());

            PoaSubscriber notary = subscriberRepo
                    .fetchSubscriberByEmailId(savePoaDto.getNotaryEmail());



            PowerOfAttorney powerOfAttorney = new PowerOfAttorney();
            powerOfAttorney.setPrincipleEmail(powerOfAttorney.getPrincipleEmail());
            powerOfAttorney.setPrincipleIdDocNumber(savePoaDto.getPrincipleIdDocNumber());
            powerOfAttorney.setPrincipleName(principal.getFullName());
            powerOfAttorney.setPrincipleSuid(principal.getSubscriberUid());



            powerOfAttorney.setAgentEmail(savePoaDto.getAgentEmail());
            powerOfAttorney.setAgentIdDocNumber(savePoaDto.getAgentIdDocNumber());
            powerOfAttorney.setAgentName(agent.getFullName());
            powerOfAttorney.setAgentSuid(agent.getSubscriberUid());


            powerOfAttorney.setNotaryEmail(savePoaDto.getNotaryEmail());
            powerOfAttorney.setNotaryIdDocNumber(savePoaDto.getNotaryIdDocNumber());
            powerOfAttorney.setNotaryName(notary.getFullName());
            powerOfAttorney.setNotarySuid(notary.getSubscriberUid());

            powerOfAttorney.setEffectiveFrom(savePoaDto.getEffectiveFrom());

            powerOfAttorney.setScope(savePoaDto.getScope());

            powerOfAttorney.setCreatedOn(AppUtilPOA.getDate());
            powerOfAttorney.setUpdatedOn(AppUtilPOA.getDate());

            powerOfAttorneyRepo.save(powerOfAttorney);

            PoaSubscriberFcmToken subscriberFcmToken = subscriberFcmTokenRepoIface.findBysubscriberUid(principal.getSubscriberUid());
            NotificationDTOPOA notificationDTO = getNotificationDTOPOA("POA has been approved. Provision your poa credentials request document", principal.getFullName(), subscriberFcmToken);
            HttpHeaders notifyHeaders = new HttpHeaders();
            notifyHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> requestNotify = new HttpEntity<>(notificationDTO, notifyHeaders);
            restTemplate.exchange(notificationUrl, HttpMethod.POST, requestNotify, Object.class);
            return AppUtilPOA.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_REQUEST_SUBMITTED_SUCCESS,null,locale), null);

        } catch (Exception e) {
            logger.info("{}",e.getMessage());
            return AppUtilPOA.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG,null,locale),null);

        }
    }



    private ApiResponsePOA buildError(String message,Locale locale) {
        return AppUtilPOA.createApiResponse(
                false,
                messageSource.getMessage(message, null, locale),
                null
        );
    }

    private PoaTemporary buildTemporaryEntity(SavePoaDto dto,
                                              PoaSubscriber principal,
                                              PoaSubscriber agent,
                                              PoaSubscriber notary) {

        PoaTemporary temp = new PoaTemporary();

        temp.setPrincipleEmail(principal.getEmailId());
        temp.setPrincipleIdDocNumber(principal.getIdDocNumber());
        temp.setPrincipleName(principal.getFullName());
        temp.setPrincipleSuid(principal.getSubscriberUid());

        temp.setAgentEmail(agent.getEmailId());
        temp.setAgentIdDocNumber(agent.getIdDocNumber());
        temp.setAgentName(agent.getFullName());
        temp.setAgentSuid(agent.getSubscriberUid());

        temp.setNotaryEmail(notary.getEmailId());
        temp.setNotaryIdDocNumber(notary.getIdDocNumber());
        temp.setNotaryName(notary.getFullName());
        temp.setNotarySuid(notary.getSubscriberUid());

        temp.setEffectiveDate(dto.getEffectiveFrom());
        temp.setScope(dto.getScope());
        temp.setCreatedOn(AppUtilPOA.getDate());
        temp.setUpdatedOn(AppUtilPOA.getDate());
        temp.setStatus("APPLIED");

        return temp;
    }
    private ApiResponsePOA successResponse(Object data) {
        return AppUtilPOA.createApiResponse(
                true,
                messageSource.getMessage(AppConstants.API_RESPONSE_REQUEST_SUBMITTED_SUCCESS, null, locale),
                data
        );
    }
    private String generateAuthorizationForm(SavePoaDto dto) throws IOException {

        PowerOfAttorneyDto authorizationDetails = new PowerOfAttorneyDto();
        authorizationDetails.setEndDate(dto.getEffectiveFrom());
        authorizationDetails.setStartDate(AppUtilPOA.getDate().substring(0, 10));
        authorizationDetails.setScope(dto.getScope());
        authorizationDetails.setAgentName(dto.getAgentName());
        authorizationDetails.setPrincipalName(dto.getPrincipleName());

        return modifypoa(authorizationDetails);
    }

    private ApiResponsePOA validateField(String value, String message,Locale locale) {
        if (value == null || value.isBlank()) {
            return buildError(message,locale);
        }
        return null;
    }

    public ApiResponsePOA validatePowerOfAttorney(SavePoaDto powerOfAttorney, Locale locale) {

        String principalEmail = powerOfAttorney.getPrincipleEmail();
        String agentEmail = powerOfAttorney.getAgentEmail();
        String notaryEmail = powerOfAttorney.getNotaryEmail();
        String principalName = powerOfAttorney.getPrincipleName();
        String agentName = powerOfAttorney.getAgentName();
        String principalId = powerOfAttorney.getPrincipleIdDocNumber();
        String agentId = powerOfAttorney.getAgentIdDocNumber();

        ApiResponsePOA response;


        if ((response = validateField(principalEmail,
                AppConstants.API_ERROR_PRINCIPAL_EMAIL_REQUIRED, locale)) != null) return response;

        if ((response = validateField(agentEmail,
                AppConstants.API_ERROR_AGENT_EMAIL_REQUIRED, locale)) != null) return response;

        if ((response = validateField(notaryEmail,
                AppConstants.API_ERROR_NOTARY_EMAIL_REQUIRED, locale)) != null) return response;

        if ((response = validateField(principalName,
                AppConstants.API_ERROR_PRINCIPAL_NAME_REQUIRED, locale)) != null) return response;

        if ((response = validateField(agentName,
                AppConstants.API_ERROR_AGENT_NAME_REQUIRED, locale)) != null) return response;

        if ((response = validateField(principalId,
                AppConstants.API_ERROR_PRINCIPAL_ID_REQUIRED, locale)) != null) return response;

        if ((response = validateField(agentId,
                AppConstants.API_ERROR_AGENT_ID_REQUIRED, locale)) != null) return response;


        PoaSubscriber principal = subscriberRepo.fetchSubscriberByIDDocNumber(principalId);
        PoaSubscriber agent = subscriberRepo.fetchSubscriberByIDDocNumber(agentId);
        PoaSubscriber notary = subscriberRepo.fetchSubscriberByEmailId(notaryEmail);

        if (principal == null)
            return buildError(AppConstants.API_ERROR_PRINCIPAL_NOT_FOUND, locale);

        if (agent == null)
            return buildError(AppConstants.API_ERROR_AGENT_NOT_FOUND, locale);

        if (notary == null)
            return buildError(AppConstants.API_ERROR_NOTARY_NOT_FOUND, locale);


        if (!principal.getIdDocNumber().equals(principalId))
            return buildError(AppConstants.API_ERROR_PRINCIPAL_ID_MISMATCH, locale);

        if (!agent.getIdDocNumber().trim().equals(agentId.trim()))
            return buildError(AppConstants.API_ERROR_AGENT_ID_MISMATCH, locale);

        if (principalEmail.equals(agentEmail))
            return buildError(AppConstants.API_ERROR_PRINCIPAL_AGENT_SAME, locale);

        if (principalEmail.equals(notaryEmail))
            return buildError(AppConstants.API_ERROR_PRINCIPAL_NOTARY_SAME, locale);

        if (agentEmail.equals(notaryEmail))
            return buildError(AppConstants.API_ERROR_AGENT_NOTARY_SAME, locale);


        return AppUtilPOA.createApiResponse(
                true,
                messageSource.getMessage(AppConstants.API_RESPONSE_REQUEST_SUBMITTED_SUCCESS, null, locale),
                null
        );
    }

    private float getX(JsonNode node, String fieldName) {
        String[] parts = node.get(fieldName).asText().split(",");
        return Float.parseFloat(parts[0]);
    }

    private float getY(JsonNode node, String fieldName) {
        String[] parts = node.get(fieldName).asText().split(",");
        return Float.parseFloat(parts[1]);
    }

    private void writeText(PdfDocument pdfDocument,
                           String value,
                           float x,
                           float y) throws IOException {

        PdfCanvas pdfCanvas = new PdfCanvas(pdfDocument.getPage(1));

        Rectangle rectangle = new Rectangle(x, y, 1000, 50);

        try (Canvas canvas = new Canvas(pdfCanvas, rectangle)) {

            Text pdfText = new Text(value.toUpperCase())
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(11)
                    .setFontColor(ColorConstants.BLACK);

            Paragraph paragraph = new Paragraph(pdfText)
                    .setWidth(500)
                    .setMultipliedLeading(1);

            canvas.add(paragraph);
        }
    }

}
