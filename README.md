# LOL比赛选手拍卖系统 - 后端服务

## 项目概述

这是一个用于小型LOL比赛的选手拍卖系统的后端服务，支持管理员和队长参与拍卖流程。

## 系统功能

### 核心流程
1. **登录系统**：管理员和队长通过用户名密码登录，系统生成JWT token
2. **管理员操作**：从待拍卖池抽取队员开始拍卖
3. **队长竞价**：队长在拍卖期间出价，系统自动判断最高价
4. **拍卖结束**：时间结束后，出价最高的队长获得队员，队员加入队长队伍
5. **循环拍卖**：重复上述流程，每个队长最多可获得4名队员
6. **实时推送**：流程变化时通过WebSocket推送给在线的管理员和队长
7. **数据同步**：管理员或队长掉线后重新登录时，系统会同步最新数据

## 技术栈

- **Spring Boot 4.0.1**
- **MyBatis**
- **MySQL**
- **WebSocket (STOMP)**
- **JWT认证**
- **Maven**

## 数据库设计

### 表结构
- `user` - 用户表（管理员/队长）
- `player` - 队员表（待拍卖池）
- `team` - 队伍表（每个队长一个队伍）
- `auction` - 拍卖表
- `bid` - 竞价表

详细SQL文件位置：`target/sql/create_all_tables.sql`

## API接口

### 认证相关
- `POST /api/auth/login` - 用户登录

### 队员管理
- `GET /api/player/pool` - 获取待拍卖池（管理员）
- `GET /api/player/all` - 获取所有队员（管理员）
- `GET /api/player/team/{teamId}` - 获取指定队伍的队员

### 拍卖管理
- `POST /api/auction/start` - 开始拍卖（管理员）
- `GET /api/auction/current` - 获取当前拍卖
- `POST /api/auction/bid` - 出价（队长）
- `POST /api/auction/finish/{auctionId}` - 结束拍卖（管理员）
- `GET /api/auction/{auctionId}/bids` - 获取拍卖的竞价列表

### 队伍管理
- `GET /api/team/all` - 获取所有队伍
- `GET /api/team/captain/{captainId}` - 根据队长ID获取队伍
- `GET /api/team/{teamId}` - 根据队伍ID获取队伍信息

### 系统状态
- `GET /api/system/status` - 获取系统状态（用于掉线后同步数据）

## WebSocket推送

WebSocket端点：`/ws`

推送主题：
- `/topic/system-status` - 系统状态变化
- `/topic/auction` - 拍卖相关事件
- `/topic/bid` - 竞价事件
- `/topic/assignment` - 队员分配事件

## 定时任务

系统每10秒自动检查过期的拍卖并自动结束，无需手动干预。

## 配置说明

### application.yml
- 数据库连接配置
- MyBatis配置
- 服务器端口：8080

### JWT配置
- Token有效期：24小时
- Secret Key在`JwtUtil.java`中配置

## 使用说明

### 1. 初始化数据库
执行 `target/sql/create_all_tables.sql` 创建所有数据表

### 2. 配置数据库连接
修改 `application.yml` 中的数据库连接信息

### 3. 创建初始用户
在数据库中手动插入管理员和队长用户（密码需要BCrypt加密）

### 4. 启动服务
```bash
mvn spring-boot:run
```

### 5. 前端集成
- 登录后获取token，后续请求在Header中携带：`Authorization: Bearer {token}`
- 建立WebSocket连接接收实时推送
- 掉线后重新登录时调用 `/api/system/status` 同步数据

## 业务规则

1. **用户类型**：只有ADMIN（管理员）和CAPTAIN（队长）两种类型
2. **队伍限制**：每个队长最多拥有4名队员
3. **拍卖规则**：
   - 同时只能有一个进行中的拍卖
   - 出价必须高于当前最高价
   - 拍卖时间结束后自动结束
4. **状态流转**：
   - 队员：POOL → AUCTIONING → SOLD
   - 拍卖：ACTIVE → FINISHED

## 注意事项

1. 密码存储使用BCrypt加密
2. JWT token需要在请求头中携带
3. WebSocket连接需要在登录后建立
4. 定时任务会自动处理过期的拍卖
