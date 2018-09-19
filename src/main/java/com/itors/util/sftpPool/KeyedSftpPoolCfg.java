package com.itors.util.sftpPool;

import java.util.HashMap;

import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
/**
 * 
 * @author lijl
 * 2018-09-19
 */
public class KeyedSftpPoolCfg extends GenericKeyedObjectPoolConfig{
	
	private HashMap<String,String> perInitSize;

	public HashMap<String,String> getPerInitSize() {
		return perInitSize;
	}

	public void setPerInitSize(HashMap<String,String> perInitSize) {
		this.perInitSize = perInitSize;
	}
	
	
}
