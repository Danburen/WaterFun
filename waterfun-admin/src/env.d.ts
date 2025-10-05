import { type I18n } from 'vue-i18n'

declare module '@vue/runtime-core' {
    interface ComponentCustomProperties {
        $t: I18n['t']
        $i18n: I18n
    }
}

declare module '*.css' {
    const css: string
    export default css
}