<script setup lang="ts">
import TableContainer from "~/components/TableContainer.vue";
import { getUserList, getUserDetail } from "@/api/user";
import { Page, Pagination } from "~/types/api";
import { UserAdminDto, UserDetailDto } from "@/api/user";
import { formatDate } from "@waterfun/web-core/src/timer";
import { useRouter } from "vue-router";
import { ref, onMounted } from "vue";
import { ElDrawer, ElDescriptions, ElDescriptionsItem, ElDivider, ElTag } from "element-plus";

const router = useRouter();

const loading = ref<boolean>(false);
const drawerLoading = ref<boolean>(false);
const formData = ref<UserAdminDto[]>();

const pagination = ref<Pagination>({
    size: 10,
    number: 1,
    totalElements: 0,
    totalPages: 0,
})

const dialogVisible = ref<boolean>(false);
const drawerVisible = ref<boolean>(false);
const currentUserDetail = ref<UserDetailDto | null>(null);

const handleEdit = (row: UserAdminDto) => {
    router.push({ 
        name: 'userEdit', 
        params: { uid: row.uid }
    })
}

const handleDelete = (row: UserAdminDto) => {
    console.log(row);
}

const handeShowInfo = async (row: UserAdminDto) => {
    drawerLoading.value = true;
    try {
        const response = await getUserDetail(row.uid);
        currentUserDetail.value = response.data;
        drawerVisible.value = true;
    } catch (error) {
        console.error('获取用户详情失败:', error);
    } finally {
        drawerLoading.value = false;
    }
}

onMounted(() => {
    loading.value = true;
    getUserList().then(res => {
        console.log(res);
        formData.value = res.data.content;
        pagination.value = res.data.page;
        loading.value = false;
    })
})
</script>

<template>
  <TableContainer 
    title="用户管理"
    :showPagination="true"
    :total="pagination.totalElements"
    :pageSize="pagination.size"
    :currentPage="pagination.number"
    >
    <el-table   width="100%" v-loading="loading" :data = "formData" style="width: 100%">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="uid" label="UID" width="180" />
      <el-table-column prop="username" label="用户名" width="180" />
      <el-table-column prop="nickname" label="昵称" width="180">
          <template #default="{ row }">
            {{ row.nickName ?? '(无昵称)' }}
          </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="240">
        <template #default="scope">
          <span>{{ formatDate(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="accountStatus" label="账号状态" width="120">
        <template #default="scope">
          {{ scope.row.accountStatus === 'ACTIVE' ? '正常' : '禁用' }}
        </template>
      </el-table-column>
      <el-table-column title="操作" width="220px" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          <el-button type="info" size="small" @click="handeShowInfo(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </TableContainer>
  
  <!-- 用户详情抽屉 -->
  <el-drawer
    v-model="drawerVisible"
    title="用户详情"
    size="50%"
    :before-close="() => drawerVisible = false"
  >
    <div v-loading="drawerLoading">
      <el-descriptions :column="1" border v-if="currentUserDetail">
        <el-descriptions-item label="基本信息">
          <el-descriptions :column="2" size="small">
            <el-descriptions-item label="用户ID">{{ currentUserDetail.info.uid }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ currentUserDetail.info.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ currentUserDetail.info.nickname || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag type="success" v-if="currentUserDetail.info.accountStatus === 'ACTIVE'">正常</el-tag>
              <el-tag type="warning" v-else-if="currentUserDetail.info.accountStatus === 'SUSPENDED'">暂停</el-tag>
              <el-tag type="danger" v-else-if="currentUserDetail.info.accountStatus === 'DEACTIVATED'">停用</el-tag>
              <el-tag type="info" v-else>{{ currentUserDetail.info.accountStatus }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(currentUserDetail.info.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDate(currentUserDetail.info.updatedAt) }}</el-descriptions-item>
            <el-descriptions-item label="最后活跃">{{ formatDate(currentUserDetail.info.lastActiveAt) }}</el-descriptions-item>
            <el-descriptions-item label="头像">
              <el-image
                v-if="currentUserDetail.info.avatarUrl"
                :src="currentUserDetail.info.avatarUrl"
                style="width: 60px; height: 60px; border-radius: 50%"
                fit="cover"
              />
              <span v-else>无头像</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-descriptions-item>
        
        <el-descriptions-item label="个人档案">
          <el-descriptions :column="2" size="small">
            <el-descriptions-item label="简介">{{ currentUserDetail.profile.bio || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="性别">
              <span v-if="currentUserDetail.profile.gender === 'MALE'">男</span>
              <span v-else-if="currentUserDetail.profile.gender === 'FEMALE'">女</span>
              <span v-else-if="currentUserDetail.profile.gender === 'OTHER'">其他</span>
              <span v-else>未知</span>
            </el-descriptions-item>
            <el-descriptions-item label="生日">{{ currentUserDetail.profile.birthDate || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="居住地">{{ currentUserDetail.profile.residence || '未设置' }}</el-descriptions-item>
          </el-descriptions>
        </el-descriptions-item>
        
        <el-descriptions-item label="统计数据">
          <el-descriptions :column="3" size="small">
            <el-descriptions-item label="等级">{{ currentUserDetail.counter.level }}</el-descriptions-item>
            <el-descriptions-item label="经验值">{{ currentUserDetail.counter.exp }}</el-descriptions-item>
            <el-descriptions-item label="粉丝数">{{ currentUserDetail.counter.followerCnt }}</el-descriptions-item>
            <el-descriptions-item label="关注数">{{ currentUserDetail.counter.followingCnt }}</el-descriptions-item>
            <el-descriptions-item label="获赞数">{{ currentUserDetail.counter.likeCnt }}</el-descriptions-item>
            <el-descriptions-item label="帖子数">{{ currentUserDetail.counter.postCnt }}</el-descriptions-item>
          </el-descriptions>
        </el-descriptions-item>
        
        <el-descriptions-item label="联系方式">
          <el-descriptions :column="2" size="small">
            <el-descriptions-item label="邮箱">{{ currentUserDetail.maskedData.emailMasked || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱状态">{{ currentUserDetail.maskedData.emailVerified ? '已验证' : '未验证' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ currentUserDetail.maskedData.phoneMasked || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="手机状态">{{ currentUserDetail.maskedData.phoneVerified ? '已验证' : '未验证' }}</el-descriptions-item>
          </el-descriptions>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </el-drawer>
</template>

<style scoped>
</style>