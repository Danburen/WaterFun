<script setup lang="ts">
import { useI18n } from "vue-i18n";
import {
  createUser,
  getUserDetail,
  updateUserDatum,
  updateUserInfo,
  updateUserProfile,
  type AccountStatus,
  type Gender,
} from "~/api/user";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode?: "create" | "edit";
    uid?: number | null;
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

const { t } = useI18n();
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
          callback(new Error(t("user.input.phone")));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  username: [{ required: true, message: t("user.input.username"), trigger: "blur" }],
  password: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        const password = (value || "").trim();
        const isOptionalType = createForm.userType === 1 || createForm.userType === 3;
        if (!isOptionalType && !password) {
          callback(new Error(t("user.input.password")));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  userType: [{ required: true, message: t("user.input.type"), trigger: "change" }],
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

const statusLabel = (status: AccountStatus) => t(`user.statusMap.${status.toLowerCase()}`);
const genderLabel = (gender: Gender) => t(`user.genderMap.${gender.toLowerCase()}`);

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
  if (props.uid == null || Number.isNaN(props.uid)) return;
  loading.value = true;
  try {
    const response = await getUserDetail(props.uid);
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
    if (props.uid == null || Number.isNaN(props.uid)) {
      ElMessage.error(t("user.error.invalidId"));
      visible.value = false;
      return;
    }
    try {
      await loadUserData();
    } catch (e) {
      console.error(e);
      ElMessage.error(t("user.error.fetchDetail"));
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
      ElMessage.success(t("user.success.create"));
      visible.value = false;
      emit("success");
    } catch (e) {
      console.error(e);
      ElMessage.error(t("user.error.create"));
    } finally {
      submitting.value = false;
    }
    return;
  }

  if (props.uid == null || Number.isNaN(props.uid)) return;
  submitting.value = true;
  try {
    await Promise.all([
      updateUserInfo(props.uid, {
        username: userInfoForm.username,
        nickname: userInfoForm.nickname || undefined,
        avatarUrl: userInfoForm.avatarUrl || undefined,
        accountStatus: userInfoForm.accountStatus,
      }),
      updateUserProfile(props.uid, {
        bio: userProfileForm.bio || undefined,
        gender: userProfileForm.gender,
        birthDate: userProfileForm.birthDate || undefined,
        residence: userProfileForm.residence || undefined,
      }),
      updateUserDatum(props.uid, {
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
    ElMessage.success(t("user.success.updateInfo"));
    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error(t("user.error.saveInfo"));
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
    :title="isCreateMode ? t('user.create') : t('user.edit')"
    width="760"
    destroy-on-close
    @closed="handleClosed"
  >
    <div v-if="isCreateMode">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px" class="form-block">
        <el-form-item :label="t('user.phone')" prop="phone">
          <el-input v-model="createForm.phone" :placeholder="t('user.input.phone')" />
        </el-form-item>
        <el-form-item :label="t('user.username')" prop="username">
          <el-input v-model="createForm.username" :placeholder="t('user.input.username')" />
        </el-form-item>
        <el-form-item :label="t('auth.password')" prop="password">
          <el-input
            v-model="createForm.password"
            type="password"
            show-password
            :placeholder="t('user.input.password')"
          />
        </el-form-item>
        <el-form-item :label="t('user.type')" prop="userType">
          <el-select v-model="createForm.userType" style="width: 100%">
            <el-option :label="t('user.typeMap.normal')" :value="0" />
            <el-option :label="t('user.typeMap.tester')" :value="1" />
            <el-option :label="t('user.typeMap.admin')" :value="2" />
            <el-option :label="t('user.typeMap.system')" :value="3" />
            <el-option :label="t('user.typeMap.superAdmin')" :value="4" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div v-else v-loading="loading">
      <el-form :model="userInfoForm" label-width="120px" class="form-block">
        <el-form-item :label="t('user.username')">
          <el-input v-model="userInfoForm.username" disabled />
        </el-form-item>
        <el-form-item :label="t('user.type')">
          <el-select v-model="userInfoForm.userType" disabled style="width: 100%">
            <el-option :label="t('user.typeMap.normal')" :value="0" />
            <el-option :label="t('user.typeMap.tester')" :value="1" />
            <el-option :label="t('user.typeMap.admin')" :value="2" />
            <el-option :label="t('user.typeMap.system')" :value="3" />
            <el-option :label="t('user.typeMap.superAdmin')" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('user.nickname')">
          <el-input v-model="userInfoForm.nickname" :placeholder="t('user.input.nickname')" />
        </el-form-item>
        <el-form-item :label="t('user.avatar')">
          <el-input v-model="userInfoForm.avatarUrl" :placeholder="t('user.input.avatar')" />
        </el-form-item>
        <el-form-item :label="t('user.status')">
          <el-select v-model="userInfoForm.accountStatus" style="width: 100%">
            <el-option :label="statusLabel('ACTIVE')" value="ACTIVE" />
            <el-option :label="statusLabel('SUSPENDED')" value="SUSPENDED" />
            <el-option :label="statusLabel('DEACTIVATED')" value="DEACTIVATED" />
            <el-option :label="statusLabel('DELETED')" value="DELETED" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-divider />

      <el-form :model="userProfileForm" label-width="120px" class="form-block">
        <el-form-item :label="t('user.bio')">
          <el-input v-model="userProfileForm.bio" type="textarea" :rows="3" :placeholder="t('user.input.bio')" />
        </el-form-item>
        <el-form-item :label="t('user.gender')">
          <el-select v-model="userProfileForm.gender" style="width: 100%">
            <el-option :label="genderLabel('MALE')" value="MALE" />
            <el-option :label="genderLabel('FEMALE')" value="FEMALE" />
            <el-option :label="genderLabel('OTHER')" value="OTHER" />
            <el-option :label="genderLabel('UNKNOWN')" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('user.birthDate')">
          <el-date-picker
            v-model="userProfileForm.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('user.input.birthDate')"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('user.residence')">
          <el-input v-model="userProfileForm.residence" :placeholder="t('user.input.residence')" />
        </el-form-item>
      </el-form>

      <el-divider />

      <el-form :model="userDatumForm" label-width="120px" class="form-block">
        <el-form-item :label="t('user.email')">
          <el-input v-model="userDatumForm.email" :placeholder="t('user.input.email')" />
        </el-form-item>
        <el-form-item :label="t('user.phone')">
          <el-input v-model="userDatumForm.phone" :placeholder="t('user.input.phone')" />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('cancel.title') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">{{ t('save.title') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-block {
  padding-right: 12px;
}
</style>

