# WaterFun Service API Documentation

## 项目概述
WaterFun Service 是一个基于 Spring Boot 的后端服务项目，提供了一系列 RESTful API 接口。

## 基础信息
- 基础URL: `http://localhost:8080`
- 所有请求和响应均使用 JSON 格式
- 认证方式: Bearer Token

## 接口列表

### 1. 认证相关接口

#### 1.1 用户登录
**基础请求体 (Method | Interface)** 适用于首次登录
1. 密码登录 (`POST`|`/api/auth/login/password`)
    ```json
      {
      "username": "string",
      "password": "string",
      "captcha": "string",
      "loginType": "string"
      }
    ```
   loginType 必须是 `password`，表示密码登录。
2. 手机验证码登录 (`POST`|`/api/auth/login/sms`)
    ```json
    {
      "phoneNumber": "string",
      "smsCode": "string",
      "loginType": "string"
    }
    ```
   loginType 必须是 `sms`，表示手机验证码登录。
3. 邮箱验证码登录 (`POST`|`/api/auth/login/email`)
    ```json
    {
      "email": "string",
      "emailCode": "string",
      "loginType": "string"
    }
    ```
   loginType 必须是 `email`，表示邮箱验证码登录。
    > 再次登录请求体为基础请求体加上`accessToken`,`refreshToken` 字段。 
    > 例如(再次密码登录):
    ```json
    {
      "username": "string",
      "password": "string",
      "captcha": "string",
      "loginType": "password"
    }
    ```

**响应**:
[ApiResponse + LoginResponseData](#2-有具体数据返回通常用于登录等需要返回用户信息的接口)

  ```json
  {
    "code": "number",
    "message": "string",
    "data": {
      "username": "string",
      "userUid": "number",
      "expireIn": "number"
    }
  }
  ```
  
#### 1.2 用户获取 邮箱/手机验证码
- **接口**: `/api/auth/send-email-code`
- **方法**: POST
- **描述**: 发送邮箱验证码接口
- **请求体**:
  ```json
  {
    "email": "string",
    "purpose": "string"  
  }
  ```
  其中 `purpose` 可以是 `register` 或 `resetPassword` 或 'login'
- **响应**:
 [ResponseCode](#1-无具体数据返回仅代表返回成功或失败结果消息)
```json
{
  "code": "number",
  "message": "string"
}
```

### 2. 通用的响应体

#### 1. 无具体数据返回，仅代表返回成功或失败结果消息。
- **ResponseCode**
```json
{
  "code": "number",
  "message": "string"
}
```
`code` 字段表示响应具体code码(非状态码)，`message` 字段表示响应消息。

#### 2. 有具体数据返回，通常用于登录等需要返回用户信息的接口
**ApiResponse**
```json
{
  "code": "number",
  "message": "string",
  "data": "object"
}
```
其中 `code` 字段表示响应具体code码(非状态码)，`message` 字段表示响应消息，`data` 字段包含具体的数据。

`data`可以是以下任意的一种:

* `LoginResponseData`
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "username": "string",
  "userUid": "number",
  "expireIn": "number"
}
```
其中 `accessToken` 是访问令牌，`refreshToken` 是刷新令牌，`username` 是用户名，`userUid` 是用户ID，`expireIn` 是令牌过期时间（单位：秒）。

## Response Code 响应码对照表

| Code | msg值 | 描述 |
|------|-------|------|
| 200 | SUCCESS | 请求成功 |
| 400 | BAD_REQUEST | 错误请求 |
| 401 | UNAUTHORIZED | 未授权 |
| 403 | FORBIDDEN | 禁止访问 |
| 404 | NOT_FOUND | 资源未找到 |
| 500 | INTERNAL_SERVER_ERROR | 服务器内部错误 |
| 50000 | UNKNOWN_ERROR | 未知错误 |

### 用户信息相关错误
| Code | msg值 | 描述 |
|------|-------|------|
| 40001 | USERNAME_EMPTY | 用户名为空 |
| 40002 | PASSWORD_EMPTY | 密码为空 |
| 40003 | USERNAME_OR_PASSWORD_INCORRECT | 用户名或密码错误 |
| 40004 | CAPTCHA_EXPIRED | 验证码已过期 |
| 40005 | CAPTCHA_INCORRECT | 验证码不正确 |
| 40006 | VERIFY_CODE_EXPIRED | 验证码已过期 |
| 40007 | VERIFY_CODE_INCORRECT | 验证码不正确 |
| 40008 | SMS_CODE_EXPIRED | 短信验证码已过期 |
| 40009 | SMS_CODE_INCORRECT | 短信验证码不正确 |
| 40010 | EMAIL_CODE_EXPIRED | 邮箱验证码已过期 |
| 40011 | EMAIL_CODE_INCORRECT | 邮箱验证码不正确 |
| 40012 | CAPTCHA_EMPTY | 验证码为空 |
| 40013 | SMS_CODE_EMPTY | 短信验证码为空 |
| 40014 | EMAIL_CODE_EMPTY | 邮箱验证码为空 |
| 40015 | PHONE_NUMBER_INVALID | 手机号无效 |
| 40016 | EMAIL_ADDRESS_INVALID | 邮箱地址无效 |

### 认证相关错误
| Code | msg值 | 描述 |
|------|-------|------|
| 40101 | ACCESS_TOKEN_EXPIRED | 访问令牌已过期 |
| 40102 | ACCESS_TOKEN_INVALID | 访问令牌无效 |
| 40103 | ACCESS_TOKEN_MISSING | 访问令牌缺失 |
| 40104 | REFRESH_TOKEN_EXPIRED | 刷新令牌已过期 |
| 40105 | REFRESH_TOKEN_INVALID | 刷新令牌无效 |
| 40106 | REFRESH_TOKEN_MISSING | 刷新令牌缺失 |

## 注意事项
1. 所有需要认证的接口都需要在请求头中携带 `Authorization: Bearer {token}`
2. 请求体中的时间格式统一使用 ISO 8601 标准
3. 分页接口统一使用 `page` 和 `size` 参数
4. 所有接口的响应时间不应超过 3 秒

## 更新日志
- 2024-03-21: 初始版本 