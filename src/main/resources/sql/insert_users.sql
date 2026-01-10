-- 插入用户数据
-- 注意：密码已使用BCrypt加密，所有用户的初始密码都是 "123456"
-- 如需修改密码，请使用 PasswordUtil.encode() 方法重新生成BCrypt哈希值

-- 插入管理员
INSERT INTO `user` (`username`, `password`, `userType`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pDHw', 'ADMIN');

-- 插入4个队长
INSERT INTO `user` (`username`, `password`, `userType`) VALUES
('captain1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pDHw', 'CAPTAIN'),
('captain2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pDHw', 'CAPTAIN'),
('captain3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pDHw', 'CAPTAIN'),
('captain4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pDHw', 'CAPTAIN');

-- 说明：
-- 1. 管理员用户名：admin，密码：123456
-- 2. 队长用户名：captain1, captain2, captain3, captain4，密码都是：123456
-- 3. 如需生成新的BCrypt密码，可以使用以下Java代码：
--    String password = PasswordUtil.encode("你的密码");
--    System.out.println(password);
