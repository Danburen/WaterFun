import i18n from "@/utils/i18n.js";

let cachedI18n: any = null;

export function translate(key: string, options?: { args?: Record<string, any>; default?: string }): string {
    if (!cachedI18n) {
        try {
            cachedI18n = i18n;
        } catch (e) {
            console.error("Failed to initialize i18n:", e);
            return options?.default || key;
        }
    }

    if (!cachedI18n) return options?.default || key;

    try {
        return cachedI18n.t(key, options?.args || {});
    } catch (e) {
        console.warn(`Translation error for key "${key}":`, e);
        return options?.default || key;
    }
}