import { createI18n } from 'vue-i18n';

import enLocaleData  from '@/locales/en.json'; // synchronized load en.json
const localeFiles = import.meta.glob('../locales/*.json'); // Preload all lang file

/**
 * Asynchronous load language
 * @param lang the language to load
 * @returns {Promise<Awaited<unknown>>}
 */
const loadLanguage = async (lang) => {
    const loaded = i18n.global.availableLocales;
    if (loaded.includes(lang)) {
        i18n.global.locale.value = lang;
        return Promise.resolve(lang);
    }
    try {
        const path = `../locales/${lang}.json`;
        const module = await localeFiles[path](); // execute preload model
        i18n.global.setLocaleMessage(lang, module.default);
        i18n.global.locale.value = lang;
        return Promise.resolve(lang);
    } catch (error) {
        console.error(`加载语言文件失败: ${lang}`, error);
        i18n.global.setLocaleMessage('en', enLocaleData);
        i18n.global.locale.value = 'en';
        return Promise.reject(error);
    }
};

const i18n = new createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: {
        en: enLocaleData
    }
});
/**
 * Get browser default language
 * @returns {string}
 */
const getBrowserLanguage = () => {
    const lang = navigator.language || 'en';
    return lang.split('-')[0]
};



const initLanguage = async() =>{
    const browserLang = getBrowserLanguage();
    try {
        await loadLanguage(browserLang);
    } catch (error) {
        console.warn(`Falling back to default language 'en'.`);
        await loadLanguage('en');
    }
};

export {
    i18n,loadLanguage,initLanguage,
};