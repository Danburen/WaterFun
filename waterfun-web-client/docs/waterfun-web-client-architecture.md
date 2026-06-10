# WaterFun Web Client — 项目架构分析

## 1. 技术栈总览

| 层面 | 技术 | 说明 |
|------|------|------|
| 框架 | Nuxt 3 (^3.16.2) | SSR/SSG 支持，文件路由 |
| UI | Vue 3 + Element Plus (^2.9.9) | 组件库 |
| 状态管理 | Pinia + pinia-plugin-persistedstate | 全局状态 + localStorage 持久化 |
| 国际化 | @nuxtjs/i18n (v9) | `no_prefix` 策略，cookie 存储语言偏好 |
| HTTP | Axios (1.13.5) | 封装了 CSRF/Token 刷新/错误映射 |
| 表单验证 | vee-validate (^4.15.0) + async-validator | |
| 构建 | Vite (^7.x) + Nuxt | |
| CSS | Sass, 全局 Scoped CSS | |
| 工作空间 | pnpm workspace | `@waterfun/web-core` 为共享核心库 |

## 2. 目录结构

```
waterfun-web-client/
├── .env.development          # 开发环境变量
├── .env.production           # 生产环境变量
├── nuxt.config.ts            # Nuxt 配置（模块、i18n、路径别名等）
├── package.json              # @waterfun/web-client
├── tsconfig.json
├── i18n/
│   └── locales/
│       ├── en_US.json        # 英语翻译
│       └── zh_CN.json        # 中文翻译
├── public/                   # 静态资源
│   ├── favicon.ico
│   ├── logo.svg
│   └── robots.txt
├── server/                   # Nitro server 端（当前仅 tsconfig）
├── src/
│   ├── app.vue               # 根组件 → <NuxtPage/>
│   ├── error.vue             # Nuxt 错误页面（SSR 错误捕获）
│   ├── 404.vue               # 404 页面（太空中迷失主题）
│   ├── env.d.ts
│   ├── test.vue              # 测试页
│   ├── assets/
│   │   ├── logo.svg
│   │   ├── svg/              # SVG 资源（astronaut-1, astronaut-2, stars）
│   │   └── styles/
│   │       └── global.css    # 全局样式（工具类 .g-section-title 等）
│   ├── styles/
│   │   └── global.css        # 被 app.vue 引用
│   ├── types/                # 客户端类型定义
│   │   ├── index.d.ts        # re-export
│   │   ├── json-bigint.d.ts  # bigint 类型声明
│   │   ├── eneity/
│   │   │   └── user.d.ts
│   │   ├── api/              # API 类型（与 web-core 重复，部分冗余）
│   │   └── sys/
│   │       ├── lang.d.ts     # LanguageTypes
│   │       └── vue.d.ts
│   ├── utils/                # 工具函数
│   │   ├── axiosRequest.ts   # Axios 封装（CSRF、Token 刷新、错误处理）
│   │   ├── translator.ts     # i18n 翻译服务（NuxtI18nService）
│   │   ├── errorMessage.ts   # 错误码 → 翻译消息
│   │   ├── validator.ts      # vee-validate 验证规则工厂
│   │   ├── consts.ts         # LangMap 常量
│   │   └── date.ts           # 日期格式化
│   ├── composables/          # Nuxt composables
│   │   └── useAuth.ts        # 认证逻辑（login/register/logout + 状态同步）
│   ├── middleware/
│   │   └── auth.global.ts    # 全局路由守卫（白名单 + 重定向 /login）
│   ├── plugins/
│   │   ├── init.client.ts    # 应用挂载后自动刷新用户信息
│   │   ├── seo.global.ts     # SEO title 模板
│   │   └── async-validator.client.ts  # 全局注入 async-validator
│   ├── stores/               # Pinia 状态仓库
│   │   ├── authStore.ts      # Access Token 存储 + persist
│   │   ├── userInfoStore.ts  # 用户基本信息 + persist
│   │   ├── userProfileStore.ts  # 用户档案 + 头像缓存 + persist
│   │   ├── userAccountStore.ts  # 账号信息（手机/邮箱掩码）
│   │   ├── postStore.ts      # 帖子列表/详情/草稿（Options Store）
│   │   └── cacheStore.ts     # 图片资源缓存（Memory → IDB → Network）
│   ├── api/                  # API 请求层
│   │   ├── authApi.ts        # 登录/注册/验证码/登出/刷新Token
│   │   ├── accountApi.ts     # 账号管理（密码/邮箱/手机绑定）
│   │   ├── userApi.ts        # 用户信息/档案/设置/头像
│   │   ├── publicUserApi.ts  # 公开用户信息（对外展示）
│   │   ├── postApi.ts        # 帖子 CRUD + 分类/标签
│   │   ├── bannerApi.ts      # Banner 轮播图
│   │   ├── notificationApi.ts  # 系统通知
│   │   ├── uploadApi.ts      # 文件上传（COS 预签名）
│   │   ├── resourceApi.ts    # 静态资源（如 EULA 协议文本）
│   │   ├── auth/             # auth 子模块 API
│   │   ├── account/          # account 子模块 API
│   │   └── user/             # user 子模块 API
│   ├── components/           # 组件
│   │   ├── HeaderNavMenu.vue           # 顶部导航栏（Logo + 导航 + 搜索 + 用户菜单）
│   │   ├── BannerCarousel.vue          # 首页 Banner 轮播
│   │   ├── AvatarUploadModal.vue       # 头像上传弹窗
│   │   ├── LegalDocument.vue           # 法律文档（EULA）展示组件
│   │   ├── UserCenterSideBar.vue       # 个人中心侧边栏
│   │   ├── auth/
│   │   │   ├── AuthBox.vue             # 认证页容器（居中卡片）
│   │   │   ├── VerifyingCodeButton.vue # 发送验证码按钮（带倒计时）
│   │   │   └── verify/
│   │   │       ├── UnifiedVerify.vue   # 统一安全验证弹窗
│   │   │       ├── CaptchaVerify.vue   # 图形验证码
│   │   │       ├── ActivateVerify.vue  # 激活验证
│   │   │       └── ThreeStepVerify.vue # 三步验证流程
│   │   ├── account/
│   │   │   └── UnbindEmailDialog.vue   # 解绑邮箱确认弹窗
│   │   └── message-center/
│   │       ├── MessageCenterSidebar.vue    # 消息中心侧边栏导航
│   │       ├── SystemNotificationModule.vue  # 系统通知模块
│   │       ├── MentionMessageModule.vue    # 提及消息（预留）
│   │       ├── ReplyMessageModule.vue      # 回复消息（预留）
│   │       └── SubscribeMessageModule.vue  # 订阅消息（预留）
│   └── pages/                # Nuxt 文件路由页面
│       ├── index.vue                     # 首页（Banner + 文章列表 + 侧边栏）
│       ├── Login.vue                     # 登录（密码/快捷登录双 Tab）
│       ├── Register.vue                  # 注册
│       ├── community.vue                 # 社区页（建设中占位）
│       ├── about.vue                     # 关于
│       ├── playground.vue                # 测试/游乐场
│       ├── account.vue                   # 账号管理
│       ├── message-center.vue            # 消息中心
│       ├── profile.vue                   # 个人中心布局（侧边栏 + NuxtPage）
│       ├── EulaView.vue                  # EULA 协议查看
│       ├── VerifyDemo.vue                # 验证流程演示
│       ├── post/
│       │   ├── index.vue                 # 社区帖子列表
│       │   ├── [id].vue                  # 帖子详情
│       │   └── create.vue                # 创建/编辑帖子
│       ├── profile/
│       │   ├── info.vue                  # 个人信息编辑
│       │   ├── account.vue               # 账号设置
│       │   └── notifications.vue         # 通知设置
│       ├── account/
│       │   └── password.vue              # 修改密码
│       └── User/
│           └── [uid].vue                 # 用户公开主页

waterfun-web-core/              # 共享核心库（workspace）
├── package.json                # @waterfun/web-core
├── tsconfig.json
├── src/
│   ├── types/
│   │   ├── index.d.ts          # re-export api/auth, api/response, sys/lang
│   │   ├── api/
│   │   │   ├── auth.d.ts       # 认证类型（LoginRequest, AccessTokenResponse 等）
│   │   │   └── response.d.ts   # 通用响应类型（ResBody, PromiseResBody, ApiRes 等）
│   │   ├── eneity/user.d.ts    # 用户实体类型
│   │   ├── sys/lang.d.ts       # LanguageTypes
│   │   └── ui/tagNav.ts
│   ├── cache/                  # 图片缓存系统
│   │   ├── types.d.ts          # CacheItem 接口
│   │   ├── imgMemoryLRU.ts     # 内存 LRU 缓存
│   │   └── imgIDB.ts           # localStorage 持久化缓存（命名含 IDB 但实为 localStorage）
│   ├── interface/
│   │   └── translate.ts        # I18nTranslator / I18nService 接口
│   ├── errors/
│   │   └── APIError.ts         # APIError 类（错误码 → i18n key）
│   ├── fingerprint.ts          # 设备指纹生成（SHA-256 + 设备特征）
│   ├── simple-cypto.ts         # SHA-256 哈希
│   ├── dataMapper.ts           # ArrayBuffer ↔ Base64 转换 / CamelCase 类型
│   ├── regex.ts                # 正则表达式（username/phone/email）
│   ├── timer.ts                # 日期格式化
│   ├── triggerControl.ts       # throttle / deBounce / deBounceLeading
│   └── ErrorCodeEnum.ts        # 错误码枚举 + 自动映射 i18n key
```

