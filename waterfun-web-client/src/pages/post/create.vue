<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getUploadPolicy, uploadFileToCos, uploadCallback } from '~/api/uploadApi'

const postStore = usePostStore()
const { categories, tags } = storeToRefs(postStore)
const router = useRouter()
const { t } = useI18n()

const form = reactive({
  title: '',
  subtitle: '',
  content: '',
  summary: '',
  categoryId: undefined as number | undefined,
  tagIds: [] as number[],
  newTags: [] as string[],
  coverageImgId: '' as string
})

const submitting = ref(false)
const isEdit = ref(false)
const postId = ref<number | null>(null)
const newTagInput = ref('')

const onUploadImg = async (files: File[], callback: (urls: string[], names: string[]) => void) => {
  const uuids: string[] = []
  const names: string[] = []
  for (const file of files) {
    try {
      const ext = file.name.split('.').pop() || 'png'
      const policyResp = await getUploadPolicy({
        bizType: 'POST_CONTENT_IMAGE',
        exts: [ext]
      })
      if (!policyResp.data?.length) {
        ElMessage.error('获取上传策略失败')
        continue
      }
      const { url: cosUrl, method, token } = policyResp.data[0]
      const uploadResp = await uploadFileToCos(cosUrl, method, file)
      if (!uploadResp.ok) {
        ElMessage.error(`上传图片失败: ${file.name}`)
        continue
      }
      if (!token) {
        ElMessage.error('上传策略缺少 token')
        continue
      }
      const callbackResp = await uploadCallback({ token })
      const uuid = callbackResp.data?.uuid
      if (uuid) {
        uuids.push(`Res://${uuid}`)
        names.push(file.name)
      }
    } catch (e) {
      ElMessage.error(`上传图片失败: ${file.name}`)
    }
  }
  if (uuids.length) {
    callback(uuids, names)
  }
}

let autoSaveTimer: ReturnType<typeof setTimeout> | null = null
watch(() => ({ title: form.title, content: form.content }), () => {
  if (!postId.value) return
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => { handleSaveDraft() }, 30000)
}, { deep: true })

const handleCreateDraft = async () => {
  try {
    const id = await postStore.createDraft()
    postId.value = id
    isEdit.value = true
    ElMessage.success(t('message.success.draftCreated'))
    return id
  } catch (err) {
    ElMessage.error(t('message.failed.draftCreateFailed'))
    throw err
  }
}

const handlePublish = async () => {
  if (!form.title || !form.content || !form.categoryId) {
    ElMessage.warning(t('post.publishRequired'))
    return
  }
  submitting.value = true
  try {
    let id = postId.value
    if (!id) {
      id = await handleCreateDraft()
    }
    await postStore.publishPost(id!, {
      title: form.title,
      subtitle: form.subtitle || undefined,
      content: form.content,
      summary: form.summary || undefined,
      coverageImgId: form.coverageImgId || undefined,
      newTags: form.newTags.length ? form.newTags : undefined,
      tagIds: form.tagIds.length ? form.tagIds : undefined,
      categoryId: form.categoryId!
    })
    ElMessage.success(t('message.success.publishSuccess'))
    router.push(`/post/${id}`)
  } catch {
    ElMessage.error(t('message.failed.publishFailed'))
  } finally {
    submitting.value = false
  }
}

const handleSaveDraft = async () => {
  if (!postId.value) {
    ElMessage.warning(t('post.createDraft'))
    return
  }
  if (!form.title || !form.content || !form.categoryId) {
    ElMessage.warning(t('post.publishRequired'))
    return
  }
  submitting.value = true
  try {
    await postStore.tempSavePost(postId.value!, {
      title: form.title,
      subtitle: form.subtitle || undefined,
      content: form.content,
      summary: form.summary || undefined,
      coverageImgId: form.coverageImgId || undefined,
      newTags: form.newTags.length ? form.newTags : undefined,
      tagIds: form.tagIds.length ? form.tagIds : undefined,
      categoryId: form.categoryId!
    })
    ElMessage.success(t('message.success.draftSaved'))
  } catch {
    ElMessage.error(t('message.failed.saveFailed'))
  } finally {
    submitting.value = false
  }
}

