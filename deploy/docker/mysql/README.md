# MySQL Keyring 配置指南

`user_data` 表使用了 InnoDB 表空间加密（`ENGINE=InnoDB ENCRYPTION='Y'`），需要 MySQL keyring 管理 master key。

## 配置说明

### 1. keyring.cnf

```ini
[mysqld]
early-plugin-load=keyring_file.so
keyring_file_data=/var/lib/mysql-keyring/keyring
```

挂载路径：`./mysql/keyring.cnf:/etc/mysql/conf.d/keyring.cnf:ro`

### 2. Docker 卷

`mysql_keyring` named volume 用于持久化 keyring 文件：
```yaml
volumes:
  - mysql_data:/var/lib/mysql
  - ./mysql/keyring.cnf:/etc/mysql/conf.d/keyring.cnf:ro
  - mysql_keyring:/var/lib/mysql-keyring
```

## 首次部署步骤

### 情况 A：MySQL <= 8.0.33（含 keyring_file.so 插件）

```bash
# 1. 创建 keyring 卷
docker volume create waterfun-prod_mysql_keyring

# 2. 设置正确权限（MySQL 容器内 mysql 用户 UID=999）
docker run --rm \
  -v waterfun-prod_mysql_keyring:/var/lib/mysql-keyring \
  alpine sh -c "chown -R 999:999 /var/lib/mysql-keyring && chmod 750 /var/lib/mysql-keyring"

# 3. 启动 MySQL
docker compose -f deploy/docker/docker-compose.prod.yml up -d mysql

# 4. 验证 keyring 文件已生成
docker exec waterfun-mysql ls -la /var/lib/mysql-keyring/
# 应看到 keyring 文件（约 50 字节）
docker exec waterfun-mysql mysql -uroot -p'${DB_PASSWORD}' \
  -e "SELECT PLUGIN_NAME, PLUGIN_STATUS FROM INFORMATION_SCHEMA.PLUGINS WHERE PLUGIN_NAME LIKE '%keyring%';"
# 应看到 keyring_file | ACTIVE
```

### 情况 B：MySQL >= 8.0.34（keyring_file.so 插件被移除）

> **MySQL 8.0.34+ 发行版中 `keyring_file.so` 插件文件已被移除。** 但 `keyring_file.so` 仍存在于某些 Docker 镜像的 `/usr/lib64/mysql/plugin/` 目录中，可通过 `INSTALL PLUGIN` 手动激活。

```bash
# 1～3 步同上（创建卷、设权限、启动）

# 4. 手动安装 keyring 插件
docker exec waterfun-mysql mysql -uroot -p'${DB_PASSWORD}' \
  -e "INSTALL PLUGIN keyring_file SONAME 'keyring_file.so';"

# 5. 验证
docker exec waterfun-mysql ls -la /var/lib/mysql-keyring/
docker exec waterfun-mysql mysql -uroot -p'${DB_PASSWORD}' \
  -e "SELECT PLUGIN_NAME, PLUGIN_STATUS FROM INFORMATION_SCHEMA.PLUGINS WHERE PLUGIN_NAME LIKE '%keyring%';"
```

## 运维操作

### 验证 keyring 状态

```bash
# 检查 keyring 文件
docker exec waterfun-mysql ls -la /var/lib/mysql-keyring/

# 检查 keyring 插件状态
docker exec waterfun-mysql mysql -uroot -p'${DB_PASSWORD}' \
  -e "SELECT PLUGIN_NAME, PLUGIN_STATUS FROM INFORMATION_SCHEMA.PLUGINS WHERE PLUGIN_NAME LIKE '%keyring%';"

# 检查加密表空间
docker exec waterfun-mysql mysql -uroot -p'${DB_PASSWORD}' \
  -e "SELECT SPACE, NAME, ENCRYPTION FROM INFORMATION_SCHEMA.INNODB_TABLESPACES WHERE ENCRYPTION = 'Y';"
```

### 重建 keyring 卷

> ⚠️ 仅在 keyring 文件损坏或丢失时操作。**如果 `mysql_data` 卷中的表已加密，重建 keyring 卷会导致数据无法读取。**

```bash
docker compose -f deploy/docker/docker-compose.prod.yml down
docker volume rm waterfun-prod_mysql_keyring
docker volume create waterfun-prod_mysql_keyring
docker run --rm \
  -v waterfun-prod_mysql_keyring:/var/lib/mysql-keyring \
  alpine sh -c "chown -R 999:999 /var/lib/mysql-keyring && chmod 750 /var/lib/mysql-keyring"
docker compose -f deploy/docker/docker-compose.prod.yml up -d
# 新部署不需要重装插件（volumes 全新初始化）
# 如果从旧数据恢复，需要重跑 INSTALL PLUGIN
```

### 从旧数据恢复

迁移数据到新环境时，`mysql_data` 和 `mysql_keyring` **两个卷必须一起备份恢复**：

```bash
# 备份时
docker run --rm -v waterfun-prod_mysql_data:/data alpine tar czf /tmp/mysql_data.tar.gz -C /data .
docker run --rm -v waterfun-prod_mysql_keyring:/data alpine tar czf /tmp/mysql_keyring.tar.gz -C /data .

# 恢复时
docker run --rm -v waterfun-prod_mysql_data:/data alpine tar xzf /tmp/mysql_data.tar.gz -C /data
docker run --rm -v waterfun-prod_mysql_keyring:/data alpine tar xzf /tmp/mysql_keyring.tar.gz -C /data
# 同时恢复 keyring 文件后，不需要重装插件
```

## 常见问题

### Q：`Can't find master key from keyring`

```
Caused by: java.sql.SQLException: Can't find master key from keyring,
please check in the server log if a keyring is loaded and initialized successfully.
```

**原因**：`mysql_data` 卷中有加密表，但 `mysql_keyring` 卷的 keyring 文件缺失或不匹配。

**解决**：
1. 如果是新部署（无需恢复旧数据）：按上方"首次部署步骤"重新初始化两个卷即可
2. 如果是数据迁移：需要同时恢复 `mysql_data` 和 `mysql_keyring` 两个卷的备份

### Q：`daemon_keyring_proxy_plugin` 而不是 `keyring_file`

```
PLUGIN_NAME                 PLUGIN_STATUS
daemon_keyring_proxy_plugin ACTIVE
```

**原因**：`keyring_file.so` 插件未成功加载（MySQL 8.0.34+ 已移除自动加载，或 `keyring.cnf` 配置错误）。

**解决**：运行 `INSTALL PLUGIN keyring_file SONAME 'keyring_file.so';`

### Q：权限错误

```
Can't create keyring file. Please check if a keyring is loaded and initialized successfully.
```

**原因**：Docker named volume 初始 owner 是 root，MySQL 用户（UID 999）写不进去。

**解决**：创建卷后用 alpine 容器预设权限：
```bash
docker run --rm -v mysql_keyring:/var/lib/mysql-keyring \
  alpine sh -c "chown -R 999:999 /var/lib/mysql-keyring && chmod 750 /var/lib/mysql-keyring"
```

## MySQL 版本与 keyring 兼容性

| MySQL 版本 | keyring_file.so 可用 | 加载方式 |
|-----------|---------------------|---------|
| <= 8.0.33 | ✅ 内置 | `early-plugin-load=keyring_file.so` |
| 8.0.34 - 8.0.46 | ⚠️ 文件可能仍在插件目录 | `INSTALL PLUGIN keyring_file SONAME 'keyring_file.so'` |
| >= 8.4 | ❌ 彻底移除 | 需改用 Component（`INSTALL COMPONENT 'file://component_keyring_file'`） |
