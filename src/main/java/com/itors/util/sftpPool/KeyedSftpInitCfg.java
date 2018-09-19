package com.itors.util.sftpPool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.itors.service.ISysCfgService;
import com.itors.vo.SftpConfig;
/**
 * 
 * @author lijl
 * 2018-09-19
 */
@Component
public class KeyedSftpInitCfg implements ApplicationListener<ContextRefreshedEvent> {

	private static final Logger logger = LoggerFactory
			.getLogger(KeyedSftpInitCfg.class);
	//空配置
	private static final String CFG_NULL = "null";
	@Value("${sftp.pool.initParam:null}")
	private String initParam;
	@Value("${sftp.pool.maxTotalPerKey:8}")
	private String maxTotalPerKey;
	@Value("${sftp.pool.maxIdlePerKey:8}")
	private String maxIdlePerKey;
	@Value("${sftp.pool.minIdlePerKey:0}")
	private String minIdlePerKey;
	@Value("${sftp.pool.maxTotal:-1}")
	private String maxTotal;
	@Value("${sftp.pool.maxWaitMillis:3000}")
	private String maxWaitMillis;
	@Value("${sftp.pool.timeBetweenEvictionRunsMillis:-1}")
	private String timeBetweenEvictionRunsMillis;
	@Value("${sftp.pool.softMinEvictableIdleTimeMillis:0}")
	private String softMinEvictableIdleTimeMillis;
	@Value("${sftp.pool.numTestsPerEvictionRun:10}")
	private String numTestsPerEvictionRun;
	@Value("${sftp.pool.lifo:true}")
	private String lifo;
	@Value("${sftp.pool.testOnBorrow:true}")
	private String testOnBorrow;
	@Value("${sftp.pool.fairness:false}")
	private String fairness;
	@Value("${sftp.pool.minEvictableIdleTimeMillis:1800000}")
	private String minEvictableIdleTimeMillis;
	@Value("${sftp.pool.testOnCreate:true}")
	private String testOnCreate;
	@Value("${sftp.pool.testOnReturn:true}")
	private String testOnReturn;
	@Value("${sftp.pool.testWhileIdle:true}")
	private String testWhileIdle;
	@Value("${sftp.pool.blockWhenExhausted:true}")
	private String blockWhenExhausted;
	@Value("${sftp.pool.jmxEnabled:true}")
	private String jmxEnabled;
	@Value("${sftp.pool.jmxNamePrefix:pool}")
	private String jmxNamePrefix;

	@Autowired
	private ISysCfgService sysCfgService;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().getParent() == null) {
			Map<String, String> cfgs = null;
			if(!CFG_NULL.equals(initParam)){
				cfgs = getPerCfg(initParam);
			}
			if(null == cfgs||cfgs.isEmpty()){
				return;
			}
			ConcurrentHashMap<String,SftpConfig> sftps = new ConcurrentHashMap<String,SftpConfig >();
		    for (Map.Entry<String, String> entry : cfgs.entrySet()) {
		    	SftpConfig systemFileMsg = initSftpCfg(entry.getKey());
		    	if(null==systemFileMsg){
		    		logger.info("cant get [{}]sftp info from db.",entry.getKey());
		    		continue;
		    	}
		    	sftps.put(entry.getKey(),systemFileMsg);
	        }
		    KeyedSftpPoolCfg poolCfg = initPoolCfg();
		    poolCfg.setPerInitSize((HashMap<String,String>)cfgs);
			try {
				KeyedSftpFactory.init(poolCfg, sftps);
			} catch (Exception e) {
				logger.error("init sftp error."+e.getMessage());
			}
		}
	}
	/**
	 * 获取sftp配置信息
	 * @param initParam
	 * @return
	 */
	private Map<String,String> getPerCfg(String initParam){
		String perCfgs[] = initParam.split(",");
		Map<String, String> map = new HashMap<String, String>();
		for(String perCfg:perCfgs){
			if(perCfg.contains(":")){
				String perCfgInfo[] = perCfg.split(":");
				map.put(perCfgInfo[0], perCfgInfo[1]);
			}else{
				continue;
			}
		}
		return map;
	}

	// 获取 ftp 参数
	private SftpConfig initSftpCfg(String cfgName) {
		Map map=new HashMap();
		map.put("cfgName", cfgName);
		return sysCfgService.getSConfigByName(map);
	}

	// 获取 pool 参数
	private KeyedSftpPoolCfg initPoolCfg() {
		KeyedSftpPoolCfg poolConfig = new KeyedSftpPoolCfg();		
		poolConfig.setMinIdlePerKey(Integer.parseInt(minIdlePerKey));
		poolConfig.setMaxIdlePerKey(Integer.parseInt(maxIdlePerKey));
		poolConfig.setMaxTotalPerKey(Integer.parseInt(maxTotalPerKey));
		poolConfig.setMaxTotal(Integer.parseInt(maxTotal));
		// 超时时间。每次取池中取的对象等待最大时长。
		poolConfig.setMaxWaitMillis(Long.parseLong(maxWaitMillis));
		// 空闲池对象检测线程检测的周期，毫秒数。如果为负值，表示不运行检测线程。默认为-1.
		poolConfig.setTimeBetweenEvictionRunsMillis(Long
				.parseLong(timeBetweenEvictionRunsMillis));
		// 连接空闲的最小时间，达到此值后空闲链接将会被移除，且保留minIdle个空闲连接数。
		poolConfig.setSoftMinEvictableIdleTimeMillis(Long
				.parseLong(softMinEvictableIdleTimeMillis));
		// 线程每次清理的过期池化对象个数
		poolConfig.setNumTestsPerEvictionRun(Integer
				.parseInt(numTestsPerEvictionRun));
		poolConfig.setLifo(Boolean.parseBoolean(lifo)); // true: 先进先出 false：先进后出
		poolConfig.setTestOnBorrow(Boolean.parseBoolean(testOnBorrow)); // 借出是否校验
		poolConfig.setFairness(Boolean.parseBoolean(fairness));// 是否使用公平锁
		poolConfig.setMinEvictableIdleTimeMillis(Integer
				.parseInt(minEvictableIdleTimeMillis)); // 最小的驱逐时间
		poolConfig.setTestOnCreate(Boolean.parseBoolean(testOnCreate)); // 在创建对象的时候是否检测对象
		poolConfig.setTestOnReturn(Boolean.parseBoolean(testOnReturn));// 在归还对象时检验是否有效
		poolConfig.setTestWhileIdle(Boolean.parseBoolean(testWhileIdle));// 在获取空闲对象的时候是否检测对象是否有效
		poolConfig.setBlockWhenExhausted(Boolean
				.parseBoolean(blockWhenExhausted));// 在对象池耗尽时是否阻塞默认true。false的话超时就没有作用了。
		poolConfig.setJmxEnabled(Boolean.parseBoolean(jmxEnabled));// 是否允许jmx的方式创建一个配置实例，默认true
		poolConfig.setJmxNamePrefix(jmxNamePrefix);// jmx默认的前缀名，默认为pool
		return poolConfig;
	}

}
