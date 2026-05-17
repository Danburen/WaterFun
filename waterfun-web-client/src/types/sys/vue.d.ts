import 'vue-i18n'

declare module '@vue/runtime-core' {
    interface ComponentCustomProperties {
        $t: (key: string, params?: Record<string, any>) => string
    }
}