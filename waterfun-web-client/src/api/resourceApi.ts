import request from "../utils/axiosRequest"

import type { FileResDataType } from "@waterfun/web-core/src/types/api/response";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type {LanguageTypes} from "~/types/sys/lang";

export const getEula = (lang:LanguageTypes): PromiseResBody<FileResDataType> => {
    return request.get(`/resource/legal/licence/${lang}/eula.txt`);
}
