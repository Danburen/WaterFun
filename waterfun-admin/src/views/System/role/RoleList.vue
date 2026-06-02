<script setup lang="ts">
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {PageOptions} from "~/types";
import {
  deleteRoles,
  deleteRole,
  getRoleAllIds,
  listRoles,
  RoleResp,
} from "~/api/role";

import {formatISOData} from "@waterfun/web-core/src/timer";
import { ElMessage } from "element-plus";
import {OptionResItem} from "@waterfun/web-core/src/types/api/response";
import {useRouter} from "vue-router";
import RoleEditDialog from "./components/RoleEditDialog.vue";
import {ElMessageBox} from "element-plus";

const router = useRouter();

const roleList = ref<RoleResp[]>([]);
const roleOptions = ref<OptionResItem[]>([]);
const searchForm = ref({
  name: '',
  code: '',
  parentId: null,
})
const pageOpts =ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const loading = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentRoleId = ref<number | null>(null);
const selectedRoleIds = ref<number[]>([]);

const selectable = (row: RoleResp) => row.isSystem == false;

const handleAdd = () => {
  dialogMode.value = "create";
  currentRoleId.value = null;
  dialogVisible.value = true;
}

const fetchRoleIds = () => {
  getRoleAllIds()
      .then(res => {
        roleOptions.value = res.data;
      })
      .catch(e => {
        console.error(e);
        ElMessage.error('获取角色信息失败');
      });
}

const fetchData = () => {
  loading.value = true;
  listRoles(
      pageOpts.value.currentPage - 1,
      pageOpts.value.pageSize,
      searchForm.value.name,
      searchForm.value.code,
      searchForm.value.parentId,
  ).then(res => {
    roleList.value = res.data.content || [];
    pageOpts.value.total = res.data.page.totalElements || 0;
  }).catch(e => {
    console.log(e);
    ElMessage.error('获取数据失败');
  }).finally(() => {
    loading.value = false;
  });
}

const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    name: "",
    code: "",
    parentId: null,
  };
  fetchData();
}
const handleSearch = () => {
  pageOpts.value.currentPage = 1;
  fetchData();
}

const handleEdit = async (row: RoleResp) => {
  dialogMode.value = "edit";
  currentRoleId.value = row.id;
  dialogVisible.value = true;
}

const handleDelete = async (row: RoleResp) => {
  try {
    await ElMessageBox.confirm('确定删除该角色吗？', '删除', {
      type: "warning",
    });
    await deleteRole(row.id);
    ElMessage.success('角色删除成功');
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('删除角色失败');
    }
  }
}

const handleSelectionChange = (rows: RoleResp[]) => {
  selectedRoleIds.value = rows.map(item => item.id);
}

const handleBatchDelete = async () => {
  if (selectedRoleIds.value.length === 0) {
    return;
  }

  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRoleIds.value.length} 个角色吗？`, '删除', {
      type: "warning",
    });

    const res = await deleteRoles(selectedRoleIds.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success('角色删除成功');
    } else if (result.success === 0) {
      ElMessage.error('删除角色失败');
    } else {
      ElMessage.warning(`${'角色删除成功'} ${result.success}/${result.requested}`);
    }

    selectedRoleIds.value = [];
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('删除角色失败');
    }
  }
}

const handleDialogSuccess = () => {
  fetchData();
  fetchRoleIds();
}

const gotoDetail = (id: number) => {
  router.push({name: "roleDetail", params: {id}});
}

const getParentRoleName = (parentId: number | null | undefined) => {
  if (parentId == null) return "";
  const option = roleOptions.value.find((r) => r.id === parentId);
  return option?.name || '无';
};


onMounted(() => {
  fetchData()
  fetchRoleIds()
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
        <el-form-item label="角色名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入角色名称"
          />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input
            v-model="searchForm.code"
            placeholder="请输入角色编码"
          />
        </el-form-item>
        <el-form-item label="父级角色ID">
          <el-select
            v-model="searchForm.parentId"
            placeholder="请选择父级角色"
            style="width: 150px"
          >
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="`${item.id} (${item.name} 【${item.code}】)`"
              :value="item.id"
              :disabled="item.disabled"
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
      title="role.title"
      show-add-btn
      :show-remove-btn="true"
      :disable-delete="selectedRoleIds.length === 0"
      :total="pageOpts.total"
      @add="handleAdd"
      @remove="handleBatchDelete"
      @change="fetchData"
    >
      <el-table
        v-loading="loading"
        :data="roleList"
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
          label="角色名称"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              @click="gotoDetail(row.id)"
            >
              {{ row.name }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="code"
          label="角色编码"
        />
        <el-table-column
          prop="parentId"
          label="父级角色ID"
        >
          <template #default="{ row }">
            <el-link
              v-if="row.parentId != null"
              type="primary"
              :underline="false"
              @click="gotoDetail(row.parentId)"
            >
              {{ row.parentId }} ({{ getParentRoleName(row.parentId) }})
            </el-link>
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="orderWeight"
          sortable
          label="角色排序"
          width="80px"
        />
        <el-table-column
          prop="description"
          label="角色描述"
        />
        <el-table-column
          prop="createdAt"
          sortable
          column-key="date"
          label="创建时间"
        >
          <template #default="scope">
            <span>{{ formatISOData(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="260px"
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
              v-if="! scope.row.isSystem"
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
            <el-popover
              placement="bottom"
              trigger="click"
              :width="130"
              popper-style="min-width: auto; padding: 8px;"
            >
              <template #reference>
                <el-button
                  size="small"
                  type="success"
                >
                  更多
                </el-button>
              </template>
              <div style="display: flex; flex-direction: column; gap: 8px;">
                <el-button
                  size="small"
                  type="primary"
                  plain
                  style="margin: 0; width: 100%;"
                  @click="router.push({ name: 'rolePermissionAssign', params: { id: scope.row.id } })"
                >
                  分配权限
                </el-button>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  style="margin: 0; width: 100%;"
                  @click="router.push({ name: 'roleUserAssign', params: { id: scope.row.id } })"
                >
                  分配用户
                </el-button>
              </div>
            </el-popover>
          </template>
        </el-table-column>
      </el-table>

      <RoleEditDialog
        v-model="dialogVisible"
        :mode="dialogMode"
        :role-id="currentRoleId"
        :role-options="roleOptions"
        :disabled-parent-ids="currentRoleId ? [currentRoleId] : []"
        @success="handleDialogSuccess"
      />
    </TableContainer>
  </div>
</template>

<style scoped>
.list-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