## 3. 核心数据流

### 3.1 认证流程

```
[Login.vue / Register.vue]
  │ 表单 → validate (vee-validate)
  │ → generateFingerprint() (web-core)
  │ → login() / register() (authApi)
  │ → axiosRequest (CSRF + Token)
  │
  ▼
[useAuth composable]
  │ handleAuthSuccess():
  │   authStore.setToken()        ← accessToken + expire
  │   userInfoStore.fetchAndUpdateUserInfo()
  │   userProfileStore.fetchAndUpdateUserProfile()
  │   userAccountStore.fetchAccountInfoAndUpdate()
  │
  ▼ 路由跳转 → 首页
```

### 3.2 Token 刷新机制

```
axiosRequest 响应拦截器捕获 401
  │ → isRefreshing 互斥锁
  │ → fetch(`${API_BASE}/auth/refresh`, POST)
  │ → authStore.setToken(newAccessToken)
  │ → userInfoStore.fetchAndUpdateUserInfo()
  │ → onRefreshed() 重放等待队列中的请求
  │
  └── 刷新失败 → authStore.removeToken() → window.location.href = '/login'
```

### 3.3 全局路由守卫

```
middleware/auth.global.ts
  │
  ├── 白名单: /login, /register, /, /post → 放行
  ├── 服务端渲染 → 放行
  └── authStore.isAccess === false → navigateTo(/login?redirect=...)
```

