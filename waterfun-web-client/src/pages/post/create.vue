<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import MarkdownIt from 'markdown-it'
import { previewContent, previewContentAlone, searchTags, getHotTags } from '~/api/postApi'
import { getUploadPolicy, uploadFileToCos, uploadCallback } from '~/api/uploadApi'
import type { TagResponse, PostSaveReq } from '~/api/postApi'

const postStore = usePostStore()
const { categories } = storeToRefs(postStore)
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
const md = new MarkdownIt()
const previewHtml = ref('')
const previewLoading = ref(false)
const showPreview = ref(false)
const wordCount = computed(() => form.content.length)
const emptyPreviewHtml = '<div class="preview-empty"><i class="fas fa-file-alt"></i><p>暂无内容，输入 Markdown 后可预览</p></div>'

const insertMarkdown = (before: string, after: string) => {
  const ta = textareaRef.value
  if (!ta) return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const selected = form.content.slice(start, end)
  form.content = form.content.slice(0, start) + before + selected + after + form.content.slice(end)
  nextTick(() => {
    ta.selectionStart = start + before.length
    ta.selectionEnd = start + before.length + selected.length
    ta.focus()
  })
}

const hotTags = ref<TagResponse[]>([])
const searchResults = ref<TagResponse[]>([])
const searchLoading = ref(false)
let searchAbort: AbortController | null = null

const fetchHotTags = async () => {
  try {
    const res = await getHotTags({ page: 1, size: 20 })
    hotTags.value = ((res.data as any)?.content || []) as TagResponse[]
  } catch { /* ignore */ }
}

const handleTagSearch = async (keyword: string) => {
  if (!keyword.trim()) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  if (searchAbort) searchAbort.abort()
  searchAbort = new AbortController()
  try {
    const res = await searchTags(keyword, 10)
    searchResults.value = (res.data || []) as TagResponse[]
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

const handlePreview = async () => {
  showPreview.value = true
  if (!form.content) {
    previewHtml.value = ''
    return
  }
  previewLoading.value = true
  try {
    const res = postId.value
      ? await previewContent(postId.value, form.content)
      : await previewContentAlone(form.content)
    previewHtml.value = md.render(res.data)
  } catch {
    previewHtml.value = '<div class="preview-empty" style="color:red"><p>预览加载失败</p></div>'
  } finally {
    previewLoading.value = false
  }
}

const tagFetchSuggestions = (query: string, cb: (items: { value: string }[]) => void) => {
  handleTagSearch(query)
  if (query.trim()) {
    setTimeout(() => {
      cb(searchResults.value.map(r => ({ value: r.name, ...r })))
    }, 0)
  } else {
    cb([])
  }
}

const selectTagFromSearch = (tag: TagResponse) => {
  if (!form.tagIds.includes(tag.id)) {
    form.tagIds.push(tag.id)
  }
  newTagInput.value = ''
  searchResults.value = []
}

const toggleHotTag = (tag: TagResponse) => {
  const idx = form.tagIds.indexOf(tag.id)
  if (idx === -1) {
    form.tagIds.push(tag.id)
  } else {
    form.tagIds.splice(idx, 1)
  }
}

const addNewTag = () => {
  const val = newTagInput.value.trim()
  if (!val) return
  if (form.newTags.includes(val)) return
  if (form.newTags.length >= 5) {
    ElMessage.warning('最多添加 5 个自定义标签')
    return
  }
  form.newTags.push(val)
  newTagInput.value = ''
  searchResults.value = []
}

const removeNewTag = (index: number) => {
  form.newTags.splice(index, 1)
}

const textareaRef = ref<HTMLTextAreaElement | null>(null)

const insertImage = async () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.multiple = true
  input.onchange = async () => {
    const files = Array.from(input.files || [])
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
        if (!token) {
          ElMessage.error('上传策略缺少 token')
          continue
        }
        const uploadResp = await uploadFileToCos(cosUrl, method, file)
        if (!uploadResp.ok) {
          ElMessage.error(`上传图片失败: ${file.name}`)
          continue
        }
        const { uuid } = (await uploadCallback({ token })).data
        const markdownImg = `![${file.name}](res://${uuid})\n`
        const ta = textareaRef.value
        if (ta) {
          const start = ta.selectionStart
          form.content = form.content.slice(0, start) + markdownImg + form.content.slice(ta.selectionEnd)
          nextTick(() => {
            ta.selectionStart = ta.selectionEnd = start + markdownImg.length
            ta.focus()
          })
        }
      } catch {
        ElMessage.error(`上传图片失败: ${file.name}`)
      }
    }
  }
  input.click()
}

