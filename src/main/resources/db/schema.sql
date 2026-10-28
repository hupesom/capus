-- ============================================================
-- 校园二手交易平台 数据库建表脚本
-- 用法：mysql -uroot -p < schema.sql
-- 生成时间：2026-10-27
-- ============================================================

CREATE DATABASE IF NOT EXISTS `trade` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `trade`;

-- ---------- 用户表 ----------
CREATE TABLE IF NOT EXISTS `user` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(20) NOT NULL COMMENT '用户名',
  `password` varchar(32) NOT NULL COMMENT '密码',
  `nickname` varchar(10) DEFAULT '' COMMENT '昵称',
  `email` varchar(128) DEFAULT '' COMMENT '邮箱',
  `user_pic` varchar(1000) DEFAULT '' COMMENT '头像',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ---------- 商品类目表 ----------
CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_name` varchar(50) NOT NULL COMMENT '类目名称',
  `category_alias` varchar(50) NOT NULL COMMENT '类目别名',
  `create_user` int NOT NULL COMMENT '创建人id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------- 商品表 ----------
CREATE TABLE IF NOT EXISTS `goods` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL COMMENT '商品标题',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价/参考价',
  `cover_img` varchar(500) DEFAULT NULL COMMENT '商品主图(OSS地址)',
  `category_id` bigint NOT NULL COMMENT '类目id',
  `seller_id` int NOT NULL COMMENT '卖家id',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1在售 2已售出 3已下架 4待审核',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `create_user` int NOT NULL COMMENT '发布人id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------- 订单表 ----------
CREATE TABLE IF NOT EXISTS `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `buyer_id` int NOT NULL COMMENT '买家id',
  `seller_id` int NOT NULL COMMENT '卖家id',
  `goods_id` bigint NOT NULL COMMENT '商品id',
  `price` decimal(10,2) NOT NULL COMMENT '成交价',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1待确认 2已完成 3已取消',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------- 收藏表 ----------
CREATE TABLE IF NOT EXISTS `favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '收藏人',
  `goods_id` bigint NOT NULL COMMENT '商品id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_goods` (`user_id`,`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------- 收货地址表 ----------
CREATE TABLE IF NOT EXISTS `address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `receiver` varchar(50) NOT NULL COMMENT '联系人',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `address` varchar(200) NOT NULL COMMENT '地址/面交地点',
  `is_default` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------- 初始化类目（全局可见，create_user=1 表示平台预置）----------
INSERT INTO category (category_name, category_alias, create_user) VALUES
  ('教材教辅','jc',1),
  ('电子设备','dz',1),
  ('服饰鞋包','fs',1),
  ('生活用品','sh',1),
  ('运动户外','yd',1);
