<script setup lang="ts">
import TableContainer from "~/components/TableContainer.vue";
import {PageOptions} from "~/types";
import {
  deleteRole,
  getRoleAllIds,
  listRoles,
  RoleResp,
} from "~/api/role";
import {useI18n} from "vue-i18n";
import {formatISOData} from "@waterfun/web-core/src/timer";
import {OptionResItem} from "@waterfun/web-core/src/types";
import {useRouter} from "vue-router";
import RoleEditDialog from "./components/RoleEditDialog.vue";
import {ElMessageBox} from "element-plus";

const {t} = useI18n()
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
        ElMessage.error(t("role.error.fetch"));
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
    ElMessage.error(t("error.fetch"));
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
    await ElMessageBox.confirm(t("role.confirm.delete"), t("operation.delete"), {
      type: "warning",
    });
    await deleteRole(row.id);
    ElMessage.success(t("role.success.delete"));
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("role.error.delete"));
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
  return option?.name || t("none.title");
};


onMounted(() => {
  fetchData()
  fetchRoleIds()
});
</script>

<template>
  <TableContainer
      title="role.title"
      showAddBtn
      :show-remove-btn="false"
      @add="handleAdd"
      @change="fetchData"
      :total="pageOpts.total"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
  >
    <el-form inline class="search-form" :model="searchForm">
      <el-form-item :label="t('role.name')">
        <el-input :placeholder="t('role.input.name')" v-model="searchForm.name"/>
      </el-form-item>
      <el-form-item :label="t('role.code')">
        <el-input :placeholder="t('role.input.code')" v-model="searchForm.code"/>
      </el-form-item>
      <el-form-item :label="t('role.parentId')">
        <el-select v-model="searchForm.parentId" :placeholder="t('role.input.parentId')" style="width: 150px">
          <el-option v-for="item in roleOptions"
                     :key="item.id"
                     :label="`${item.id} (${item.name} 【${item.code}】)`"
                     :value="item.id"
                     :disabled="item.disabled"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">{{ t('query.title') }}</el-button>
        <el-button @click="handleReset">{{ t('reset.title') }}</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="roleList" border fit highlight-current-row style="width: 100%">
      <el-table-column type="selection" :selectable="selectable" width="55" />
      <el-table-column prop="id" label="ID" width="80">
      </el-table-column>
      <el-table-column prop="name" :label="t('role.name')">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="gotoDetail(row.id)">
            {{ row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="code" :label="t('role.code')"/>
      <el-table-column prop="parentId" :label="t('role.parentId')">
        <template #default="{ row }">
          <el-link v-if="row.parentId != null" type="primary" :underline="false" @click="gotoDetail(row.parentId)">
            {{ row.parentId }} ({{ getParentRoleName(row.parentId) }})
          </el-link>
          <span v-else>{{ t('none.title') }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="orderWeight" sortable :label="t('order.title')" width="80px"></el-table-column>
      <el-table-column prop="description" :label="t('role.description')"/>
      <el-table-column prop="createdAt" sortable column-key="date" :label="t('create.time')">
        <template #default="scope">
          <span>{{ formatISOData(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('operation.title')" width="150px" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">{{ t('operation.edit') }}</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row)">{{ t('operation.delete') }}</el-button>
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
</template>

<style scoped>
</style>