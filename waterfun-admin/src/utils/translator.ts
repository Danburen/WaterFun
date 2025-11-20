import type { I18nService,I18nTranslator } from '@waterfun/web-core/src/interface/translate'
let translatorInstance: I18nTranslator;
import i18n from "@/utils/i18n";

export class VueI18nService implements I18nService {
    private cachedI18n: any = null;

    getTranslator(): I18nTranslator {
        return <I18nTranslator>{
            translate: (key, options) => {
                if (!this.cachedI18n) {
                    try {
                        this.cachedI18n = i18n
                    } catch (e) {
                        console.error('Failed to initialize i18n:', e);
                        return options?.default || key;
                    }
                }

                if (!this.cachedI18n) return options?.default || key;

                try {
                    return this.cachedI18n.t(key, options?.args || {});
                } catch (e) {
                    console.warn(`Translation error for key "${key}":`, e);
                    return options?.default || key;
                }
            }
        }
    }
}

export function translate(key: string, options?: { args?: Record<string, any>; default?: string }): string {
    if(translatorInstance == null) {
        translatorInstance = new VueI18nService().getTranslator();
    }
    return translatorInstance.translate(key, options);
}