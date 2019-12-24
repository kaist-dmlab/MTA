package com.mobilemr.task_allocation.util;

import java.text.SimpleDateFormat;

public class Date {

	public static final String CURRENT_DATE_TIME = new SimpleDateFormat(
			"yyMMdd-HHmmss").format(new java.util.Date());

	public static final String CURRENT_TIME = new SimpleDateFormat("mmss")
			.format(new java.util.Date());

}