### 3.4 启动初始化

```
plugins/init.client.ts
  └── app:mounted → 如果已登录 → 自动刷新用户信息 + 账号信息
```

## 4. Store 分层设计

| Store | 职责 | 持久化 |
|-------|------|--------|
| `authStore` | accessToken + expire | localStorage |
| `userInfoStore` | uid, username, nickname, avatar | localStorage |
| `userProfileStore` | bio, gender, birthday, residence, avatarCache | localStorage (仅 avatarCache) |
| `userAccountStore` | phoneMasked, emailMasked, verified 状态 | 不持久化 |
| `postStore` | 帖子列表、详情、草稿、分类、标签 | 不持久化 |
| `cacheStore` | 图片资源三级缓存（内存 → localStorage → 网络） | 不持久化 |

## 5. 图片资源缓存体系

```
cacheStore.load(path, fetchFn)
  │
  ├── 1️⃣ memory (Map LRU, max 150)
  │   → @waterfun/web-core/cache/imgMemoryLRU
  │
  ├── 2️⃣ localStorage (持久化, 7天过期)
  │   → @waterfun/web-core/cache/imgIDB (实际为 localStorage)
  │
  └── 3️⃣ network (fetchFn)
      → setMemory + setIDB 写入缓存
```

## 6. API 端点总览

| 模块 | 端点前缀 | 说明 |
|------|----------|------|
| Auth | `/auth/*` | 登录、注册、验证码、Token 刷新、CSRF |
| Account | `/auth/account/*` | 密码/邮箱/手机管理 |
| User | `/user/*` | 用户信息、档案、设置、头像 |
| Public | `/public/user/*` | 公开用户信息 |
| Post | `/posts/*`, `/post/*` | 帖子 CRUD、分类、标签 |
| Banner | `/banners/*` | 轮播图 |
| Notification | `/notifications/*` | 系统通知 |
| Upload | `/upload/*` | 文件上传策略、上传回调 |
| Resource | `/resource/*` | 静态资源（EULA 等） |

## 7. 关键设计要点

1. **CSRF 防护**：非 GET 请求自动获取 `XSRF-TOKEN` cookie 并注入 `X-XSRF-TOKEN` 请求头，首次无 cookie 则先请求 `/auth/csrf-token`
2. **Token 自动刷新**：响应 401 时自动刷新 accessToken，排队等待的请求自动重放
3. **设备指纹**：登录/注册时生成设备指纹（SHA-256 摘要），用于安全审计
4. **头像缓存**：头像 URL 使用预签名 URL（过期机制），userProfileStore 管理缓存失效
5. **国际化**：i18n 使用 `no_prefix` 策略，通过 cookie 记录语言偏好，翻译 key 自动从后端错误码映射
6. **文件路由**：Nuxt 自动根据 `pages/` 目录生成路由，profile 页面使用嵌套路由（`profile.vue` + `profile/` 子页面）
7. **图片三级缓存**：Memory LRU → localStorage → Network，避免重复网络请求
8. **错误处理**：Axios 拦截器统一处理错误码 → 通过 `ErrorCodeEnum` 自动映射 i18n key 显示友好消息
9. **持久化策略**：仅 auth token 和用户基本信息持久化到 localStorage，防止敏感信息泄露
