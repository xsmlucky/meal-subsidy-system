-- =============================================================================
-- 订餐业务测试 SQL（MySQL 8.0）
-- 先执行 schema.sql，再启动应用生成演示账号，或按需手工准备数据。
--
-- 事务与行锁原理（务必读完再测并发）：
-- 1. InnoDB 默认 RR。SELECT ... FOR UPDATE 对 WHERE 命中的主键/唯一索引行加 X 锁。
-- 2. 锁的持有范围是「当前事务」，不是「当前语句」。事务未 COMMIT/ROLLBACK 前，
--    其他会话对同一行的 FOR UPDATE / UPDATE 会等待。
-- 3. 因此必须：START TRANSACTION → FOR UPDATE 读余额 → 判断 → UPDATE 账户 → INSERT 订单/流水 → COMMIT。
--    若 FOR UPDATE 在事务外执行，语句一结束锁就释放，两个请求都会读到 300，各扣 200，出现超扣。
-- 4. 联合索引 uk_user_id_account_month 保证一人一月一行，行锁粒度就是该账户。
-- =============================================================================

USE `meal_subsidy`;

-- -----------------------------------------------------------------------------
-- 0. 查看演示账户（应用启动后 zhangsan 当月应有 300 可用）
-- -----------------------------------------------------------------------------
SELECT u.id, u.username, a.account_month, a.available_amount, a.frozen_amount, a.used_amount
FROM sys_user u
JOIN meal_account a ON a.user_id = u.id AND a.is_deleted = 0
WHERE u.username = 'zhangsan';

SELECT id, package_name, price, meal_type FROM meal_package WHERE is_deleted = 0;

-- 记下员工 id、账户 id、套餐 id，下面用变量模拟（按实际查询结果改）
SET @emp_id = (SELECT id FROM sys_user WHERE username = 'zhangsan' AND is_deleted = 0 LIMIT 1);
SET @dept_id = (SELECT dept_id FROM sys_user WHERE id = @emp_id);
SET @pkg_id = (SELECT id FROM meal_package WHERE package_name = '午餐套餐A' AND is_deleted = 0 LIMIT 1);
SET @pkg_name = (SELECT package_name FROM meal_package WHERE id = @pkg_id);
SET @pkg_price = (SELECT price FROM meal_package WHERE id = @pkg_id);
SET @month = DATE_FORMAT(CURDATE(), '%Y-%m');
SET @order_date = CURDATE();

-- -----------------------------------------------------------------------------
-- 1. 正确路径：事务内行锁 + 预占 + 写快照订单 + 写流水（对应提交订餐）
-- -----------------------------------------------------------------------------
START TRANSACTION;

-- 锁账户行，后续会话会被阻塞
SELECT id, available_amount, frozen_amount, used_amount
FROM meal_account
WHERE user_id = @emp_id AND account_month = @month AND is_deleted = 0
LIMIT 1
FOR UPDATE;

-- 校验可用额度（应用里不足则抛 INSUFFICIENT_BALANCE 并回滚）
-- 预占：可用减少，预占增加。此处不要改 used_amount（审核通过才实扣）
UPDATE meal_account
SET available_amount = available_amount - @pkg_price,
    frozen_amount = frozen_amount + @pkg_price
WHERE user_id = @emp_id AND account_month = @month AND is_deleted = 0
  AND available_amount >= @pkg_price;

INSERT INTO meal_order (
  order_no, user_id, dept_id, package_id,
  package_name, package_price, order_amount,
  order_date, meal_type, order_status, refund_deadline, is_deleted
) VALUES (
  CONCAT('MO_TEST_', UNIX_TIMESTAMP()),
  @emp_id, @dept_id, @pkg_id,
  @pkg_name, @pkg_price, @pkg_price,          -- 快照：此后改套餐表不影响本单
  @order_date, 'LUNCH', 'PENDING',
  CONCAT(@order_date, ' 10:30:00'),
  0
);

SET @oid = LAST_INSERT_ID();

INSERT INTO meal_account_log (
  log_no, account_id, user_id, dept_id, change_type, biz_source, change_amount,
  before_available, before_frozen, before_used,
  after_available, after_frozen, after_used,
  order_id, operator_id, is_deleted
)
SELECT
  CONCAT('AL_TEST_', UNIX_TIMESTAMP()),
  a.id, a.user_id, a.dept_id, 'FREEZE', 'ORDER', @pkg_price,
  a.available_amount + @pkg_price, a.frozen_amount - @pkg_price, a.used_amount,
  a.available_amount, a.frozen_amount, a.used_amount,
  @oid, @emp_id, 0
