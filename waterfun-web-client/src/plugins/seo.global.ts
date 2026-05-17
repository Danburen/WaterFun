export default defineNuxtPlugin(()=>{
  useHead({
    titleTemplate: (title?: string) =>
      title ? `${title} - WaterFun` : 'WaterFun',
  })
})