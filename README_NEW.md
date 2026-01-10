# LOL选手拍卖系统 - 新版本说明

## 系统架构变更

### 新增概念：拍卖流程（AuctionSession）

系统现在支持多个独立的拍卖流程，每个流程包含：
- 流程名称
- 数据来源Excel文件
- 队长信息（从Excel中指定列）
- 队员信息（从Excel导入）

## 数据库结构

所有SQL文件位于：`src/main/resources/sql/create_all_tables.sql`

### 主要表结构：

1. **auction_session** - 拍卖流程表
   - sessionName: 流程名称
   - dataSourceFile: Excel文件路径
   - captainIds: 队长序号列表（JSON格式）
   - status: CREATED/ACTIVE/FINISHED

2. **player** - 队员表（字段已更新）
   - groupId: 队员序号
   - groupName: 群内名字
   - gameId: 游戏ID名字
   - position: 擅长位置
   - heroes: 擅长英雄
   - cost: 费用
   - sessionId: 所属拍卖流程

3. **team** - 队伍表（增加sessionId和captainName）
4. **auction** - 拍卖表（增加sessionId）
5. **bid** - 竞价表（保持不变）

## 业务流程

### 管理员操作：
1. 登录系统
2. 查看拍卖流程列表
3. 创建新流程：
   - 输入流程名称
   - 上传Excel文件（包含队员和队长信息）
   - 指定队长序号（如：1,2,3 表示Excel的第1、2、3列为队长信息）
4. 进入拍卖流程
5. 开始拍卖（从待拍卖池抽取队员）
6. 结束拍卖

### 队长操作：
1. 登录系统
2. 查看拍卖流程列表
3. 进入拍卖流程（只能进入已分配给自己的流程）
4. 参与竞价
5. 查看队伍信息

## Excel文件格式

### 队员信息格式（假设列顺序）：
| 序号 | 群内名字 | 游戏ID | 擅长位置 | 擅长英雄 | 费用 |
|------|---------|--------|---------|---------|------|
| 1    | 张三    | player1| TOP,ADC  | 亚索,烬 | 100 |

### 队长信息：
- 队长信息可以从表头行读取，或者从指定列读取
- 根据captainIndices参数指定哪些列为队长

## API接口

### 拍卖流程管理
- `POST /api/session/create` - 创建新流程（管理员）
- `GET /api/session/list` - 获取流程列表
- `GET /api/session/{sessionId}` - 获取流程详情
- `POST /api/session/{sessionId}/activate` - 激活流程（管理员）

### 拍卖相关
- `GET /api/auction/current?sessionId={id}` - 获取当前拍卖
- `POST /api/auction/start` - 开始拍卖（管理员）
- `POST /api/auction/bid` - 出价（队长）
- `POST /api/auction/finish/{auctionId}` - 结束拍卖（管理员）

### 数据查询
- `GET /api/player/pool?sessionId={id}` - 获取待拍卖池
- `GET /api/team/session/{sessionId}` - 获取流程的队伍列表

## 前端界面

### 1. 登录页面 (`/login`)
- 用户名密码登录
- JWT Token认证

### 2. 拍卖流程列表 (`/sessions`)
- 显示所有拍卖流程
- 管理员可以创建新流程
- 点击流程卡片进入拍卖大厅

### 3. 拍卖大厅 (`/auction/:sessionId`)
- 左侧：当前拍卖信息、竞价历史
- 右侧：队伍信息、待拍卖池
- 队长可以出价
- 实时更新（通过定时刷新）

## 部署说明

### 后端
1. 执行SQL文件创建数据库表
2. 配置`application.yml`中的数据库连接
3. 创建uploads目录用于存储Excel文件
4. 运行 `mvn spring-boot:run`

### 前端
1. 安装依赖：`npm install`
2. 配置API地址（如需要）：修改`src/api/request.js`中的baseURL
3. 运行开发服务器：`npm run dev`
4. 构建生产版本：`npm run build`

## 注意事项

1. Excel文件解析逻辑在`ExcelServiceImpl.java`中，需要根据实际Excel格式调整列索引
2. 队长序号与用户ID的关联逻辑需要在`AuctionSessionServiceImpl.canUserAccessSession`中完善
3. WebSocket实时推送功能需要进一步完善
4. 文件上传路径配置在`application.yml`的`file.upload-dir`中

## 后续优化建议

1. 完善Excel解析逻辑，支持更灵活的格式
2. 实现WebSocket实时推送，替代定时刷新
3. 添加队长序号与用户ID的映射功能
4. 增加拍卖历史记录查看
5. 优化前端UI/UX
6. 添加数据导出功能
