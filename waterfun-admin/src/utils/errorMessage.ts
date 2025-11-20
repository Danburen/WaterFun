import { ErrorCode, AUTO_ERROR_CODE_MESSAGE_KEY_MAP } from '@waterfun/web-core/src/ErrorCodeEnum'
import { HttpStatus } from "@waterfun/web-core/src/ErrorCodeEnum";
import { translate } from '@/utils/translator'

/**
 * get the error code translate message
 * @param code error code

 * @return translated error code message
 */
export function getErrorMessage(code: ErrorCode | HttpStatus,): string {
    if (Object.values(HttpStatus).includes(code as HttpStatus)) {
        return translate(`message.error.http.${code}`)
    }

    const messageKey = AUTO_ERROR_CODE_MESSAGE_KEY_MAP[code] || 'unknownError'
    if (messageKey === 'unknownError') {
        return translate('message.error.unknownError') + ` (${code})`
    }

    if (code >= 40100 && code <= 40199) {
        return translate(`message.error.auth.${messageKey}`)
    }

    return translate(`message.error.badRequest.${messageKey}`)
}