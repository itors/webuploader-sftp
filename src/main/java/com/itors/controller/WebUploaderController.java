package com.itors.controller;

import java.io.IOException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.itors.util.Constant;
import com.itors.util.DateUtil;
import com.itors.util.SFTPUtils;
import com.itors.vo.SystemFileMsg;

@Controller
public class WebUploaderController {
	
	private static Log logger = LogFactory.getLog(WebUploaderController.class);
	
	@Autowired
	private SystemFileMsg sysFile;

    /**
     * ��֤ÿ���ֿ飬��鵱ǰ�ֿ��Ƿ��ϴ��ɹ�
     * @param request
     * @param response
     */
    @RequestMapping(params="/checkChunks")
    @ResponseBody
    public Object checkChunk(HttpServletRequest request) {
    	logger.debug("---------------- ��ȡ ���ϴ����ļ� ------------------");
    	
    	List<Map> blocks = new ArrayList<Map>();
    	// �ֿ�Ŀ¼
		String fileMd5 = request.getParameter("fileMd5");
		// �ֿ��С
		String ftpUploadDirTemp = "";
		SFTPUtils sftpUtils = new SFTPUtils();
		try {
			sftpUtils.open(sysFile.getSftpHost(),sysFile.getSftpPort(),sysFile.getSftpUsername(),sysFile.getSftpPassword());
    		// �õ��ֿ����ʱ�ļ���
        	ftpUploadDirTemp = sysFile.getFileUploadPath()+"/"+Constant.TEMPLATE_DIR+"/"+fileMd5;
        	if(sftpUtils.isDirExist(ftpUploadDirTemp)){
        		Vector vector = sftpUtils.getAllFiles(ftpUploadDirTemp,Constant.BLOCK_SUFFIX);
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
			sftpUtils.close();
		}
		return blocks;
    }
    
    /**
     * �ϲ��ļ�
     * @param request
     * @param response
     * @throws IOException 
     */
    @RequestMapping(params="/mergeChunks")
    @ResponseBody
    public Object mergeChunks(HttpServletRequest request,HttpServletResponse response) throws IOException {
    	logger.debug("----------------�ϲ��ļ�start------------------");
    	// ��Ҫ�ϲ����ļ��ı��
		String fileMd5 = request.getParameter("fileMd5");
		String fileName = request.getParameter("fileName");
		String fileSize = request.getParameter("fileSize");
		SFTPUtils sftpUtils = new SFTPUtils();
		Map<String, Object> mesMap = new HashMap<String, Object>();
    	Map<String,Object> map = new HashMap<String,Object>();
    	StringBuilder command = new StringBuilder("cat ");
		try {
			String fileSuffix = fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase();
    		// �õ��ֿ����ʱ�ļ���
        	String tempPath = DateUtil.parseDateToStr(new Date(), DateUtil.DATE_FORMAT_YYYYMMDD);	
        	String blockDir = sysFile.getFileUploadPath()+"/"+Constant.TEMPLATE_DIR + "/" + fileMd5;
           	String ftpUploadDir = sysFile.getFileUploadPath()+"/"+tempPath;
        	sftpUtils.open(sysFile.getSftpHost(),sysFile.getSftpPort(),sysFile.getSftpUsername(),sysFile.getSftpPassword());
			if(sftpUtils.isDirExist(blockDir)) {
				sftpUtils.cd(blockDir);
				//У���Ƿ��ϴ���
				String ftpFileName = System.currentTimeMillis()+"."+fileSuffix;
				String realFile = ftpUploadDir+"/"+ftpFileName;
				Vector vector = sftpUtils.getAllFiles(blockDir,Constant.BLOCK_SUFFIX);
				int fileNum = vector.size();
				if(sftpUtils.isFileExist(blockDir+"/"+(vector.size()-1)+Constant.BLOCK_SUFFIX)){
					for (int i = 0; i < fileNum; i++) {
						command.append(" "+blockDir+"/"+i + Constant.BLOCK_SUFFIX);
					}
					command.append("> " + realFile);
				}
				int result = sftpUtils.execute(sysFile.getSftpHost(), sysFile.getSftpPort(),sysFile.getSftpUsername(),sysFile.getSftpPassword(),command.toString());
				String rmFile = "rm -rf " + blockDir;
				if(result!=-1){
					result = sftpUtils.execute(sysFile.getSftpHost(), sysFile.getSftpPort(),sysFile.getSftpUsername(),sysFile.getSftpPassword(),rmFile);
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
			sftpUtils.close();
		}
		return map;
    }
    
    
    /**
     * �ϵ�����
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping(params="/upLoadFileBlock")
    @ResponseBody
    public boolean upLoadFileBlock(@RequestParam("file") MultipartFile file,HttpServletRequest request) throws IOException{
    	logger.debug("------------�ϴ��ֿ�start-------------------");
    	boolean uploadFlag = true;
		String fileMd5 = request.getParameter("fileMd5");
		String chunk = request.getParameter("chunk");
		SFTPUtils sftpUtils = new SFTPUtils();
		if(null==chunk ||"".equals(chunk) || null== fileMd5 || "".equals(fileMd5) ){
			return false;
		}
		// 4.��ʼ�����ļ�
		try {
			String ftpUploadDir = sysFile.getFileUploadPath()+"/"+Constant.TEMPLATE_DIR+"/"+fileMd5;
			logger.debug(ftpUploadDir);
        	// 2.�����ļ�
        	sftpUtils.uploadFileResumeNoMonitors(ftpUploadDir, chunk+ Constant.BLOCK_SUFFIX ,file.getInputStream(), sysFile.getSftpHost(), sysFile.getSftpPort(),sysFile.getSftpUsername(),sysFile.getSftpPassword());
		} catch (Exception e) {
			logger.error(e.getMessage());
			uploadFlag = false;
		}finally { 
			sftpUtils.close();
		}
		return uploadFlag;
    }
}