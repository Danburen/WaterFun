<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="handleVisibleChange"
    :title="title"
    width="400px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="avatar-upload-modal">
      <div class="avatar-preview-container">
        <div class="avatar-preview-wrapper">
          <div 
            class="avatar-preview" 
            :style="{ backgroundImage: `url(${previewUrl})` }"
          >
            <div v-if="!previewUrl" class="avatar-placeholder">
              <el-icon class="avatar-icon"><Plus /></el-icon>
              <span>上传头像</span>
            </div>
            <div class="avatar-mask"></div>
          </div>
        </div>
      </div>
      
      <div class="avatar-upload-actions">
        <el-upload
          class="avatar-uploader"
          action=""
          :show-file-list="false"
          :http-request="handleFileUpload"
          :before-upload="beforeUpload"
          :disabled="uploading"
        >
          <el-button :type="previewUrl ? 'primary' : 'default'" :disabled="uploading">
            <el-icon v-if="!previewUrl"><Upload /></el-icon>
            <el-icon v-else><Plus /></el-icon>
            {{ previewUrl ? '重新上传' : '选择图片' }}
          </el-button>
        </el-upload>
      </div>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ saving ? '保存中...' : '保存' }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
//@ts-ignore
import { Upload, Plus } from '@element-plus/icons-vue';
import type { UploadProps } from 'element-plus';
import { callbackAvatarUpload, getAvatarUploadPolicy, uploadFileToCos } from '~/api/userApi';

interface Props {
  visible: boolean;
  title?: string;
}

const props = withDefaults(defineProps<Props>(), {
  title: '上传头像'
});

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'submitted': [];
}>();

const previewUrl = ref<string>('');
const uploading = ref(false);
const saving = ref(false);
const selectedFile = ref<File | null>(null);

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('头像必须是 JPG 或 PNG 格式!');
    return false;
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('头像大小不能超过 2MB!');
    return false;
  }
  return true;
};

const handleFileUpload = async (uploadFile: any) => {
  const file = uploadFile.file;
  uploading.value = true;
  
  try {
    const reader = new FileReader();
    reader.onload = (e) => {
      previewUrl.value = e.target?.result as string;
      selectedFile.value = file;
    };
    reader.readAsDataURL(file);
  } catch (error) {
    ElMessage.error('图片预览失败');
  } finally {
    uploading.value = false;
  }
};

const handleSave = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择图片');
    return;
  }
  
  saving.value = true;
  
  try {
    const suffix = selectedFile.value.name.split('.').pop()?.toLowerCase() || '';
    const policyResponse = (await getAvatarUploadPolicy(suffix));
    
    if (!policyResponse.data || policyResponse.data.length === 0) {
      throw new Error(policyResponse.message || '获取上传策略失败');
    }
    
    const { key, url: cosUrl, method, token } = policyResponse.data[0];
    console.log('获取上传策略成功:', { key, cosUrl, method, token });
    const response = await uploadFileToCos(cosUrl, method, selectedFile.value);
    console.log('上传到COS成功:', response);
    if (!response.ok) {
      throw new Error('上传到COS失败');
    }

    await callbackAvatarUpload({
      token: token || '',
    });

    emit('submitted');
    emit('update:visible', false);
    ElMessage.success('头像上传成功');
    
    previewUrl.value = '';
    selectedFile.value = null;
  } catch (error) {
    console.error('头像上传失败:', error);
    ElMessage.error('头像上传失败，请重试');
  } finally {
    saving.value = false;
  }
};

const handleVisibleChange = (newVisible: boolean) => {
  emit('update:visible', newVisible);
  if (!newVisible) {
    previewUrl.value = '';
    selectedFile.value = null;
  }
};

const handleCancel = () => {
  emit('update:visible', false);
};
</script>

<style scoped>
.avatar-upload-modal {
  text-align: center;
}

.avatar-preview-container {
  margin-bottom: 20px;
}

.avatar-preview-wrapper {
  display: inline-block;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #e0e0e0;
  position: relative;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
  color: #909399;
}

.avatar-icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.1) 100%);
}

.avatar-upload-actions {
  margin-bottom: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>