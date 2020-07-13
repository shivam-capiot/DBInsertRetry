package com.hdfc.transactionalerts.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class KafkaConfig {

	private static Properties consumerProps = new Properties();
	private static String consumerTopic;
	private static Long consumerPollMillis;
	
	private static final String DEFAULT_CONSUMER_POLL_MS = "100";
	
	private static Logger logger = Logger.getLogger(KafkaConfig.class);
	
	public static void loadConfig() throws Exception {
		
		String kafkaConfigFile = DBInsertRetryConfig.getKafkaConfigFile();
		Properties fileProps = new Properties();
		try (InputStream input = new FileInputStream(kafkaConfigFile)) {
			fileProps.load(input);
		} catch (IOException e) {
			logger.error(String.format("Exception occurred while reading file at Location: %s", kafkaConfigFile), e);
			throw e; 
		}
		
		consumerTopic = fileProps.getProperty("kafka.consumer.topic");
		consumerPollMillis = Long.valueOf(fileProps.getProperty("consumer.poll.ms", DEFAULT_CONSUMER_POLL_MS));
		consumerProps.load(new FileInputStream(fileProps.getProperty("kafka.consumer.prop").trim()));
	}

	public static Properties getConsumerProps() {
		return consumerProps;
	}

	public static String getFailureConsumerTopic() {
		return consumerTopic;
	}

	public static Long getConsumerPollMillis() {
		return consumerPollMillis;
	}

}
