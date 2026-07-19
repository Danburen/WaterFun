<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { getCategoryOptions } from "~/api/category";
import { createPost, getPostById, putPost, replacePostTags, previewContent, type PostResp, type PostStatus, type PostVisibility, type PostType } from "~/api/post";
import { getTagOptions, searchTags as apiSearchTags, getHotTags } from "~/api/tag";
import { uploadImage, type AdminUploadBizType } from "~/api/upload";
import { getCurrentUserInfo } from "~/api/me";
import MarkdownIt from 'markdown-it';
import { getTagColor } from '@waterfun/web-core/src/tagColor';

const route = useRoute();
const router = useRouter();

const postId = computed(() => route.query.id as string | undefined);
const isEdit = computed(() => !!postId.value);

const submitting = ref(false);
const loading = ref(false);
const loadingOptions = ref(false);
const uploadingCover = ref(false);
const showPreview = ref(false);
const previewHtml = ref('');
const previewLoading = ref(false);
const md = new MarkdownIt();
const emptyPreviewHtml = '<div style="display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:360px;color:#94a3b8"><i class="fa-solid fa-file-alt" style="font-size:48px;opacity:0.3"></i><p style="margin-top:12px">暂无内容</p></div>';

const categoryOptions = ref<OptionResItem<number>[]>([]);
const tagOptions = ref<OptionResItem<number>[]>([]);
const hotTags = ref<OptionResItem<number>[]>([]);
const searchResults = ref<OptionResItem<number>[]>([]);
const searchLoading = ref(false);
const showSearchResults = ref(false);
const userOptions = ref<OptionResItem<string>[]>([]);
const moreOptionsExpanded = ref(false);

const title = ref('');
const content = ref('');
const subtitle = ref('');
const summary = ref('');
const coverageUuid = ref('');
const coverageUrl = ref('');
const slug = ref('');
const visibility = ref<PostVisibility>('PUBLIC');
const authorId = ref<string | null>(null);
const categoryId = ref<number | null>(null);
const tagIds = ref<number[]>([]);
const postType = ref<PostType>('COMMON');
const isPinned = ref(true);

const titleErr = ref('');
const contentErr = ref('');
const categoryIdErr = ref('');

const visibilityOptions = [
  { label: "公开", value: "PUBLIC" },
  { label: "私密", value: "PRIVATE" },
  { label: "粉丝可见", value: "FANS_ONLY" },
];
const postTypeOptions = [
  { label: "普通帖子", value: "COMMON" },
  { label: "公告", value: "NOTICE" },
];

const wordCount = computed(() => content.value.length);
const textareaRef = ref<HTMLTextAreaElement | null>(null);

const insertMarkdown = (before: string, after: string) => {
  const ta = textareaRef.value;
  if (!ta) return;
  const start = ta.selectionStart;
  const end = ta.selectionEnd;
  const selected = content.value.slice(start, end);
  content.value = content.value.slice(0, start) + before + selected + after + content.value.slice(end);
  nextTick(() => {
    ta.selectionStart = start + before.length;
    ta.selectionEnd = start + before.length + selected.length;
    ta.focus();
  });
};

const insertImage = async () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.multiple = true;
  input.onchange = async () => {
    const files = Array.from(input.files || []);
    for (const file of files) {
      try {
        const uuid = await uploadImage('POST_CONTENT_IMAGE' as AdminUploadBizType, file);
        if (!uuid) {
          ElMessage.error(`上传图片失败: ${file.name}`);
          continue;
        }
        const markdownImg = `![${file.name}](res://${uuid})\n`;
        const ta = textareaRef.value;
        if (ta) {
          const start = ta.selectionStart;
          content.value = content.value.slice(0, start) + markdownImg + content.value.slice(ta.selectionEnd);
          nextTick(() => {
            ta.selectionStart = ta.selectionEnd = start + markdownImg.length;
            ta.focus();
          });
        }
      } catch {
        ElMessage.error(`上传图片失败: ${file.name}`);
      }
    }
  };
  input.click();
};

