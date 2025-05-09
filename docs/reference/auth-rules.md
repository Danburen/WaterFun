# 认证规则说明

## 用户名规则
- **最小长度**: `username.min_length` (默认: 6)
- **最大长度**: `username.max_length` (默认: 20)
- **允许字符**: 正则表达式 `[a-zA-Z0-9_]`
- **错误提示**: 见 `username.error_messages`

## 密码规则
- **复杂度要求**:
    - 必须包含大小写字母、数字和特殊符号（`@#$%`等）。
- **前端验证逻辑**:
  ```javascript
  // 示例：密码强度校验
  if (!/(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*\W)/.test(password)) {
    throw new Error("密码不符合复杂度要求");
  }
  ```