package com.hdfc.transactionalerts.utils;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.MDC;
import org.json.JSONObject;

import com.hdfc.transactionalerts.config.DBInsertRetryConfig;

public class TrackingContext {
	private List<SimpleEntry<String, String>> mTrackVals = new ArrayList<SimpleEntry<String, String>>();
	private String mTrackParamsStr;
	private static Map<Long, TrackingContext> mTrackCtx = new HashMap<Long, TrackingContext>();
	private static TrackingContext DEFAULT_TRACKING_CONTEXT = new TrackingContext();

	private static List<String> trackingElements = DBInsertRetryConfig.getTrackingElements();
	
	private TrackingContext() {
		mTrackParamsStr = "";
	}

	private TrackingContext(JSONObject notificationJson) {
		
		StringBuffer strBldr = new StringBuffer();
		for(String trackingElement : trackingElements){
			String value = notificationJson.optString(trackingElement);
			mTrackVals.add(new SimpleEntry<String, String>(trackingElement, value.trim()));
			strBldr.append(String.format("[%s: %s] ", trackingElement, value.trim()));
		}
		
		strBldr.setLength(strBldr.length() - 1);
		mTrackParamsStr = strBldr.toString();
	}

	public static void setTrackingContext(JSONObject notificationJson) { 
		TrackingContext trkCtx = new TrackingContext(notificationJson);
		MDC.put("trkctx", trkCtx.toString());
		mTrackCtx.put(Thread.currentThread().getId(), trkCtx);
	}

	public String toString() {
		return mTrackParamsStr;
	}

	public static void clear() {
		mTrackCtx.remove(Thread.currentThread().getId());
		MDC.clear();
	}
	
	public static TrackingContext getTrackingContext() {
		TrackingContext trackCtx = mTrackCtx.get(Thread.currentThread().getId());
		return (trackCtx != null) ? trackCtx : DEFAULT_TRACKING_CONTEXT;
	}

	public static void duplicateContextFromThread(long sourceThreadID) {
		TrackingContext trkngCtx = mTrackCtx.get(sourceThreadID);
		if (trkngCtx != null) {
			mTrackCtx.put(Thread.currentThread().getId(), trkngCtx);
		}
	}
}