const handlePreview = async () => {
  showPreview.value = true;
  if (!content.value) {
    previewHtml.value = '';
    return;
  }
  previewLoading.value = true;
  try {
    const res = await previewContent(content.value);
    previewHtml.value = md.render(res.data || '');
  } catch {
    previewHtml.value = '<div style="color:red;padding:20px">预览渲染失败</div>';
  } finally {
    previewLoading.value = false;
  }
};

const loadOptions = async () => {
  loadingOptions.value = true;
  try {
    const [me, cr, tr, hr] = await Promise.all([
      getCurrentUserInfo(),
      getCategoryOptions(),
      getTagOptions({ limit: 100 }),
      getHotTags(20),
    ]);
    const myInfo = me.data;
    if (myInfo) {
      const displayName = myInfo.nickname || myInfo.username || `Admin #${myInfo.uid}`;
      userOptions.value = [{ id: myInfo.uid, name: displayName }];
      if (!isEdit.value) {
        authorId.value = myInfo.uid;
      }
    }
    categoryOptions.value = cr.data || [];
    tagOptions.value = tr.data || [];
    hotTags.value = hr.data || [];
  } catch {
    ElMessage.error('获取数据失败');
  } finally {
    loadingOptions.value = false;
  }
};

const fillForm = (data: PostResp) => {
  title.value = data.title || '';
  content.value = data.content || '';
  subtitle.value = data.subtitle || '';
  summary.value = data.summary || '';
  coverageUuid.value = data.coverImg || '';
  coverageUrl.value = data.coverImage?.url || '';
  slug.value = data.slug || '';
  visibility.value = data.visibility || 'PUBLIC';
  authorId.value = data.authorId != null ? String(data.authorId) : null;
  categoryId.value = data.categoryId ? Number(data.categoryId) : null;
  tagIds.value = (data.tagIds || []).map(id => Number(id));
  postType.value = data.type || 'COMMON';
  isPinned.value = data.isPinned ?? false;
};

const loadDetail = async () => {
  if (!isEdit.value || !postId.value) return;
  loading.value = true;
  try {
    const res = await getPostById(postId.value);
    fillForm(res.data);
  } catch {
    ElMessage.error('获取文章数据失败');
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await loadOptions();
  await loadDetail();
});

const toggleTag = (id: number) => {
  const idx = tagIds.value.indexOf(id);
  if (idx >= 0) tagIds.value.splice(idx, 1);
  else tagIds.value.push(id);
};

const toggleHotTag = (tag: OptionResItem<number>) => {
  toggleTag(tag.id);
};

const removeTagId = (id: number) => {
  tagIds.value = tagIds.value.filter(tid => tid !== id);
};

const newTagInput = ref('');
const tagInputRef = ref<HTMLInputElement | null>(null);

const handleTagSearch = async (keyword: string) => {
  if (!keyword.trim()) {
    searchResults.value = [];
    showSearchResults.value = false;
    return;
  }
  searchLoading.value = true;
  showSearchResults.value = true;
  try {
    const res = await apiSearchTags(keyword, 10);
    searchResults.value = res.data || [];
  } catch {
    searchResults.value = [];
  } finally {
    searchLoading.value = false;
  }
};

const selectTagFromSearch = (tag: OptionResItem<number>) => {
  if (!tagIds.value.includes(tag.id)) {
    tagIds.value.push(tag.id);
  }
  newTagInput.value = '';
  searchResults.value = [];
  showSearchResults.value = false;
};

const onTagInputKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    const input = newTagInput.value.trim();
    if (input && tagOptions.value.length > 0) {
      const found = tagOptions.value.find(t => t.name === input);
      if (found) {
        selectTagFromSearch(found);
        return;
      }
    }
    if (searchResults.value.length > 0) {
      selectTagFromSearch(searchResults.value[0]);
    }
  }
  if (e.key === 'Backspace' && !newTagInput.value && tagIds.value.length > 0) {
    tagIds.value.pop();
  }
  if (e.key === 'Escape') {
    showSearchResults.value = false;
  }
};

const selectedTagName = (id: number): string => {
  return hotTags.value.find(t => t.id === id)?.name
    || searchResults.value.find(r => r.id === id)?.name
    || tagOptions.value.find(t => t.id === id)?.name
    || String(id);
};

const onClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement;
  if (!target.closest('.tag-search-wrapper')) {
    showSearchResults.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', onClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside);
});

