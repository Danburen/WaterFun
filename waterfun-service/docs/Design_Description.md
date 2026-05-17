### 权限设计(分层设计/多接口): 
### `惰性API/MENU/BUTTON` 类型
此类型的权限放在accessToken 的 jwt scope载体当中
### `DATA/实时性高的API`
此类型的权限一般用来控制某个具体的数据范围，或者删除支付等操作。

### 加密设计
采用KEK,DEK轮换模式。
GCM模式