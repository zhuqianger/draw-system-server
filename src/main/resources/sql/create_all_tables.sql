-- 创建LOL比赛选手拍卖系统的所有数据表

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键，自增',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名，唯一',
    `password` VARCHAR(255) NOT NULL COMMENT '密码，加密存储',
    `userType` VARCHAR(20) NOT NULL DEFAULT 'CAPTAIN' COMMENT '用户类型：ADMIN-管理员，CAPTAIN-队长',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_userType` (`userType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 拍卖流程表（AuctionSession）
CREATE TABLE IF NOT EXISTS `auction_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '拍卖流程ID，主键，自增',
    `sessionName` VARCHAR(100) NOT NULL COMMENT '拍卖流程名称',
    `dataSourceFile` VARCHAR(255) COMMENT '数据来源Excel文件路径',
    `captainIds` TEXT COMMENT '队长ID列表（JSON格式）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED-已创建，ACTIVE-进行中，FINISHED-已结束',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拍卖流程表';

-- 3. 队员表（重新设计字段）
CREATE TABLE IF NOT EXISTS `player` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '队员ID，主键，自增',
    `sessionId` BIGINT NOT NULL COMMENT '所属拍卖流程ID',
    `groupId` INT COMMENT '队员序号',
    `groupName` VARCHAR(50) COMMENT '群内名字',
    `gameId` VARCHAR(50) COMMENT '游戏ID名字',
    `position` VARCHAR(50) COMMENT '擅长的位置（可多个，逗号分隔）',
    `heroes` VARCHAR(255) COMMENT '擅长的英雄（可多个，逗号分隔）',
    `rank` VARCHAR(20) COMMENT '段位',
    `cost` DECIMAL(10,2) COMMENT '费用',
    `status` VARCHAR(20) NOT NULL DEFAULT 'POOL' COMMENT '状态：POOL-待拍卖池，AUCTIONING-拍卖中，SOLD-已售出',
    `currentAuctionId` BIGINT NULL COMMENT '当前拍卖ID，如果正在拍卖中',
    `teamId` BIGINT NULL COMMENT '所属队伍ID，如果已售出',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sessionId` (`sessionId`),
    KEY `idx_status` (`status`),
    KEY `idx_teamId` (`teamId`),
    KEY `idx_currentAuctionId` (`currentAuctionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='队员表';

-- 4. 队伍表
CREATE TABLE IF NOT EXISTS `team` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '队伍ID，主键，自增',
    `sessionId` BIGINT NOT NULL COMMENT '所属拍卖流程ID',
    `captainId` BIGINT NULL COMMENT '队长ID（创建流程时可能为空，队长加入时更新）',
    `userId` BIGINT NULL COMMENT '队长用户ID（来自user表）',
    `teamName` VARCHAR(50) COMMENT '队伍名称',
    `captainName` VARCHAR(50) COMMENT '队长名称（从Excel导入）',
    `playerCount` INT NOT NULL DEFAULT 0 COMMENT '队员数量，最多4人',
    `totalCost` DECIMAL(10,2) COMMENT '队伍总费用（Excel费用平均数×5）',
    `nowCost` DECIMAL(10,2) COMMENT '队伍当前剩余费用',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_captain` (`sessionId`, `captainId`),
    KEY `idx_sessionId` (`sessionId`),
    KEY `idx_playerCount` (`playerCount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='队伍表';

-- 5. 拍卖表
CREATE TABLE IF NOT EXISTS `auction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '拍卖ID，主键，自增',
    `sessionId` BIGINT NOT NULL COMMENT '所属拍卖流程ID',
    `playerId` BIGINT NOT NULL COMMENT '被拍卖的队员ID',
    `startTime` DATETIME NOT NULL COMMENT '拍卖开始时间',
    `endTime` DATETIME NOT NULL COMMENT '拍卖结束时间',
    `duration` INT NOT NULL COMMENT '拍卖时长（秒）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-进行中，FINISHED-已结束，CANCELLED-已取消',
    `winningBidId` BIGINT NULL COMMENT '获胜竞价ID',
    `winningTeamId` BIGINT NULL COMMENT '获胜队伍ID',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sessionId` (`sessionId`),
    KEY `idx_playerId` (`playerId`),
    KEY `idx_status` (`status`),
    KEY `idx_endTime` (`endTime`),
    KEY `idx_winningTeamId` (`winningTeamId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拍卖表';

-- 6. 竞价表
CREATE TABLE IF NOT EXISTS `bid` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '竞价ID，主键，自增',
    `auctionId` BIGINT NOT NULL COMMENT '拍卖ID',
    `teamId` BIGINT NOT NULL COMMENT '出价队伍ID',
    `captainId` BIGINT NOT NULL COMMENT '出价队长ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '出价金额',
    `bidTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '出价时间',
    `isWinner` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否获胜：0-否，1-是',
    PRIMARY KEY (`id`),
    KEY `idx_auctionId` (`auctionId`),
    KEY `idx_teamId` (`teamId`),
    KEY `idx_captainId` (`captainId`),
    KEY `idx_bidTime` (`bidTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞价表';
