/*
Navicat MySQL Data Transfer

Source Server         : tets
Source Server Version : 50703
Source Host           : localhost:3306
Source Database       : webuploader-sftp

Target Server Type    : MYSQL
Target Server Version : 50703
File Encoding         : 65001

Date: 2018-09-19 20:15:17
*/
drop database if exists webuploader-sftp;
create database webuploader-sftp;
use webuploader-sftp;

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for sys_cfg
-- ----------------------------
DROP TABLE IF EXISTS `sys_cfg`;
CREATE TABLE `sys_cfg` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cfg_name` varchar(255) DEFAULT NULL COMMENT '配置名',
  `cfg_value` varchar(255) DEFAULT '' COMMENT '配置值',
  `create_user` int(11) DEFAULT NULL COMMENT '创建人',
  `create_dt` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of sys_cfg
-- ----------------------------
INSERT INTO `sys_cfg` VALUES ('1', 'sftp_01', '{\"fileUploadPath\":\"/home/mysftp\",\"sftpHost\":\"172.20.10.6\",\"sftpPassword\":\"root\",\"sftpPort\":22,\"sftpUsername\":\"root\",\"timeout\":\"10000\"}\r\n', '1', '2018-09-19 15:54:30');
INSERT INTO `sys_cfg` VALUES ('2', 'sftp_02', '{\"fileUploadPath\":\"/home/mysftp\",\"sftpHost\":\"172.20.10.6\",\"sftpPassword\":\"root\",\"sftpPort\":22,\"sftpUsername\":\"root\",\"timeout\":\"10000\"}\r\n', '1', '2018-09-19 15:54:30');
