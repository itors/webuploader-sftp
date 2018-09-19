package com.itors.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.itors.util.Constant;
import com.itors.util.DateUtil;
import com.itors.util.SftpUtil;
import com.itors.util.sftpPool.KeyedSftpFactory;
import com.itors.vo.SftpConfig;

@Controller
public class WebUploaderController {
	
	private static Log logger = LogFactory.getLog(WebUploaderController.class);
    @RequestMapping(params="/test")
    @ResponseBody
    public Object test(HttpServletRequest request) {
    	return 22;
    }
    /**
     * ��֤ÿ���ֿ飬��鵱ǰ�ֿ��Ƿ��ϴ��ɹ�
     * @param request
     * @param response
     * @throws Exception 
     */
    @RequestMapping("/checkChunks")
    @ResponseBody
    public Object checkChunk(HttpServletRequest request) throws Exception {
    	logger.debug("---------------- ��ȡ ���ϴ����ļ��� ------------------");
    	List<Map> blocks = new ArrayList<Map>();
    	// �ֿ�Ŀ¼
		String fileMd5 = request.getParameter("fileMd5");
		// �ֿ��С
		String ftpUploadDirTemp = "";
		// �ӳ��л�ȡ����
		SftpUtil sftpUtil = KeyedSftpFactory.getSftpPool().borrowSftpUtil(Constant.SFTP_01);
		SftpConfig sftpCfg = sftpUtil.getSftpCfg();
		try {
    		// �õ��ֿ����ʱ�ļ���
        	ftpUploadDirTemp = sftpCfg.getFileUploadPath()+"/"+Constant.TEMPLATE_DIR+"/"+fileMd5;
        	if(sftpUtil.isDirExist(ftpUploadDirTemp)){
        		Vector vector = sftpUtil.getAllFiles(ftpUploadDirTemp,Constant.BLOCK_SUFFIX);
           	 	for(Object obj :vector){
                    if(obj instanceof com.jcraft.jsch.ChannelSftp.LsEntry){
                        String fileName = ((com.jcraft.jsch.ChannelSftp.LsEntry)obj).getFilename();
                        Long size = ((com.jcraft.jsch.ChannelSftp.LsEntry) obj).getAttrs().getSize();
                        Map map = new HashMap();
                        map.put("blockName", fileName);
                        map.put("size", size);
                        blocks.add(map);
                    }
                }
        	}
		}catch(Exception e) {
			logger.error(e.getMessage(),e);
		}finally {
			 KeyedSftpFactory.getSftpPool().returnSftpUtil(Constant.SFTP_01, sftpUtil);
		}
		return blocks;
    }
    
    /**
     * �ϵ�����
     * @param request
     * @param response
     * @throws Exception 
     */
    @RequestMapping("/upLoadFileBlock")
    @ResponseBody
    public boolean upLoadFileBlock(@RequestParam("file") MultipartFile file,HttpServletRequest request) throws Exception{
    	logger.debug("------------�ϴ��ֿ�start-------------------");
    	boolean uploadFlag = true;
		String fileMd5 = request.getParameter("fileMd5");
		String chunk = request.getParameter("chunk");
		SftpUtil sftpUtil = KeyedSftpFactory.getSftpPool().borrowSftpUtil(Constant.SFTP_01);
		SftpConfig sftpCfg = sftpUtil.getSftpCfg();
		if(null==chunk ||"".equals(chunk) || null== fileMd5 || "".equals(fileMd5) ){
			return false;
		}
		// 4.��ʼ�����ļ�
		try {
			String ftpUploadDir = sftpCfg.getFileUploadPath()+"/"+Constant.TEMPLATE_DIR+"/"+fileMd5;
			logger.debug(ftpUploadDir);
        	// 2.�����ļ�
			sftpUtil.uploadFileToSftp(ftpUploadDir, chunk+ Constant.BLOCK_SUFFIX , file.getInputStream());
		} catch (Exception e) {
			logger.error(e.getMessage());
			uploadFlag = false;
		}finally { 
			 KeyedSftpFactory.getSftpPool().returnSftpUtil(Constant.SFTP_01, sftpUtil);
		}
		return uploadFlag;
    }
    
    /**
     * �ϲ��ļ�
     * @param request
     * @param response
     * @throws Exception 
     */
    @RequestMapping("/mergeChunks")
    @ResponseBody
    public Object mergeChunks(HttpServletRequest request,HttpServletResponse response) throws Exception {
    	logger.debug("----------------�ϲ��ļ�start------------------");
    	// ��Ҫ�ϲ����ļ��ı��
		String fileMd5 = request.getParameter("fileMd5");
		String fileName = request.getParameter("fileName");
		String fileSize = request.getParameter("fileSize");
		SftpUtil sftpUtil = KeyedSftpFactory.getSftpPool().borrowSftpUtil(Constant.SFTP_01);
		SftpConfig sftpCfg = sftpUtil.getSftpCfg();
		
		Map<String, Object> mesMap = new HashMap<String, Object>();
    	Map<String,Object> map = new HashMap<String,Object>();
    	StringBuilder command = new StringBuilder("cat ");
		try {
			String fileSuffix = fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase();
    		// �õ��ֿ����ʱ�ļ���
        	String tempPath = DateUtil.parseDateToStr(new Date(), DateUtil.DATE_FORMAT_YYYYMMDD);	
        	String blockDir = sftpCfg.getFileUploadPath()+"/"+Constant.TEMPLATE_DIR + "/" + fileMd5;
           	String ftpUploadDir = sftpCfg.getFileUploadPath()+"/"+tempPath;
			if(sftpUtil.isDirExist(blockDir)) {
				sftpUtil.cd(blockDir);
				//У���Ƿ��ϴ���
				String ftpFileName = System.currentTimeMillis()+"."+fileSuffix;
				String realFile = ftpUploadDir+"/"+ftpFileName;
				Vector vector = sftpUtil.getAllFiles(blockDir,Constant.BLOCK_SUFFIX);
				int fileNum = vector.size();
				if(sftpUtil.isFileExist(blockDir+"/"+(vector.size()-1)+Constant.BLOCK_SUFFIX)){
					for (int i = 0; i < fileNum; i++) {
						command.append(" "+blockDir+"/"+i + Constant.BLOCK_SUFFIX);
					}
					command.append("> " + realFile);
				}
				int result = sftpUtil.executeShell(command.toString());
				String rmFile = "rm -rf " + blockDir;
				if(Constant.EXEC_SHELL_RESULT_ERROR.equals(result)){
					result = sftpUtil.executeShell(rmFile);
				}
				map.put(Constant.RESULT_STAT, Constant.RESULT_STAT_SUCCESS);
				map.put("msg", "�ϴ��ɹ�");
			}else {
				// �ϴ�ʧ��
				logger.debug("�ϲ�ʧ�ܣ�û��ȡ���ֿ��ļ�");
    			map.put(Constant.RESULT_STAT, Constant.RESULT_STAT_ERROR);
				map.put("msg", "�ϴ�ʧ��");
			}
		}catch(Exception e) {
			logger.error(e.getMessage(),e);
			map.put(Constant.RESULT_STAT, Constant.RESULT_STAT_ERROR);
			map.put("msg", "�ϴ�ʧ��");
		}finally {
			 KeyedSftpFactory.getSftpPool().returnSftpUtil(Constant.SFTP_01, sftpUtil);
		}
		return map;
    }
}