const buildSaveReq = (): PostSaveReq => ({
  title: form.title,
  subtitle: form.subtitle || undefined,
  content: form.content,
  summary: form.summary || undefined,
  coverageImgId: form.coverageImgId || undefined,
  newTags: form.newTags.length ? form.newTags : undefined,
  tagIds: form.tagIds.length ? form.tagIds : undefined,
  categoryId: form.categoryId!
})

const handlePublish = async () => {
  if (!form.title || !form.content || !form.categoryId) {
    ElMessage.warning(t('post.publishRequired'))
    return
  }
  submitting.value = true
  try {
    const data = buildSaveReq()
    if (postId.value) {
      await postStore.publishPost(postId.value, data)
      ElMessage.success(t('message.success.publishSuccess'))
      router.push(`/post/${postId.value}`)
    } else {
      await postStore.publishNewPost(data)
      ElMessage.success(t('message.success.publishSuccess'))
      router.push('/post')
    }
  } catch {
    ElMessage.error(t('message.failed.publishFailed'))
  } finally {
    submitting.value = false
  }
}

let autoSaveTimer: ReturnType<typeof setTimeout> | null = null
watch(() => ({ title: form.title, content: form.content }), () => {
  if (!postId.value) return
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => { handleSaveDraft() }, 30000)
}, { deep: true })

const handleSaveDraft = async () => {
  if (!form.title || !form.content || !form.categoryId) {
    ElMessage.warning(t('post.publishRequired'))
    return
  }
  submitting.value = true
  try {
    const data = buildSaveReq()
    if (postId.value) {
      await postStore.tempSavePost(postId.value, data)
    } else {
      await postStore.tempSaveNewPost(data)
    }
    ElMessage.success(t('message.success.draftSaved'))
  } catch {
    ElMessage.error(t('message.failed.saveFailed'))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  postStore.fetchCategories()
  fetchHotTags()
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
        <div class="page-header-row">
          <h1 class="page-title">{{ isEdit ? $t('post.edit') : $t('post.create') }}</h1>
        </div>
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
            <div class="editor-header">
              <label class="form-label">{{ $t('post.content') }} <span class="required">*</span></label>
            </div>
            <div class="wf-editor-wrap">
              <div class="wf-editor-toolbar">
                <div class="toolbar-left">
                  <button class="toolbar-btn" title="加粗" @click="insertMarkdown('**', '**')"><i class="fas fa-bold"></i></button>
                  <button class="toolbar-btn" title="斜体" @click="insertMarkdown('*', '*')"><i class="fas fa-italic"></i></button>
                  <button class="toolbar-btn" title="删除线" @click="insertMarkdown('~~', '~~')"><i class="fas fa-strikethrough"></i></button>
                  <span class="toolbar-divider"></span>
                  <button class="toolbar-btn" title="标题" @click="insertMarkdown('### ', '')"><i class="fas fa-heading"></i></button>
                  <button class="toolbar-btn" title="引用" @click="insertMarkdown('> ', '')"><i class="fas fa-quote-right"></i></button>
                  <button class="toolbar-btn" title="代码块" @click="insertMarkdown('```\n', '\n```')"><i class="fas fa-code"></i></button>
                  <span class="toolbar-divider"></span>
                  <button class="toolbar-btn" title="无序列表" @click="insertMarkdown('- ', '')"><i class="fas fa-list-ul"></i></button>
                  <button class="toolbar-btn" title="有序列表" @click="insertMarkdown('1. ', '')"><i class="fas fa-list-ol"></i></button>
                  <span class="toolbar-divider"></span>
                  <button class="toolbar-btn" title="链接" @click="insertMarkdown('[', '](url)')"><i class="fas fa-link"></i></button>
                  <button class="toolbar-btn" title="图片" @click="insertImage"><i class="fas fa-image"></i></button>
                </div>
                <div class="wf-tab-switcher">
                  <button class="wf-tab-btn" :class="{ active: !showPreview }" @click="showPreview = false">
                    <i class="fas fa-pen"></i> 编辑
                  </button>
                  <button class="wf-tab-btn" :class="{ active: showPreview }" @click="handlePreview">
                    <i class="fas fa-eye"></i> 预览
                  </button>
                </div>
              </div>
              <div class="editor-content-area">
                <textarea
                  v-show="!showPreview"
                  ref="textareaRef"
                  v-model="form.content"
                  class="wf-input editor-textarea"
                  placeholder="开始撰写你的内容...&#10;&#10;支持 Markdown 语法。分享有价值的知识，让社区因你而精彩！"
                ></textarea>
                <div v-show="showPreview" class="preview-panel wf-markdown">
                  <div v-if="previewLoading" class="preview-loading"><i class="fas fa-spinner fa-pulse"></i> 预览加载中...</div>
                  <div v-else v-html="previewHtml || emptyPreviewHtml"></div>
                </div>
              </div>
              <div class="wf-editor-footer">
                <div class="editor-footer-left">
                  <span><i class="fab fa-markdown"></i> 支持 Markdown</span>
                  <span><i class="fas fa-keyboard"></i> Ctrl+P 切换预览</span>
                </div>
                <span>{{ wordCount }} 字</span>
              </div>
            </div>
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

            <div v-if="hotTags.length" class="hot-tags-section">
              <span class="hot-tags-label">热门标签：</span>
              <el-tag
                v-for="tag in hotTags"
                :key="tag.id"
                :type="form.tagIds.includes(tag.id) ? 'primary' : 'info'"
                :effect="form.tagIds.includes(tag.id) ? 'dark' : 'plain'"
                style="cursor:pointer;margin:2px 4px 2px 0"
                @click="toggleHotTag(tag)"
              >{{ tag.name }}</el-tag>
            </div>

            <div class="tag-search-row">
              <el-autocomplete
                v-model="newTagInput"
                :fetch-suggestions="tagFetchSuggestions"
                :trigger-on-focus="false"
                placeholder="搜索或输入标签名称"
                style="width: 240px"
                @select="(item: any) => selectTagFromSearch(item as unknown as TagResponse)"
                @keyup.enter="addNewTag"
              >
                <template #append>
                  <el-button @click="addNewTag">添加</el-button>
                </template>
              </el-autocomplete>
            </div>

            <div v-if="form.tagIds.length" class="selected-tags">
              <el-tag
                v-for="tagId in form.tagIds"
                :key="tagId"
                closable
                type="primary"
                @close="form.tagIds = form.tagIds.filter(id => id !== tagId)"
              >
                {{ hotTags.find(t => t.id === tagId)?.name || searchResults.find(r => r.id === tagId)?.name || `Tag-${tagId}` }}
              </el-tag>
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

.page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--wf-text-primary);
  letter-spacing: -0.5px;
  margin: 0;
}

