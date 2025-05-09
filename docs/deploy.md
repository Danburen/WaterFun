
DataBase:

```mysql
CREATE DATABASE business_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'business_user'@'%' IDENTIFIED BY 'StrongPassword123!';

-- 授予新用户对新数据库的完全权限
GRANT ALL PRIVILEGES ON business_db.* TO 'business_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

To start mysql & redis server:
```shell
@echo off
net start MySQL
cd /d E:\Program Files\Redis\
redis-server.exe redis.conf
echo redis service start successfully
pause
```


Frontend:

dependence:
```shell
# 全局安装 serve 工具
npm install -g serve
```

```shell
# 进入 dist 目录并启动服务
serve -s dist
```

To assign other port:
```shell
serve -s -p [port] dist 
```
