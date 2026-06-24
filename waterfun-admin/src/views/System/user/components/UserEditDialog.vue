<script setup lang="ts">
import { createUser, getUserDetail, updateUserDatum, updateUserInfo, updateUserProfile, type AccountStatus, type Gender, type UserType } from "~/api/user";
import { ElMessage } from "element-plus";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{ modelValue: boolean; mode?: "create" | "edit"; uid?: string | null }>(), { mode: "edit", uid: null });
const emit = defineEmits<{ "update:modelValue": [value: boolean]; success: [] }>();

const loading = ref(false);
const submitting = ref(false);
const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });
const isCreateMode = computed(() => props.mode === "create");
const isValidUid = computed(() => props.uid != null && props.uid !== '');

// Create form
const phone = ref(''); const username = ref(''); const password = ref(''); const createUserType = ref(0);
// Edit forms
const editUsername = ref(''); const editNickname = ref(''); const editAvatarUrl = ref(''); const editUserType = ref<UserType>('COMMON'); const editAccountStatus = ref<AccountStatus>('ACTIVE');
const editBio = ref(''); const editGender = ref<Gender>('UNKNOWN'); const editBirthDate = ref(''); const editResidence = ref('');
const editEmail = ref(''); const editPhone = ref('');
const datumEmailInit = ref(''); const datumPhoneInit = ref('');

// Validation errors
const phoneErr = ref(''); const usernameErr = ref(''); const passwordErr = ref(''); const userTypeErr = ref('');

const statusOpts = [['ACTIVE', '正常'], ['SUSPENDED', '已停用'], ['DEACTIVATED', '已注销'], ['DELETED', '已删除']] as const;
const genderOpts = [['MALE', '男'], ['FEMALE', '女'], ['OTHER', '其他'], ['UNKNOWN', '未知']] as const;
const createUserTypeOpts = [[0, '普通用户'], [1, '测试用户'], [2, '管理员'], [3, '系统用户'], [4, '超管']] as const;
const userTypeLabel = (t: UserType) => ({ COMMON: '普通用户', ADMIN: '管理员', BOT: '机器人', MODERATOR: '审核员', VIP: 'VIP用户' })[t] ?? '未知';

const resetForms = () => {
  phone.value = ''; username.value = ''; password.value = ''; createUserType.value = 0;
  editUsername.value = ''; editNickname.value = ''; editAvatarUrl.value = ''; editUserType.value = 'COMMON'; editAccountStatus.value = 'ACTIVE';
  editBio.value = ''; editGender.value = 'UNKNOWN'; editBirthDate.value = ''; editResidence.value = '';
  editEmail.value = ''; editPhone.value = '';
  datumEmailInit.value = ''; datumPhoneInit.value = '';
  phoneErr.value = ''; usernameErr.value = ''; passwordErr.value = ''; userTypeErr.value = '';
};

const loadUserData = async () => {
  if (!isValidUid.value) return;
  loading.value = true;
  try {
    const res = await getUserDetail(props.uid!);
    editUsername.value = res.data.info.username || ''; editNickname.value = res.data.info.nickname || ''; editAvatarUrl.value = res.data.info.avatar || '';
    editUserType.value = res.data.info.userType || 'COMMON'; editAccountStatus.value = res.data.info.accountStatus || 'ACTIVE';
    editBio.value = res.data.profile?.bio || ''; editGender.value = (res.data.profile?.gender as Gender) || 'UNKNOWN'; editBirthDate.value = res.data.profile?.birthDate || ''; editResidence.value = res.data.profile?.residence || '';
    editEmail.value = res.data.maskedData?.emailMasked || ''; editPhone.value = res.data.maskedData?.phoneMasked || '';
    datumEmailInit.value = editEmail.value; datumPhoneInit.value = editPhone.value;
  } finally { loading.value = false; }
};

watch(() => visible.value, async (open) => {
  if (!open) return;
  resetForms();
  if (isCreateMode.value) return;
  if (!isValidUid.value) { ElMessage.error('无效的用户UID'); visible.value = false; return; }
  try { await loadUserData(); } catch { ElMessage.error('获取用户详情失败'); visible.value = false; }
});

