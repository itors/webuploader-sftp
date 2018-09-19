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
	//������
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
	 * ��ȡsftp������Ϣ
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

	// ��ȡ ftp ����
	private SftpConfig initSftpCfg(String cfgName) {
		Map map=new HashMap();
		map.put("cfgName", cfgName);
		return sysCfgService.getSConfigByName(map);
	}

	// ��ȡ pool ����
	private KeyedSftpPoolCfg initPoolCfg() {
		KeyedSftpPoolCfg poolConfig = new KeyedSftpPoolCfg();		
		poolConfig.setMinIdlePerKey(Integer.parseInt(minIdlePerKey));
		poolConfig.setMaxIdlePerKey(Integer.parseInt(maxIdlePerKey));
		poolConfig.setMaxTotalPerKey(Integer.parseInt(maxTotalPerKey));
		poolConfig.setMaxTotal(Integer.parseInt(maxTotal));
		// ��ʱʱ�䡣ÿ��ȡ����ȡ�Ķ���ȴ����ʱ����
		poolConfig.setMaxWaitMillis(Long.parseLong(maxWaitMillis));
		// ���гض������̼߳������ڣ������������Ϊ��ֵ����ʾ�����м���̡߳�Ĭ��Ϊ-1.
		poolConfig.setTimeBetweenEvictionRunsMillis(Long
				.parseLong(timeBetweenEvictionRunsMillis));
		// ���ӿ��е���Сʱ�䣬�ﵽ��ֵ��������ӽ��ᱻ�Ƴ����ұ���minIdle��������������
		poolConfig.setSoftMinEvictableIdleTimeMillis(Long
				.parseLong(softMinEvictableIdleTimeMillis));
		// �߳�ÿ������Ĺ��ڳػ��������
		poolConfig.setNumTestsPerEvictionRun(Integer
				.parseInt(numTestsPerEvictionRun));
		poolConfig.setLifo(Boolean.parseBoolean(lifo)); // true: �Ƚ��ȳ� false���Ƚ����
		poolConfig.setTestOnBorrow(Boolean.parseBoolean(testOnBorrow)); // ����Ƿ�У��
		poolConfig.setFairness(Boolean.parseBoolean(fairness));// �Ƿ�ʹ�ù�ƽ��
		poolConfig.setMinEvictableIdleTimeMillis(Integer
				.parseInt(minEvictableIdleTimeMillis)); // ��С������ʱ��
		poolConfig.setTestOnCreate(Boolean.parseBoolean(testOnCreate)); // �ڴ��������ʱ���Ƿ������
		poolConfig.setTestOnReturn(Boolean.parseBoolean(testOnReturn));// �ڹ黹����ʱ�����Ƿ���Ч
		poolConfig.setTestWhileIdle(Boolean.parseBoolean(testWhileIdle));// �ڻ�ȡ���ж����ʱ���Ƿ�������Ƿ���Ч
		poolConfig.setBlockWhenExhausted(Boolean
				.parseBoolean(blockWhenExhausted));// �ڶ���غľ�ʱ�Ƿ�����Ĭ��true��false�Ļ���ʱ��û�������ˡ�
		poolConfig.setJmxEnabled(Boolean.parseBoolean(jmxEnabled));// �Ƿ�����jmx�ķ�ʽ����һ������ʵ����Ĭ��true
		poolConfig.setJmxNamePrefix(jmxNamePrefix);// jmxĬ�ϵ�ǰ׺����Ĭ��Ϊpool
		return poolConfig;
	}

}
