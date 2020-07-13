package com.hdfc.transactionalerts.consumers;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.hdfc.transactionalerts.config.DbConfig;
import com.hdfc.transactionalerts.utils.Constants;
import com.hdfc.transactionalerts.utils.TrackingContext;

public class DBProcessor implements Constants{

	private static final Logger logger = Logger.getLogger(DBProcessor.class);
	
	private static final String insertQuery  = String.format("INSERT INTO %s (HASH, SEQID, ALERTTYPE, CARDTYPE, RECIPIENT, INSERTTS, REQBODY, FLAG, RETRYCOUNT, TXNTYPE, RULEID, TXNTIME, MSGTYPE) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", FAILED_AXIOM_TABLE);
	
	public static void processRecord(ConsumerRecord<String, String> record, RebalanceListener rebalanceListener) throws Exception {

		JSONObject notificationJson;
    	try {
    		notificationJson = new JSONObject(record.value());
    	} catch (JSONException e) {
    		logger.error(String.format("Unable to parse Json for record - %s", record.value()), e);
    		return;
		}
    	
    	Connection conn = DbConfig.getDBConnection();
		try {
			
			TrackingContext.setTrackingContext(notificationJson);
			String reqXmlStr = notificationJson.getString("reqXml");
			PreparedStatement insertStatement = conn.prepareStatement(insertQuery);
			insertStatement.setString(1, notificationJson.getString("cardNumber"));
			insertStatement.setString(2, notificationJson.getString("terminalTransactionSequenceNumber"));
			insertStatement.setString(3, notificationJson.getString("alertType"));
			insertStatement.setString(4, notificationJson.getString("cardType"));
			insertStatement.setString(5, notificationJson.getString("recipient"));
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("IST"));
			Date insertDate = new Date();
			insertStatement.setTimestamp(6, new Timestamp(insertDate.getTime()), cal);
			Clob clob = conn.createClob();
			clob.setString(1, reqXmlStr);
			insertStatement.setClob(7, clob);
			insertStatement.setString(8, notificationJson.getString("flag"));
			insertStatement.setInt(9, notificationJson.optInt("retryCount", 0));
			insertStatement.setString(10, notificationJson.optString("txnType"));
			insertStatement.setString(11, notificationJson.getString(RULEID));
			insertStatement.setTimestamp(12, getTransactionTime(notificationJson.optString("custom_transactionTime"), insertDate), cal);
			insertStatement.setString(13, notificationJson.optString("messageType"));

			insertStatement.executeUpdate();
			logger.info("Record inserted into DB for future processing");
		} catch (Exception e) {
			logger.error("Unable to insert record into DB ", e);
		} finally {
			rebalanceListener.addOffset(record.topic(), record.partition(), record.offset());
        	TrackingContext.clear();
		}
	}
	
	private static Timestamp getTransactionTime(String transactionTimeStr, Date insertDate) {

		Date txnTime = insertDate;
		try {
			txnTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(transactionTimeStr);
		} catch (ParseException e) {
			logger.warn("Error occurred in getting transaction Time");
		}
		return new Timestamp(txnTime.getTime());
	}


}
