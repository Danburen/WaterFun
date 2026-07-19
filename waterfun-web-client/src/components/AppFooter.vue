<template>
  <div>
    <el-divider style="margin:0" />
    <footer class="app-footer">
      <div class="footer-links">
        <NuxtLink to="/about" class="footer-link">{{ $t('footer.about') || '关于我们' }}</NuxtLink>
        <NuxtLink to="/EulaView" class="footer-link">{{ $t('footer.eula') || '用户协议' }}</NuxtLink>
        <NuxtLink to="/legal/terms" class="footer-link">{{ $t('footer.terms') || '服务条款' }}</NuxtLink>
        <NuxtLink to="/legal/privacy" class="footer-link">{{ $t('footer.privacy') || '隐私政策' }}</NuxtLink>
        <NuxtLink to="/contact" class="footer-link">{{ $t('footer.contact') || '联系我们' }}</NuxtLink>
      </div>
      <div class="footer-copyright">© 2026 WaterFun. All rights reserved.</div>
    </footer>
  </div>
</template>

<style scoped>
.app-footer {
  padding: 32px 24px;
  text-align: center;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.footer-link {
  font-size: 14px;
  color: #64748b;
  text-decoration: none;
  transition: color 0.15s;
}

.footer-link:hover {
  color: #3b82f6;
}

.footer-copyright {
  font-size: 13px;
  color: #94a3b8;
}

@media (max-width: 768px) {
  .app-footer {
    padding: 24px 16px;
  }
  .footer-links {
    gap: 12px;
  }
  .footer-link {
    font-size: 13px;
  }
}
</style>
