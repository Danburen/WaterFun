# Web 客户端帖子功能实现计划

## 概述

为 web-client 实现帖子（Posts）的浏览、创建/编辑、管理功能，对接后端 post 相关 API。

## 已实现的接口

### `postApi.ts` - 帖子相关 API

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/posts/list` | 分页获取公开帖子列表 | ✅ |
| GET | `/api/posts/{id}` | 获取帖子详情（公开） | ✅ |
| DELETE | `/api/posts/{id}` | 删除帖子 | ✅ |
| GET | `/api/posts/me/{id}` | 获取我的帖子详情（作者视图） | ✅ |
| GET | `/api/posts/me/list` | 分页获取我的帖子列表 | ✅ |
| POST | `/api/posts/draft` | 新建草稿（返回 postId） | ✅ |
| GET | `/api/posts/{id}/edit` | 获取编辑草稿数据 | ✅ |
| POST | `/api/posts/{id}/publish` | 发布/保存帖子 | ✅ |
| GET | `/api/posts/{id}/content/preview` | 预览内容 | ✅ |
| POST | `/api/posts/{id}/temp-save` | 临时保存 | ✅ |
| GET | `/api/post/category` | 获取分类列表 | ✅ |
| GET | `/api/post/tag` | 获取标签列表 | ✅ |

### 其他已存在 API

| 文件 | 说明 |
|------|------|
| `uploadApi.ts` | 上传策略获取、文件上传、上传回调 |
| `authApi.ts` | 登录、注册、验证码、登出、刷新 token |
| `accountApi.ts` | 账户信息、密码管理、邮箱/手机绑定 |
| `userApi.ts` | 用户信息、个人资料、头像、设置 |
| `notificationApi.ts` | 系统通知列表、未读数、标记已读 |

## 待实现的接口

### postApi.ts 补充

| 方法 | 路径 | 说明 | 优先级 |
|------|------|------|--------|
| POST | `/api/post/tag` | 创建标签 | medium |
| PUT | `/api/post/tag` | 更新标签 | medium |
| GET | `/api/post/tag/{id}` | 获取单个标签 | low |
| DELETE | `/api/post/tag/{id}` | 删除标签 | low |
| POST | `/api/post/category` | 创建分类 | medium |
| PUT | `/api/post/category` | 更新分类 | medium |
| DELETE | `/api/post/category?id=` | 删除分类 | low |
| GET | `/api/post/category/{id}` | 获取单个分类 | low |

### 新建 `publicUserApi.ts` - 公开用户 API

| 方法 | 路径 | 说明 | 优先级 |
|------|------|------|--------|
| GET | `/api/public/user/{userUid}/info` | 获取用户公开基本信息 | high |
| GET | `/api/public/user/{userUid}/profile` | 获取用户公开资料 | high |
| GET | `/api/public/user/{userUid}/card` | 获取用户公开卡片 | high |
| GET | `/api/public/user/{userUid}/follower` | 获取粉丝列表 | medium |
| GET | `/api/public/user/{userUid}/following` | 获取关注列表 | medium |
| GET | `/api/public/user/{userUid}/avatar` | 获取用户头像 | low |

### 待补充 Store

| Store | 说明 | 优先级 |
|-------|------|--------|
| `publicUserStore.ts` | 公开用户信息缓存（卡片、资料） | medium |

### 待实现/修改页面

| 页面 | 说明 | 优先级 |
|------|------|--------|
| `pages/index.vue` | **主界面** — 论坛风格首页：分类标签栏 + 帖子列表 + 右侧边栏（用户卡片/热门话题/活跃成员/公告/友链） | high |
| `components/HeaderNavMenu.vue` | **导航栏** — 重构为参考设计的轻量化样式，Logo + 导航项 + 搜索框 + 通知/消息图标 + 用户菜单 | high |

## 文件结构

```
waterfun-web-client/src/
├── api/
│   ├── postApi.ts               # 帖子相关 API ✅ 已建，待补充 category/tag CRUD
│   ├── publicUserApi.ts         # 公开用户 API 🔜 新建
│   ├── uploadApi.ts             # 上传 API ✅ 已建
│   ├── authApi.ts               # 认证 API ✅ 已建
│   ├── accountApi.ts            # 账户 API ✅ 已建
│   ├── userApi.ts               # 用户 API ✅ 已建
│   └── notificationApi.ts       # 通知 API ✅ 已建
├── stores/
│   ├── postStore.ts             # 帖子状态 ✅ 已建
│   └── publicUserStore.ts       # 公开用户状态 🔜 新建
├── pages/
│   ├── index.vue                # 主界面 🔜 重写（论坛风格）
│   ├── post/
│   │   ├── index.vue            # 帖子列表页 ✅ 已建
│   │   ├── [id].vue             # 帖子详情页 ✅ 已建
│   │   └── create.vue           # 创建/编辑帖子页 ✅ 已建
│   └── profile/                 # 个人中心 ✅ 已建
└── components/
    └── HeaderNavMenu.vue        # 导航栏 🔜 重构
```

## API 请求规范

- 基准路径：`/api`（由 axiosRequest 拦截器自动拼接）
- 认证方式：Bearer token（`request` 实例自动处理）
- 通用响应体：
  ```typescript
  { success: boolean, code: string, message: string, data: T }
  ```
- 分页：Spring Pageable（page 从 0 开始，size 默认 10/20）
- 日期格式：`Instant { seconds: number, nanos: number }`

## 注意事项

1. 帖子内容为 Markdown 格式，图片使用 `res://uuid` 引用
2. 上传使用 presignedUrl 方式（`getUploadPolicy` → 直传 COS → `uploadCallback`）
3. 主界面/文章列表页已加入 auth 白名单，未登录用户可浏览
