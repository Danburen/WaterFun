<script setup lang="ts">
import {type FormInstance, type FormRules} from "element-plus";
import VerifyingCodeButton from "~/components/auth/VerifyingCodeButton.vue";
import {useAuth} from "~/composables/useAuth";
import { generateFingerprint } from "@waterfun/web-core/src/fingerprint";
import type { RegisterRequest } from "~/api/authApi";
import { useI18n } from "vue-i18n";
import {REGEX} from "@waterfun/web-core/src/regex";
import {translate} from "~/utils/translator";

const createFieldValidator = (options: { regex?: RegExp, emptyErrorKey: string, invalidErrorKey?: string, allowEmpty?: boolean }) => {
  return (_: any, value: any, callback: any) => {
    if (!value && !options.allowEmpty) { callback(new Error(translate('auth.validate.' + options.emptyErrorKey))); return; }
    if (options.allowEmpty && !value) { callback(); return; }
    if (options.regex && options.invalidErrorKey && !options.regex.test(value)) { callback(new Error(translate('auth.validate.' + options.invalidErrorKey))); return; }
    callback();
  }
}

const validateUsername = (allowEmpty?: boolean) => {
  return (rule: any, value: any, callback: any) => {
    if (!value && !allowEmpty) { callback(new Error(translate('auth.validate.usernameEmpty'))); return; }
    if (allowEmpty && !value) { callback(); return; }
    if (value.length < 4 || value.length > 20) { callback(new Error(translate('auth.validate.usernameOutOfLength'))); return; }
    if (!REGEX.username.test(value)) { callback(new Error(translate('auth.validate.invalidUsername'))); return; }
    callback();
  }
}

const validatePhoneNumber = (allowEmpty?: boolean) => createFieldValidator({ regex: REGEX.phone, emptyErrorKey: 'phoneEmpty', invalidErrorKey: 'invalidPhone', allowEmpty })

const validateEmail = (allowEmpty?: boolean) => createFieldValidator({ regex: REGEX.email, emptyErrorKey: 'emailEmpty', invalidErrorKey: 'invalidEmail', allowEmpty })

const validatePassword = (allowEmpty?: boolean) => {
  return (rule: any, value: any, callback: any) => {
    if (!value && !allowEmpty) { callback(new Error(translate('auth.validate.passwordEmpty'))); return; }
    if (allowEmpty && !value) { callback(); return; }
    if (value.length < 8) { callback(new Error(translate('auth.validate.passwordTooShort'))); return; }
    if (!/[a-z]/.test(value) || !/[A-Z]/.test(value) || !/[0-9]/.test(value)) { callback(new Error(translate('auth.validate.passwordInvalid'))); return; }
    callback();
  }
}

const validateVerifyCode = (allowEmpty: boolean) => {
  return (rule: any, value: any, callback: any) => {
    if (!value && !allowEmpty) callback(new Error(translate('auth.validate.verifyCodeEmpty')));
    else callback();
  }
}

const registerFormRef = ref<FormInstance>()
const i18n = useI18n();
const buttonLoad = ref(false);
const licenceCheck = ref(false);
const expandShow = ref(true);

const router = useRouter();
const route = useRoute();

const { tryRegister } = useAuth();

definePageMeta({
  ssr: false,
})

const registerForm = reactive({
  phone: '',
  username: '',
  smsCode: '',
  email: '',
  password: '',
})

const regRules = reactive<FormRules<typeof registerForm>>({
  phone: [{validator:validatePhoneNumber(false), trigger:"blur"}],
  smsCode:[{validator:validateVerifyCode(false), trigger:"blur"}],
  username:[{validator:validateUsername(false), trigger:"blur"}],
  email:[{validator:validateEmail(true), trigger:"blur"}],
  password:[{validator:validatePassword(true),trigger:"blur"}],
})

const handleRegisterClick = async () => {
  buttonLoad.value = true;
  const dfp =  await generateFingerprint();
  tryRegister({ 
    ...registerForm,
      verify: {
        target: registerForm.phone,
        code: registerForm.smsCode,
        channel: 'sms',
        scene: 'register',
        deviceFp: dfp
      },
    } as RegisterRequest).finally(()=> {
    buttonLoad.value = false;
  })
}

watch(() => route.query.userAgreementConfirm, (val) => {
  if (val === 'true') {
    licenceCheck.value = true;
    router.replace({ query: {} })
  }
}, { immediate: true })
</script>

