package com.itors.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.itors.vo.SftpConfig;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
/**
 * 此类中所有有关 sftp 上传、下载及命令执行均不关闭连接。
 * 使用时请通过sftp连接池使用。
 * 单独使用请慎重！！！！！！
 * @author itors
 */
public class SftpUtil {
	
	private static final Logger log = LoggerFactory.getLogger(SftpUtil.class);
	
    private Session session;
    private Channel channel;
    private ChannelSftp chnSftp;
	private SftpConfig sftpCfg;
    private ChannelExec channelExec; 
    private FileOutputStream fos = null;

	
	public SftpUtil(){}
	
	public SftpUtil(SftpConfig sftpConfig) throws JSchException, SftpException{
		this.sftpCfg = sftpConfig;
		openchnSftp(sftpConfig);
	}
	
	/**
	 * 打开普通 sftp 上传通道
	 * @param systemFileMsg
	 * @throws SftpException 
	 * @throws JSchException 
	 */
	private void openchnSftp(SftpConfig systemFileMsg) throws JSchException, SftpException{
		connect(systemFileMsg.getSftpHost(), systemFileMsg.getSftpPort(), systemFileMsg.getSftpUsername(), systemFileMsg.getSftpPassword());
	}
	
    public void connect(String host, int port, String username,String password) throws JSchException, SftpException {
        try {
        	JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sshConfig);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            chnSftp = (ChannelSftp)channel;
            log.info("打开sftp连接成功，可以进行连接");
        }catch(Exception e) {
        	log.info("打开sftp连接时错误", e);
        }
    	
    }
	
	/**
	 *  上传文件
	 * @param ftpUploadDir 存放文件目录
	 * @param filename 文件名
	 * @param input 上传文件流
	 * @return
	 */
    public boolean uploadFileToSftp(String ftpUploadDir, String filename, InputStream input) {
        boolean result = false;
        try {
        	changeChannelByType(Constant.CHANNEL_TYPE_SFTP);
            createDir(ftpUploadDir);
            chnSftp.put(input, filename,chnSftp.RESUME);
            result = true;
        } catch (SftpException var5) {
        	log.error(var5.getMessage(), var5);
        }
        return result;
    }
    public  String createDir(String sftpDirPath) throws SftpException {
        if(isDirExist(sftpDirPath)) {
            cd(sftpDirPath);
            return "0:dir is exist !";
        } else {
            String[] pathArry = sftpDirPath.split("/");
            String[] var2 = pathArry;
            int var3 = pathArry.length;
            
            for(int var4 = 0; var4 < var3; ++var4) {
            	if(var4 == 0) {// 进入根目录再比较
            		cd("/");
            	}
                String path = var2[var4];
                if(!path.equals("")) {
                    if(isDirExist(path)) {
                        cd(path);
                    } else {
                        chnSftp.mkdir(path);
                        chnSftp.cd(path);
                    }
                }
            }

            return "1:创建目录成功";
        }
    }
    
    public  void cd(String sftpPath) throws SftpException {
        chnSftp.cd(sftpPath);
    }

    public  boolean isDirExist(String directory) throws SftpException {
        boolean isDirExistFlag = false;

        try {
            SftpATTRS e = chnSftp.lstat(directory);
            isDirExistFlag = true;
            return e.isDir();
        } catch (Exception var3) {
            if(var3.getMessage().equalsIgnoreCase("no such file")) {
                isDirExistFlag = false;
            }

            return isDirExistFlag;
        }
    }
	/**
	 * 下载文件
	 * @param remotepath 文件路径 
	 * @param ftpFileName 文件名
	 * @return 文件流
	 */
    public InputStream downSftpFile(String remotepath, String ftpFileName) {
        InputStream is = null;
        try {
        	changeChannelByType(Constant.CHANNEL_TYPE_SFTP);
            cd(remotepath);
            is = getFile(ftpFileName);
        } catch (Exception var5) {
        	log.error("下载失败！", var5);
        }
        return is;
    }
    public  InputStream getFile(String sftpFilePath) throws SftpException {
        return isFileExist(sftpFilePath)?chnSftp.get(sftpFilePath):null;
    }
    public  boolean isFileExist(String srcSftpFilePath) throws SftpException {
        boolean isExitFlag = false;
        if(getFileSize(srcSftpFilePath) >= 0L) {
            isExitFlag = true;
        }

        return isExitFlag;
    }
    public  long getFileSize(String srcSftpFilePath) throws SftpException {
        long filesize = 0L;
        try {
            SftpATTRS e = chnSftp.lstat(srcSftpFilePath);
            filesize = e.getSize();
        } catch (Exception var4) {
            filesize = -1L;
            if(var4.getMessage().toLowerCase().equals("no such file")) {
                filesize = -2L;
            }
        }
        return filesize;
    }
    /**
     * 执行shell命令
     * @param command
     * @return
     */
    public int executeShell(final String command) {
		int returnCode = 0;
		try {
			changeChannelByType(Constant.CHANNEL_TYPE_EXEC);
			channelExec.setCommand(command);
			channelExec.setInputStream(null);
			BufferedReader input = new BufferedReader(new InputStreamReader(
					channelExec.getInputStream()));
			log.info("The remote command is :" + command);
			channel.connect();
			// 接收远程服务器执行命令的结果
			String line;
			while ((line = input.readLine()) != null) {
				log.info(line);
			}
			input.close();
			changeChannelByType(Constant.CHANNEL_TYPE_SFTP);
			// 得到returnCode
			if (channelExec.isClosed()) {
				returnCode = channelExec.getExitStatus();
			}
		} catch (Exception e) {
			log.info("An exception occurred while executing the shell command:" + command);
		}
		return returnCode;
	}
    
    /**
     * 获取文件夹里 以 fileSuffix 为后缀的文件
     * @param directory
     * @param fileSuffix   譬如  ".block"
     * @return
     * @throws SftpException
     */
	public Vector getAllFiles(String directory,String fileSuffix) throws SftpException{
		this.cd(directory);
		Vector vector = chnSftp.ls(directory + "/*" + fileSuffix);
		return vector;
	}
    
    
    /**
     * 关闭连接
     */
	public  void close() {
        try {
            if(fos != null) {
                fos.close();
            }

            if(channel.isConnected()) {
                channel.disconnect();
                log.info("Channel connect  disconnect!");
            }

            if(session.isConnected()) {
                session.disconnect();
                log.info("Session connect disconnect!");
            }

            log.info("关闭sftp连接");
        } catch (IOException var1) {
            log.error("关闭sftp连接时出现异常", var1);
        }

    }

    /**
     * 交换通道
     * @param type
     */
    private void changeChannelByType(String type){
    	try {
			if( Constant.CHANNEL_TYPE_SFTP.equals(type)){
				if(null != chnSftp &&  chnSftp.isConnected() ){
					return ;
				}
				if( channelExec.isConnected() ){
					channelExec.disconnect();
				}
				channel = session.openChannel(type);
				channel.connect();
				chnSftp = (ChannelSftp)channel;
			}else if( Constant.CHANNEL_TYPE_EXEC.equals(type)){
				if( null != channelExec &&  channelExec.isConnected() ){
					return ;
				}
				if( chnSftp.isConnected() ){
					chnSftp.disconnect();
				}
				channel = session.openChannel(type);
				channelExec = (ChannelExec)channel;
			}
		} catch (JSchException e) {
			log.error("An exception occurred during handoff:",e.getMessage());
		}
    }
    
    /**
     * 获取sftp 连接信息
     * @return
     */
    public SftpConfig getSftpCfg(){
    	return sftpCfg;
    }
    
    /**
     * 是否已连接
     * @return
     */
	public boolean isConnected() {
		return null != session && session.isConnected();
	}
    
}