.editor-card {
  background: var(--wf-bg-white);
  border: 1px solid var(--wf-border);
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
  color: var(--wf-text-primary);
  margin-bottom: 8px;
}

.form-label .required {
  color: #ef4444;
  margin-left: 2px;
}

.form-hint {
  font-size: 13px;
  color: var(--wf-text-muted);
  margin-top: 6px;
}

.hot-tags-section {
  margin-bottom: 10px;
}

.hot-tags-label {
  font-size: 13px;
  color: var(--wf-text-secondary);
  margin-right: 6px;
}

.tag-search-row {
  margin-bottom: 10px;
}

.selected-tags, .new-tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 32px;
  background: #f8fafc;
  border-top: 1px solid var(--wf-border);
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.editor-header .form-label {
  margin-bottom: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.toolbar-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--wf-text-secondary);
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: var(--wf-transition);
}

.toolbar-btn:hover {
  color: var(--wf-text-primary);
  background: var(--wf-border-light);
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--wf-border);
  margin: 0 4px;
}

.editor-content-area {
  position: relative;
  min-height: 320px;
}

.editor-textarea {
  min-height: 320px;
  border: none;
  border-radius: 0;
  resize: vertical;
  font-family: inherit;
}

.preview-panel {
  width: 100%;
  min-height: 320px;
  padding: 20px 24px;
  background: var(--wf-bg-white);
}

.editor-footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editor-footer-left span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  color: var(--wf-text-muted);
  text-align: center;
}

.preview-empty i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.preview-empty p {
  font-size: 14px;
  color: var(--wf-text-muted);
}

@media (max-width: 900px) {
  .editor-main {
    padding: 20px 16px 40px;
  }
  .editor-body {
    padding: 20px;
  }
  .action-bar {
    padding: 16px 20px;
  }
  .toolbar-left {
    display: none;
  }
}
</style>