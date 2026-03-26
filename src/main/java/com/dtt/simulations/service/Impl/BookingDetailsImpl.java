
package com.dtt.simulations.service.Impl;

import com.dtt.simulations.constants.AppConstants;
import com.dtt.simulations.dto.BookingDetailsDto;
import com.dtt.simulations.dto.NotificationContextDTO;
import com.dtt.simulations.dto.NotificationDTO;
import com.dtt.simulations.dto.NotificationDataDTO;
import com.dtt.simulations.enums.ServiceNames;
import com.dtt.simulations.model.HotelSimulator;
import com.dtt.simulations.model.Subscriber;
import com.dtt.simulations.model.SubscriberFCMToken;
import com.dtt.simulations.repo.*;
import com.dtt.simulations.responseentity.ApiResponse;
import com.dtt.simulations.responseentity.AppUtil;
import com.dtt.simulations.service.Iface.BookingDetailsIface;
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
public class BookingDetailsImpl implements BookingDetailsIface {

    private final HotelSimulatorRepo hotelSimulatorRepo;
    private final SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface;
    private final SubscriberRepoIface subscriberRepoIface;
    private final MessageSource messageSource;
    private final LogModelServiceImpl logModelService;

    public BookingDetailsImpl(
            HotelSimulatorRepo hotelSimulatorRepo,
            SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface,
            SubscriberRepoIface subscriberRepoIface,
            MessageSource messageSource,
            LogModelServiceImpl logModelService) {

        this.hotelSimulatorRepo = hotelSimulatorRepo;
        this.subscriberFcmTokenRepoIface = subscriberFcmTokenRepoIface;
        this.subscriberRepoIface = subscriberRepoIface;
        this.messageSource = messageSource;
        this.logModelService = logModelService;
    }


    @Value("${notify.url}")
    public String notifyUrl;

    Logger logger = LoggerFactory.getLogger(BookingDetailsImpl.class);

    Random random = new Random();

    @Override
    public ApiResponse saveBookingDetails(BookingDetailsDto bookingDetailsDto) {
        Locale locale = LocaleContextHolder.getLocale();
        try {

            Date start = AppUtil.getCurrentDate();

            if (bookingDetailsDto == null) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_DTO_NULL, null, locale),
                        null);
            }

            if (bookingDetailsDto.getIdDocNumber() == null || bookingDetailsDto.getIdDocNumber().isEmpty()) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_ID_DOC_NULL, null, locale),
                        null);
            }

            if (bookingDetailsDto.getDocumentType() == null || bookingDetailsDto.getDocumentType().isEmpty()) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage(AppConstants.API_ERROR_DOCUMENT_TYPE_NULL, null, locale),
                        null);
            }

            HotelSimulator hotelSimulator = new HotelSimulator();
            hotelSimulator.setName(bookingDetailsDto.getFullName());
            hotelSimulator.setGender(bookingDetailsDto.getGender());
            hotelSimulator.setPhoto(bookingDetailsDto.getPhoto());
            hotelSimulator.setDateOfBirth(bookingDetailsDto.getDateOfBirth());
            hotelSimulator.setDocumentNumber(bookingDetailsDto.getIdDocNumber());


            int randomNumber = random.nextInt(1000) + 1;
            hotelSimulator.setRoomAllocated(String.valueOf(randomNumber));
            hotelSimulator.setCreationDate(AppUtil.getDate());
            hotelSimulatorRepo.save(hotelSimulator);

            Subscriber subscriber;
            if ("passport".equals(bookingDetailsDto.getDocumentType())) {
                subscriber = subscriberRepoIface.findByPassportNumber(bookingDetailsDto.getIdDocNumber());
            } else if ("emirates_id".equals(bookingDetailsDto.getDocumentType())) {
                subscriber = subscriberRepoIface.findByNationalIdNumber(bookingDetailsDto.getIdDocNumber());
            } else {
                subscriber = subscriberRepoIface.findBySubscriberUid(bookingDetailsDto.getIdDocNumber());
            }

            SubscriberFCMToken subscriberFcmToken =
                    subscriberFcmTokenRepoIface.findBysuid(subscriber.getSubscriberUid());

            NotificationDTO notificationBody = getNotificationDTO(subscriberFcmToken, subscriber);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> requestEntity = new HttpEntity<>(notificationBody, headers);


            RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> res = restTemplate.exchange(
                        notifyUrl, HttpMethod.POST, requestEntity, String.class);

                if (res.getStatusCode().is2xxSuccessful()) {
                    logger.info("Notification sent:{} ",res.getBody());
                } else {
                    logger.info("Notification failed: {}" , res.getStatusCode());
                }


            Date end = AppUtil.getCurrentDate();
            logModelService.setLogModelDTO(true, subscriber.getSubscriberUid(), null,
                    ServiceNames.OTHER.toString(), AppUtil.getUUId(),
                    "Hotel check-in completed", start, end, "false");

            return AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_BOOKING_SUCCESS, null, locale),
                    String.valueOf(randomNumber));

        } catch (Exception e) {
            logger.error("{}",e.getMessage());
            return AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, null, locale),
                    null);
        }
    }

    public NotificationDTO  getNotificationDTO(SubscriberFCMToken subscriberFcmToken, Subscriber subscriber) {
        Locale locale = LocaleContextHolder.getLocale();
        NotificationDataDTO dataDTO = new NotificationDataDTO();
        NotificationContextDTO contextDTO = new NotificationContextDTO();
        NotificationDTO notificationBody = new NotificationDTO();
        notificationBody.setTo(subscriberFcmToken.getFcmToken());
        notificationBody.setPriority("high");

        String title = messageSource.getMessage(
                "api.response.notification.title",
                new Object[]{subscriber.getFullName()},
                locale
        );

        String body = messageSource.getMessage(
                "api.response.notification.hotel.booked",
                null,
                locale
        );

        dataDTO.setTitle(title);
        dataDTO.setBody(body);

        Map<String, String> immigrationNotification = new HashMap<>();
        immigrationNotification.put("Hotel", "Booked");
        contextDTO.setPrefImmigrationAuthority(immigrationNotification);
        dataDTO.setNotificationContext(contextDTO);

        notificationBody.setData(dataDTO);
        return notificationBody;
    }


    @Override
    public ApiResponse getAllBookingDetails() {
        Locale locale = LocaleContextHolder.getLocale();
        try {


            return AppUtil.createApiResponse(true,
                    messageSource.getMessage(AppConstants.API_RESPONSE_FETCH_SUCCESS, null, locale),
                    hotelSimulatorRepo.allBookings());

        } catch (Exception e) {
            logger.error("{}",e.getMessage());
            return AppUtil.createApiResponse(false,
                    messageSource.getMessage(AppConstants.API_ERROR_SOMETHING_WENT_WRONG, null, locale),
                    null);
        }
    }
}