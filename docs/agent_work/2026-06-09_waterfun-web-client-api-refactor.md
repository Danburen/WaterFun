# WaterFun Web Client 接口重构 & 功能完善记录

**日期**: 2026-06-09  
**依据**: `docs/reference/user.all.openapi.json`  
**参考风格**: `docs/reference/style/`（非 admin 页面，如 `waterfun_community.html`、`waterfun_post_detail.html`）  

---

## 一、API 接口层重构

### 1.1 notificationApi.ts — 统一通知 API（重写）

| 变更 | 说明 |
|------|------|
| 删除旧 API | `listSystemNotifications`、`SystemNotificationRes`、`InstantDto` 等 |
| 新增统一接口 | `listNotifications(params)` — 支持 `type`(LIKE/REPLY/MENTION/SYSTEM) 和 `group`(SYSTEM/INTERACTION) 筛选 |
| 新增 | `getUnreadCount`、`markNotificationRead`、`markAllNotificationsRead`、`batchMarkNotificationsRead`、`deleteNotification` |
| 新增类型 | `InboxNotificationRes`（含 `NotificationContent` 嵌套对象）、`NotificationType`、`NotificationGroup` |

### 1.2 commentApi.ts — 新建

| 接口 | method | path |
|------|--------|------|
| `postComment` | POST | `/comments` |
| `listComments` | GET | `/comments/list?postId=&cursor=&limit=` |
| `listReplies` | GET | `/comments/{rootId}/replies` |
| `getComment` | GET | `/comments/{commentId}` |
| `likeComment` | POST | `/comments/{id}/like` |
| `deleteComment` | DELETE | `/comments/{id}` |

类型定义：`CommentResponse`、`CreateCommentReq`、`CursorPageComment<T>`、`UserBrief`

### 1.3 postApi.ts — 补充

| 新增接口 | path |
|----------|------|
| `likePost` | POST `/posts/{id}/like` |
| `collectPost` | POST `/posts/{id}/collection` |
| `categoryOptions` | GET `/post/category/options` |
| `searchTags` | GET `/post/tags/search?keyword=&limit=` |
| `searchTagOptions` | GET `/post/tags/search/options` |
| `getTag` | GET `/post/tags/{id}` |
| `deleteTag` | DELETE `/post/tags/{id}` |
| `getHotTags` | GET `/post/tags/hot?page=&size=` |

补充字段：`PostCardResp` 新增 `userBrief`、`type`(COMMON/NOTICE)、`isPinned`；`PostDetailResp` 新增 `type`、`isPinned`

### 1.4 publicUserApi.ts — 重写

| 变更 | 说明 |
|------|------|
| 路径修改 | `/public/user/{uid}/...` → `/user/{uid}/...`（对齐 OpenAPI） |
| 删除旧接口 | `fetchUserPublicInfo`、`fetchUserFollowers`、`fetchUserFollowing`（签名已变） |
| 保留重命名 | `fetchUserPublicProfile`（新路径+新返回类型）、`fetchUserPublicCard`、`fetchUserPublicAvatar` |
| 新增 | `toggleFollowUser`（POST）、`fetchFollowings`、`fetchFollowers` |
| 类型更新 | `UserPublicProfileResp` 现包含 `UserBrief` + `bio/gender/followers/followings/likeCount/postCount`；`UserPublicCardResp` 使用 `UserBrief` 结构 |

### 1.5 userApi.ts — 补充

| 新增 | path |
|------|------|
| `getPermissions` | GET `/user/permissions` |
| `followUser` | POST `/user/{uid}/follow` |
| `getFollowings` | GET `/user/{uid}/followings` |
| `getFollowers` | GET `/user/{uid}/followers` |

---

## 二、Store 状态管理层

### 2.1 notificationStore.ts — 新建

- 分 tab 缓存（system/reply/mention/subscribe）
- 游标分页加载（`fetchNotifications(tab, reset)`）
- 未读数管理（`fetchUnreadCount`）
- 标记已读/全部已读/删除
- SSE 实时通知（`connectSSE` / `disconnectSSE`）：收到推送后自动增未读数 + 插入对应 tab 列表头部

### 2.2 commentStore.ts — 新建

- 一级评论游标分页（`fetchComments(postId, reset)`）
- 子回复游标分页（`fetchReplies(rootId, reset)`）
- 发布评论（`addComment(data)`：成功后自动刷新列表）
- 评论点赞/删除

---

## 三、页面 & 组件功能完善

### 3.1 post/[id].vue — 帖子详情（重写）