const handleCoverUpload = async () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.onchange = async () => {
    const file = input.files?.[0];
    if (!file) return;
    uploadingCover.value = true;
    const uuid = await uploadImage('POST_COVERAGE_IMAGE' as AdminUploadBizType, file);
    if (uuid) {
      coverageUuid.value = uuid;
      coverageUrl.value = URL.createObjectURL(file);
      ElMessage.success('封面图上传成功');
    } else {
      ElMessage.error('封面图上传失败');
    }
    uploadingCover.value = false;
  };
  input.click();
};

const removeCoverImage = () => {
  coverageUuid.value = '';
  coverageUrl.value = '';
};

const validate = () => {
  titleErr.value = '';
  contentErr.value = '';
  categoryIdErr.value = '';
  let ok = true;
  if (!title.value.trim()) { titleErr.value = '请输入标题'; ok = false; }
  if (!content.value.trim()) { contentErr.value = '请输入内容'; ok = false; }
  return ok;
};

const buildPayload = (saveStatus: PostStatus) => ({
  title: title.value,
  content: content.value,
  subtitle: subtitle.value || undefined,
  summary: summary.value || undefined,
  coverageUuid: coverageUuid.value || undefined,
  slug: slug.value || undefined,
  status: saveStatus,
  visibility: visibility.value as PostVisibility,
  authorId: authorId.value ?? undefined,
  categoryId: categoryId.value != null ? String(categoryId.value) : undefined,
  tagIds: tagIds.value.length ? tagIds.value.map(id => String(id)) : undefined,
  type: postType.value as PostType,
  isPinned: isPinned.value,
});

const handleSaveDraft = async () => {
  if (!validate()) return;
  submitting.value = true;
  try {
    const payload = buildPayload('DRAFT');
    if (isEdit.value && postId.value) {
      await putPost(postId.value, payload);
      if (tagIds.value.length) {
        await replacePostTags(postId.value, { tagIds: tagIds.value.map(id => String(id)) });
      }
      ElMessage.success('草稿保存成功');
    } else {
      await createPost(payload);
      ElMessage.success('草稿创建成功');
    }
    router.push({ name: 'contentPostList' });
  } catch {
    ElMessage.error('草稿保存失败');
  } finally {
    submitting.value = false;
  }
};

const handlePublish = async () => {
  if (!validate()) return;
  submitting.value = true;
  try {
    const payload = buildPayload('PUBLISHED');
    if (isEdit.value && postId.value) {
      await putPost(postId.value, payload);
      if (tagIds.value.length) {
        await replacePostTags(postId.value, { tagIds: tagIds.value.map(id => String(id)) });
      }
      ElMessage.success('文章发布成功');
    } else {
      await createPost(payload);
      ElMessage.success('文章发布成功');
    }
    router.push({ name: 'contentPostList' });
  } catch {
    ElMessage.error('文章发布失败');
  } finally {
    submitting.value = false;
  }
};

const handleCancel = async () => {
  if (title.value || content.value) {
    try {
      await ElMessageBox.confirm('有未保存的内容，确定离开吗？', '提示', {
        type: 'warning',
        confirmButtonText: '离开',
        cancelButtonText: '继续编辑',
      });
    } catch {
      return;
    }
  }
  router.back();
};
</script>

