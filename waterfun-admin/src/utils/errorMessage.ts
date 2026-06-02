import { ErrorCode, AUTO_ERROR_CODE_MESSAGE_KEY_MAP } from '@waterfun/web-core/src/ErrorCodeEnum'
import { HttpStatus } from "@waterfun/web-core/src/ErrorCodeEnum";

const httpStatusMessages: Record<number, string> = {
  [HttpStatus.OK]: '请求成功',
  [HttpStatus.BAD_REQUEST]: '请求参数错误',
  [HttpStatus.UNAUTHORIZED]: '未授权',
  [HttpStatus.FORBIDDEN]: '无权限访问',
  [HttpStatus.NOT_FOUND]: '资源不存在',
  [HttpStatus.INTERNAL_SERVER_ERROR]: '服务器内部错误',
}

const errorMessages: Record<string, string> = {
  unknownError: '未知错误',
  usernameEmptyOrInvalid: '用户名为空或格式不正确',
  passwordEmptyOrInvalid: '密码为空或格式不正确',
  usernameOrPasswordIncorrect: '用户名或密码错误',
  captchaExpired: '图形验证码已过期',
  captchaIncorrect: '图形验证码不正确',
  verifyCodeExpired: '验证码已过期',
  verifyCodeIncorrect: '验证码错误或已过期',
  smsCodeExpired: '短信验证码已过期',
  smsCodeIncorrect: '短信验证码错误',
  emailCodeExpired: '邮箱验证码已过期',
  emailCodeIncorrect: '邮箱验证码错误',
  captchaEmpty: '图形验证码不能为空',
  smsCodeEmpty: '短信验证码不能为空',
  emailCodeEmpty: '邮箱验证码不能为空',
  phoneNumberEmptyOrInvalid: '手机号为空或格式不正确',
  emailAddressEmptyOrInvalid: '邮箱地址为空或格式不正确',
  userAlreadyExists: '用户已存在',
  userNotFound: '用户不存在',
  roleNotFound: '角色不存在',
  roleAlreadyExists: '角色已存在',
  permissionNotFound: '权限不存在',
  permissionAlreadyExists: '权限已存在',
  redundantOperation: '重复操作',
  invalidPath: '路径无效',
  requestNotInWhitelist: '请求不在白名单中',
  invalidContentType: 'Content-Type 无效',
}

export function getErrorMessage(code: ErrorCode | HttpStatus): string {
  if (Object.values(HttpStatus).includes(code as HttpStatus)) {
    return httpStatusMessages[code] || `未知错误 (${code})`
  }

  const messageKey = AUTO_ERROR_CODE_MESSAGE_KEY_MAP[code] || 'unknownError'
  return errorMessages[messageKey] || `未知错误 (${code})`
}
