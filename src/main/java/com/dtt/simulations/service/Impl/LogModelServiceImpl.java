package com.dtt.simulations.service.Impl;

import com.dtt.simulations.dto.LogModelDTO;
import com.dtt.simulations.enums.LogMessageType;
import com.dtt.simulations.enums.TransactionType;
import com.dtt.simulations.responseentity.AppUtil;
import com.dtt.simulations.service.Iface.LogModelServiceIface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import ug.daes.DAESService;
import ug.daes.Result;


import java.text.ParseException;
import java.util.Date;


@Service
public class LogModelServiceImpl implements LogModelServiceIface {

	private static final Logger logger = LoggerFactory.getLogger(LogModelServiceImpl.class);
	private static final String CLASS = "LogModelServiceImpl";

	private final KafkaTemplate<String, LogModelDTO> kafkaTemplate;

	public LogModelServiceImpl(KafkaTemplate<String, LogModelDTO> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}


	@Value("${kafka.topic.log}")
	private String logTopic;

	@Override
	public void setLogModelDTO(Boolean response, String identifier, String geoLocation, String serviceName,
							   String correlationID, String message, Date startTime, Date endTime,
							   String otpStatus) throws ParseException {

		try {
			LogModelDTO logModel = new LogModelDTO();
			logModel.setIdentifier(identifier);
			logModel.setCorrelationID(correlationID);
			logModel.setTransactionID(null);
			logModel.setTimestamp(AppUtil.getTimeStampString(new Date()));
			logModel.setStartTime(AppUtil.getTimeStampString(startTime));
			logModel.setEndTime(AppUtil.getTimeStampString(endTime));
			logModel.setServiceName(String.valueOf(serviceName));
			logModel.setLogMessage(message);
			logModel.setTransactionType(TransactionType.BUSINESS.toString());
			logModel.setGeoLocation(geoLocation);
			logModel.seteSealUsed(false);
			logModel.setSignatureType(null);
			logModel.setCallStack(otpStatus);
			logModel.setLogMessageType(
					Boolean.TRUE.equals(response)
							? LogMessageType.SUCCESS.toString()
							: LogMessageType.FAILURE.toString()
			);
			logModel.setChecksum(null);

			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(logModel);

			Result checksumResult = DAESService.addChecksumToTransaction(json);
			logModel.setChecksum(new String(checksumResult.getResponse()));

			kafkaTemplate.send(logTopic, logModel);

			logger.info("{} - Log published to Kafka topic {}", CLASS, logTopic);

		} catch (Exception e) {
			logger.error("{} - Error sending log to Kafka", CLASS, e);
		}
	}
}

