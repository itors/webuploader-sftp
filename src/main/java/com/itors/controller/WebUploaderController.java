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
     * 验证每个分块，检查当前分块是否上传成功
     * @param request
     * @param response
     */
    @RequestMapping("/checkChunks")
    @ResponseBody
    public Object checkChunk(HttpServletRequest request) {
    	logger.debug("---------------- 获取 已上传的文件 ------------------");
    	
    	List<Map> blocks = new ArrayList<Map>();
    	// 分块目录
		String fileMd5 = request.getParameter("fileMd5");
		// 分块大小
		String ftpUploadDirTemp = "";
		SFTPUtils sftpUtils = new SFTPUtils();
		try {
			sftpUtils.open(sysFile.getSftpHost(),sysFile.getSftpPort(),sysFile.getSftpUsername(),sysFile.getSftpPassword());
    		// 得到分块的临时文件夹
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
     * 合并文件
     * @param request
     * @param response
     * @throws IOException 
     */
    @RequestMapping("/mergeChunks")
    @ResponseBody
    public Object mergeChunks(HttpServletRequest request,HttpServletResponse response) throws IOException {
    	logger.debug("----------------合并文件start------------------");
    	// 需要合并的文件的标记
		String fileMd5 = request.getParameter("fileMd5");
		String fileName = request.getParameter("fileName");
		String fileSize = request.getParameter("fileSize");
		SFTPUtils sftpUtils = new SFTPUtils();
		Map<String, Object> mesMap = new HashMap<String, Object>();
    	Map<String,Object> map = new HashMap<String,Object>();
    	StringBuilder command = new StringBuilder("cat ");
		try {
			String fileSuffix = fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase();
    		// 得到分块的临时文件夹
        	String tempPath = DateUtil.parseDateToStr(new Date(), DateUtil.DATE_FORMAT_YYYYMMDD);	
        	String blockDir = sysFile.getFileUploadPath()+"/"+Constant.TEMPLATE_DIR + "/" + fileMd5;
           	String ftpUploadDir = sysFile.getFileUploadPath()+"/"+tempPath;
        	sftpUtils.open(sysFile.getSftpHost(),sysFile.getSftpPort(),sysFile.getSftpUsername(),sysFile.getSftpPassword());
			if(sftpUtils.isDirExist(blockDir)) {
				sftpUtils.cd(blockDir);
				//校验是否上传完
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
				map.put("msg", "上传成功");
			}else {
				// 上传失败
				logger.debug("合并失败，没有取到分块文件");
    			map.put(Constant.RESULT_STAT, Constant.RESULT_STAT_ERROR);
				map.put("msg", "上传失败");
			}
		}catch(Exception e) {
			logger.error(e.getMessage(),e);
			map.put(Constant.RESULT_STAT, Constant.RESULT_STAT_ERROR);
			map.put("msg", "上传失败");
		}finally {
			sftpUtils.close();
		}
		return map;
    }
    
    
    /**
     * 断点续传
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/upLoadFileBlock")
    @ResponseBody
    public boolean upLoadFileBlock(@RequestParam("file") MultipartFile file,HttpServletRequest request) throws IOException{
    	logger.debug("------------上传分块start-------------------");
    	boolean uploadFlag = true;
		String fileMd5 = request.getParameter("fileMd5");
		String chunk = request.getParameter("chunk");
		SFTPUtils sftpUtils = new SFTPUtils();
		if(null==chunk ||"".equals(chunk) || null== fileMd5 || "".equals(fileMd5) ){
			return false;
		}
		// 4.开始解析文件
		try {
			String ftpUploadDir = sysFile.getFileUploadPath()+"/"+Constant.TEMPLATE_DIR+"/"+fileMd5;
			logger.debug(ftpUploadDir);
        	// 2.保存文件
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
