<script setup lang="ts">
import request from "../utils/axiosRequest";
import {onBeforeMount, reactive, ref, watch} from "vue";
import {ElMessage, type FormInstance, type FormRules} from "element-plus";
import {deBounce, throttle} from "@waterfun/web-core/src/triggerControl"
import VerifyingCodeButton from "~/components/auth/VerifyingCodeButton.vue";
import AuthBox from "~/components/auth/AuthBox.vue";
import {validateAuthname, validatePassword, validateVerifyCode} from "~/utils/validator";
import {getCaptcha} from "~/api/authApi";
import {useAuth} from "~/composables/useAuth";
import type { LoginRequest } from "~/api/authApi";
import {useI18n} from "vue-i18n";
import {useRouter} from "vue-router";
import { generateFingerprint } from "@waterfun/web-core/src/fingerprint";
import { convertArrayBufferToBase64 } from "@waterfun/web-core/src/dataMapper";

type LoginTabType = 'password'|'fast-auth';

const i18n = useI18n();
const router = useRouter();

const { tryLogin } = useAuth()

const passAuthForm = ref<FormInstance>();
const fastAuthForm = ref<FormInstance>();
const loginTab = ref<LoginTabType>('password');
const captchaLoading = ref(false);

const buttonLoad = ref(false)

const captchaImage = useState('captchaImage', () => '');


const passLoginForm = reactive({
  username:'',
  password:'',
  captcha:'',
  loginType: 'password',
})

const fastLoginForm = reactive({
  username:'',
  verifyCode:'',
  loginType: 'sms',
})

const passAuthRules = reactive<FormRules<typeof passLoginForm>>({
  username:[{validator: validateAuthname('password'),trigger: "blur" }],
  password:[{validator: validatePassword(false),trigger: "blur"}],
  captcha:[{validator: validateVerifyCode(false), trigger: "blur"}],
})

const fastAuthRules = reactive<FormRules<typeof fastLoginForm>>({
  username: [{validator: validateAuthname('sms'),trigger: "blur" }],
  verifyCode: [{validator: validateVerifyCode(false), trigger: "blur"}],
})

const resetForm = () => {
  passAuthForm.value?.resetFields();
  fastAuthForm.value?.resetFields();
}

const buildRequest= async (): Promise<LoginRequest> => {
  const dfp =  await generateFingerprint();
  if (loginTab.value === "password") {
    const { loginType, ...passFormData } = passLoginForm;
    return {
      ...passFormData,
      deviceFp: dfp
    } as LoginRequest;
  } else {
    const isEmailLogin = fastLoginForm.username.includes('@');
    return {
      target: fastLoginForm.username,
      code: fastLoginForm.verifyCode,
      channel: isEmailLogin ? 'email' : 'sms',
      scene: 'login',
      deviceFp: dfp
    } as LoginRequest;
  }
}

const submitForm = (form:FormInstance | undefined) => {
  if(!form) return;
  form.validate((valid)=>{
    buttonLoad.value = true;
    if(valid){
      buildRequest().then(async (loginRes)=>{
        return tryLogin(loginRes, getLoginType()).then(()=>{
          const redirect = router.currentRoute.value.query.redirect as string | undefined;
          router.push(redirect || "/");
        }).catch(err=>{
          if (loginTab.value === 'password') refreshCaptcha()
        }).finally(()=>{
          buttonLoad.value = false;
        })
      }).catch(()=>{
        buttonLoad.value = false;
      })
    }else{
      buttonLoad.value = false;
    }
  })
}

watch(()=>fastLoginForm.username,deBounce((value:string)=>{
  if(loginTab.value === 'fast-auth'){
      if(value.includes('@')){
        fastLoginForm.loginType = 'email';
        fastAuthRules.username = [{validator: validateAuthname('email'),trigger: "blur" }];
      }else{
        fastLoginForm.loginType = 'sms';
        fastAuthRules.username = [{validator: validateAuthname('sms'),trigger: "blur" }];
      }
    }
},300))

const refreshCaptcha = throttle(()=>{
  if(! captchaLoading.value){ 
    captchaLoading.value = true;
    getCaptcha().then(res=>{
      const base64 = convertArrayBufferToBase64(res);
      captchaImage.value = `data:image/jpeg;base64,${base64}`;
    }).catch(err=>{
      ElMessage.error(i18n.t('message.error.apiError'));
    }).finally(()=>{
      captchaLoading.value = false;
    })
  }
},1000,()=>{
  ElMessage.error(i18n.t('message.throttled.clickTooFast'));
})