FROM meal_account a
WHERE a.user_id = @emp_id AND a.account_month = @month AND a.is_deleted = 0;

COMMIT;

SELECT * FROM meal_account WHERE user_id = @emp_id AND account_month = @month;
SELECT id, order_no, package_name, package_price, order_status FROM meal_order WHERE id = @oid;
SELECT log_no, change_type, biz_source, change_amount, before_available, after_available
FROM meal_account_log WHERE order_id = @oid;

-- -----------------------------------------------------------------------------
-- 2. 快照校验：改套餐价格后，历史订单 package_price 不变
-- -----------------------------------------------------------------------------
UPDATE meal_package SET price = 99.00 WHERE id = @pkg_id;
SELECT o.package_price AS order_snapshot, p.price AS current_package_price
FROM meal_order o
JOIN meal_package p ON p.id = o.package_id
WHERE o.id = @oid;
-- 期望：order_snapshot 仍为 15.00，current_package_price 为 99.00
UPDATE meal_package SET price = 15.00 WHERE id = @pkg_id;

-- -----------------------------------------------------------------------------
-- 3. 撤销待审核：释放预占（对应 CANCELLED）
-- -----------------------------------------------------------------------------
START TRANSACTION;
SELECT * FROM meal_account
WHERE user_id = @emp_id AND account_month = @month AND is_deleted = 0
FOR UPDATE;

UPDATE meal_account
SET available_amount = available_amount + @pkg_price,
    frozen_amount = frozen_amount - @pkg_price
WHERE user_id = @emp_id AND account_month = @month;

UPDATE meal_order
SET order_status = 'CANCELLED', cancel_time = NOW()
WHERE id = @oid AND order_status = 'PENDING';

COMMIT;

-- -----------------------------------------------------------------------------
-- 4. 非法状态：已撤销再审核应失败（应用抛 ILLEGAL_ORDER_STATUS）
-- -----------------------------------------------------------------------------
SELECT order_status FROM meal_order WHERE id = @oid;
-- 期望：CANCELLED，审核接口应拒绝

-- -----------------------------------------------------------------------------
-- 5. 并发超扣对照（请开两个查询窗口手动演示）
-- 窗口 A：
--   START TRANSACTION;
--   SELECT * FROM meal_account WHERE user_id=@emp_id AND account_month=@month FOR UPDATE;
--   -- 先不要 COMMIT，把 available 看成 20，准备扣 15
-- 窗口 B：
--   START TRANSACTION;
--   SELECT * FROM meal_account WHERE user_id=@emp_id AND account_month=@month FOR UPDATE;
--   -- 此时 B 会卡住，直到 A COMMIT。这就是行锁防超扣。
-- 错误示范（不要这么写业务）：
--   -- 事务外单独 SELECT 余额，再另开事务 UPDATE，两个请求都会看到 20，各扣 15 变成 -10。
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- 6. 月报走索引（EXPLAIN 应显示 idx_dept_id_order_date_order_status）
-- 实扣口径：APPROVED；退餐备查：REFUNDED；金额用订单快照，不 JOIN 套餐表
-- -----------------------------------------------------------------------------
EXPLAIN
SELECT
  COUNT(DISTINCT CASE WHEN order_status = 'APPROVED' THEN user_id END) AS user_count,
  COUNT(CASE WHEN order_status = 'APPROVED' THEN 1 END) AS approved_count,
  IFNULL(SUM(CASE WHEN order_status = 'APPROVED' THEN order_amount END), 0) AS approved_amount,
  COUNT(CASE WHEN order_status = 'REFUNDED' THEN 1 END) AS refund_count,
  IFNULL(SUM(CASE WHEN order_status = 'REFUNDED' THEN order_amount END), 0) AS refund_amount
FROM meal_order
WHERE dept_id = @dept_id
  AND is_deleted = 0
  AND order_date >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
  AND order_date < DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH);

-- -----------------------------------------------------------------------------
-- 7. 权限：员工不能看到他人订单（应用层 ORDER_ACCESS_DENIED）
-- -----------------------------------------------------------------------------
-- 用管理员账号审核他人部门订单应 403；用 zhangsan 访问别人的 orderId 应 1004
SELECT id, user_id, dept_id, order_status FROM meal_order ORDER BY id DESC LIMIT 5;
