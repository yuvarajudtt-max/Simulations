package com.dtt.simulations.service.Impl;

import com.dtt.simulations.constants.AppConstants;
import com.dtt.simulations.dto.*;
import com.dtt.simulations.model.Subscriber;
import com.dtt.simulations.model.SubscriberOnboardingData;
import com.dtt.simulations.repo.SubscriberOnboardingDataRepoIface;
import com.dtt.simulations.repo.SubscriberRepoIface;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;
import com.dtt.simulations.service.Iface.FaceVerificationIface;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.Locale;
import java.util.Optional;

@Service
public class FaceVerificationImpl implements FaceVerificationIface {

    private static final Logger logger = LoggerFactory.getLogger(FaceVerificationImpl.class);
    private final SubscriberRepoIface subscriberRepoIface;
    private final SubscriberOnboardingDataRepoIface onboardingRepo;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${subscriber.ob.data}") String subscriberObDataUrl;
    @Value("${verify.face}") String verifyFaceUrl;


    public FaceVerificationImpl(SubscriberRepoIface subscriberRepoIface,
                                SubscriberOnboardingDataRepoIface onboardingRepo,
                                MessageSource messageSource) {
        this.subscriberRepoIface = subscriberRepoIface;
        this.onboardingRepo = onboardingRepo;
        this.messageSource = messageSource;
    }

    @Override
    public ApiResponse verifyFace(VerifyFaceDto dto) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            if (isInvalid(dto.getCapturedImage()) || isInvalid(dto.getDocumentNumber())) {
                return errorResponse(AppConstants.API_ERROR_ID_DOC_NULL, locale);
            }

            Subscriber subscriber = subscriberRepoIface.getSubscriberDetail(dto.getDocumentNumber());
            SubscriberOnboardingData obData = onboardingRepo
                    .findTopByIdDocNumberIgnoreCaseOrderByCreatedDateDesc(dto.getDocumentNumber())
                    .orElse(null);

            if (subscriber == null || obData == null) {
                return errorResponse(AppConstants.API_ERROR_SUBSCRIBER_DETAILS_NOT_FOUND, locale);
            }

            FaceVerificationResponseDto responseDto = mapToResponseDto(subscriber, obData);

            String storedSelfie = fetchStoredSelfie(subscriber.getSubscriberUid());
            if (storedSelfie == null) {
                return errorResponse(AppConstants.API_ERROR_FACE_DATA_EMPTY, locale);
            }

            ApiResponse faceMatchResult = callFaceComparisonApi(storedSelfie, dto.getCapturedImage());
            if (faceMatchResult == null || !faceMatchResult.isSuccess()) {
                String msg = (faceMatchResult != null) ? faceMatchResult.getMessage() : getMsg(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, locale);
                return AppUtil.createApiResponse(false, msg, null);
            }

            responseDto.setUserPhoto(storedSelfie);
            return AppUtil.createApiResponse(true, getMsg(AppConstants.API_RESPONSE_FACE_VERIFIED_SUCCESS, locale), responseDto);

        } catch (Exception e) {
            logger.error("Face Verification Failed: {}", e.getMessage(), e);
            return errorResponse(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, locale);
        }
    }



    private boolean isInvalid(String str) {
        return str == null || str.isBlank();
    }

    private String getMsg(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

    private ApiResponse errorResponse(String key, Locale locale) {
        return AppUtil.createApiResponse(false, getMsg(key, locale), null);
    }

    private FaceVerificationResponseDto mapToResponseDto(Subscriber s, SubscriberOnboardingData ob) throws JsonProcessingException {
        FaceVerificationResponseDto dto = new FaceVerificationResponseDto();
        dto.setName(s.getFullName());
        dto.setEmail(s.getEmailId());
        dto.setMobileNumber(s.getMobileNumber());
        dto.setIdDocNumber(s.getIdDocNumber());
        dto.setDob(s.getDateOfBirth());
        dto.setGender(ob.getGender());

        JsonNode node = objectMapper.readTree(ob.getOnboardingDataFieldsJson());
        dto.setNationality(node.has("nationality") ? node.get("nationality").asText() : "N/A");
        return dto;
    }

    private String fetchStoredSelfie(String suid) {
        CheckSubscriberFaceDto req = new CheckSubscriberFaceDto();
        req.setSuid(suid);
        req.setSelfieRequired(true);

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                subscriberObDataUrl, HttpMethod.POST, new HttpEntity<>(req, createHeaders()), ApiResponse.class);

        return Optional.ofNullable(response.getBody())
                .filter(ApiResponse::isSuccess)
                .map(res -> objectMapper.convertValue(res.getResult(), CheckSubscriberFaceResponseDto.class))
                .map(data -> data.getSubscriberData().getSubscriberSelfie())
                .orElse(null);
    }

    private ApiResponse callFaceComparisonApi(String img1, String img2) {
        VerifyImageDto req = new VerifyImageDto();
        req.setImage1(img1);
        req.setImage2(img2);
        req.setLivenesscheck(false);

        HttpHeaders headers = createHeaders();
        headers.setBasicAuth("admin", "K9rzgr47wz");

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                verifyFaceUrl, HttpMethod.POST, new HttpEntity<>(req, headers), ApiResponse.class);

        return response.getBody();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.add("deviceId", "WEB");
        return headers;
    }
}