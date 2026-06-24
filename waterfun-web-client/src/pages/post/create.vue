<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import MarkdownIt from 'markdown-it'
import { previewContent, previewContentAlone, searchTags, getHotTags } from '~/api/postApi'
import { getUploadPolicy, uploadFileToCos, uploadCallback } from '~/api/uploadApi'
import type { TagResponse, PostSaveReq } from '~/api/postApi'

const postStore = usePostStore()
const { categories, editDraft } = storeToRefs(postStore)
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const form = reactive({
  title: '',
  subtitle: '',
  content: '',
  summary: '',
  categoryId: undefined as string | undefined,
  tagIds: [] as string[],
  newTags: [] as string[],
  coverageImgId: '' as string
})

const toBigInt = (v: string) => BigInt(v)

const submitting = ref(false)
const isEdit = ref(false)
const postId = ref<string | null>(null)
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
const showSearchResults = ref(false)
const tagInputRef = ref<HTMLInputElement | null>(null)
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
    showSearchResults.value = false
    return
  }
  searchLoading.value = true
  showSearchResults.value = true
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

const selectTagFromSearch = (tag: TagResponse) => {
  if (!form.tagIds.includes(tag.id)) {
    form.tagIds.push(tag.id)
  }
  newTagInput.value = ''
  searchResults.value = []
  showSearchResults.value = false
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
  showSearchResults.value = false
}

const removeNewTag = (index: number) => {
  form.newTags.splice(index, 1)
}

const removeTagId = (id: string) => {
  form.tagIds = form.tagIds.filter(tid => tid !== id)
}

const selectedTagName = (id: string): string => {
  return hotTags.value.find(t => t.id === id)?.name || searchResults.value.find(r => r.id === id)?.name || ''
}

const onTagInputKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    e.preventDefault()
    if (searchResults.value.length > 0) {
      selectTagFromSearch(searchResults.value[0])
    } else {
      addNewTag()
    }
  }
  if (e.key === 'Backspace' && !newTagInput.value && form.tagIds.length > 0) {
    form.tagIds.pop()
  }
  if (e.key === 'Escape') {
    showSearchResults.value = false
  }
}

const onClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.tag-search-wrapper')) {
    showSearchResults.value = false
  }
}

onMounted(() => {
  postStore.fetchCategories()
  fetchHotTags()
  document.addEventListener('click', onClickOutside)
  // Edit mode: if ?id= exists, fetch draft
  const editId = route.query.id as string | undefined
  if (editId) {
    isEdit.value = true
    postId.value = editId
    postStore.fetchEditDraft(editId)
  }
})

watch(editDraft, (draft) => {
  if (!draft || !isEdit.value) return
  form.title = draft.editedTitle || ''
  form.content = draft.editedContent || ''
  form.summary = draft.editedSummary || ''
  if (draft.editedCategoryId) {
    form.categoryId = draft.editedCategoryId.id
  }
  form.tagIds = draft.editedTagIds?.map(t => t.id) || []
  form.newTags = draft.editedNewTagIds || []
}, { immediate: false })

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})

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
  tagIds: form.tagIds.length ? form.tagIds.map(toBigInt) : undefined,
  categoryId: BigInt(form.categoryId!)
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

            <div v-if="hotTags.length" class="tag-suggestions">
              <span
                v-for="tag in hotTags"
                :key="tag.id"
                class="tag-suggestion"
                :class="{ active: form.tagIds.includes(tag.id) }"
                @click="toggleHotTag(tag)"
              >{{ tag.name }}</span>
            </div>

            <div class="tag-search-wrapper">
              <div class="tag-input-wrap">
                <span
                  v-for="tagId in form.tagIds"
                  :key="tagId"
                  class="tag-chip"
                >
                  {{ selectedTagName(tagId) }}
                  <span class="remove" @click="removeTagId(tagId)">&times;</span>
                </span>
                <span
                  v-for="(tag, index) in form.newTags"
                  :key="'new-' + index"
                  class="tag-chip new-tag"
                >
                  {{ tag }}
                  <span class="remove" @click="removeNewTag(index)">&times;</span>
                </span>
                <input
                  ref="tagInputRef"
                  v-model="newTagInput"
                  class="tag-input"
                  type="text"
                  placeholder="输入标签名称搜索或按回车添加"
                  @input="handleTagSearch(newTagInput)"
                  @keydown="onTagInputKeydown"
                >
              </div>

              <div v-if="showSearchResults && newTagInput.trim()" class="tag-search-dropdown">
                <div v-if="searchLoading" class="tag-search-loading">搜索中...</div>
                <div
                  v-for="tag in searchResults"
                  :key="tag.id"
                  class="tag-search-item"
                  :class="{ selected: form.tagIds.includes(tag.id) }"
                  @click="selectTagFromSearch(tag)"
                >
                  <span class="tag-search-name">{{ tag.name }}</span>
                  <span class="tag-search-count">{{ tag.usageCount || 0 }} 帖子</span>
                </div>
                <div v-if="!searchLoading && searchResults.length === 0 && newTagInput.trim()" class="tag-search-empty">
                  按 <strong>Enter</strong> 添加「{{ newTagInput.trim() }}」为新标签
                </div>
              </div>
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

/* ========== Tags ========== */
.tag-suggestions {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.tag-suggestion {
  padding: 5px 14px;
  background: var(--wf-bg);
  border: 1px solid var(--wf-border);
  border-radius: 20px;
  font-size: 13px;
  color: var(--wf-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.tag-suggestion:hover {
  border-color: var(--wf-primary);
  color: var(--wf-primary);
  background: var(--wf-primary-light);
}

.tag-suggestion.active {
  border-color: var(--wf-primary);
  color: #fff;
  background: var(--wf-primary);
}

.tag-search-wrapper {
  position: relative;
}

.tag-input-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 10px;
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  background: var(--wf-bg-white);
  min-height: 44px;
  align-items: center;
  transition: all 0.2s ease;
}

.tag-input-wrap:focus-within {
  border-color: var(--wf-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: var(--wf-primary-light);
  color: var(--wf-primary);
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.6;
  animation: tagIn 0.2s ease;
  white-space: nowrap;
}

.tag-chip.new-tag {
  background: #ecfdf5;
  color: #059669;
}

@keyframes tagIn {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}

.tag-chip .remove {
  cursor: pointer;
  opacity: 0.5;
  transition: opacity 0.2s;
  font-size: 14px;
  line-height: 1;
  margin-left: 2px;
}

.tag-chip .remove:hover { opacity: 1; }

.tag-input {
  flex: 1;
  min-width: 100px;
  border: none;
  outline: none;
  font-size: 14px;
  color: var(--wf-text-primary);
  background: transparent;
  padding: 4px 2px;
}

.tag-input::placeholder { color: var(--wf-text-muted); }

.tag-search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 50;
  margin-top: 4px;
  background: var(--wf-bg-white);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  max-height: 240px;
  overflow-y: auto;
}

.tag-search-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.tag-search-item:hover {
  background: var(--wf-primary-light);
}

.tag-search-item.selected {
  background: var(--wf-primary-light);
  color: var(--wf-primary);
  font-weight: 600;
}

.tag-search-name {
  font-size: 14px;
  color: var(--wf-text-primary);
}

.tag-search-count {
  font-size: 12px;
  color: var(--wf-text-muted);
}

.tag-search-loading,
.tag-search-empty {
  padding: 14px;
  text-align: center;
  font-size: 13px;
  color: var(--wf-text-muted);
}

.tag-search-empty strong {
  color: var(--wf-primary);
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