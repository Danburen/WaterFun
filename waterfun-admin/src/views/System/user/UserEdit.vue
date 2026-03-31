<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserDetail, updateUser } from '@/api/userApi'
import type { UserDetailDto, UserUpdateRequest } from '@/types/user'

const props = defineProps<{
    uid: string
}>()

const activateTable = ref('userInfo')
const loading = ref(false)
const userData = ref<UserDetailDto | null>(null)

const userInfoForm = reactive({
    username: '',
    nickname: '',
    avatar_url: '',
    account_status: 'ACTIVE',
    status_change_reason: ''
})

const userProfileForm = reactive({
    bio: '',
    gender: 'UNKNOWN',
    birth_date: '',
    residence: ''
})

const userCounterData = ref({
    level: 1,
    exp: 0,
    follower_cnt: 0,
    following_cnt: 0,
    like_cnt: 0,
    post_cnt: 0
})

const loadUserData = async () => {
    loading.value = true
    try {
        const response = await getUserDetail(props.uid)
        userData.value = response.data
        
        if (response.data.user) {
            Object.assign(userInfoForm, {
                username: response.data.user.username,
                nickname: response.data.user.nickname || '',
                avatar_url: response.data.user.avatar_url || '',
                account_status: response.data.user.account_status,
                status_change_reason: response.data.user.status_change_reason || ''
            })
        }
        
        if (response.data.profile) {
            Object.assign(userProfileForm, {
                bio: response.data.profile.bio || '',
                gender: response.data.profile.gender || 'UNKNOWN',
                birth_date: response.data.profile.birth_date || '',
                residence: response.data.profile.residence || ''
            })
        }
        
        if (response.data.counter) {
            userCounterData.value = response.data.counter
        }
    } catch (error) {
        ElMessage.error('加载用户数据失败')
    } finally {
        loading.value = false
    }
}

const handleSaveUserInfo = async () => {
    loading.value = true
    try {
        const request: UserUpdateRequest = {
            username: userInfoForm.username,
            nickname: userInfoForm.nickname,
            avatar_url: userInfoForm.avatar_url,
            account_status: userInfoForm.account_status as any,
            status_change_reason: userInfoForm.status_change_reason
        }
        
        await updateUser(props.uid, request)
        ElMessage.success('基本信息更新成功')
        loadUserData()
    } catch (error) {
        ElMessage.error('更新失败')
    } finally {
        loading.value = false
    }
}

const handleSaveUserProfile = async () => {
    loading.value = true
    try {
        const request = {
            bio: userProfileForm.bio,
            gender: userProfileForm.gender,
            birth_date: userProfileForm.birth_date,
            residence: userProfileForm.residence
        }
        
        await updateUser(props.uid, request)
        ElMessage.success('档案信息更新成功')
        loadUserData()
    } catch (error) {
        ElMessage.error('更新失败')
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    loadUserData()
})
</script>

<template>
    <el-card v-loading="loading">
        <el-tabs v-model="activateTable">
            <el-tab-pane label="基本信息" name="userInfo">
                <el-form :model="userInfoForm" label-width="100px" style="max-width: 600px">
                    <el-form-item label="用户名">
                        <el-input v-model="userInfoForm.username" disabled />
                    </el-form-item>
                    <el-form-item label="昵称">
                        <el-input v-model="userInfoForm.nickname" placeholder="请输入昵称" />
                    </el-form-item>
                    <el-form-item label="头像">
                        <el-input v-model="userInfoForm.avatar_url" placeholder="请输入头像URL" />
                    </el-form-item>
                    <el-form-item label="账号状态">
                        <el-select v-model="userInfoForm.account_status" placeholder="选择状态">
                            <el-option label="正常" value="ACTIVE" />
                            <el-option label="暂停" value="SUSPENDED" />
                            <el-option label="停用" value="DEACTIVATE" />
                            <el-option label="删除" value="DELETED" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="状态原因">
                        <el-input v-model="userInfoForm.status_change_reason" type="textarea" placeholder="状态变更原因" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="handleSaveUserInfo">保存基本信息</el-button>
                    </el-form-item>
                </el-form>
            </el-tab-pane>
            
            <el-tab-pane label="档案资料" name="userProfile">
                <el-form :model="userProfileForm" label-width="100px" style="max-width: 600px">
                    <el-form-item label="个人简介">
                        <el-input v-model="userProfileForm.bio" type="textarea" :rows="4" placeholder="请输入个人简介" />
                    </el-form-item>
                    <el-form-item label="性别">
                        <el-radio-group v-model="userProfileForm.gender">
                            <el-radio label="MALE">男</el-radio>
                            <el-radio label="FEMALE">女</el-radio>
                            <el-radio label="OTHER">其他</el-radio>
                            <el-radio label="UNKNOWN">保密</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="生日">
                        <el-date-picker v-model="userProfileForm.birth_date" type="date" placeholder="选择生日" />
                    </el-form-item>
                    <el-form-item label="居住地">
                        <el-input v-model="userProfileForm.residence" placeholder="请输入居住地" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="handleSaveUserProfile">保存档案信息</el-button>
                    </el-form-item>
                </el-form>
            </el-tab-pane>
            
            <el-tab-pane label="统计数据" name="userCounter">
                <el-descriptions :column="2" border style="max-width: 600px">
                    <el-descriptions-item label="等级">{{ userCounterData.level }}</el-descriptions-item>
                    <el-descriptions-item label="经验值">{{ userCounterData.exp }}</el-descriptions-item>
                    <el-descriptions-item label="粉丝数">{{ userCounterData.follower_cnt }}</el-descriptions-item>
                    <el-descriptions-item label="关注数">{{ userCounterData.following_cnt }}</el-descriptions-item>
                    <el-descriptions-item label="获赞数">{{ userCounterData.like_cnt }}</el-descriptions-item>
                    <el-descriptions-item label="帖子数">{{ userCounterData.post_cnt }}</el-descriptions-item>
                </el-descriptions>
            </el-tab-pane>
            
            <el-tab-pane label="第三方绑定" name="userDatum">
                <div style="text-align: center; padding: 40px; color: #909399">
                    <el-icon size="48"><Connection /></el-icon>
                    <p>第三方绑定功能开发中...</p>
                </div>
            </el-tab-pane>
        </el-tabs>
    </el-card>
</template>

<style scoped>
.el-form-item {
    margin-bottom: 20px;
}

.el-descriptions {
    margin-top: 20px;
}
</style>