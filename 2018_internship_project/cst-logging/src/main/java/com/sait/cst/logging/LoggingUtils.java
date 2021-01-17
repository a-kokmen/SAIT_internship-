package com.sait.cst.logging;

import java.util.Date;

public class LoggingUtils {
	public static void INFO(String format, Object... args) {
		String message = String.format(format, args);
		System.out.println(String.format("[INFO] [%s] %s", new Date(), message));
	}

	public static void DEBUG(String format, Object... args) {
	//	String message = String.format(format, args);
	//	System.out.println(String.format("[DEBUG] [%s] %s", new Date(), message));
	}
	
	public static void WARN(String format, Object... args) {
		String message = String.format(format, args);
		System.out.println(String.format("[WARN] [%s] %s", new Date(), message));
	}

	public static void ERROR(String format, Object... args) {
		String message = String.format(format, args);
		System.out.println(String.format("[ERROR] [%s] %s", new Date(), message));
	}
}
