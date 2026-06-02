<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  deletePerms,
  deletePermission,
  getPermOptions,
  listPermissions,
  type PermissionResp,
  type PermissionType,
} from "~/api/permission";
import { ElMessage } from "element-plus";
import type { PageOptions } from "~/types";
import PermEditDialog from "./components/PermEditDialog.vue";

const router = useRouter();

const permissionList = ref<PermissionResp[]>([]);
const permOptions = ref<OptionResItem[]>([]);

const searchForm = ref<{
  name: string;
  code: string;
  type: PermissionType | "";
  resource: string;
  parentId: number | null;
}>({
  name: "",
  code: "",
  type: "",
  resource: "",
  parentId: null,
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const loading = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentPermId = ref<number | null>(null);
const selectedPermIds = ref<number[]>([]);

const permTypeOptions: { label: string; value: PermissionType }[] = [
  { label: "菜单", value: "MENU" },
  { label: "按钮", value: "BUTTON" },
  { label: "接口", value: "API" },
  { label: "数据", value: "DATA" },
  { label: "其他", value: "OTHER" },
];

const selectable = (row: PermissionResp) => row.isSystem === false;

const fetchPermOptions = async () => {
  try {
    const res = await getPermOptions();
    permOptions.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取权限信息失败');
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listPermissions({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      name: searchForm.value.name || undefined,
      code: searchForm.value.code || undefined,
      type: searchForm.value.type || undefined,
      resource: searchForm.value.resource || undefined,
      parentId: searchForm.value.parentId ?? undefined,
    });

    permissionList.value = res.data.content || [];
    pageOpts.value.total = res.data.page.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取数据失败');
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentPermId.value = null;
  dialogVisible.value = true;
};

const handleEdit = (row: PermissionResp) => {
  dialogMode.value = "edit";
  currentPermId.value = row.id;
  dialogVisible.value = true;
};

const handleDelete = async (row: PermissionResp) => {
  try {
    await ElMessageBox.confirm('确定删除该权限吗？', '删除', {
      type: "warning",
    });
    await deletePermission(row.id);
    ElMessage.success('权限删除成功');
    await fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('删除权限失败');
    }
  }
};

const handleSelectionChange = (rows: PermissionResp[]) => {
  selectedPermIds.value = rows.map(item => item.id);
};

const handleBatchDelete = async () => {
  if (selectedPermIds.value.length === 0) {
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedPermIds.value.length} 个权限吗？`,
      '删除',
      { type: "warning" }
    );

    const res = await deletePerms(selectedPermIds.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success('权限删除成功');
    } else if (result.success === 0) {
      ElMessage.error('删除权限失败');
    } else {
      ElMessage.warning(`${'权限删除成功'} ${result.success}/${result.requested}`);
    }

    selectedPermIds.value = [];
    await fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('删除权限失败');
    }
  }
};

const handleSearch = () => {
  pageOpts.value.currentPage = 1;
  fetchData();
};

const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    name: "",
    code: "",
    type: "",
    resource: "",
    parentId: null,
  };
  fetchData();
};

const handleDialogSuccess = async () => {
  await Promise.all([fetchData(), fetchPermOptions()]);
};

const gotoDetail = (row: PermissionResp | number) => {
  const id = typeof row === "number" ? row : row.id;
  router.push({ name: "permissionDetail", params: { id } });
};

onMounted(async () => {
  await Promise.all([fetchData(), fetchPermOptions()]);
});
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <el-form
        inline
        class="search-form"
        :model="searchForm"
      >
        <el-form-item label="权限名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入权限名称"
          />
        </el-form-item>

        <el-form-item label="权限编码">
          <el-input
            v-model="searchForm.code"
            placeholder="请输入权限编码"
          />
        </el-form-item>

        <el-form-item label="权限类型">
          <el-select
            v-model="searchForm.type"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in permTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="资源标识">
          <el-input
            v-model="searchForm.resource"
            placeholder="请输入资源标识"
          />
        </el-form-item>

        <el-form-item label="父级权限ID">
          <el-select
            v-model="searchForm.parentId"
            clearable
            placeholder="请选择父级权限"
            style="width: 180px"
          >
            <el-option
              v-for="item in permOptions"
              :key="item.id"
              :label="`${item.id} (${item.name} 【${item.code}】)`"
              :value="item.id"
              :disabled="item.disabled || false"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </SearchContainer>

    <TableContainer
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      title="权限管理"
      show-add-btn
      :show-remove-btn="true"
      :disable-delete="selectedPermIds.length === 0"
      :total="pageOpts.total"
      @add="handleAdd"
      @remove="handleBatchDelete"
      @change="fetchData"
    >
      <el-table
        v-loading="loading"
        :data="permissionList"
        border
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          :selectable="selectable"
          width="55"
        />
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />

        <el-table-column
          prop="name"
          label="权限名称"
          min-width="140"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              @click="gotoDetail(row)"
            >
              {{ row.name }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column
          prop="code"
          label="权限编码"
          min-width="140"
        />

        <el-table-column
          prop="type"
          label="权限类型"
          width="110"
        >
          <template #default="{ row }">
            {{ ({ menu: '菜单', button: '按钮', api: '接口', data: '数据', other: '其他' })[row.type?.toLowerCase?.() || 'other'] }}
          </template>
        </el-table-column>

        <el-table-column
          prop="resource"
          label="资源标识"
          min-width="180"
          show-overflow-tooltip
        />

        <el-table-column
          prop="parentId"
          label="父级权限ID"
          width="120"
        >
          <template #default="{ row }">
            <el-link
              v-if="row.parentId != null"
              type="primary"
              :underline="false"
              @click="gotoDetail(row.parentId)"
            >
              {{ row.parentId }}
            </el-link>
            <span v-else>无</span>
          </template>
        </el-table-column>

        <el-table-column
          prop="orderWeight"
          label="排序权重"
          width="90"
        />

        <el-table-column
          prop="isSystem"
          label="系统权限"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.isSystem ? 'warning' : 'info'"
            >
              {{ row.isSystem ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="description"
          label="权限描述"
          min-width="180"
          show-overflow-tooltip
        />

        <el-table-column
          prop="createdAt"
          label="创建时间"
          min-width="170"
        >
          <template #default="{ row }">
            <span>{{ formatISOData(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <PermEditDialog
        v-model="dialogVisible"
        :mode="dialogMode"
        :permission-id="currentPermId"
        :perm-options="permOptions"
        :disabled-parent-ids="currentPermId ? [currentPermId] : []"
        @success="handleDialogSuccess"
      />
    </TableContainer>
  </div>
</template>

<style scoped>
.search-form {
  margin-bottom: 0;
}

.list-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>