| 功能 | 状态 |
|------|------|
| 帖子点赞/取消点赞（likePost） | ✅ |
| 帖子收藏/取消收藏（collectPost） | ✅ |
| 公告/置顶 badge 展示（type/isPinned） | ✅ |
| 一级评论发布（游标分页） | ✅ |
| 评论回复（parentId + 回复目标显示 replyToDisplayName） | ✅ |
| 子回复展开/收起（游标分页加载） | ✅ |
| 评论点赞 | ✅ |
| 「加载更多」按钮 | ✅ |

### 3.2 User/[uid].vue — 用户主页（新建）

| 功能 | 状态 |
|------|------|
| 用户基础信息（头像/昵称/bio/居住地/性别） | ✅ |
| 统计卡片（粉丝/关注/帖子/获赞） | ✅ |
| 关注/取关按钮（toggleFollowUser） | ✅ |
| 帖子列表选项卡 | ✅ |
| 粉丝列表选项卡 | ✅ |
| 关注列表选项卡 | ✅ |
| 点击粉丝/关注项跳转用户主页 | ✅ |

### 3.3 post/index.vue — 社区列表（更新）

| 功能 | 状态 |
|------|------|
| 帖子卡片展示作者头像+昵称（userBrief） | ✅ |
| 公告/置顶 badge（type/isPinned） | ✅ |
| 分类导航侧边栏 | ✅ |
| 分类筛选标签 | ✅ |

### 3.4 index.vue — 首页（更新）

| 功能 | 状态 |
|------|------|
| 侧边栏新增「社区动态」卡片 | ✅ |
| 展示当前在线/今日新增/今日峰值（/online-users/stats） | ✅ |

### 3.5 消息中心组件（全部更新）

| 组件 | 变更 |
|------|------|
| `SystemNotificationModule.vue` | 改用 notificationStore；新增「全部已读」按钮；支持未读高亮 |
| `ReplyMessageModule.vue` | 从占位实现为真实 `type=REPLY` 通知列表 |
| `MentionMessageModule.vue` | 从占位实现为真实 `type=MENTION` 通知列表 |
| `SubscribeMessageModule.vue` | 从占位实现为真实 `group=INTERACTION` 通知列表 |
| `MessageCenterSidebar.vue` | 新增未读数量 badge 徽标 |

### 3.6 global.css — 全局样式系统（重写）

设计令牌（基于 style 参考）：
- `--wf-primary` / `--wf-primary-hover` / `--wf-primary-light`
- `--wf-text-primary` / `--wf-text-secondary` / `--wf-text-muted`
- `--wf-border` / `--wf-bg` / `--wf-shadow`
- `--wf-radius` / `--wf-radius-sm`

复用类：`.wf-card`、`.wf-btn`、`.wf-tag-*`、`.wf-avatar-*`、`.wf-flex-*`、`.wf-text-*`、`.wf-mt-*`、`.wf-state-wrap` 等 60+ 工具类

---

## 四、仍建议完成

| # | 项目 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | post/create.vue 编辑器增强 | 中 | 当前为纯文本 textarea，建议接入富文本/Markdown 编辑器 |
| 2 | 分页状态持久化 | 中 | index.vue 分页依赖 vue ref，F5 刷新后丢失 |
| 3 | 用户主页公开帖子 | 中 | 当前用 `fetchMyPostList` 仅限作者本人 |  
| 4 | SSE 鉴权 token | 中 | EventSource 原生不支持自定义 header，需确认后端方案（cookie? URL param?） |
| 5 | TypeCheck | 低 | 建议运行 `npx nuxt typecheck` |
| 6 | i18n 国际化 | 低 | 新增文案均硬编码，需走 `$t()` |

---

## 五、涉及文件清单

```
新增:
  src/api/commentApi.ts
  src/stores/notificationStore.ts
  src/stores/commentStore.ts

重写:
  src/api/notificationApi.ts
  src/api/publicUserApi.ts
  src/assets/styles/global.css
  src/pages/post/[id].vue
  src/components/message-center/SystemNotificationModule.vue
  src/components/message-center/ReplyMessageModule.vue
  src/components/message-center/MentionMessageModule.vue
  src/components/message-center/SubscribeMessageModule.vue
  src/pages/User/[uid].vue (之前为空文件)

更新:
  src/api/postApi.ts
  src/api/userApi.ts
  src/components/message-center/MessageCenterSidebar.vue
  src/pages/post/index.vue
  src/pages/index.vue
  src/stores/notificationStore.ts (追加 SSE)
```
