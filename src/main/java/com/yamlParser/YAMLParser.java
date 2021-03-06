package com.yamlParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

public class YAMLParser {
	public static void main(String args[]){
	}
	
	private BufferedReader reader ;	
	private Handler handler;
	private int listNums = 0;
	public YAMLParser(InputStream inputstream, Handler handler){
		this.reader = new BufferedReader(new InputStreamReader(inputstream));
		this.handler = handler;
	}
	
	public void doLoad(){
		Yaml yaml = new Yaml();
		Map result = yaml.load(reader);
		traversalMap(result,handler);
		
		//endelement
//		handler.endDocument(element, option);
	}
	
	
	public void traversalMap(Map map,Handler handler){
		Set set = map.keySet();
		for(Object key: set){
			Object value = map.get(key);
			//key is an EClass, also needs to be parsered
			if(value instanceof List){
				listNums++;
				handler.startTagElement(key, ConstantStringObject.OPTION_LIST_PARSE);
				for(Object object: (List)value){
					traversalMap((Map)object,handler);
				}
				listNums--;
				//it means this list is the firt list(root list)
//				if(listNums == 0){
//					handler.clearStack();
//				}
				
			}
			else if(value instanceof Map){
				handler.startTagElement(key, ConstantStringObject.OPTIN_MAP_PARSE);
				traversalMap((Map)value, handler);
				handler.stackPop();
			}else{
				handler.startValueElement(key.toString(), value.toString(), ConstantStringObject.OPTION_ELEMENT_PARSE);
			}
		}
	}
}
