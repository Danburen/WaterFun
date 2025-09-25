<script setup>

</script>

<template>
  <div class="container full-page-container items-center">
    <div class="main">
      <div class="left items-center">
        <div class="logo items-center">
          <img src="../assets/logo.svg" style="height: 65px;width: 65px;padding: 10px" alt="">
          <span>WaterFun</span>
        </div>
      </div>
      <div class="form-container items-center">
        <el-form class="form" label-width="auto" >
          <el-form-item :label="$t('auth.username')">
            <el-input></el-input>
          </el-form-item>
          <el-form-item :label="$t('auth.password')">
            <el-input></el-input>
          </el-form-item>
          <el-form-item :label="$t('auth.verifyCode')">
            <el-input></el-input>
          </el-form-item>
          <el-form-item>
            <el-button class="login-btn" type="primary">{{ $t('auth.btn.login') }}</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  background: linear-gradient(135deg, #3da5f3 0%, #dbeaff 100%);
}
.main {
  width: 800px;
  height: 494px;
  display: flex;
  flex-direction: row;
  background-color: var(--default-white);
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.left {
  width: 494px;
  height: 100%;
  background: linear-gradient(15deg, #7d00ff 0%, #33c2ff 100%);
  background-size: cover;
  display: flex;
  flex-direction: column;
}

.logo {
  font-size: 30px;
  color: var(--default-white);
  padding: 10px 0;
}

.form-container {
  background-color: var(--default-white);
  margin: 25px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.form {
  max-width: 200px;
}

.login-btn {
  width: 100%;
}
</style>