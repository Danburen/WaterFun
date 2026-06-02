<script setup lang="ts">
import {
  createUser,
  getUserDetail,
  updateUserDatum,
  updateUserInfo,
  updateUserProfile,
  type AccountStatus,
  type Gender,
} from "~/api/user";
import { ElMessage } from "element-plus";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode?: "create" | "edit";
    uid?: string | null;
  }>(),
  {
    mode: "edit",
    uid: null,
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const loading = ref(false);
const submitting = ref(false);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const isCreateMode = computed(() => props.mode === "create");

const createFormRef = ref();
const createForm = reactive<{
  phone: string;
  username: string;
  password: string;
  userType: number;
}>({
  phone: "",
  username: "",
  password: "",
  userType: 0,
});

const createRules = {
  phone: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        const phone = (value || "").trim();
        if (createForm.userType === 0 && !phone) {
          callback(new Error('请输入手机号'));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  username: [{ required: true, message: '请输入用户名', trigger: "blur" }],
  password: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        const password = (value || "").trim();
        const isOptionalType = createForm.userType === 1 || createForm.userType === 3;
        if (!isOptionalType && !password) {
          callback(new Error('请输入密码'));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  userType: [{ required: true, message: '请选择用户类型', trigger: "change" }],
};

const userInfoForm = reactive<{
  username: string;
  nickname: string;
  avatarUrl: string;
  userType: number;
  accountStatus: AccountStatus;
}>({
  username: "",
  nickname: "",
  avatarUrl: "",
  userType: 0,
  accountStatus: "ACTIVE",
});

const userProfileForm = reactive<{
  bio: string;
  gender: Gender;
  birthDate: string;
  residence: string;
}>({
  bio: "",
  gender: "UNKNOWN",
  birthDate: "",
  residence: "",
});

const userDatumForm = reactive({
  email: "",
  phone: "",
});

const datumInitial = reactive({
  email: "",
  phone: "",
});

const statusLabel = (status: AccountStatus) =>
  ({ ACTIVE: '正常', SUSPENDED: '已停用', DEACTIVATED: '已注销', DELETED: '已删除' })[status];
const genderLabel = (gender: Gender) =>
  ({ MALE: '男', FEMALE: '女', OTHER: '其他', UNKNOWN: '未知' })[gender];
const isValidUid = computed(() => props.uid != null && /^\d+$/.test(props.uid));

const resetForms = () => {
  Object.assign(createForm, {
    phone: "",
    username: "",
    password: "",
    userType: 0,
  });

  Object.assign(userInfoForm, {
    username: "",
    nickname: "",
    avatarUrl: "",
    userType: 0,
    accountStatus: "ACTIVE",
  });
  Object.assign(userProfileForm, {
    bio: "",
    gender: "UNKNOWN",
    birthDate: "",
    residence: "",
  });
  Object.assign(userDatumForm, {
    email: "",
    phone: "",
  });
  Object.assign(datumInitial, {
    email: "",
    phone: "",
  });
};

const loadUserData = async () => {
  if (!isValidUid.value) return;
  loading.value = true;
  try {
    const response = await getUserDetail(props.uid as string);
    Object.assign(userInfoForm, {
      username: response.data.info.username || "",
      nickname: response.data.info.nickname || "",
      avatarUrl: response.data.info.avatarUrl || "",
      userType: response.data.info.userType ?? 0,
      accountStatus: response.data.info.accountStatus || "ACTIVE",
    });

    Object.assign(userProfileForm, {
      bio: response.data.profile?.bio || "",
      gender: response.data.profile?.gender || "UNKNOWN",
      birthDate: response.data.profile?.birthDate || "",
      residence: response.data.profile?.residence || "",
    });

    Object.assign(userDatumForm, {
      email: response.data.maskedData?.emailMasked || "",
      phone: response.data.maskedData?.phoneMasked || "",
    });
    Object.assign(datumInitial, {
      email: response.data.maskedData?.emailMasked || "",
      phone: response.data.maskedData?.phoneMasked || "",
    });
  } finally {
    loading.value = false;
  }
};

watch(
  () => visible.value,
  async (open) => {
    if (!open) return;
    if (isCreateMode.value) {
      resetForms();
      return;
    }
    if (!isValidUid.value) {
      ElMessage.error('无效的用户UID');
      visible.value = false;
      return;
    }
    try {
      await loadUserData();
    } catch (e) {
      console.error(e);
      ElMessage.error('获取用户详情失败');
      visible.value = false;
    }
  }
);

const handleSave = async () => {
  if (isCreateMode.value) {
    const valid = await createFormRef.value?.validate?.().catch(() => false);
    if (!valid) return;
    submitting.value = true;
    try {
      await createUser({
        phone: createForm.phone?.trim() || undefined,
        username: createForm.username,
        password: createForm.password?.trim() || undefined,
        userType: createForm.userType,
      });
      ElMessage.success('用户创建成功');
      visible.value = false;
      emit("success");
    } catch (e) {
      console.error(e);
      ElMessage.error('创建用户失败');
    } finally {
      submitting.value = false;
    }
    return;
  }

  if (!isValidUid.value) return;
  submitting.value = true;
  try {
    const uid = props.uid as string;
    await Promise.all([
      updateUserInfo(uid, {
        username: userInfoForm.username,
        nickname: userInfoForm.nickname || undefined,
        avatarUrl: userInfoForm.avatarUrl || undefined,
        accountStatus: userInfoForm.accountStatus,
      }),
      updateUserProfile(uid, {
        bio: userProfileForm.bio || undefined,
        gender: userProfileForm.gender,
        birthDate: userProfileForm.birthDate || undefined,
        residence: userProfileForm.residence || undefined,
      }),
      updateUserDatum(uid, {
        email:
          userDatumForm.email && userDatumForm.email !== datumInitial.email
            ? userDatumForm.email
            : undefined,
        phone:
          userDatumForm.phone && userDatumForm.phone !== datumInitial.phone
            ? userDatumForm.phone
            : undefined,
      }),
    ]);
    ElMessage.success('基本信息更新成功');
    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error('保存基本信息失败');
  } finally {
    submitting.value = false;
  }
};

const handleClosed = () => {
  resetForms();
};
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="isCreateMode ? '创建用户' : '编辑用户'"
    width="760"
    destroy-on-close
    @closed="handleClosed"
  >
    <div v-if="isCreateMode">
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="120px"
        class="form-block"
      >
        <el-form-item
          label="手机号"
          prop="phone"
        >
          <el-input
            v-model="createForm.phone"
            placeholder="请输入手机号"
          />
        </el-form-item>
        <el-form-item
          label="用户名"
          prop="username"
        >
          <el-input
            v-model="createForm.username"
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item
          label="密码"
          prop="password"
        >
          <el-input
            v-model="createForm.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item
          label="用户类型"
          prop="userType"
        >
          <el-select
            v-model="createForm.userType"
            style="width: 100%"
          >
            <el-option
              label="普通用户"
              :value="0"
            />
            <el-option
              label="测试用户"
              :value="1"
            />
            <el-option
              label="管理员"
              :value="2"
            />
            <el-option
              label="系统用户"
              :value="3"
            />
            <el-option
              label="超管"
              :value="4"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div
      v-else
      v-loading="loading"
    >
      <el-form
        :model="userInfoForm"
        label-width="120px"
        class="form-block"
      >
        <el-form-item label="用户名">
          <el-input
            v-model="userInfoForm.username"
            disabled
          />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select
            v-model="userInfoForm.userType"
            disabled
            style="width: 100%"
          >
            <el-option
              label="普通用户"
              :value="0"
            />
            <el-option
              label="测试用户"
              :value="1"
            />
            <el-option
              label="管理员"
              :value="2"
            />
            <el-option
              label="系统用户"
              :value="3"
            />
            <el-option
              label="超管"
              :value="4"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input
            v-model="userInfoForm.nickname"
            placeholder="请输入昵称"
          />
        </el-form-item>
        <el-form-item label="头像">
          <el-input
            v-model="userInfoForm.avatarUrl"
            placeholder="请输入头像URL"
          />
        </el-form-item>
        <el-form-item label="用户状态">
          <el-select
            v-model="userInfoForm.accountStatus"
            style="width: 100%"
          >
            <el-option
              :label="statusLabel('ACTIVE')"
              value="ACTIVE"
            />
            <el-option
              :label="statusLabel('SUSPENDED')"
              value="SUSPENDED"
            />
            <el-option
              :label="statusLabel('DEACTIVATED')"
              value="DEACTIVATED"
            />
            <el-option
              :label="statusLabel('DELETED')"
              value="DELETED"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-divider />

      <el-form
        :model="userProfileForm"
        label-width="120px"
        class="form-block"
      >
        <el-form-item label="简介">
          <el-input
            v-model="userProfileForm.bio"
            type="textarea"
            :rows="3"
            placeholder="请输入个人简介"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select
            v-model="userProfileForm.gender"
            style="width: 100%"
          >
            <el-option
              :label="genderLabel('MALE')"
              value="MALE"
            />
            <el-option
              :label="genderLabel('FEMALE')"
              value="FEMALE"
            />
            <el-option
              :label="genderLabel('OTHER')"
              value="OTHER"
            />
            <el-option
              :label="genderLabel('UNKNOWN')"
              value="UNKNOWN"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker
            v-model="userProfileForm.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择生日"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="居住地">
          <el-input
            v-model="userProfileForm.residence"
            placeholder="请输入居住地"
          />
        </el-form-item>
      </el-form>

      <el-divider />

      <el-form
        :model="userDatumForm"
        label-width="120px"
        class="form-block"
      >
        <el-form-item label="邮箱">
          <el-input
            v-model="userDatumForm.email"
            placeholder="请输入邮箱"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="userDatumForm.phone"
            placeholder="请输入手机号"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="visible = false">
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSave"
      >
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-block {
  padding-right: 12px;
}
</style>