<template>
  <CardContainer :title="isEdit ? '编辑文章' : '新增文章'">
    <template #header-right>
      <button class="btn" @click="handleCancel"><i class="fa-solid fa-arrow-left"></i> 返回</button>
    </template>

    <div v-if="loadingOptions || loading" class="loading-wrap">
      <i class="fa-solid fa-spinner fa-spin"></i> 加载中...
    </div>

    <div v-else class="editor-layout">
      <div class="editor-main-area">
        <div class="form-field">
          <label class="form-label">标题 <span class="required">*</span></label>
          <div class="form-content">
            <input v-model="title" class="form-input" placeholder="请输入标题" maxlength="32" />
            <div v-if="titleErr" class="form-error">{{ titleErr }}</div>
          </div>
        </div>

        <div class="form-field">
          <label class="form-label">分类</label>
          <div class="form-content">
            <select v-model="categoryId" class="form-select">
              <option :value="null">无分类（全局公告）</option>
              <option v-for="item in categoryOptions" :key="item.id" :value="item.id" :disabled="item.disabled">
                {{ item.name }}
              </option>
            </select>
            <div v-if="categoryIdErr" class="form-error">{{ categoryIdErr }}</div>
          </div>
        </div>

        <div class="form-field">
          <label class="form-label">内容 <span class="required">*</span></label>
          <div class="form-content">
            <div class="wf-editor-wrap">
              <div class="wf-editor-toolbar">
                <div class="toolbar-left">
                  <button class="toolbar-btn" title="加粗" @click="insertMarkdown('**', '**')"><i class="fa-solid fa-bold"></i></button>
                  <button class="toolbar-btn" title="斜体" @click="insertMarkdown('*', '*')"><i class="fa-solid fa-italic"></i></button>
                  <button class="toolbar-btn" title="删除线" @click="insertMarkdown('~~', '~~')"><i class="fa-solid fa-strikethrough"></i></button>
                  <span class="toolbar-divider"></span>
                  <button class="toolbar-btn" title="标题" @click="insertMarkdown('### ', '')"><i class="fa-solid fa-heading"></i></button>
                  <button class="toolbar-btn" title="引用" @click="insertMarkdown('> ', '')"><i class="fa-solid fa-quote-right"></i></button>
                  <button class="toolbar-btn" title="代码块" @click="insertMarkdown('```\n', '\n```')"><i class="fa-solid fa-code"></i></button>
                  <span class="toolbar-divider"></span>
                  <button class="toolbar-btn" title="无序列表" @click="insertMarkdown('- ', '')"><i class="fa-solid fa-list-ul"></i></button>
                  <button class="toolbar-btn" title="有序列表" @click="insertMarkdown('1. ', '')"><i class="fa-solid fa-list-ol"></i></button>
                  <span class="toolbar-divider"></span>
                  <button class="toolbar-btn" title="链接" @click="insertMarkdown('[', '](url)')"><i class="fa-solid fa-link"></i></button>
                  <button class="toolbar-btn" title="图片" @click="insertImage"><i class="fa-solid fa-image"></i></button>
                </div>
                <div class="wf-tab-switcher">
                  <button class="wf-tab-btn" :class="{ active: !showPreview }" @click="showPreview = false">
                    <i class="fa-solid fa-pen"></i> 编辑
                  </button>
                  <button class="wf-tab-btn" :class="{ active: showPreview }" @click="handlePreview">
                    <i class="fa-solid fa-eye"></i> 预览
                  </button>
                </div>
              </div>
              <div class="editor-content-area">
                <textarea
                  v-show="!showPreview"
                  ref="textareaRef"
                  v-model="content"
                  class="form-textarea editor-textarea"
                  placeholder="开始撰写内容...&#10;&#10;支持 Markdown 语法"
                ></textarea>
                <div v-show="showPreview" class="preview-panel wf-markdown">
                  <div v-if="previewLoading" class="preview-loading"><i class="fa-solid fa-spinner fa-pulse"></i> 预览加载中...</div>
                  <div v-else v-html="previewHtml || emptyPreviewHtml"></div>
                </div>
              </div>
              <div class="wf-editor-footer">
                <div class="editor-footer-left">
                  <span><i class="fa-brands fa-markdown"></i> 支持 Markdown</span>
                </div>
                <span>{{ wordCount }} 字</span>
              </div>
            </div>
            <div v-if="contentErr" class="form-error">{{ contentErr }}</div>
          </div>
        </div>

        <div class="more-options-toggle" @click="moreOptionsExpanded = !moreOptionsExpanded">
          <i class="fa-solid" :class="moreOptionsExpanded ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
          更多选项
        </div>

        <template v-if="moreOptionsExpanded">
          <div class="form-field">
            <label class="form-label">摘要</label>
            <div class="form-content">
              <textarea v-model="summary" class="form-textarea" rows="3" placeholder="留空则自动从正文截取" maxlength="500"></textarea>
            </div>
          </div>

          <div class="form-field">
            <label class="form-label">副标题</label>
            <div class="form-content">
              <input v-model="subtitle" class="form-input" placeholder="请输入副标题" maxlength="64" />
            </div>
          </div>
        </template>

        <div class="form-field">
          <label class="form-label">标签</label>
          <div class="form-content">
            <div v-if="hotTags.length" class="tag-suggestions">
              <span
                v-for="tag in hotTags"
                :key="tag.id"
                class="tag-suggestion"
                :class="{ active: tagIds.includes(tag.id) }"
                @click="toggleHotTag(tag)"
              >{{ tag.name }}</span>
            </div>

            <div class="tag-search-wrapper">
              <div class="tag-input-wrap">
                <span v-for="tagId in tagIds" :key="tagId" class="tag-chip"
                  :style="{ backgroundColor: getTagColor(selectedTagName(tagId)), color: '#fff' }">
                  {{ selectedTagName(tagId) }}
                  <span class="remove" @click="removeTagId(tagId)">&times;</span>
                </span>
                <input
                  ref="tagInputRef"
                  v-model="newTagInput"
                  class="tag-input"
                  type="text"
                  placeholder="输入标签名称搜索"
                  @input="handleTagSearch(newTagInput)"
                  @keydown="onTagInputKeydown"
                />
              </div>

              <div v-if="showSearchResults && newTagInput.trim()" class="tag-search-dropdown">
                <div v-if="searchLoading" class="tag-search-loading">搜索中...</div>
                <div
                  v-for="tag in searchResults"
                  :key="tag.id"
                  class="tag-search-item"
                  :class="{ selected: tagIds.includes(tag.id) }"
                  @click="selectTagFromSearch(tag)"
                >
                  <span class="tag-search-name">{{ tag.name }}</span>
                </div>
                <div v-if="!searchLoading && searchResults.length === 0" class="tag-search-empty">
                  未找到匹配的标签
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="editor-sidebar">
        <div class="sidebar-section">
          <h3 class="sidebar-title">发布设置</h3>

          <div class="sidebar-field">
            <label class="sidebar-label">帖子类型</label>
            <select v-model="postType" class="form-select">
              <option v-for="item in postTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </div>

          <div class="sidebar-field">
            <label class="sidebar-label">可见性</label>
            <select v-model="visibility" class="form-select">
              <option v-for="item in visibilityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </div>

          <div class="sidebar-field">
            <label class="sidebar-label">作者</label>
            <select v-model="authorId" class="form-select">
              <option :value="null">系统（匿名）</option>
              <option v-for="item in userOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </option>
            </select>
          </div>

          <div class="sidebar-field">
            <label class="sidebar-label">唯一标识符</label>
            <input v-model="slug" class="form-input" placeholder="slug" maxlength="200" />
          </div>

          <div class="sidebar-field checkbox-field">
            <label>
              <input type="checkbox" v-model="isPinned" />
              置顶该帖子
            </label>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="more-options-toggle" @click="moreOptionsExpanded = !moreOptionsExpanded">
            <i class="fa-solid" :class="moreOptionsExpanded ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
            更多选项
          </div>
          <template v-if="moreOptionsExpanded">
            <div class="sidebar-field">
              <label class="sidebar-label">封面图</label>
              <div class="cover-upload-area">
                <div v-if="coverageUrl" class="cover-preview-wrap">
                  <img :src="coverageUrl" alt="封面预览" class="cover-preview-img" />
                  <div class="cover-preview-actions">
                    <button class="btn btn-sm" :disabled="uploadingCover" @click="handleCoverUpload">
                      <i v-if="uploadingCover" class="fa-solid fa-spinner fa-spin"></i>
                      <i v-else class="fa-solid fa-rotate"></i>
                      重新上传
                    </button>
                    <button class="btn btn-sm btn-danger" @click="removeCoverImage">
                      <i class="fa-solid fa-trash"></i>
                      移除
                    </button>
                  </div>
                </div>
                <template v-else>
                  <div class="cover-dropzone" @click="handleCoverUpload">
                    <i class="fa-solid fa-image"></i>
                    <span>{{ uploadingCover ? '上传中...' : '点击上传封面图' }}</span>
                  </div>
                  <p class="cover-hint">支持 jpg、png、gif 格式</p>
                </template>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div class="action-bar">
      <button class="btn" @click="handleCancel">取消</button>
      <button class="btn" :disabled="submitting" @click="handleSaveDraft">
        <i v-if="submitting" class="fa-solid fa-spinner fa-spin"></i>
        保存草稿
      </button>
      <button class="btn btn-primary" :disabled="submitting" @click="handlePublish">
        <i v-if="submitting" class="fa-solid fa-spinner fa-spin"></i>
        {{ isEdit ? '保存并发布' : '发布' }}
      </button>
    </div>
  </CardContainer>
