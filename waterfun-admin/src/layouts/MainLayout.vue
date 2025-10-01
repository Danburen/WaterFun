<script setup lang="ts">
import MainNavBar from "@/layouts/main/MainNavBar.vue";
import {ref, computed, onMounted, onBeforeMount, watch} from 'vue'
import type {BreadNavItemType, TagNavItemType} from "@/layouts/types.js";
import TagNavigation from "@/layouts/main/TagNavigation.vue";
import AsideNavBar from "@/layouts/main/AsideNavBar.vue";
import router, {type routeType} from "@/router/index.js";
import {useTagStore} from "@/store/ui/navTagStore.js";

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
  to: '/',
  closeable: false
}

const addNavTags = (to) =>{
  tagStore.addTag({
    name: to.name,
    to: to.name,
    closeable: true,
    locale: `nav.${to.meta.locale}`,
  })
}

const handleRemoveTag = (tagName:string) => {
  let name = null
  if(activeTags.value === tagName){
    name =  tagStore.getTags[tagStore.getTags.findIndex((t) => t.name === tagName) - 1].name || ''
  }
  tagStore.removeTag(tagName);
  activeTags.value = name || activeTags.value
}

const handleOrderUpdated = ({ from, to }: { from: number; to: number }) => {
  const newTags = [...tagStore.getTags]
  const [moved] = newTags.splice(from, 1)
  newTags.splice(to, 0, moved)
  tagStore.updateTags(newTags)
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
      <AsideNavBar :collapse="menuCollapse" @menuClick="" />
      <div class="content-container">
        <div class="content-header default-border-bottom">
          <TagNavigation
              class="tag-nav"
              v-model="activeTags"
              :tag-list="tagList"
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
  min-height: 100vh
}

.main-container {
  display: flex;
  flex-grow: 1;
  min-height: 0;
  min-width: 0;
}

.content-header {
  background: #f9f9f9;
}

.content-container {
  background: var(--light-gray);
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.content-main {
  flex: 1 1 auto; /* 占据剩余空间 */
  padding: 10px;
  margin: 10px;
  overflow-y: auto; /* 内容过多时滚动 */
  background: #fff;
  min-height: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  border-radius: 3px;
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
