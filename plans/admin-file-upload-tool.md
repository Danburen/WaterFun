# Admin 端通用文件上传工具实现计划

## 概述

为 admin 端实现一个基于 el-upload 封装的通用文件上传组件，使用 presignedUrl 方式上传文件到云存储。

## 技术方案

### 核心流程

```
用户选择文件 → 前端缓存 File 对象
    ↓
调用后端 API 获取 presignedUrl
    ↓
使用 presignedUrl 上传文件到存储
    ↓
emit 事件通知父组件上传结果 {key, token}
```

### 文件结构

```
waterfun-admin/src/
├── api/
│   └── resource.ts          # 通用资源上传 API
└── components/
    └── FileUploader.vue     # 文件上传组件
```

## 实现细节

### 1. API 层 (`waterfun-admin/src/api/resource.ts`)

提供以下功能：
- `getPresignedUrl(suffix)` - 获取预签名 URL
- `uploadFileToStorage(url, method, file)` - 上传文件到存储
- `uploadCallback(data)` - 上传回调（可选）

### 2. 组件层 (`waterfun-admin/src/components/FileUploader.vue`)

基于 el-upload 封装，提供以下功能：
- 支持 v-model 双向绑定已上传文件列表
- 自动获取预签名 URL
- 自动上传到存储
- 上传成功后 emit 事件给父组件
- 支持文件类型、大小限制
- 支持多文件上传

#### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modelValue | UploadedFile[] | [] | 已上传文件列表 |
| accept | string | '*' | 接受的文件类型 |
| maxSize | number | 10 | 最大文件大小(MB) |
| limit | number | 9 | 最大文件数量 |
| listType | string | 'picture-card' | 列表类型 |
| disabled | boolean | false | 是否禁用 |

#### Events

| 事件 | 参数 | 说明 |
|------|------|------|
| update:modelValue | UploadedFile[] | 文件列表更新 |
| success | (file, response) | 单个文件上传成功 |
| error | (error, file) | 上传失败 |

#### UploadedFile 类型

```typescript
interface UploadedFile {
  key: string      // 存储路径
  token: string    // 上传凭证
  name: string     // 文件名
  url?: string     // 预览地址（可选）
}
```

## 使用示例

### 基础用法

```vue
<script setup lang="ts">
import FileUploader from '~/components/FileUploader.vue'
import type { UploadedFile } from '~/components/FileUploader.vue'

const uploadedFiles = ref<UploadedFile[]>([])

const handleSuccess = (file: UploadedFile) => {
  console.log('上传成功:', file.key)
  // 将 file.key 和 file.token 保存到表单
}
</script>

<template>
  <FileUploader
    v-model="uploadedFiles"
    accept="image/*"
    :max-size="5"
    :limit="3"
    @success="handleSuccess"
  />
</template>
```

### 在 BannerCreateDialog 中使用

```vue
<script setup lang="ts">
import FileUploader from '~/components/FileUploader.vue'
import type { UploadedFile } from '~/components/FileUploader.vue'

const form = reactive({
  // ... 其他字段
  coverages: [] as UploadedFile[]
})

const handleUploadSuccess = (file: UploadedFile) => {
  form.coverages.push(file)
}

const handleSubmit = async () => {
  // 提交时使用 file.key 和 file.token
  await createBanner({
    // ... 其他字段
    coverages: form.coverages.map(f => ({
      key: f.key,
      token: f.token
    }))
  })
}
</script>

<template>
  <el-form :model="form">
    <!-- 其他表单项 -->
    
    <el-form-item label="封面图">
      <FileUploader
        v-model="form.coverages"
        accept="image/*"
        :max-size="5"
        :limit="3"
        @success="handleUploadSuccess"
      />
    </el-form-item>
  </el-form>
</template>
```

## 优势

1. **简单易用** - 基于 el-upload 封装，使用方式与 el-upload 一致
2. **自动处理** - 自动获取 presignedUrl 和上传，无需手动调用
3. **类型安全** - 完整的 TypeScript 类型定义
4. **灵活扩展** - 支持自定义文件类型、大小限制
5. **双向绑定** - 支持 v-model 绑定已上传文件列表
