<script setup lang="ts">
/**
 * RemoteSelect — 通用远程搜索下拉组件
 *
 * 封装 el-select + filterable + remote，支持输入关键字远程搜索，
 * 组件挂载时自动加载默认选项（空关键字前 N 条）。
 *
 * 用法:
 *   <RemoteSelect
 *     :fetch-fn="(keyword, limit) => getUserOptions(keyword, limit).then(r => r.data ?? [])"
 *     v-model="creatorId"
 *     placeholder="选择创建人"
 *   />
 */
import { computed, ref, watch } from "vue";
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { ElSelect, ElOption } from "element-plus";

type SelectId = string | number;

const props = withDefaults(
  defineProps<{
    /** 远程获取选项的函数: (keyword: string, limit: number) => Promise<OptionResItem[]> */
    fetchFn: (keyword: string, limit: number) => Promise<OptionResItem[]>;
    /** v-model 绑定值 */
    modelValue?: SelectId | SelectId[] | null;
    /** 占位文本 */
    placeholder?: string;
    /** 是否可清除 */
    clearable?: boolean;
    /** 是否多选 */
    multiple?: boolean;
    /** 每次请求返回的最大条数 */
    limit?: number;
    /** 禁用状态 */
    disabled?: boolean;
    /** 是否在挂载时自动加载默认选项（空关键字） */
    autoLoad?: boolean;
    /** 值 key */
    valueKey?: string;
    /** label 展示的 key */
    labelKey?: string;
  }>(),
  {
    placeholder: "请搜索选择",
    clearable: false,
    multiple: false,
    limit: 100,
    disabled: false,
    autoLoad: true,
    valueKey: "id",
    labelKey: "name",
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", val: SelectId | SelectId[] | null): void;
}>();

const options = ref<OptionResItem[]>([]);
const loading = ref(false);
const searchKeyword = ref("");

const remoteMethod = async (keyword: string) => {
  searchKeyword.value = keyword;
  loading.value = true;
  try {
    const result = await props.fetchFn(keyword, props.limit);
    options.value = result || [];
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
};

// 挂载时加载默认选项（空关键字 hot list）
if (props.autoLoad) {
  remoteMethod("");
}

// 监听 limit 变化，重新加载
watch(
  () => props.limit,
  () => {
    remoteMethod(searchKeyword.value);
  },
);
</script>

<template>
  <ElSelect
    :model-value="modelValue"
    :multiple="multiple"
    :clearable="clearable"
    :placeholder="placeholder"
    :disabled="disabled"
    :loading="loading"
    :remote="true"
    :filterable="true"
    :remote-method="remoteMethod"
    :value-key="valueKey"
    style="width: 100%"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <ElOption
      v-for="item in options"
      :key="item[valueKey]"
      :label="item[labelKey]"
      :value="item[valueKey]"
      :disabled="!!item.disabled"
    />
  </ElSelect>
</template>
