package com.mobilemr.task_allocation.util;

public class Indenter {

	public static String get(int size) {
		String indent = "";
		for (int i = 0; i < size; i++) {
			indent += " ";
		}
		return indent;
	}

}
