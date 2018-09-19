package com.itors.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.itors.vo.SftpConfig;

@Service
public interface ISysCfgService {

	public SftpConfig getSConfigByName(Map map);
	
}
