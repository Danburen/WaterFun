import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { getCategoryOptions, type CategoryOptionVO } from "~/api/category";
import { getTagOptions, type TagOptionVO } from "~/api/tag";

export const useDictStore = defineStore("dictStore", () => {
  const categoryOptions = ref<CategoryOptionVO[]>([]);
  const tagOptions = ref<TagOptionVO[]>([]);
  const loaded = ref(false);

  const categoryMap = computed(() => {
    const map = new Map<number, string>();
    categoryOptions.value.forEach((item) => map.set(item.id, item.name));
    return map;
  });

  const tagMap = computed(() => {
    const map = new Map<number, string>();
    tagOptions.value.forEach((item) => map.set(item.id, item.name));
    return map;
  });

  async function refresh() {
    const [catRes, tagRes] = await Promise.all([
      getCategoryOptions(),
      getTagOptions("", 100),
    ]);
    categoryOptions.value = catRes.data || [];
    tagOptions.value = tagRes.data || [];
    loaded.value = true;
  }

  function getCategoryName(id: number): string | undefined {
    return categoryMap.value.get(id);
  }

  function getTagName(id: number): string | undefined {
    return tagMap.value.get(id);
  }

  async function ensureLoaded() {
    if (!loaded.value) {
      await refresh();
    }
  }

  return {
    categoryOptions,
    tagOptions,
    loaded,
    categoryMap,
    tagMap,
    refresh,
    getCategoryName,
    getTagName,
    ensureLoaded,
  };
});
