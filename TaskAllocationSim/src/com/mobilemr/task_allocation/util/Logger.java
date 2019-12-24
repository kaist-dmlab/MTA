package com.mobilemr.task_allocation.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Logger {

	private static final boolean SYSTEM_OUT = false;

	private static Logger LOGGER = null;

	public static void create(String logDirName, String curDate) {
		LOGGER = new Logger(logDirName + File.separator + curDate + "_log");
	}

	private PrintWriter pw;

	public Logger(String logFilePath) {
		// Log 파일 초기화
		File logFile = new File(logFilePath);
		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
					logFile), "UTF-8"), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void println(String msg) {
		LOGGER.pw.println(Date.CURRENT_TIME + " " + msg);
		if (SYSTEM_OUT) {
			System.out.println(Date.CURRENT_TIME + " " + msg);
		}
	}

	public static void println() {
		LOGGER.pw.println();
		if (SYSTEM_OUT) {
			System.out.println();
		}
	}

	public static void printStackTrace(Throwable t) {
		t.printStackTrace(LOGGER.pw);
		if (SYSTEM_OUT) {
			t.printStackTrace(System.out);
		}
	}

}
