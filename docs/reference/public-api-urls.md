# 公开 URL 清单（无需认证，游客可访问）

所有地址均为 `GET` 方法，**不需要在请求头传 `Authorization`**。

---

## 1. 认证（登录/注册流程）

| Method | URL | 说明 |
|--------|-----|------|
| GET | `/api/auth/captcha` | 图形验证码（返回图片流） |
| GET | `/api/auth/csrf-token` | CSRF token |
| POST | `/api/auth/send-code` | 发送短信/邮件验证码 |
| POST | `/api/auth/login-by-password` | 密码登录 |
| POST | `/api/auth/login-by-code` | 验证码登录 |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/refresh` | 刷新 access token |
| GET | `/api/admin/auth/csrf-token` | 管理端 CSRF token |
| GET | `/api/admin/auth/captcha` | 管理端图形验证码 |
| POST | `/api/admin/auth/login-by-password` | 管理端密码登录 |
| POST | `/api/admin/auth/logout` | 管理端登出 |

## 2. 文章

| Method | URL | 说明 |
|--------|-----|------|
| GET | `/api/posts/list` | 文章列表（分页） |
| GET | `/api/posts/{id}` | 文章详情 |
| GET | `/api/posts/hot` | 热门文章 |

## 3. 轮播图 & 公告

| Method | URL | 说明 |
|--------|-----|------|
| GET | `/api/banners` | 所有活跃轮播图 |
| GET | `/api/banners/by-position?position=` | 按位置筛选轮播图 |
| GET | `/api/announcements` | 公告列表 |

## 4. 分类 & 标签

| Method | URL | 说明 |
|--------|-----|------|
| GET | `/api/post/category/options` | 全部分类选项 |
| GET | `/api/post/tags/hot` | 热门标签 |
| GET | `/api/post/tags/search?keyword=` | 搜索标签 |
| GET | `/api/post/tags/search/options?keyword=` | 搜索标签（选项格式） |
| GET | `/api/post/tags/{id}` | 标签详情 |

## 5. 用户公开信息

| Method | URL | 说明 |
|--------|-----|------|
| GET | `/api/user/{uid}/profile` | 用户公开资料 |
| GET | `/api/user/{uid}/card` | 用户名片 |
| GET | `/api/user/{uid}/avatar` | 用户头像 URL |
| GET | `/api/user/{uid}/followers` | 粉丝列表 |
| GET | `/api/user/{uid}/followings` | 关注列表 |

## 6. 法律资源

| Method | URL | 说明 |
|--------|-----|------|
| GET | `/api/resource/legal/{type}/{lang}/{fileName}` | 用户协议/隐私政策等 |

---

> 其余所有 `/api/**` 端点（包括 `POST /api/posts/{id}/like`、`POST /api/user/{uid}/follow` 等）都需要登录 token。
