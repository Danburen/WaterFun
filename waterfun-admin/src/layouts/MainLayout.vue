<script setup lang="ts">
import MainNavBar from "@/layouts/main/MainNavBar.vue";
import {ref, computed, onMounted, onBeforeMount, watch} from 'vue'
import type {BreadNavItemType, TagNavItemType} from "@/types/ui/tagNav.js";
import TagNavigation from "@/layouts/main/TagNavigation.vue";
import AsideNavBar from "@/layouts/main/AsideNavBar.vue";
import router, {type routeType} from "@/router";
import {useTagStore} from "~/stores/tagStore.js";

const menuCollapse = ref(false)
const tagNavContainer = ref(null)
const tagStore = useTagStore();
const activeTags = ref<string>('dashboard');

const tagList = ref<TagNavItemType[]>([]);
const breadList = computed(() => {
  return tagList.value.map((tag: TagNavItemType) => {
    return {
      name: tag.name,
      locale: tag.locale,
      to: tag.to,
    } as BreadNavItemType;
  });
});

const dashboardTagItem:TagNavItemType = {
  name: 'dashboard',
  locale: 'nav.dashboard',
  to: 'dashboard',
  closeable: false
}

const addNavTags = (to: any) =>{
  if (to.name && to.meta && to.meta.locale && to.meta.public !== true) {
    tagStore.addTag({
        name: to.name,
        to: to.name,
        params: to.params,
        closeable: true,
        locale: `nav.${to.meta.locale}`,
      })
  }
}

const handleRemoveTag = (tagName:string) => {
  let name = null
  if(activeTags.value === tagName){
    const currentTags = tagStore.getTags;
    const currentIndex = currentTags.findIndex((t) => t.name === tagName);
    const targetTag = currentTags[currentIndex - 1] || currentTags[currentIndex + 1];
    if (targetTag) {
      name = targetTag.name;
      router.push({ name: targetTag.to });
    }
  }
  tagStore.removeTag(tagName);
  if (name) {
    activeTags.value = name;
  }
}

const handleOrderUpdated = ({ from, to }: { from: number; to: number }) => {
  const newTags = [...tagStore.getTags]
  const [moved] = newTags.splice(from, 1)
  if(moved){
    newTags.splice(to, 0, moved)
    tagStore.updateTags(newTags)
  }
}

onBeforeMount(() => {
  tagStore.addTag(dashboardTagItem);
});

router.afterEach((to) => {
  addNavTags(to);
  activeTags.value = to.name as string;
})

watch(()=>tagStore.getTags,
    (newTags)=> {
      tagList.value = newTags;
    }, { immediate: true, deep: true })
</script>

<template>
  <div class="container full-page-container">
    <!-- Global Header -->
    <MainNavBar class="header-navbar" :navItems="breadList" @collapse="menuCollapse = !menuCollapse" />
    <!-- Main Container-->
    <div class="main-container">
      <aside :class="['aside-navbar', { collapsed: menuCollapse }]">
        <AsideNavBar :collapse="menuCollapse" @menuClick="" />
      </aside>
      <div class="content-container">
        <div class="content-header default-border-bottom">
          <TagNavigation
              class="tag-nav"
              v-model="activeTags"
              :tag-list="tagList"
              @order-updated="handleOrderUpdated"
              @tag-removed="handleRemoveTag"
          />
        </div>
        <div class="content-main"><RouterView /></div>
        <div class="content-footer default-border-top">
          <div class="copyright">CopyRight © WaterFun 2025</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  box-sizing: border-box;
}

.main-container {
  display: flex;
  flex-grow: 1;
  min-height: 0;
  min-width: 0;
}

.aside-navbar {
  transition: width 0.3s ease;
  width: 175px;
  flex-shrink: 0;
}

.aside-navbar.collapsed {
  width: 55px;
}

.content-header {
  background: #f9f9f9;
}

.content-container {
  background: var(--light-gray);
  display: flex;
  flex: 1;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
  transition: all 0.3s ease;
}

.content-main {
  flex: 1 1 auto; /* 占据剩余空间 */
  padding: 10px;
  overflow-y: auto; /* 内容过多时滚动 */
  background: #fff;
  min-height: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  border-radius: 3px;
  transition: all 0.3s ease;
  min-width: 0; /* 确保在flex容器中能正确收缩 */
}

.content-footer {
  max-height: 30px;
  font-size: 12px;
  background: var(--default-white);
  flex-shrink: 0;
}

.copyright {
  float: right;
  padding: 0.5em 1em;
}

.header-navbar {
  z-index: 100;
}
</style>
