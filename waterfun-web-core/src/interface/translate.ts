export interface I18nTranslator {
    /**
     * translate the target text
     * @param key translation key
     * @param options
     * @param options.args
     * @param options.default default
     */
    translate(
        key: string,
        options?: { args?: Record<string, any>; default?: string }
    ): string;
}

export interface I18nService {
    /**
     * get translate instance
     */
    getTranslator(): I18nTranslator;
}