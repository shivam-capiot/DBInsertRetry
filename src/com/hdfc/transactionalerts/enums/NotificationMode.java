package com.hdfc.transactionalerts.enums;

public enum NotificationMode {
	
	SMS("SMS"), MAIL("Mail");
	
	private String notificationMode;
	
	private NotificationMode(String notifyMode) {
		notificationMode = notifyMode;
	}

	public String toString() {
		return notificationMode;
	}
	
	public static NotificationMode forString(String notificationMode) {
		NotificationMode[] notificationModes = NotificationMode.values();
		for (NotificationMode notifyMode : notificationModes) {
			if (notificationMode.equalsIgnoreCase(notifyMode.toString())) {
				return notifyMode;
			}
		}
		return null;
	}
	
	
}
