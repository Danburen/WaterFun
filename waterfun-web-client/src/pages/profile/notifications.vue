<template>
  <div class="notifications-settings">
    <div class="settings-header">
      <h2>通知与隐私</h2>
      <p>管理你的通知偏好和隐私设置</p>
    </div>
    
    <div class="settings-content">
      <!-- 通知设置 -->
      <div class="settings-section">
        <div class="section-title">
          <h3>通知设置</h3>
        </div>
        <div class="settings-list">
          <div class="setting-item">
            <div class="setting-info">
              <h4>消息通知</h4>
              <p>接收来自其他用户的私信</p>
            </div>
            <div class="setting-action">
              <el-switch v-model="notificationSettings.messageNotifications" />
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>评论通知</h4>
              <p>当有人评论你的作品时收到通知</p>
            </div>
            <div class="setting-action">
              <el-switch v-model="notificationSettings.commentNotifications" />
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>点赞通知</h4>
              <p>当有人点赞你的作品时收到通知</p>
            </div>
            <div class="setting-action">
              <el-switch v-model="notificationSettings.likeNotifications" />
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>关注通知</h4>
              <p>当有人关注你时收到通知</p>
            </div>
            <div class="setting-action">
              <el-switch v-model="notificationSettings.followNotifications" />
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>活动通知</h4>
              <p>接收平台活动和更新通知</p>
            </div>
            <div class="setting-action">
              <el-switch v-model="notificationSettings.eventNotifications" />
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>邮件通知</h4>
              <p>通过邮件接收重要通知</p>
            </div>
            <div class="setting-action">
              <el-switch v-model="notificationSettings.emailNotifications" />
            </div>
          </div>
        </div>
      </div>
      
      <!-- 隐私设置 -->
      <div class="settings-section">
        <div class="section-title">
          <h3>隐私设置</h3>
        </div>
        <div class="settings-list">
          <div class="setting-item">
            <div class="setting-info">
              <h4>个人资料可见性</h4>
              <p>设置谁可以查看你的个人资料</p>
            </div>
            <div class="setting-action">
              <el-select v-model="privacySettings.profileVisibility" placeholder="选择可见性">
                <el-option label="公开" value="PUBLIC" />
                <el-option label="仅关注者" value="FOLLOWERS" />
                <el-option label="私密" value="PRIVATE" />
              </el-select>
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>作品可见性</h4>
              <p>设置谁可以查看你的作品</p>
            </div>
            <div class="setting-action">
              <el-select v-model="privacySettings.workVisibility" placeholder="选择可见性">
                <el-option label="公开" value="PUBLIC" />
                <el-option label="仅关注者" value="FOLLOWERS" />
                <el-option label="私密" value="PRIVATE" />
              </el-select>
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>允许评论</h4>
              <p>设置谁可以评论你的作品</p>
            </div>
            <div class="setting-action">
              <el-select v-model="privacySettings.commentPermission" placeholder="选择权限">
                <el-option label="所有人" value="ALL" />
                <el-option label="仅关注者" value="FOLLOWERS" />
                <el-option label="不允许" value="NONE" />
              </el-select>
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>允许私信</h4>
              <p>设置谁可以给你发送私信</p>
            </div>
            <div class="setting-action">
              <el-select v-model="privacySettings.messagePermission" placeholder="选择权限">
                <el-option label="所有人" value="ALL" />
                <el-option label="仅关注者" value="FOLLOWERS" />
                <el-option label="不允许" value="NONE" />
              </el-select>
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>允许关注</h4>
              <p>设置是否允许他人关注你</p>
            </div>
            <div class="setting-action">
              <el-switch v-model="privacySettings.allowFollow" />
            </div>
          </div>
          
          <div class="setting-item">
            <div class="setting-info">
              <h4>显示活跃状态</h4>
              <p>在在线状态中显示你的活跃情况</p>
            </div>
            <div class="setting-action">
              <el-switch v-model="privacySettings.showActiveStatus" />
            </div>
          </div>
        </div>
      </div>
      
      <!-- 保存设置按钮 -->
      <div class="save-settings">
        <el-button type="primary" size="large" @click="saveSettings" :loading="loading">保存设置</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getUserSettings, updateUserSettings } from '~/api/userApi';

definePageMeta({
  ssr: false
})

// 加载状态
const loading = ref(false);

// 通知设置
const notificationSettings = ref({
  messageNotifications: true,
  commentNotifications: true,
  likeNotifications: true,
  followNotifications: true,
  eventNotifications: true,
  emailNotifications: false
});

// 隐私设置
const privacySettings = ref({
  profileVisibility: 'PUBLIC',
  workVisibility: 'PUBLIC',
  commentPermission: 'ALL',
  messagePermission: 'ALL',
  allowFollow: true,
  showActiveStatus: true
});

// 保存设置
const saveSettings = async () => {
  loading.value = true;
  try {
    await updateUserSettings({
      notifications: notificationSettings.value,
      privacy: privacySettings.value
    });

    ElMessage.success('设置保存成功');
  } catch (error) {
    ElMessage.error('设置保存失败，请重试');
    console.error('保存设置失败:', error);
  } finally {
    loading.value = false;
  }
};

// 加载用户设置
const loadUserSettings = async () => {
  try {
    loading.value = true;
    // 从API获取设置
    const settingsResponse = await getUserSettings();
    
    if (settingsResponse.data?.notifications) {
      notificationSettings.value = settingsResponse.data.notifications;
    }
    
    if (settingsResponse.data?.privacy) {
      privacySettings.value = settingsResponse.data.privacy;
    }
  } catch (error) {
    console.error('加载用户设置失败:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadUserSettings();
});
</script>

<style scoped>
.notifications-settings {
  max-width: 900px;
  margin: 0 auto;
}

.settings-header {
  margin-bottom: 30px;
}

.settings-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 10px 0;
}

.settings-header p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.settings-content {
  background-color: #fff;
  border-radius: 8px;
}

.settings-section {
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.settings-section:last-child {
  border-bottom: none;
}

.section-title {
  margin-bottom: 20px;
}

.section-title h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.3s;
}

.setting-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.setting-info h4 {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0 0 5px 0;
}

.setting-info p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.setting-action {
  flex-shrink: 0;
  min-width: 150px;
}

.save-settings {
  display: flex;
  justify-content: center;
  padding: 30px;
}
</style>