package com.hdfc.transactionalerts.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class DBInsertRetryConfig {

	private static String kafkaConfigFile;
	private static String dBConfigFile;
	private static String offsetFile;
	private static List<String> trackingElements;
	private static String[] DEFAULT_TRACKING_ELEMENTS = {"cardNumber","terminalTransactionSequenceNumber", "messageType", "ruleId"};
	
	private static Logger logger = Logger.getLogger(DBInsertRetryConfig.class);

	public static void loadConfig() throws Exception {
		Properties fileProps = new Properties();
		String configFile = System.getProperty("configFile");
		if(configFile == null) 
			throw new Exception("Value for property -DconfigFile not specified");
					
		logger.info(String.format("Notification Config File Path : %s", configFile));
		try (InputStream input = new FileInputStream(configFile)) {
			fileProps.load(input);
		} catch (IOException e) {
			logger.error(String.format("Exception occurred while reading file at Location: %s", configFile), e);
			throw e; 
		} 			
		
		kafkaConfigFile = fileProps.getProperty("kafkaConfigFile");
		dBConfigFile = fileProps.getProperty("dBConfigFile");
		offsetFile = fileProps.getProperty("offsetFile");
		String trkElem = fileProps.getProperty("trackingElements");
		trackingElements = Arrays.asList(trkElem != null ? trkElem.split(",") : DEFAULT_TRACKING_ELEMENTS);
		trackingElements.forEach(elem -> elem.trim());
		logger.info(String.format("Notification Config File Loaded Successfully %s", fileProps));

	}

	public static String getKafkaConfigFile() {
		return kafkaConfigFile;
	}

	public static String getdBConfigFile() {
		return dBConfigFile;
	}

	public static String getOffsetFile() {
		return offsetFile;
	}

	public static List<String> getTrackingElements() {
		return trackingElements;
	}

}
