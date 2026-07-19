import request from "../utils/axiosRequest"

import type { FileResDataType } from "@waterfun/web-core/src/types/api/response";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type {LanguageTypes} from "~/types/sys/lang";

/**
 * 扁平文件资源 API（新）
 * 文件命名规则：{name}.md（中文默认），{name}_{locale}.md（其他语言）
 */
const resourceFile = (name: string, lang: LanguageTypes): PromiseResBody<FileResDataType> => {
    const suffix = lang === 'en_US' ? '_en_US' : '';
    return request.get(`/resource/${name}${suffix}.md`);
};

export const getEula = (lang: LanguageTypes): PromiseResBody<FileResDataType> =>
    resourceFile('eula', lang);

export const getAbout = (lang: LanguageTypes): PromiseResBody<FileResDataType> =>
    resourceFile('about', lang);

export const getPrivacy = (lang: LanguageTypes): PromiseResBody<FileResDataType> =>
    resourceFile('privacy', lang);

export const getTerms = (lang: LanguageTypes): PromiseResBody<FileResDataType> =>
    resourceFile('terms', lang);

export const getContact = (lang: LanguageTypes): PromiseResBody<FileResDataType> =>
    resourceFile('contact', lang);