</template>

<style scoped>
.required {
  color: #ef4444;
  margin-left: 2px;
}

.editor-layout {
  display: flex;
  gap: 24px;
}

.editor-main-area {
  flex: 1;
  min-width: 0;
}

.editor-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.sidebar-section {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 16px;
  margin-bottom: 16px;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
}

.sidebar-field {
  margin-bottom: 14px;
}

.sidebar-field:last-child {
  margin-bottom: 0;
}

.sidebar-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.checkbox-field label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}

.cover-upload-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cover-uuid {
  font-size: 11px;
  color: var(--text-muted);
  word-break: break-all;
}

.cover-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
}

.cover-preview-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cover-preview-img {
  width: 100%;
  max-height: 160px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--border);
}
.cover-preview-actions {
  display: flex;
  gap: 6px;
}
.cover-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 12px;
  border: 2px dashed var(--border);
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 13px;
  transition: border-color 0.2s;
}
.cover-dropzone:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.cover-dropzone i {
  font-size: 28px;
  opacity: 0.4;
}

/* Editor styles */
.wf-editor-wrap {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.wf-editor-toolbar {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  background: var(--bg);
  border-bottom: 1px solid var(--border);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}

.toolbar-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  color: var(--text-primary);
  background: var(--border-light);
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--border);
  margin: 0 4px;
}

