package com.yamlParser;

import java.util.Map;

public interface Handler {
	public void startTagElement(Object element, String option);
	public void endElement(Object element, String option);
	public void startValueElement(String key, String value, String option);
	public void clearStack();
	
}
