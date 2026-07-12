import request from "../utils/axiosRequest"

import type { FileResDataType } from "@waterfun/web-core/src/types/api/response";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type {LanguageTypes} from "~/types/sys/lang";

export const getEula = (lang:LanguageTypes): PromiseResBody<FileResDataType> => {
    return request.get(`/resource/legal/licence/${lang}/eula.txt`);
}

export const getAbout = (lang:LanguageTypes): PromiseResBody<FileResDataType> => {
    return request.get(`/resource/legal/about/${lang}/about.txt`);
}

export const getPrivacy = (lang:LanguageTypes): PromiseResBody<FileResDataType> => {
    return request.get(`/resource/legal/privacy/${lang}/privacy.txt`);
}

export const getTerms = (lang:LanguageTypes): PromiseResBody<FileResDataType> => {
    return request.get(`/resource/legal/terms/${lang}/terms.txt`);
}
