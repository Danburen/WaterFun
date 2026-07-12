<script setup lang="ts">

import { getEula } from "~/api/resourceApi";
import LegalDocument, {type LegalDocPropsType} from "~/components/LegalDocument.vue";
import { base64ToUint8Array } from "@waterfun/web-core/src/dataMapper"
import type {FileResDataType} from "@waterfun/web-core/src/types/api/response";
import {LangMap} from "~/utils/consts";
import type { DataApiResponse } from "@waterfun/web-core/src/types/api/response";
const i18n = useI18n();

const route = useRoute()
const router = useRouter()

const legalDocProps = reactive<LegalDocPropsType>({
  title: i18n.t('confirm.userAgreement'),
  content:i18n.t('info.loading'),
  lastUpdate: new Date(),
})

const returnPath = computed(() => {
  return router.getRoutes().at(-1)?.path ?? '/';
})

const handleConfirm = () =>{
  router.push({
    path: returnPath.value,
    query:{
      userAgreementConfirm: 'true',
      ...(route.query.from ? {}: {from: route.path}),
    }
  })
}

onMounted(()=>{
  getEula(LangMap[i18n.locale.value] || 'zh_CN').then((response : DataApiResponse<FileResDataType>) => {
    legalDocProps.content = new TextDecoder('utf-8').decode(
        base64ToUint8Array(response.data.content)
    );
    legalDocProps.lastUpdate = new Date(response.data.lastModified);
  }).catch(error => {
    console.log(error);
  })
})
</script>

<template>
<LegalDocument v-bind="legalDocProps" @confirm="handleConfirm"></LegalDocument>
</template>

<style scoped>

</style>