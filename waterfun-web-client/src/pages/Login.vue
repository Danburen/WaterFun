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

const resetForm = (passAuthForm: FormInstance | undefined,fastAuthForm: FormInstance | undefined) => {
  if(!passAuthForm) return;
  passAuthForm.resetFields();
  if(!fastAuthForm) return;
  fastAuthForm.resetFields();
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
        console.log(loginRes.deviceFp);
        return tryLogin(loginRes, getLoginType()).then(()=>{
          const redirect = router.currentRoute.value.query.redirect as string | undefined;
          router.push(redirect || "/");
        }).catch(err=>{
          console.log(err);
          refreshCaptcha()
          if(err.code === "verify.code.invalid"){
            ElMessage.error(i18n.t('message.error.badRequest.captchaIncorrect'));
          }else{
            ElMessage.error(i18n.t('message.error.unknownError'));
          }
        }).finally(()=>{
          buttonLoad.value = false;
        })
      }).catch(err=>{
        console.log(err)
      })
    }else{
      console.error('error occurred when submit form');
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
  console.log('refreshCaptcha')
  if(! captchaLoading.value){ 
    captchaLoading.value = true;
    getCaptcha().then(res=>{
      const base64 = convertArrayBufferToBase64(res);
      captchaImage.value = `data:image/jpeg;base64,${base64}`;
    }).catch(err=>{
      ElMessage.error(i18n.t('message.error.apiError'));
      console.log(err);
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
      <el-tabs
          type="border-card"
          :stretch="true"
          v-model="loginTab"
          @tab-click="resetForm(passAuthForm,fastAuthForm)"
          class="login-tab"
      >
        <!--密码登录-->
        <el-tab-pane :label="$t('auth.tabs.password')" name="password">
          <el-form
              ref = 'passAuthForm'
              label-width="auto"
              :model="passLoginForm"
              :rules="passAuthRules"
              class="auth-form"
          >
            <el-form-item :label="$t('auth.username')" prop="username">
              <el-input
                  v-model="passLoginForm.username"
                  :placeholder="$t('auth.placeholder.username')"
                  class="login-input"
              ></el-input>
            </el-form-item>
            <el-form-item :label="$t('auth.password')"  prop="password">
              <el-input
                  v-model="passLoginForm.password"
                  type="password"
                  :placeholder="$t('auth.placeholder.password')"
                  class="login-input"
                  show-password
              ></el-input>
            </el-form-item>
            <!--验证码-->
            <el-form-item :label="$t('auth.verifyCode')" prop="captcha">
              <div class="captcha-container">
                <el-input v-model="passLoginForm.captcha" :placeholder="$t('auth.placeholder.verifyCode')" style="width: 60%" prop="verifyCode"></el-input>
                  <el-image v-loading="captchaLoading" v-if="captchaImage" :src="captchaImage" @click="()=>{ refreshCaptcha();}" class="captcha-image" alt="Captcha"/>
                  <el-skeleton v-else :rows="1" animated style="width: 120px; height: 20px" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" @click="submitForm(passAuthForm)">{{ $t('auth.btn.login') }}</el-button>
              <div class="password-addition">
                <el-button size="small" link>{{ $t('auth.forgetPassword') }}</el-button>
                <el-button size="small" link class="to-register" @click.prevent="router.push('/register')" >{{ $t('auth.toRegister') }}</el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <!--快捷登录-->
        <el-tab-pane :label="$t('auth.tabs.emailPhone')" name="fast-auth">
          <el-form
              ref = 'fastAuthForm'
              label-width="auto"
              :model="fastLoginForm"
              :rules="fastAuthRules"
              class="auth-form"
          >
            <el-form-item :label="$t('auth.email') + '/' + $t('auth.phone')" prop="username">
              <el-input
                  ref='usernameInputRef'
                  v-model="fastLoginForm.username"
                  :placeholder="$t('auth.placeholder.emailPhone')"
                  class="login-input"
              ></el-input>
            </el-form-item>
            <el-form-item :label="$t('auth.verifyCode')" prop="verifyCode">
              <el-input
                  v-model="fastLoginForm.verifyCode"
                  :placeholder="$t('auth.placeholder.verifyCode')"
                  class="login-input"
              >
                <template #append>
                  <VerifyingCodeButton 
                    :username="fastLoginForm.username"
                    :getType="fastLoginForm.username.includes('@') ? 'email'
                    : 'sms'"
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
                  :loading = "buttonLoad"
              >{{ $t('auth.btn.login') }} / {{$t('auth.btn.register')}}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </auth-box>
</template>

<style scoped>

.auth-form{
  font-size: small;
  margin-top: 20px;
}

.password-addition{
  display: flex;
  align-items: center;
  width: 100%;
}

.password-addition .to-register{
  margin-left: auto;
}

.login-btn{
  width: 100%;
  margin: 5px auto;
}

.captcha-container {
  display: flex;
  align-items: center;
  width: 100%;
}
:deep(.el-tabs__content) {
  padding-bottom: 0;
}
</style>