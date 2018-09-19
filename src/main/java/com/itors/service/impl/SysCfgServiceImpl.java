package com.itors.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.itors.dao.SysCfgDao;
import com.itors.service.ISysCfgService;
import com.itors.vo.SftpConfig;

@Component
public class SysCfgServiceImpl implements ISysCfgService {

	@Autowired
	private SysCfgDao sysCfgDao;
	
	
	@Override
	public SftpConfig getSConfigByName(Map map){
		String cfg = sysCfgDao.getSConfigByName(map);
		return JSON.parseObject(cfg, SftpConfig.class);
	}
}
