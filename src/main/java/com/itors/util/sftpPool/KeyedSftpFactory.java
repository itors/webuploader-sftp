package com.itors.util.sftpPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.itors.util.SftpUtil;
import com.itors.vo.SftpConfig;

/**
 * @author itors
 * @date 2018-09-14
 */
public class KeyedSftpFactory implements KeyedPooledObjectFactory<String,SftpUtil> {
	private static final Logger logger = LoggerFactory.getLogger(KeyedSftpFactory.class);
	private static KeyedSftpPoolCfg sftpPoolCfg = null;
	private static Map<String,SftpConfig>  sftpCfgs = null;
	private static GenericKeyedSftpPool<String,SftpUtil> pool = null;
	
	private KeyedSftpFactory(){}
	
	public static void init(KeyedSftpPoolCfg pooCfg, ConcurrentHashMap<String,SftpConfig> sftpcfgs) throws Exception{
		if(null == sftpcfgs || sftpcfgs.isEmpty()){
			/**
			 * 
			 */
			 throw new Exception("cant not init sftp pool: the config of sftp  is null.");
		}
		sftpPoolCfg = pooCfg;
		sftpCfgs = sftpcfgs;
		logger.info("pool config:{}",JSON.toJSON(sftpPoolCfg));
		logger.info("sftp config:{}",JSON.toJSON(sftpCfgs));
		initPool();
	}
	
	// 鍙栧緱瀵硅薄姹犲伐鍘傚疄渚�
	public synchronized static GenericKeyedSftpPool<String,SftpUtil> getSftpPool() {
		return initPool();
	}
	
	private  static GenericKeyedSftpPool<String,SftpUtil> initPool(){
		if (pool == null) {
			// 闈炵┖鍒ゆ柇
			if(null == sftpPoolCfg){
				sftpPoolCfg = (KeyedSftpPoolCfg)new GenericKeyedObjectPoolConfig();
			}
			pool = new GenericKeyedSftpPool<String,SftpUtil>(new KeyedSftpFactory(),sftpPoolCfg);
			
			Map<String,String> initCfg = sftpPoolCfg.getPerInitSize();
		    for (Map.Entry<String, String> entry : initCfg.entrySet()) {
		    	if(null==entry.getValue()||"0".equals(entry.getValue())){
		    		continue;
		    	}
		    	int poolInitSize = Integer.parseInt(entry.getValue());
		    	logger.info("starting init multi-sftp[{}] pool. size:{} ",entry.getKey(),poolInitSize);
		    	for(int i = 0;i<poolInitSize;i++){
		    		try {
						pool.addObject(entry.getKey());
					} catch (Exception e) {
						logger.error("cant not init sftpUtil pool ." + e.getMessage());
					}
		    	}
	        }
		}
		return pool;
	}
	
	@Override
	public PooledObject<SftpUtil> makeObject(String key) throws Exception {
		SftpConfig sftpCfg =  sftpCfgs.get(key);
		if(null==sftpCfg){
			logger.error("cant not get sftp config by the key:{}",key);
			return null;
		}
		SftpUtil s = new SftpUtil(sftpCfg);
		logger.info("new sftp connect to pool [{}]...",key);
		return new DefaultPooledObject<SftpUtil>(s);
	}

	@Override
	public void destroyObject(String key, PooledObject<SftpUtil> p)
			throws Exception {
		SftpUtil sftpUtil = p.getObject();
		sftpUtil.close();
		logger.info("closed sftp connect,destroy one  SftpUtil from pool [{}] ...",key);
	}

	@Override
	public boolean validateObject(String key, PooledObject<SftpUtil> p) {
		logger.info("validate the Object of [{}]pool  ...",key);
        return p.getObject().isConnected();
	}

	@Override
	public void activateObject(String key, PooledObject<SftpUtil> p)
			throws Exception {
		logger.info("borrow sftp from [{}]pool : activateObject...",key);
	}

	@Override
	public void passivateObject(String key, PooledObject<SftpUtil> p)
			throws Exception {
		logger.info("return sftp from [{}]pool :passivateObject...",key);
	}

	
}
