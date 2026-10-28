# 校园二手交易平台（trade）

> 面向校园场景的二手商品交易后端服务。买家浏览商品、收藏、下单，卖家发布商品、上传图片，AI 帮卖家一键生成商品信息。

Spring Boot 4 + MyBatis + MySQL + Redis + JWT + 阿里云 OSS + 阿里云百炼多模态大模型

---

## 一、功能模块

| 模块 | 说明 | 关键接口 |
|---|---|---|
| 用户 | 注册、登录、获取当前用户、改昵称/邮箱、换头像、改密码 | `/user/register`、`/user/login`、`/user/updatePwd` |
| 商品 | 发布、列表（分页/类目筛选/关键词搜索）、详情、改价、上下架、删除、我发布的 | `/goods`、`/goods/detail`、`/goods/status` |
| 类目 | 用户自建类目（仅创建人可改） | `/category`、`/category/detail` |
| 收藏 | 收藏/取消收藏（原子 SQL）、我的收藏 | `/favorites`、`/favorites/mine` |
| 订单 | 下单（服务端取价、防超卖）、我的订单、确认收货 | `/orders`、`/orders/confirm` |
| 收货地址 | 增删改查、设为默认地址 | `/address`、`/address/list` |
| AI 识别 | 上传商品图 → 大模型看图 → 自动生成标题/描述/建议价/类目/成色 | `/ai/goods/analyze` |
| 文件上传 | 图片上传阿里云 OSS，返回公网地址 | `/upload` |

## 二、技术要点

### 1. 越权防护：SQL 里带 `create_user`

商品和类目的修改、删除都不是只按 `id` 匹配，而是 `where id = ? and create_user = ?`。
`id` 来自客户端，可以被篡改；带上 `create_user` 后，别人改你的商品**匹配不到任何行**，等于没改。

### 2. 下单防篡改 + 防超卖

- **防篡改**：下单时价格从 `goods` 表重新查，不信客户端传的 `price`。否则 1 分钱就能买走别人 150 块的东西。
- **防超卖**：扣减库存的 SQL 带 `where stock = #{stock}` 条件，更新行数为 0 就说明已被抢光，直接回滚事务。

### 3. 改密码强制下线

登录成功后 token 存进 Redis（TTL 1 小时）。改密码时删掉 Redis 里的 token，
旧 token 立刻失效——不删的话，用户改完密码后旧 token 还能用 1 小时。

### 4. 收藏用一条 SQL 搞定

```sql
insert into favorites(user_id, goods_id)
select #{userId}, id from goods where id = #{goodsId} and status = 1
```

一条 SQL 同时完成「查商品存不存在 → 查在不在售 → 插收藏」。
如果拆成「先查再插」，中间有个时间差，商品可能刚好被别人买走。**一条 SQL 没这个缝。**

### 5. AI 识别（阿里云百炼多模态）

- 图片先传 OSS 拿公网地址，再让模型自己去拉图（比传 base64 请求体小得多）
- 用 OpenAI 兼容端点 + `RestClient`，不用官方 SDK，减少依赖
- 大模型返回的是**文本**不是 JSON，所以要剥掉 ` ```json ` 包裹再解析
- 价格字段模型可能返回 `"150元"`，用正则提取数字
- **降级**：AI 挂了不让整个发布流程崩溃，提示用户手动填写

### 6. 统一返回体 + 全局异常处理

`Result<T>` 统一包装，`code` 与 HTTP 状态码保持一致。
业务层用 `throw new BusinessException("原因")`，由 `GlobalExceptionHandler` 按异常类型分派状态码，
避免「同一个错误两种状态码」导致前端没法统一判断。

## 三、项目结构

```
src/main/java/com/example/trade/
├── controller/     # 接口层，只接收参数、返回 JSON
├── service/        # 业务接口
│   └── impl/       # 业务实现，核心逻辑在这层
├── mapper/         # MyBatis Mapper 接口
├── entity/         # 实体类
├── config/         # 配置类
├── except/         # 业务异常 + 全局异常处理器
├── interceptors/   # 登录拦截器
└── utils/          # 工具类（JWT / MD5 / ThreadLocal）
```

分层原则：**Controller 薄、Service 厚、Mapper 最薄。**
判断「能不能做」「该谁做」放 Service，判断「数据长什么样」放 Entity。

## 四、环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 6+

## 五、本地运行

### 1. 建库导表

```bash
mysql -uroot -p < src/main/resources/db/schema.sql
```

脚本会创建 `trade` 数据库和全部 5 张表。

### 2. 配置环境变量

| 环境变量 | 说明 | 是否必需 |
|---|---|---|
| `DB_USERNAME` | MySQL 用户名，默认 `root` | 否 |
| `DB_PASSWORD` | MySQL 密码，默认 `000000` | 否 |
| `ALIYUN_OSS_ID` | 阿里云 OSS AccessKeyId | 上传图片需要 |
| `ALIYUN_OSS_SECRET` | 阿里云 OSS AccessKeySecret | 上传图片需要 |
| `DASHSCOPE_API_KEY` | 百炼 API Key | AI 识别需要 |

> 不配 OSS / 百炼 也能启动，**只是对应功能不可用**（应用不会因为缺密钥而启动失败）。

### 3. 启动

```bash
mvn spring-boot:run
```

服务默认监听 `8080`。

## 六、接口一览

完整接口见各 Controller 源码。带鉴权的接口需要在请求头携带：

```
Authorization: <token>
```

## 七、说明

- 本项目为后端服务，未包含前端界面
- 测试类位于 `src/test/`，覆盖鉴权规则、Mapper 绑定、核心业务流程