const addNewTag = () => {
  const val = newTagInput.value.trim()
  if (!val) return
  if (form.newTags.includes(val)) return
  if (form.newTags.length >= 5) {
    ElMessage.warning('最多添加 5 个标签')
    return
  }
  form.newTags.push(val)
  newTagInput.value = ''
}

const removeNewTag = (index: number) => {
  form.newTags.splice(index, 1)
}

onMounted(() => {
  postStore.fetchCategories()
  postStore.fetchTags()
})
</script>
<template>
  <div>
    <HeaderNavMenu />
    <div class="editor-main">
      <div class="page-header">
        <el-breadcrumb separator=">">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/post' }">社区</el-breadcrumb-item>
          <el-breadcrumb-item>{{ isEdit ? $t('post.edit') : $t('post.create') }}</el-breadcrumb-item>
        </el-breadcrumb>
        <h1 class="page-title">{{ isEdit ? $t('post.edit') : $t('post.create') }}</h1>
      </div>

      <div class="editor-card">
        <div class="editor-body">
          <div class="form-group">
            <label class="form-label">{{ $t('post.title') }} <span class="required">*</span></label>
            <el-input
              v-model="form.title"
              :placeholder="$t('post.placeholder.title')"
              maxlength="32"
              show-word-limit
            />
          </div>

          <div class="form-group">
            <label class="form-label">{{ $t('post.category') }} <span class="required">*</span></label>
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </div>

          <div class="form-group">
            <label class="form-label">{{ $t('post.content') }} <span class="required">*</span></label>
            <ClientOnly>
              <MdEditor
                v-model="form.content"
                :on-upload-img="onUploadImg"
                language="zh-CN"
                :toolbars-exclude="['github']"
                preview-theme="default"
                style="min-height: 400px; border: 1px solid #e2e8f0; border-radius: 8px;"
              />
            </ClientOnly>
          </div>

          <div class="form-group">
            <label class="form-label">{{ $t('post.summary') }}</label>
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="3"
              placeholder="请输入摘要"
              maxlength="500"
              show-word-limit
            />
          </div>

          <div class="form-group">
            <label class="form-label">{{ $t('post.subtitle') }}</label>
            <el-input
              v-model="form.subtitle"
              placeholder="请输入副标题"
              maxlength="64"
              show-word-limit
            />
          </div>

          <div class="form-group">
            <label class="form-label">{{ $t('post.tags') }}</label>
            <div v-if="tags.length" class="existing-tags">
              <el-checkbox-group v-model="form.tagIds">
                <el-checkbox v-for="tag in tags" :key="tag.id" :label="tag.id">
                  {{ tag.name }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
            <div class="new-tag-row">
              <el-input
                v-model="newTagInput"
                :placeholder="$t('post.addTag')"
                style="width: 240px"
                @keyup.enter="addNewTag"
              >
                <template #append>
                  <el-button @click="addNewTag">{{ $t('post.addTag') }}</el-button>
                </template>
              </el-input>
            </div>
            <div v-if="form.newTags.length" class="new-tags-list">
              <el-tag
                v-for="(tag, index) in form.newTags"
                :key="index"
                closable
                type="success"
                @close="removeNewTag(index)"
              >{{ tag }}</el-tag>
            </div>
            <p class="form-hint">{{ $t('post.tagLimit') }}</p>
          </div>
        </div>

        <div class="action-bar">
          <el-button @click="handleSaveDraft" :disabled="submitting" :loading="submitting">
            {{ $t('post.saveDraft') }}
          </el-button>
          <el-button type="primary" @click="handlePublish" :disabled="submitting" :loading="submitting">
            {{ $t('post.publish') }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.editor-main {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 12px 0 0;
  letter-spacing: -0.5px;
}

.editor-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05), 0 1px 2px -1px rgba(0, 0, 0, 0.05);
}

.editor-body {
  padding: 28px 32px;
}

.form-group {
  margin-bottom: 22px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.form-label .required {
  color: #ef4444;
  margin-left: 2px;
}

.form-hint {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 6px;
}

.existing-tags {
  margin-bottom: 12px;
}

.new-tag-row {
  margin-bottom: 8px;
}

.new-tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 32px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

@media (max-width: 768px) {
  .editor-main {
    padding: 20px 16px 40px;
  }
  .editor-body {
    padding: 20px;
  }
  .action-bar {
    padding: 16px 20px;
  }
}
</style>