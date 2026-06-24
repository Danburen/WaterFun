<template>
  <div class="profile-info">
    <div class="profile-header">
      <div class="profile-avatar">
        <el-avatar 
          :size="100" 
          :src="editForm.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
          class="avatar"
        />
        <div class="avatar-upload">
          <el-button type="primary" size="small" @click="showAvatarModal = true">更换头像</el-button>
        </div>
      </div>
    </div>
    <div class="g-form">
      <div class="g-form-section">
        <div class="g-section-title">基本信息</div>
        <div class="g-form-row" >
          <div class="g-form-item">
            <label class="g-form-label">昵称</label>
            <el-input 
              v-model="editForm.nickname" 
              placeholder="请输入昵称" 
              maxlength="50"
            />
          </div>
          
          <div class="g-form-item">
            <label class="g-form-label">性别</label>
            <el-radio-group v-model="editForm.gender">
              <el-radio value="MALE">男</el-radio>
              <el-radio value="FEMALE">女</el-radio>
            </el-radio-group>
          </div>
        </div>
        
        <div class="g-form-row">
          <div class="g-form-item"> 
            <label class="g-form-label">生日</label>
            <el-date-picker
              v-model="editForm.birthday"
              type="date"
              placeholder="选择生日"
              style="width: 100%"
            />
          </div>
        </div>
        
        <div class="g-form-row">
          <div class="g-form-item full-width">
            <label class="g-form-label">个人介绍</label> 
            <el-input 
              v-model="editForm.bio" 
              type="textarea" 
              :rows="4" 
              placeholder="关于你的个性、兴趣或经验..."
              maxlength="100"
            />
          </div>
        </div>
        
        <div class="g-form-row">
          <div class="g-form-item full-width">  
            <label class="g-form-label">{{ $t('user.profile.residence') }}</label>
              <el-cascader
                v-model="editForm.residence"
                :placeholder="$t('option.select') + $t('user.profile.residence')"
                :options="locationOptions"
                style="width: 100%"
              />
          </div>
        </div>
      </div>
    </div>

    <div class="save-button-container">
      <el-button type="primary" size="large" @click="saveChanges" :loading="loading">
        保存
      </el-button>
    </div>
  </div>
  
  <AvatarUploadModal
      v-model:visible="showAvatarModal"
      @submitted="handleAvatarSubmitted"
    />
</template>

<script setup lang="ts">
definePageMeta({
  ssr: false
})

import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserInfoStore } from "~/stores/userInfoStore";
import { useUserProfileStore } from "~/stores/userProfileStore";
import { useI18n } from 'vue-i18n';
import { provinceAndCityData } from 'element-china-area-data';
import AvatarUploadModal from '~/components/AvatarUploadModal.vue';

const router = useRouter();
const userInfoStore = useUserInfoStore();
const userProfileStore = useUserProfileStore();

const i18n = useI18n();

// 动态设置页面标题，确保在客户端渲染时获取正确的昵称
onMounted(() => {
  useHead({
    title: i18n.t('user.profile.info', { nickname: userInfoStore.userInfo.nickname
      || userInfoStore.userInfo.username || '用户' })
  })
})


const loading = ref(false);
const showAvatarModal = ref(false);


const editForm = ref({
  nickname: '',
  bio: '',
  gender: '',
  avatar: '',
  birthday: undefined as string | Date | undefined,
  residence: [] as string[],
});
const locationOptions = provinceAndCityData as any[];

const userProfile = computed(() => userProfileStore.userProfile);


const saveChanges = async () => {
  loading.value = true;
  try {
    const residence = Array.isArray(editForm.value.residence)
      ? editForm.value.residence.join(' ')
      : editForm.value.residence;
    const birthday = editForm.value.birthday
      ? (editForm.value.birthday instanceof Date
          ? editForm.value.birthday.toISOString().split('T')[0]
          : editForm.value.birthday)
      : undefined;

    await userProfileStore.updateUserProfile({
      bio: editForm.value.bio,
      gender: editForm.value.gender,
      birthday: birthday as any,
      residence,
    });

    // 如果有昵称变更，单独调用昵称更新
    if (editForm.value.nickname && editForm.value.nickname !== userInfoStore.userInfo.nickname) {
      // 尝试更新昵称（如果后端支持）
      try {
        await userInfoStore.updateNickname(editForm.value.nickname);
      } catch {
        // 昵称更新失败不影响主流程
        console.warn('昵称更新失败');
      }
    }

    ElMessage.success(i18n.t('message.success.saveSuccess'));
  } catch (error) {
    ElMessage.error(i18n.t('message.failed.saveFailed'));
    console.error('更新用户资料失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleAvatarSubmitted = () => {
  ElMessage.success('头像已提交审核，审核通过后会自动生效');
};

const loadUserData = async () => {
  const profile = userProfileStore.userProfile;
  editForm.value = {
    nickname: userInfoStore.userInfo.nickname || userInfoStore.userInfo.username || '',
    bio: profile.bio || '',
    gender: profile.gender || '',
    avatar: await userProfileStore.getAvatarUrl() || '',
    birthday: profile.birthday ? new Date(profile.birthday) : undefined as any,
    residence: profile.residence ? profile.residence.split(' ') : [],
  };
};

onMounted(async () => {
  await loadUserData();
});
</script>

<style scoped>
.profile-info {
  max-width: 800px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.profile-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
}

.profile-avatar {
  position: relative;
  display: inline-block;
}

.avatar {
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 15px;
}

.avatar-upload {
  margin-top: 10px;
}


.save-button-container {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0;
  margin-top: 30px;
}

.save-button-container .el-button {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.full-width {
  flex: 0 0 100%;
  min-width: 100%;
}

.bio {
  min-height: 80px;
  word-break: break-word;
  line-height: 1.8;
}
</style>