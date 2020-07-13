package com.hdfc.transactionalerts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.log4j.Logger;

import com.hdfc.transactionalerts.config.DBInsertRetryConfig;
import com.hdfc.transactionalerts.config.DbConfig;
import com.hdfc.transactionalerts.config.KafkaConfig;
import com.hdfc.transactionalerts.consumers.DBProcessor;
import com.hdfc.transactionalerts.consumers.RebalanceListener;
import com.hdfc.transactionalerts.utils.Constants;

public class DBInsertRetry implements Constants{

	private static final Logger logger = Logger.getLogger(DBInsertRetry.class);
	
	static  {
		//Load Property files before starting application
		try {
			//Always load this first as it holds information regarding other file locations
			DBInsertRetryConfig.loadConfig();
			KafkaConfig.loadConfig();
			DbConfig.loadConfig();
			
		} catch (Exception e) {
			logger.error("Error occurred in Loading Config Files", e);
			throw new RuntimeException(e.getMessage());
		}
	}
	
	public static void main(String args[]) throws Exception {
		
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(KafkaConfig.getConsumerProps());
		final Thread mainThread = Thread.currentThread();
		
		//Added a shutDownHook to exit cleanly
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				logger.error("Exiting cleanly... Calling Consumer wakeup");
				consumer.wakeup();
				
				try {
					mainThread.join();
				} catch (InterruptedException e) {
					logger.error("InterruptedException occurred while waiting for main Thread to finish", e);
				}
			}
		});
		DbConfig.getDBConnection();
		String topicName = KafkaConfig.getFailureConsumerTopic();
		RebalanceListener rebalanceListner = new RebalanceListener(consumer);
		consumer.subscribe(Arrays.asList(topicName), rebalanceListner);
		logger.info(String.format("Subscribing to consumer topic : %s", topicName));
		
		consumer.poll(0);
		consumingMessages(consumer, rebalanceListner);
	}

	private static void consumingMessages(KafkaConsumer<String, String> consumer, RebalanceListener rebalanceListener) {
		
		try {
			while (true) {
				
				ConsumerRecords<String, String> records = consumer.poll(KafkaConfig.getConsumerPollMillis());
				int totalRecordCount = records.count();
				long startTime = System.currentTimeMillis();
				
                for (ConsumerRecord<String, String> record : records) {
                	logger.info(String.format("Record Offset :", record.offset()));
                	try {
                		DBProcessor.processRecord(record, rebalanceListener);
                	} catch (SQLException | ClassNotFoundException e) {
                		try {
                			//
                			Thread.sleep(100);
                		} catch (InterruptedException x) {
						}
                		break;
					} catch (Exception e) {
						//Ideally code will never come here.
						continue;
					}
                } 
                
        		if(totalRecordCount > 0) {
        			long endTime = System.currentTimeMillis();
        			consumer.commitAsync(rebalanceListener.getCurrentOffsets(), null);
        			logger.info(String.format("Time taken to process %d records : %s ms", totalRecordCount, (endTime - startTime)));
        		}
			}
			
		} catch (WakeupException e) {
			logger.warn("Consumer Wakeup invoked...");
		} catch (Exception e) {
			logger.error("Unexpected Error occurred", e);
		} finally {
			try {
				DbConfig.closeConnection();
				consumer.commitSync(rebalanceListener.getCurrentOffsets());
				writeOffsetToFile(rebalanceListener.getCurrentOffsets().toString());
			} finally {
				consumer.close();
				logger.info("Consumer Closed....");
			}
		}
		
	}
	
	private static void writeOffsetToFile(String data) {
        File file = new File(DBInsertRetryConfig.getOffsetFile());
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, false);
            fr.write(data);
        } catch (IOException e) {
        	logger.error("An error occured while writing data to offset File");
        }finally{
            //close resources
            try {
                fr.close();
            } catch (IOException e) {
            	logger.error("An error occured while closing offset File");
            }
        }
        
    }
	
}
