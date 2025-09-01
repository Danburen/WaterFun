import {createI18n} from "vue-i18n";
import zh from '../../i18n/locales/zh_CN.json'
import en from '../../i18n/locales/en_US.json'
const i18n = createI18n({
    legacy: false,
    locale: 'zh',
    fallbackLocale: 'en',
    messages: {
        zh,
        en
    }
})

export default i18n;