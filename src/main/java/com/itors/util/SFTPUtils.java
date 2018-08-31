package com.itors.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

public class SFTPUtils {
	private Logger log = Logger.getLogger(SFTPUtils.class);
	private Session session;
	private Channel channel;
	private FileOutputStream fos = null;
	public ChannelSftp chnSftp;
	public ChannelExec channelExec;
	public final int FILE_TYPE = 1;
	public final int DIR_TYPE = 2;

	public SFTPUtils() {

	}
	public void open(String host, int port, String username, String password) {
		try {
			connect(host, port, username, password);
			log.info("打开sftp连接成功，可以进行连接");
		} catch (Exception var1) {
			log.info("打开sftp连接时错误", var1);
		}
	}
	public ChannelSftp connectNew(String host, int port, String username,
			String password) throws JSchException, SftpException {
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
			chnSftp = (ChannelSftp) channel;
			log.info("打开sftp连接成功，可以进行连接");
			return chnSftp;
		} catch (Exception e) {
			log.info("打开sftp连接时错误", e);
		}
		return null;
	}

	public void connect(String host, int port, String username, String password)
			throws JSchException, SftpException {
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
			chnSftp = (ChannelSftp) channel;
			log.info("打开sftp连接成功，可以进行连接");
		} catch (Exception e) {
			log.info("打开sftp连接时错误", e);
		}

	}

	public void cd(String sftpPath) throws SftpException {
		chnSftp.cd(sftpPath);
	}

	public String pwd() throws SftpException {
		return chnSftp.pwd();
	}

	public List<String> listFiles(String directory, int fileType)
			throws SftpException {
		ArrayList fileList = new ArrayList();
		if (isDirExist(directory)) {
			boolean itExist = false;
			Vector vector = chnSftp.ls(directory);

			for (int i = 0; i < vector.size(); ++i) {
				Object obj = vector.get(i);
				String str = obj.toString().trim();
				int tag = str.lastIndexOf(':') + 3;
				String strName = str.substring(tag).trim();
				itExist = isDirExist(directory + "/" + strName);
				if (fileType == 1 && !itExist) {
					fileList.add(directory + "/" + strName);
				}

				if (fileType == 2 && itExist && !strName.equals(".")
						&& !strName.equals("..")) {
					fileList.add(directory + "/" + strName);
				}
			}
		}
		return fileList;
	}

	public boolean isDirExist(String directory) throws SftpException {
		boolean isDirExistFlag = false;

		try {
			SftpATTRS e = chnSftp.lstat(directory);
			isDirExistFlag = true;
			return e.isDir();
		} catch (Exception var3) {
			if (var3.getMessage().equalsIgnoreCase("no such file")) {
				isDirExistFlag = false;
			}
			return isDirExistFlag;
		}
	}

	public InputStream getFile(String sftpFilePath) throws SftpException {
		return isFileExist(sftpFilePath) ? chnSftp.get(sftpFilePath) : null;
	}

	public InputStream getInputStreamFile(String sftpFilePath)
			throws SftpException {
		return getFile(sftpFilePath);
	}

	public ByteArrayInputStream getByteArrayInputStreamFile(String sftpFilePath)
			throws SftpException, IOException {
		if (isFileExist(sftpFilePath)) {
			byte[] srcFtpFileByte = inputStreamToByte(getFile(sftpFilePath));
			ByteArrayInputStream srcFtpFileStreams = new ByteArrayInputStream(
					srcFtpFileByte);
			return srcFtpFileStreams;
		} else {
			return null;
		}
	}

	public byte[] getByteArray(String sftpFilePath) throws SftpException,
			IOException {
		if (isFileExist(sftpFilePath)) {
			return inputStreamToByte(getFile(sftpFilePath));
		} else {
			return null;
		}
	}

	public String delFile(String sftpFilePath) throws SftpException {
		String retInfo = "";
		if (isFileExist(sftpFilePath)) {
			chnSftp.rm(sftpFilePath);
			retInfo = "1:File deleted.";
		} else {
			retInfo = "2:Delete file error,file not exist.";
		}

		return retInfo;
	}

	public String moveFile(String srcSftpFilePath, String distSftpFilePath)
			throws SftpException, IOException {
		String retInfo = "";
		boolean dirExist = false;
		boolean fileExist = false;
		fileExist = isFileExist(srcSftpFilePath);
		dirExist = isDirExist(distSftpFilePath);
		if (!fileExist) {
			return "0:file not exist !";
		} else {
			if (!dirExist) {
				createDir(distSftpFilePath);
				dirExist = true;
			}

			if (dirExist && fileExist) {
				String fileName = srcSftpFilePath.substring(
						srcSftpFilePath.lastIndexOf("/"),
						srcSftpFilePath.length());
				ByteArrayInputStream srcFtpFileStreams = getByteArrayInputStreamFile(srcSftpFilePath);
				chnSftp.put(srcFtpFileStreams, distSftpFilePath + fileName);
				chnSftp.rm(srcSftpFilePath);
				retInfo = "1:move success!";
			}

			return retInfo;
		}
	}

	public String copyFile(String srcSftpFilePath, String distSftpFilePath)
			throws SftpException, IOException {
		String retInfo = "";
		boolean dirExist = false;
		boolean fileExist = false;
		fileExist = isFileExist(srcSftpFilePath);
		dirExist = isDirExist(distSftpFilePath);
		if (!fileExist) {
			return "0:file not exist !";
		} else {
			if (!dirExist) {
				createDir(distSftpFilePath);
				dirExist = true;
			}

			if (dirExist && fileExist) {
				String fileName = srcSftpFilePath.substring(
						srcSftpFilePath.lastIndexOf("/"),
						srcSftpFilePath.length());
				ByteArrayInputStream srcFtpFileStreams = getByteArrayInputStreamFile(srcSftpFilePath);
				chnSftp.put(srcFtpFileStreams, distSftpFilePath + fileName);
				retInfo = "1:copy file success!";
			}

			return retInfo;
		}
	}

	public String createDir(String sftpDirPath) throws SftpException {
		if (isDirExist(sftpDirPath)) {
			cd(sftpDirPath);
			return "0:dir is exist !";
		} else {
			String[] pathArry = sftpDirPath.split("/");
			String[] var2 = pathArry;
			int var3 = pathArry.length;

			for (int var4 = 0; var4 < var3; ++var4) {
				if (var4 == 0) {// 进入根目录再比较
					cd("/");
				}
				String path = var2[var4];
				if (!path.equals("")) {
					if (isDirExist(path)) {
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

	public boolean isFileExist(String srcSftpFilePath) throws SftpException {
		boolean isExitFlag = false;
		if (getFileSize(srcSftpFilePath) >= 0L) {
			isExitFlag = true;
		}

		return isExitFlag;
	}

	public long getFileSize(String srcSftpFilePath) throws SftpException {
		long filesize = 0L;

		try {
			SftpATTRS e = chnSftp.lstat(srcSftpFilePath);
			filesize = e.getSize();
		} catch (Exception var4) {
			filesize = -1L;
			if (var4.getMessage().toLowerCase().equals("no such file")) {
				filesize = -2L;
			}
		}

		return filesize;
	}

	public void close() {
		try {
			if (fos != null) {
				fos.close();
			}

			if (channel.isConnected()) {
				channel.disconnect();
				log.info("Channel connect  disconnect!");
			}

			if (session.isConnected()) {
				session.disconnect();
				log.info("Session connect disconnect!");
			}

			log.info("关闭sftp连接");
		} catch (IOException var1) {
			log.error("关闭sftp连接时出现异常", var1);
		}

	}

	public byte[] inputStreamToByte(InputStream iStrm) throws IOException {
		ByteArrayOutputStream bytestream = new ByteArrayOutputStream();

		int ch;
		while ((ch = iStrm.read()) != -1) {
			bytestream.write(ch);
		}

		byte[] imgdata = bytestream.toByteArray();
		bytestream.close();
		return imgdata;
	}

	public void down(String remotePath, String ftpFileName, String localDir,
			String host, int port, String username, String password)
			throws JSchException, SftpException {
		downFile(remotePath, ftpFileName, localDir, host, port, username,
				password);
	}

	public InputStream downFile(String remotepath, String ftpFileName,
			String localDir, String host, int port, String username,
			String password) {
		InputStream is = null;

		try {
			open(host, port, username, password);
			log.info("操作1 得到当前工作目录地址：" + pwd());
			cd(remotepath);
			log.info("操作2 改变目录为配置的远程目录：" + pwd());
			is = getFile(ftpFileName);
			log.info("下载文件到" + localDir + "成功！");
		} catch (Exception var5) {
			log.error("下载失败！", var5);
		}

		return is;
	}

	/**
	 * 
	 * @param ftpUploadDir
	 *            上传目录
	 * @param filename
	 *            文件名
	 * @param input
	 * @param host
	 *            主机
	 * @param port
	 *            端口
	 * @param username
	 *            用户名
	 * @param password密码
	 * @return
	 */
	public boolean uploadFile(String ftpUploadDir, String filename,
			InputStream input, String host, int port, String username,
			String password) {
		boolean result = false;

		try {
			open(host, port, username, password);
			createDir(ftpUploadDir);
			chnSftp.put(input, filename);
			close();
			result = true;
		} catch (SftpException var5) {
			log.error(var5.getMessage(), var5);
		}

		return result;
	}

	/**
	 * 不带进度的 断点续传模式
	 * 
	 * @param ftpUploadDir
	 *            上传目录
	 * @param filename
	 *            文件名
	 * @param input
	 * @param host
	 *            主机
	 * @param port
	 *            端口
	 * @param username
	 *            用户名
	 * @param password密码
	 * @return
	 */
	public boolean uploadFileResumeNoMonitors(String ftpUploadDir,
			String filename, InputStream input, String host, int port,
			String username, String password) {
		return this.uploadFileByType(ftpUploadDir, filename, input, host, port,
				username, password, chnSftp.RESUME, null);
	}

	/**
	 * 带进度的 断点续传模式
	 * 
	 * @param ftpUploadDir
	 *            上传目录
	 * @param filename
	 *            文件名
	 * @param input
	 * @param host
	 *            主机
	 * @param port
	 *            端口
	 * @param username
	 *            用户名
	 * @param password密码
	 * @return
	 */
	public boolean uploadFileResume(String ftpUploadDir, String filename,
			InputStream input, String host, int port, String username,
			String password, SftpProgressMonitor monitor) {
		return this.uploadFileByType(ftpUploadDir, filename, input, host, port,
				username, password, chnSftp.RESUME, monitor);
	}

	private boolean uploadFileByType(String ftpUploadDir, String filename,
			InputStream input, String host, int port, String username,
			String password, int type, SftpProgressMonitor monitor) {
		boolean result = false;
		try {
			open(host, port, username, password);
			createDir(ftpUploadDir);
			chnSftp.put(input, filename, monitor, type);
			close();
			result = true;
		} catch (SftpException var5) {
			log.error(var5.getMessage(), var5);
		}
		return result;
	}

	public Vector getAllFiles(String directory, String fileSuffix)
			throws SftpException {
		this.cd(directory);
		Vector vector = chnSftp.ls(directory + "/*" + fileSuffix);
		return vector;
	}

	/**
	 * 执行shell命令
	 * 
	 * @param command
	 * @return
	 */
	public int execute(String host, int port, String username, String password,
			final String command) {
		int returnCode = 0;
		JSch jsch = new JSch();
		try {
			// 创建session并且打开连接，因为创建session之后要主动打开连接
			Session session = jsch.getSession(username, host, port);
			session.setPassword(password);
			Properties sshConfig = new Properties();
			sshConfig.put("StrictHostKeyChecking", "no");
			session.setConfig(sshConfig);
			session.connect();
			// 打开通道，设置通道类型，和执行的命令
			Channel channel = session.openChannel("exec");
			ChannelExec channelExec = (ChannelExec) channel;
			channelExec.setCommand(command);
			channelExec.setInputStream(null);
			BufferedReader input = new BufferedReader(new InputStreamReader(
					channelExec.getInputStream()));
			channelExec.connect();
			log.info("The remote command is :" + command);
			// 接收远程服务器执行命令的结果
			String line;
			while ((line = input.readLine()) != null) {
				// stdout.add(line);
				log.info(line);
			}
			input.close();
			// 得到returnCode
			if (channelExec.isClosed()) {
				returnCode = channelExec.getExitStatus();
			}
			// 关闭通道
			channelExec.disconnect();
			session.disconnect();
		} catch (JSchException e) {
			log.error(e.getMessage());
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return returnCode;
	}

}