<template>
  <auth-box title="创建账号" subtitle="注册一个 WaterFun 账号开始探索">
    <el-form
      :model="registerForm"
      ref="registerFormRef"
      class="register-form"
      :rules="regRules"
      label-position="top"
      size="large"
    >
      <el-form-item :label="$t('auth.username')" prop="username">
        <el-input
          :placeholder="$t('auth.placeholder.username')"
          v-model="registerForm.username"
        />
      </el-form-item>
      <el-form-item :label="$t('auth.phone')" prop="phone">
        <el-input :placeholder="$t('auth.placeholder.phone')" v-model="registerForm.phone" />
      </el-form-item>
      <el-form-item :label="$t('auth.verifyCode')" prop="smsCode">
        <el-input
          v-model="registerForm.smsCode"
          :placeholder="$t('auth.placeholder.verifyCode')"
        >
          <template #append>
            <VerifyingCodeButton 
              :username="registerForm.phone" 
              :getType="registerForm.phone ? 'sms' : 'email'" 
              :scene="'register'" 
            />
          </template>
        </el-input>
      </el-form-item>
      <el-form-item class="supplementary-header">
        <div class="supplementary-toggle" @click="expandShow = !expandShow">
          <el-text tag="b" size="small">{{ $t('auth.supplementaryInfo') }}</el-text>
          <el-tag size="small" type="info" effect="plain">选填</el-tag>
          <el-text type="primary" size="small">{{ expandShow ? $t('auth.btn.collapse') : $t('auth.btn.expand') }}</el-text>
        </div>
      </el-form-item>
      <el-collapse-transition>
        <div v-show="expandShow" class="supplementary-body">
          <el-form-item :label="$t('auth.password')" prop="password">
            <el-input
              type="password"
              v-model="registerForm.password"
              :placeholder="$t('auth.placeholder.password')"
              show-password
            />
          </el-form-item>
          <el-form-item :label="$t('auth.email')" prop="email">
            <el-input
              v-model="registerForm.email"
              :placeholder="$t('auth.placeholder.email')"
            />
          </el-form-item>
        </div>
      </el-collapse-transition>
      <el-form-item>
        <el-button type="primary" class="login-btn" @click="handleRegisterClick" :loading="buttonLoad" :disabled="!licenceCheck">
          {{ $t('auth.btn.register') }}
        </el-button>
      </el-form-item>
      <div class="form-footer">
        <div class="agreement-area">
          <el-checkbox size="small" v-model="licenceCheck">
            {{ $t('confirm.confirmReadLicences') }}
          </el-checkbox>
          <div class="legal-links">
            <el-button size="small" link @click="router.push({ path: '/EulaView', query: { from: route.path } })">{{ $t('confirm.userAgreement') }}</el-button>
            <span class="separator">|</span>
            <el-button size="small" link @click="router.push('/legal/terms')">{{ $t('auth.terms') }}</el-button>
            <span class="separator">|</span>
            <el-button size="small" link @click="router.push('/legal/privacy')">{{ $t('auth.privacy') }}</el-button>
          </div>
        </div>
        <el-button size="small" link class="to-login" @click.prevent="router.push('/login')">
          {{ $t('auth.toLogin') }}
        </el-button>
      </div>
    </el-form>
  </auth-box>
</template>

<style scoped>
.register-form {
  width: 100%;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.register-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.supplementary-header {
  margin-bottom: 0 !important;
}

.supplementary-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 0;
  width: 100%;
  user-select: none;
}

.supplementary-toggle:hover {
  opacity: 0.8;
}

.supplementary-toggle :last-child {
  margin-left: auto;
}

.supplementary-body {
  padding-top: 4px;
}

.login-btn {
  width: 100%;
  padding: 22px 0;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  border: none;
  border-radius: 8px;
  margin-top: 4px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.form-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: -4px;
}

.form-footer :deep(.el-checkbox__label) {
  font-size: 12px;
}

.form-footer .to-login {
  margin-left: auto;
  white-space: nowrap;
}

.agreement-area {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.legal-links {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: 24px;
}

.legal-links :deep(.el-button) {
  font-size: 12px;
  padding: 0;
  min-height: auto;
  height: auto;
  color: #3b82f6;
}

.separator {
  color: #cbd5e1;
  font-size: 12px;
}
</style>