const handleSave = async () => {
  if (isCreateMode.value) {
    let ok = true; phoneErr.value = ''; usernameErr.value = ''; passwordErr.value = ''; userTypeErr.value = '';
    if (createUserType.value === 0 && !phone.value.trim()) { phoneErr.value = '请输入手机号'; ok = false; }
    if (!username.value.trim()) { usernameErr.value = '请输入用户名'; ok = false; }
    if (createUserType.value !== 1 && createUserType.value !== 3 && !password.value.trim()) { passwordErr.value = '请输入密码'; ok = false; }
    if (createUserType.value == null) { userTypeErr.value = '请选择用户类型'; ok = false; }
    if (!ok) return;

    submitting.value = true;
    try {
      await createUser({ phone: phone.value.trim() || undefined, username: username.value, password: password.value.trim() || undefined, userType: createUserType.value });
      ElMessage.success('用户创建成功'); visible.value = false; emit("success");
    } catch { ElMessage.error('创建用户失败'); }
    finally { submitting.value = false; }
    return;
  }

  if (!isValidUid.value) return;
  submitting.value = true;
  try {
    const uid = props.uid!;
    await Promise.all([
      updateUserInfo(uid, { username: editUsername.value, nickname: editNickname.value || undefined, avatarUrl: editAvatarUrl.value || undefined, accountStatus: editAccountStatus.value }),
      updateUserProfile(uid, { bio: editBio.value || undefined, gender: editGender.value, birthDate: editBirthDate.value || undefined, residence: editResidence.value || undefined }),
      updateUserDatum(uid, { email: editEmail.value && editEmail.value !== datumEmailInit.value ? editEmail.value : undefined, phone: editPhone.value && editPhone.value !== datumPhoneInit.value ? editPhone.value : undefined }),
    ]);
    ElMessage.success('基本信息更新成功'); visible.value = false; emit("success");
  } catch { ElMessage.error('保存基本信息失败'); }
  finally { submitting.value = false; }
};
</script>

<template>
  <BaseDialog v-model="visible" :title="isCreateMode ? '创建用户' : '编辑用户'" width="760px">
    <div v-if="isCreateMode">
      <div class="form-block">
        <div class="form-field">
          <label class="form-label">手机号</label>
          <div class="form-content"><input v-model="phone" class="form-input" placeholder="请输入手机号" /><div v-if="phoneErr" class="form-error">{{ phoneErr }}</div></div>
        </div>
        <div class="form-field">
          <label class="form-label">用户名</label>
          <div class="form-content"><input v-model="username" class="form-input" placeholder="请输入用户名" /><div v-if="usernameErr" class="form-error">{{ usernameErr }}</div></div>
        </div>
        <div class="form-field">
          <label class="form-label">密码</label>
          <div class="form-content"><input v-model="password" type="password" class="form-input" placeholder="请输入密码" /><div v-if="passwordErr" class="form-error">{{ passwordErr }}</div></div>
        </div>
        <div class="form-field">
          <label class="form-label">用户类型</label>
          <div class="form-content">
            <select v-model="createUserType" class="form-select">
              <option v-for="[val, label] in createUserTypeOpts" :key="val" :value="val">{{ label }}</option>
            </select>
            <div v-if="userTypeErr" class="form-error">{{ userTypeErr }}</div>
          </div>
        </div>
      </div>
    </div>

    <div v-else>
      <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
      <div v-else>
        <div class="form-block">
          <div class="form-field">
            <label class="form-label">用户名</label>
            <div class="form-content"><input v-model="editUsername" class="form-input" disabled /></div>
          </div>
          <div class="form-field">
            <label class="form-label">用户类型</label>
            <div class="form-content">
              <span class="form-static-text">{{ userTypeLabel(editUserType) }}</span>
            </div>
          </div>
          <div class="form-field">
            <label class="form-label">昵称</label>
            <div class="form-content"><input v-model="editNickname" class="form-input" placeholder="请输入昵称" /></div>
          </div>
          <div class="form-field">
            <label class="form-label">头像</label>
            <div class="form-content"><input v-model="editAvatarUrl" class="form-input" placeholder="请输入头像URL" /></div>
          </div>
          <div class="form-field">
            <label class="form-label">用户状态</label>
            <div class="form-content">
              <select v-model="editAccountStatus" class="form-select">
                <option v-for="[val, label] in statusOpts" :key="val" :value="val">{{ label }}</option>
              </select>
            </div>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid var(--border); margin: 16px 0;" />

        <div class="form-block">
          <div class="form-field">
            <label class="form-label">简介</label>
            <div class="form-content"><textarea v-model="editBio" class="form-textarea" rows="3" placeholder="请输入个人简介"></textarea></div>
          </div>
          <div class="form-field">
            <label class="form-label">性别</label>
            <div class="form-content">
              <select v-model="editGender" class="form-select">
                <option v-for="[val, label] in genderOpts" :key="val" :value="val">{{ label }}</option>
              </select>
            </div>
          </div>
          <div class="form-field">
            <label class="form-label">生日</label>
            <div class="form-content"><input v-model="editBirthDate" type="date" class="form-input" /></div>
          </div>
          <div class="form-field">
            <label class="form-label">居住地</label>
            <div class="form-content"><input v-model="editResidence" class="form-input" placeholder="请输入居住地" /></div>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid var(--border); margin: 16px 0;" />

        <div class="form-block">
          <div class="form-field">
            <label class="form-label">邮箱</label>
            <div class="form-content"><input v-model="editEmail" class="form-input" placeholder="请输入邮箱" /></div>
          </div>
          <div class="form-field">
            <label class="form-label">手机号</label>
            <div class="form-content"><input v-model="editPhone" class="form-input" placeholder="请输入手机号" /></div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <button class="btn" @click="visible = false">取消</button>
      <button class="btn btn-primary" :disabled="submitting" @click="handleSave"><i v-if="submitting" class="fa-solid fa-spinner fa-spin"></i> 保存</button>
    </template>
  </BaseDialog>
</template>
