package com.itors.util.sftpPool;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

/**
 * @author lijl
 * @date 2018-09-14
 */
public class GenericKeyedSftpPool<String,SftpUtil> extends GenericKeyedObjectPool{

	public GenericKeyedSftpPool(KeyedPooledObjectFactory factory,
			GenericKeyedObjectPoolConfig config) {
		super(factory, config);
	}
	
	public SftpUtil borrowSftpUtil(String key) throws Exception{
		return (SftpUtil)super.borrowObject(key);
	}
	public void returnSftpUtil(String key,SftpUtil sftpUtil){
		super.returnObject(key, sftpUtil);
	}
	
	
	@SuppressWarnings("unchecked")
	public Map getPoolInfo(){
		Map map = new HashMap();
		map.put("blockWhenExhausted",super.getBlockWhenExhausted());
		map.put("borrowedCount",super.getBorrowedCount());
		map.put("createdCount",super.getCreatedCount());
		map.put("destroyedByBorrowValidationCount",super.getDestroyedByBorrowValidationCount());
		map.put("destroyedCount",super.getDestroyedByEvictorCount());
		map.put("destroyedCount",super.getDestroyedCount());
		map.put("evictionPolicy",super.getEvictionPolicy());
		map.put("evictionPolicy",super.getEvictionPolicyClassName());
		map.put("fairness",super.getFairness());
		map.put("oname",super.getJmxName());
		map.put("lifo",super.getLifo());
		map.put("maxBorrowWaitTimeMillis",super.getMaxBorrowWaitTimeMillis());
		map.put("maxIdlePerKey",super.getMaxIdlePerKey());
		map.put("maxTotal",super.getMaxTotal());
		map.put("maxTotalPerKey",super.getMaxTotalPerKey());
		map.put("maxWaitMillis",super.getMaxWaitMillis());
		map.put("activeTimes",super.getMeanActiveTimeMillis());
		map.put("waitTimes",super.getMeanBorrowWaitTimeMillis());
		map.put("idleTimes",super.getMeanIdleTimeMillis());
		map.put("minEvictableIdleTimeMillis",super.getMinEvictableIdleTimeMillis());
		map.put("minIdlePerKey",super.getMinIdlePerKey());
		map.put("numIdle",super.getNumActive());
		map.put("numActivePerKey",super.getNumActivePerKey());
		map.put("numIdle",super.getNumIdle());
		map.put("numTestsPerEvictionRun",super.getNumTestsPerEvictionRun());
		map.put("numWaiters",super.getNumWaiters());
		map.put("numWaitersByKey",super.getNumWaitersByKey());
		map.put("returnedCount",super.getReturnedCount());
		map.put("softMinEvictableIdleTimeMillis",super.getSoftMinEvictableIdleTimeMillis());
		map.put("swallowedExceptionListener",super.getSwallowedExceptionListener());
		map.put("testOnBorrow",super.getTestOnBorrow());
		map.put("testOnCreate",super.getTestOnCreate());
		map.put("testOnReturn",super.getTestOnReturn());
		map.put("testWhileIdle",super.getTestWhileIdle());
		map.put("timeBetweenEvictionRunsMillis",super.getTimeBetweenEvictionRunsMillis());
		return map;
	}


}
