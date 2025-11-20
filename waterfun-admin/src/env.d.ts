import { type Composer } from 'vue-i18n'

declare module 'vue' {
    interface ComponentCustomProperties {
        $t: Composer['t']
        $i18n: {
            locale: string
            availableLocales: string[]
            t: Composer['t']
        }
    }
}

declare module '*.css' {
    const css: string
    export default css
}