
AVATAR WORKFLOW:
1. FIRST UPLOAD
    1. FRONTEND -> BACKEND
    2. BACKEND -> COS(ROOT KEY: TEMP)
    3. BACKEND -> DB (task: target_id = "user:10086:avatar", content = TEMP COS URL, status = "pending")
    4. COS -> BACKEND (callback: target_id = "user:10086:avatar", content = COS URL)
    5. BACKEND -> DB (update task: content = COS URL, status = "APPROVED")

2. REUPLOAD
用户第一次上传头像
↓
生成 target_id = "user:10086:avatar"
↓
无记录 → 新建 task
↓
用户再次编辑上传
↓
同样 target_id = "user:10086:avatar"
↓
有记录 → 更新原 task（替换 content，update_at 刷新）
↓
删除旧 COS 文件，上传新文件

内容管理（Content Management）
├── 帖子/评论管理（UGC内容）
├── 板块/分类管理
└── 多媒体资源管理

用户管理（User Management）
├── 用户资料管理
│   ├── 头像审核
│   ├── 昵称/Bio审核
│   └── 个人主页背景审核
├── 用户行为管理
│   ├── 注册审核（如需要）
│   └── 实名认证审核
└── 权限/角色管理

内容审核（Content Moderation）—— 横向支撑层
├── 文本内容审核（接入帖子、评论、Bio等）
├── 图片内容审核（接入头像、帖子图片等）
├── 视频/音频审核
└── 审核策略配置