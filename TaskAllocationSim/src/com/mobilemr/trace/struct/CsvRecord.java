package com.mobilemr.trace.struct;

import java.lang.reflect.Field;

public class CsvRecord {

	protected String getInternalCsvTitle() {
		Field[] fields = this.getClass().getFields();
		if (fields.length > 0) {
			// 필드가 있을 경우
			String ret = "";
			for (Field field : fields) {
				String fieldName = field.getName();
				fieldName = fieldName == null ? "" : fieldName;
				fieldName = fieldName.replace(",", "");
				ret += fieldName + ",";
			}
			// 마지막 쉼표 제거
			return ret.substring(0, ret.length() - 1);
		} else {
			// 필드가 없을 경우
			return "";
		}
	}

	public String toCsvString() {
		Field[] fields = this.getClass().getFields();
		if (fields.length > 0) {
			// 필드가 있을 경우
			String ret = "";
			try {
				for (Field field : fields) {
					Object fieldValue = field.get(this);
					String fieldValueStr = fieldValue == null ? "" : fieldValue
							.toString();
					fieldValueStr = fieldValueStr.replace(",", "");
					ret += fieldValueStr + ",";
				}
				// 마지막 쉼표 제거
				return ret.substring(0, ret.length() - 1);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		} else {
			// 필드가 없을 경우
			return "";
		}
	}

}
