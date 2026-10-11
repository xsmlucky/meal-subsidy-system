-- =============================================================================
-- 员工后勤餐补订餐管理系统 - MySQL 8.0 建表
-- 适配 MyBatis-Plus：BIGINT 自增主键、is_deleted 逻辑删除、create_time / update_time 自动填充
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `meal_subsidy`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `meal_subsidy`;

DROP TABLE IF EXISTS `meal_refund`;
DROP TABLE IF EXISTS `meal_audit`;
DROP TABLE IF EXISTS `meal_order`;
DROP TABLE IF EXISTS `meal_month_stat`;
DROP TABLE IF EXISTS `dish`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `dept`;
DROP TABLE IF EXISTS `meal_account_log`;
DROP TABLE IF EXISTS `meal_account`;
DROP TABLE IF EXISTS `meal_package`;

-- -----------------------------------------------------------------------------
-- 1. 部门表
-- -----------------------------------------------------------------------------
CREATE TABLE `dept` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_name`    VARCHAR(64)  NOT NULL                COMMENT '部门名称',
  `dept_code`    VARCHAR(32)  NOT NULL                COMMENT '部门编码，唯一',
  `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-停用 1-启用',
  `remark`       VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`   TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- -----------------------------------------------------------------------------
-- 2. 用户表
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_user` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`     VARCHAR(64)   NOT NULL                COMMENT '登录账号，唯一',
  `password`     VARCHAR(128)  NOT NULL                COMMENT '登录密码，BCrypt加密存储',
  `real_name`    VARCHAR(64)   NOT NULL                COMMENT '真实姓名',
  `employee_no`  VARCHAR(32)            DEFAULT NULL   COMMENT '工号',
  `dept_id`      BIGINT        NOT NULL                COMMENT '所属部门ID，关联 dept.id，数据权限按此过滤',
  `role`         VARCHAR(32)   NOT NULL                COMMENT '角色：EMPLOYEE-普通员工 ADMIN-后勤管理员',
  `status`       TINYINT       NOT NULL DEFAULT 1      COMMENT '账号状态：0-停用 1-启用',
  `is_deleted`   TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- -----------------------------------------------------------------------------
-- 3. 菜品表
-- -----------------------------------------------------------------------------
CREATE TABLE `dish` (
  `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dish_name`    VARCHAR(64)    NOT NULL                COMMENT '菜品名称',
  `price`        DECIMAL(10,2)  NOT NULL                COMMENT '菜品单价（元）',
  `meal_type`    VARCHAR(32)    NOT NULL                COMMENT '适用餐次：BREAKFAST-早餐 LUNCH-午餐 DINNER-晚餐',
  `status`       TINYINT        NOT NULL DEFAULT 1      COMMENT '上架状态：0-下架 1-上架',
  `sort_no`      INT            NOT NULL DEFAULT 0      COMMENT '排序号，越小越靠前',
  `remark`       VARCHAR(255)            DEFAULT NULL   COMMENT '备注',
  `is_deleted`   TINYINT        NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_meal_type_status` (`meal_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品表';

-- -----------------------------------------------------------------------------
-- 4. 月度餐补统计台账（一人一月一行，订餐时行锁本表防超扣）
-- -----------------------------------------------------------------------------
CREATE TABLE `meal_month_stat` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT         NOT NULL                COMMENT '员工用户ID，关联 sys_user.id',
  `dept_id`         BIGINT         NOT NULL                COMMENT '部门ID，关联 dept.id',
  `stat_month`      CHAR(7)        NOT NULL                COMMENT '统计月份，格式 YYYY-MM',
  `quota_amount`    DECIMAL(10,2)  NOT NULL DEFAULT 0.00   COMMENT '本月发放餐补额度（元）',
  `frozen_amount`   DECIMAL(10,2)  NOT NULL DEFAULT 0.00   COMMENT '预占额度（元），待审核订单占用',
  `used_amount`     DECIMAL(10,2)  NOT NULL DEFAULT 0.00   COMMENT '已用额度（元），审核通过后实扣；可用额度=quota_amount-frozen_amount-used_amount',
  `order_count`     INT            NOT NULL DEFAULT 0      COMMENT '本月有效订餐份数（已通过未退款）',
  `refund_count`    INT            NOT NULL DEFAULT 0      COMMENT '本月退款通过份数',
  `refund_amount`   DECIMAL(10,2)  NOT NULL DEFAULT 0.00   COMMENT '本月退款通过金额（元）',
  `is_deleted`      TINYINT        NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id_stat_month` (`user_id`, `stat_month`),
  KEY `idx_dept_id_stat_month` (`dept_id`, `stat_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度餐补台账：一人一月一行；订餐事务内对本行 SELECT ... FOR UPDATE 悲观锁防超扣';

-- -----------------------------------------------------------------------------
-- 5. 订餐订单表（菜品名称、价格下单快照，改菜品不影响历史订单）
-- -----------------------------------------------------------------------------
CREATE TABLE `meal_order` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`         VARCHAR(32)    NOT NULL                COMMENT '订单号，全局唯一',
  `user_id`          BIGINT         NOT NULL                COMMENT '下单员工ID，关联 sys_user.id',
  `dept_id`          BIGINT         NOT NULL                COMMENT '下单时部门ID，管理员数据权限按此过滤',
  `dish_id`          BIGINT         NOT NULL                COMMENT '菜品ID，关联 dish.id',
  `dish_name`        VARCHAR(64)    NOT NULL                COMMENT '菜品名称快照',
  `dish_price`       DECIMAL(10,2)  NOT NULL                COMMENT '菜品单价快照（元）',
  `order_amount`     DECIMAL(10,2)  NOT NULL                COMMENT '订单金额（元），等于菜品单价快照',
  `order_date`       DATE           NOT NULL                COMMENT '用餐日期',
  `meal_type`        VARCHAR(32)    NOT NULL                COMMENT '餐次：BREAKFAST-早餐 LUNCH-午餐 DINNER-晚餐',
  `order_status`     VARCHAR(32)    NOT NULL                COMMENT '订单状态：PENDING-待审核 APPROVED-已通过 REJECTED-已驳回 CANCELLED-已撤销 REFUND_PENDING-退款待审 REFUNDED-已退款',
  `reject_reason`    VARCHAR(255)            DEFAULT NULL   COMMENT '订单审核驳回原因',
  `refund_deadline`  DATETIME       NOT NULL                COMMENT '申请退款截止时间',
  `is_deleted`       TINYINT        NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_dept_id_order_date_order_status` (`dept_id`, `order_date`, `order_status`) COMMENT '部门月度统计联合索引',
  KEY `idx_user_id_order_date` (`user_id`, `order_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订餐订单表';

-- -----------------------------------------------------------------------------
-- 6. 订单审核表
-- -----------------------------------------------------------------------------
CREATE TABLE `meal_audit` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`       BIGINT        NOT NULL                COMMENT '订单ID，关联 meal_order.id',
  `dept_id`        BIGINT        NOT NULL                COMMENT '订单所属部门ID',
  `auditor_id`     BIGINT        NOT NULL                COMMENT '审核人用户ID，关联 sys_user.id',
  `audit_result`   VARCHAR(32)   NOT NULL                COMMENT '审核结果：PASS-通过 REJECT-驳回',
  `audit_opinion`  VARCHAR(255)           DEFAULT NULL   COMMENT '审核意见，驳回时必填',
  `audit_time`     DATETIME      NOT NULL                COMMENT '审核时间',
  `is_deleted`     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单审核表';

-- -----------------------------------------------------------------------------
-- 7. 退款记录表（员工申请，管理员审核）
-- -----------------------------------------------------------------------------
CREATE TABLE `meal_refund` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no`       VARCHAR(32)    NOT NULL                COMMENT '退款单号，全局唯一',
  `order_id`        BIGINT         NOT NULL                COMMENT '原订单ID，关联 meal_order.id',
  `user_id`         BIGINT         NOT NULL                COMMENT '申请员工ID，关联 sys_user.id',
  `dept_id`         BIGINT         NOT NULL                COMMENT '部门ID，关联 dept.id',
  `refund_amount`   DECIMAL(10,2)  NOT NULL                COMMENT '退款金额（元），取订单快照金额',
  `refund_reason`   VARCHAR(255)            DEFAULT NULL   COMMENT '退款申请原因',
  `refund_status`   VARCHAR(32)    NOT NULL                COMMENT '退款状态：PENDING-待审核 APPROVED-通过 REJECTED-驳回',
  `auditor_id`      BIGINT                  DEFAULT NULL   COMMENT '退款审核人ID',
  `audit_opinion`   VARCHAR(255)            DEFAULT NULL   COMMENT '退款审核意见',
  `audit_time`      DATETIME                DEFAULT NULL   COMMENT '退款审核时间',
  `is_deleted`      TINYINT        NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_dept_id_refund_status` (`dept_id`, `refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';

SET FOREIGN_KEY_CHECKS = 1;
