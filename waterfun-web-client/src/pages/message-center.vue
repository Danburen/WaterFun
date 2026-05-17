<template>
  <div class="message-center-page">
    <HeaderNavMenu />
    <div class="message-center-wrap">
      <div class="left-column">
        <MessageCenterSidebar :active-tab="activeTab" @tab-change="switchTab" />
      </div>
      <div class="right-column">
        <SystemNotificationModule v-show="activeTab === 'system'" :active="activeTab === 'system'" />
        <SubscribeMessageModule v-show="activeTab === 'subscribe'" />
        <ReplyMessageModule v-show="activeTab === 'reply'" />
        <MentionMessageModule v-show="activeTab === 'mention'" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import HeaderNavMenu from "~/components/HeaderNavMenu.vue";
import MessageCenterSidebar, { type MessageTabType } from "~/components/message-center/MessageCenterSidebar.vue";
import SystemNotificationModule from "~/components/message-center/SystemNotificationModule.vue";
import SubscribeMessageModule from "~/components/message-center/SubscribeMessageModule.vue";
import ReplyMessageModule from "~/components/message-center/ReplyMessageModule.vue";
import MentionMessageModule from "~/components/message-center/MentionMessageModule.vue";

definePageMeta({
  ssr: false,
});

const route = useRoute();
const router = useRouter();

const validTabs: MessageTabType[] = ["subscribe", "reply", "mention", "system"];
const parseTabFromRoute = (): MessageTabType => {
  const tab = String(route.query.tab || "system") as MessageTabType;
  return validTabs.includes(tab) ? tab : "system";
};

const activeTab = ref<MessageTabType>(parseTabFromRoute());

const switchTab = (tab: MessageTabType) => {
  activeTab.value = tab;
};

watch(
  () => route.query.tab,
  () => {
    activeTab.value = parseTabFromRoute();
  }
);

watch(activeTab, (tab) => {
  router.replace({ query: { ...route.query, tab } });
});
</script>

<style scoped>
.message-center-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f6fbff 0%, #f8fafc 100%);
}

.message-center-wrap {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.left-column {
  min-width: 0;
}

.right-column {
  min-width: 0;
}

@media (max-width: 768px) {
  .message-center-wrap {
    grid-template-columns: 1fr;
    padding: 12px;
  }
}
</style>
