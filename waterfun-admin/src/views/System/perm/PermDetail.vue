<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { formatISOData } from "@waterfun/web-core/src/timer";
import { useRoute, useRouter } from "vue-router";
import { getPermOptions, getPermUsers, getPermission, type PermissionResp } from "~/api/permission";
import { ElMessage } from "element-plus";
import PermEditDialog from "./components/PermEditDialog.vue";

const route = useRoute();
const router = useRouter();

const permissionId = computed(() => Number(route.params.id));
const loading = ref(false);
const permOptions = ref<OptionResItem[]>([]);
const permissionDetail = ref<PermissionResp | null>(null);

const editDialogVisible = ref(false);
type SimpleOption = { id: number; name: string };
const assignedUserOptions = ref<SimpleOption[]>([]);
const collapseActive = ref(["users"]);

const fetchPermOptions = async () => {
  try {
    const res = await getPermOptions();
    permOptions.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取权限信息失败');
  }
};

const fetchPermDetail = async () => {
  if (Number.isNaN(permissionId.value)) {
    ElMessage.error('无效的权限 ID');
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getPermission(permissionId.value);
    permissionDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取权限信息失败');
  } finally {
    loading.value = false;
  }
};

const openEditDialog = () => {
  if (Number.isNaN(permissionId.value)) return;
  editDialogVisible.value = true;
};

const gotoPermissionDetail = (id: number) => {
  router.push({ name: "permissionDetail", params: { id } });
};

const gotoUserDetail = (uid: string) => {
  router.push({ name: "userDetail", params: { uid: String(uid) } });
};

const fetchPermUsersPreview = async () => {
  if (Number.isNaN(permissionId.value)) return;
  try {
    const all: SimpleOption[] = [];
    let page = 0;
    let totalPages = 1;
    while (page < totalPages) {
      const res = await getPermUsers(permissionId.value, page, 100);
      (res.data.content || []).forEach((item) => {
        all.push({ id: item.userUid, name: `${item.username}${item.nickname ? ` (${item.nickname})` : ""}` });
      });
      totalPages = res.data.page.totalPages || 0;
      page += 1;
    }
    assignedUserOptions.value = all;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取权限用户失败');
  }
};

const handleEditSuccess = async () => {
  await Promise.all([fetchPermDetail(), fetchPermOptions()]);
};

onMounted(async () => {
  await Promise.all([fetchPermOptions(), fetchPermDetail(), fetchPermUsersPreview()]);
});
</script>

<template>
  <div
    v-loading="loading"
    class="perm-detail"
  >
    <CardContainer title="权限详情">
      <template #header-right>
        <el-button
          text
          @click="router.back()"
        >
          返回
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openEditDialog"
        >
          编辑
        </el-button>
      </template>

      <el-descriptions
        v-if="permissionDetail"
        :column="2"
        border
      >
        <el-descriptions-item label="ID">
          {{ permissionDetail.id }}
        </el-descriptions-item>
        <el-descriptions-item label="权限名称">
          {{ permissionDetail.name }}
        </el-descriptions-item>
        <el-descriptions-item label="权限编码">
          {{ permissionDetail.code || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="权限类型">
          {{ ({ menu: '菜单', button: '按钮', api: '接口', data: '数据', other: '其他' })[permissionDetail.type.toLowerCase()] || '其他' }}
        </el-descriptions-item>
        <el-descriptions-item label="资源标识">
          {{ permissionDetail.resource || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="父级权限ID">
          <el-link
            v-if="permissionDetail.parentId != null"
            type="primary"
            :underline="false"
            @click="gotoPermissionDetail(permissionDetail.parentId)"
          >
            {{ permissionDetail.parentId }}
          </el-link>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="排序权重">
          {{ permissionDetail.orderWeight ?? '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="系统权限">
          <el-tag
            size="small"
            :type="permissionDetail.isSystem ? 'warning' : 'info'"
          >
            {{ permissionDetail.isSystem ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="权限描述">
          {{ permissionDetail.description || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatISOData(permissionDetail.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatISOData(permissionDetail.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>
    </CardContainer>

    <CardContainer
      style="margin-top: 12px"
      title="已分配用户"
    >
      <el-collapse v-model="collapseActive">
        <el-collapse-item
          title="已分配用户"
          name="users"
        >
          <el-row :gutter="10">
            <el-col
              v-for="item in assignedUserOptions"
              :key="`user-${item.id}`"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
              class="option-col"
            >
              <el-link
                type="primary"
                :underline="false"
                @click="gotoUserDetail(item.id)"
              >
                {{ item.name }}
              </el-link>
            </el-col>
            <el-col
              v-if="assignedUserOptions.length === 0"
              :span="24"
            >
              暂无数据
            </el-col>
          </el-row>
        </el-collapse-item>
      </el-collapse>
    </CardContainer>

    <PermEditDialog
      v-model="editDialogVisible"
      mode="edit"
      :permission-id="permissionId"
      :perm-options="permOptions"
      :disabled-parent-ids="[permissionId]"
      @success="handleEditSuccess"
    />
  </div>
</template>

<style scoped>
.option-col {
  margin-bottom: 8px;
}
</style>