.wf-tab-switcher {
  display: flex;
  gap: 2px;
  margin-left: auto;
}

.wf-tab-btn {
  padding: 4px 14px;
  border: 1px solid var(--border);
  background: var(--bg-white);
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 6px;
  font-size: 13px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.wf-tab-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.wf-tab-btn.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.editor-content-area {
  position: relative;
}

.editor-textarea {
  min-height: 360px;
  border: none;
  border-radius: 0;
  resize: vertical;
  font-family: inherit;
  line-height: 1.7;
  font-size: 14px;
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

.preview-panel {
  width: 100%;
  min-height: 360px;
  padding: 20px 24px;
  background: var(--bg-white);
}

.preview-panel :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
}

.preview-panel :deep(pre) {
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  color: var(--text-muted);
  gap: 8px;
}

.wf-editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-top: 1px solid var(--border);
  background: var(--bg);
  font-size: 12px;
  color: var(--text-muted);
}

/* Tag styles */
.tag-suggestions {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.tag-suggestion {
  padding: 4px 12px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 20px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.tag-suggestion:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-light);
}

.tag-suggestion.active {
  border-color: var(--primary);
  color: #fff;
  background: var(--primary);
}

.tag-search-wrapper {
  position: relative;
}

.tag-input-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-white);
  min-height: 44px;
  align-items: center;
  transition: all 0.2s ease;
}

.tag-input-wrap:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.6;
  animation: tagIn 0.2s ease;
  white-space: nowrap;
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
  color: var(--text-primary);
  background: transparent;
  padding: 4px 2px;
}

.tag-input::placeholder { color: var(--text-muted); }

.tag-search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 50;
  margin-top: 4px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
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
  background: var(--primary-light);
}

.tag-search-item.selected {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}

.tag-search-name {
  font-size: 14px;
  color: var(--text-primary);
}

.tag-search-loading,
.tag-search-empty {
  padding: 14px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

.more-options-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-muted);
  padding: 6px 0;
  margin-bottom: 6px;
  user-select: none;
  transition: color 0.15s;
}
.more-options-toggle:hover {
  color: var(--primary);
}
.more-options-toggle i {
  font-size: 12px;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

@media (max-width: 960px) {
  .editor-layout {
    flex-direction: column;
  }
  .editor-sidebar {
    width: 100%;
  }
}
</style>
