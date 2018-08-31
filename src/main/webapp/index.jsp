<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>webuploader组件上传</title>
<!-- 1.准备好资源 -->
<script type="text/javascript" src="./js/jquery-1.10.1.min.js"></script>
<script type="text/javascript" src="./js/webuploader.js"></script>
<style type="text/css">
#dndArea{width:200px;height: 100px;border-color:red;border-style: dashed;}
.webuploader-container {
	position: relative;
}
.webuploader-element-invisible {
	position: absolute !important;
	clip: rect(1px 1px 1px 1px); /* IE6, IE7 */
    clip: rect(1px,1px,1px,1px);
}
.webuploader-pick {
	position: relative;
	display: inline-block;
	cursor: pointer;
	background: #00b7ee;
	padding: 10px 15px;
	color: #fff;
	text-align: center;
	border-radius: 3px;
	overflow: hidden;
}
.webuploader-pick-hover {
	background: #00a2d4;
}

.webuploader-pick-disable {
	opacity: 0.6;
	pointer-events:none;
}
.percentUpload{
	width:
}

</style>
</head>
<body>
	<!-- 2.设计页面元素 -->
	<div id="uploader">
		<!--该元素用于文件拖拽  -->
		<div id="dndArea"></div>
		<!-- 用于显示文件列表名 -->
		<div id="fileList"></div>
		<!-- 用于选择文件 -->
		<div id="filePicker">点击选择文件</div>
	</div>
	
	<!-- 3.使用webuploader进行渲染 -->
	<script type="text/javascript">
		var checkChunkUrl = "./checkChunks.action";
		var uploadUrl = "./upLoadFileBlock.action";
		var mergerBlockUrl = "./mergeChunks.action";
		var fileMd5;
		var fileName;
		var fileSize;
		var chunkSize = 10*1024*1024;
		var uploadedBlock = "";
		// 监听分块上传中的三个时间点
		WebUploader.Uploader.register({
			"before-send-file":"beforeSendFile",
			"before-send":"beforeSend",
			"after-send-file":"afterSendFile"
		},{
			// 时间点1：第一个分块进行上传之前调用此函数
			beforeSendFile:function(file){
				// 创建一个diferred
				fileSize =file.size;
				var deferred = WebUploader.Deferred();
				// 1.计算文件的唯一标记，用于断点续传和秒传
				(new WebUploader.Uploader()).md5File(file,0,chunkSize).progress(function(percentage){
					//console.log(percentage)
				}).then(function(val){
					fileMd5 = val;
					console.log(fileMd5)
					//获取 分块信息
					$.ajax({
						type:"POST",
						url:checkChunkUrl,
						async: false,
						dataType:"json",
						data:{
							"fileMd5":val,
						},
						success:function(data){
							uploadedBlock = JSON.stringify(data);
						},
						complete:function(){
							deferred.resolve();
						}
					}); 
				});
				// 2.请求后台是否保存过该文件，如果保存过则跳过，实现秒传
				fileName=file.name; //为自定义参数文件名赋值
				return deferred.promise();
			},
			// 时间点2：如果有分块上传，则每个分块上传前调用此函数
			// 注： 因为后台后才采用sftp resume 方式上传文件 所以并不用没次去调接口查询
			beforeSend:function(block){
				var deferred = WebUploader.Deferred();
				if(fileSize<chunkSize){
					this.owner.options.formData.chunk = "0";
				}
				this.owner.options.formData.fileMd5 = fileMd5;
				this.owner.options.formData.chunkSize = chunkSize;
				var uploadedFlag = false;
				var temp ={
						blockName :block.chunk+".block",
						size:chunkSize
				}
				//是否传过
				if(uploadedBlock.indexOf(JSON.stringify(temp))!=-1){
					uploadedFlag = true;
				};
				if(uploadedFlag){
					deferred.reject();
				}else{
					deferred.resolve();
				}
				return deferred.promise();
			},
			// 时间点3：所有分块上传完成之后调用此函数
			afterSendFile:function(){
				// 1.如果分块上传则上传成功之后合并所有分块文件
				$.ajax({
					type:"POST",
					url:mergerBlockUrl,
					dataType:"json",
					data:{
						"fileMd5":fileMd5,
						"fileName":fileName
					},
					success:function(data){
						
					}
				});
			}
		});
		// 1.初始化WebUploader,以及配置全局参数
		var uploader =  WebUploader.create({
			// flash地址
			swf: "./js/Uploader.swf",
			// 设置提交的服务器地址
			server:uploadUrl,
			// 渲染文件上传元素
			pick:"#filePicker",
			// 是否自动上传
			auto:true,
			// 开启拖拽功能，指定拖拽区域
			dnd:"#dndArea",
			// 屏蔽拖拽区域外的功能
			disableGlobalDnd:true,
			// 开启截图粘贴功能，通过粘贴来添加截屏的图片
			paste :"#uploader",
			// 是否要分片上传，开启分块上传
			chunked:true,
			// 每块大小(默认50M)
			chunkSize:chunkSize,
			//上传中准备下一个 分块
			prepareNextFile: false
		});
		// 4.实现选择文件，并提示文件的功能
		// file代表你选择到的那个文件，选择完file时就赋值唯一id了
		uploader.on("fileQueued",function(file){
			// 文件信息追加到fileList
			$("#fileList").append("<div id="+file.id+"><span><img/>"+file.name+"</span><div><span class='percentage'></span></div><span class='state'></span></div>");
			uploader.md5File(file).progress(function(percentage) {
	           // console.log('Percentage:', percentage);
	        }).then(function(val) {
	            console.log('md5 result:', val);
	        });
			// 制作缩略图 针对图片
			// error如果不是图片则有error,src代表生产后的缩略图的地址
/* 			uploader.makeThumb(file,function(error,src){
				// 判断是否已经成功生成缩略图
				if(error){
					$("#"+file.id).find("img").replaceWith("无法预览");
				}
				// 成功
				$("#"+file.id).find("img").attr("src",src);
			}); */
		});
		// 5.上传过程实现文件上传监控
		// percentage,代表文件百分比，0.1
		uploader.on("uploadProgress",function(file,percentage){
			$("#"+file.id).find("span.percentage").text(Math.round(percentage*100)+"%");
		});
	</script>
</body>
</html>