onBeforeMount(() => {
  refreshCaptcha();
});

function getLoginType(){
  if(loginTab.value === 'password'){
    return 'password'
  }else{
    if(fastLoginForm.username.includes('@')){
      return 'email'
    }else{
      return 'sms'
    }
  }
}
</script>

<template>
    <auth-box>
      <div class="tab-switcher">
        <button
          class="tab-btn"
          :class="{ active: loginTab === 'password' }"
          @click="loginTab = 'password'; resetForm()"
        >
          {{ $t('auth.tabs.password') }}
        </button>
        <button
          class="tab-btn"
          :class="{ active: loginTab === 'fast-auth' }"
          @click="loginTab = 'fast-auth'; resetForm()"
        >
          {{ $t('auth.tabs.emailPhone') }}
        </button>
      </div>

      <el-form
        v-show="loginTab === 'password'"
        ref="passAuthForm"
        :model="passLoginForm"
        :rules="passAuthRules"
        class="auth-form"
        label-position="top"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="passLoginForm.username"
            :placeholder="$t('auth.placeholder.username')"
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="passLoginForm.password"
            type="password"
            :placeholder="$t('auth.placeholder.password')"
            class="login-input"
            show-password
          />
        </el-form-item>
        <el-form-item prop="captcha">
          <div class="captcha-container">
            <el-input v-model="passLoginForm.captcha" :placeholder="$t('auth.placeholder.verifyCode')" />
            <div class="captcha-image-wrap" @click="refreshCaptcha">
              <el-image
                v-loading="captchaLoading"
                v-if="captchaImage"
                :src="captchaImage"
                class="captcha-image"
                alt="Captcha"
              />
              <el-skeleton v-else :rows="1" animated style="width: 100px; height: 36px" />
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="buttonLoad"
            @click="submitForm(passAuthForm)"
          >
            {{ $t('auth.btn.login') }}
          </el-button>
        </el-form-item>
        <div class="form-footer">
          <el-button size="small" link>{{ $t('auth.forgetPassword') }}</el-button>
          <el-button size="small" link class="to-register" @click.prevent="router.push('/register')">
            {{ $t('auth.toRegister') }}
          </el-button>
        </div>
      </el-form>

      <el-form
        v-show="loginTab === 'fast-auth'"
        ref="fastAuthForm"
        :model="fastLoginForm"
        :rules="fastAuthRules"
        class="auth-form"
        label-position="top"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="fastLoginForm.username"
            :placeholder="$t('auth.placeholder.emailPhone')"
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="verifyCode">
          <el-input
            v-model="fastLoginForm.verifyCode"
            :placeholder="$t('auth.placeholder.verifyCode')"
            class="login-input"
          >
            <template #append>
              <VerifyingCodeButton 
                :username="fastLoginForm.username"
                :getType="fastLoginForm.username.includes('@') ? 'email' : 'sms'"
                :scene="'login'" 
              />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            @click="submitForm(fastAuthForm)"
            :loading="buttonLoad"
          >
            {{ $t('auth.btn.login') }} / {{ $t('auth.btn.register') }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="legal-links">
        <el-button size="small" link @click="router.push('/legal/terms')">
          {{ $t('auth.terms') }}
        </el-button>
        <span style="color:#cbd5e1">|</span>
        <el-button size="small" link @click="router.push('/legal/privacy')">
          {{ $t('auth.privacy') }}
        </el-button>
      </div>
    </auth-box>
</template>

<style scoped>
.tab-switcher {
  display: flex;
  gap: 2px;
  margin-bottom: 20px;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 2px;
}

.tab-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: #1e293b;
  background: rgba(255,255,255,0.5);
}

.tab-btn.active {
  background: #fff;
  color: #3b82f6;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.captcha-container {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
}

.captcha-image-wrap {
  flex-shrink: 0;
  cursor: pointer;
  line-height: 0;
}

.captcha-image {
  width: 100px;
  height: 36px;
  border-radius: 6px;
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
  justify-content: space-between;
  margin-top: -4px;
}

.legal-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}
</style>
