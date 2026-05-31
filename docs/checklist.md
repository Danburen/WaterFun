# Java/JPA 开发常见问题 Checklist

> 基于实际踩坑经验整理，新项目或生成代码后逐项检查。

---

## 一、JPA / Hibernate

### 1.1 联合主键（@EmbeddedId + @MapsId）

- [ ] `@MapsId("xxx")` 上方的字段名**不能**叫 `xxx`
- [ ] 检查 `@EmbeddedId` 类中的字段名与实体类中 `@MapsId` 关联字段是否冲突
- [ ] 冲突时优先修改 `@ManyToOne` 关联实体字段名，不动 `@EmbeddedId`
- [ ] 示例：
  ```java
  // ❌ 冲突
  @MapsId("resourceUuid")
  private Resource resourceUuid;

  // ✅ 正确
  @MapsId("resourceUuid")
  private Resource resource;
  ```

### 1.2 字段类型与数据库映射

- [ ] `@Column(name = "xxx")` 与数据库实际列名一致
- [ ] 实体字段类型与数据库列类型匹配（`String` ↔ `varchar`，`Long` ↔ `bigint`）
- [ ] `@Size` / `@Length` 与数据库字段长度一致
- [ ] 枚举类型是否加了 `@Enumerated(EnumType.STRING)`（默认是 `ORDINAL`，数据库里是数字）

### 1.3 关联关系

- [ ] `@OneToMany` / `@ManyToOne` 的 `mappedBy` 指向对方实体中的字段名
- [ ] 双向关联时，两边都要设置（或配置 `orphanRemoval` / `cascade`）
- [ ] `fetch = FetchType.LAZY` 是否满足需求（默认 `@ManyToOne` 是 `EAGER`，注意 N+1）
- [ ] `@JoinColumn` 的 `name` 是**本表**的外键列名，`referencedColumnName` 是**对方表**的列名

### 1.4 审计字段

- [ ] `createdAt` / `updatedAt` 是否加了自动填充（`@CreationTimestamp` / `@UpdateTimestamp` 或 JPA Auditing）
- [ ] 不要依赖数据库 `DEFAULT`，实体层也要赋值（防止插入时 null）

### 1.5 保存/更新操作

- [ ] `save()` 前检查 `@EmbeddedId` 是否已设置
- [ ] 关联实体用 `getReference()`（只有 ID 时）或直接用内存中已有实体
- [ ] 不要只设 `id` 不设关联实体，也不要只设实体不设 `id`

---

## 二、参数校验（Validation）

### 2.1 DTO 与 Entity 分离

- [ ] Controller 入参必须是 DTO，**禁止**直接暴露 Entity
- [ ] Entity 上的 `@NotNull` 等约束仅作为数据库兜底，不依赖其返回错误给前端
- [ ] DTO 层校验失败返回 400，Entity 层校验泄露返回 500 并告警

### 2.2 异常处理

- [ ] `ConstraintViolationException` 处理器要区分 DTO 错误和 Entity 错误
- [ ] Entity 校验泄露时打印完整调用栈，方便定位问题代码
- [ ] 不要暴露内部字段名（`uuid`、`createdAt` 等）给前端

### 2.3 校验注解使用

- [ ] `@NotNull` 用于任何类型，`@NotBlank` 仅用于 `String`
- [ ] `@NotEmpty` 用于集合 / 字符串，不能用于其他类型
- [ ] 分组校验（`groups`）是否配置正确

---

## 三、日志与异常

- [ ] 参数校验失败用 `log.warn()` 或 `log.debug()`，不用 `log.info()`
- [ ] 系统内部错误用 `log.error()`，并打印完整堆栈
- [ ] 生产环境日志不要打印敏感信息（密码、token、手机号等）
- [ ] 异常信息返回给前端前做脱敏处理

---

## 四、安全

- [ ] 数据库字段名不要通过错误信息泄露给前端
- [ ] 接口返回的 JSON 不要包含内部字段（如 `id` 如果是自增主键，考虑用 UUID 对外暴露）
- [ ] SQL 注入检查：是否用了 JPQL / Criteria API，没有字符串拼接 SQL
- [ ] `@Column` 的 `insertable = false, updatable = false` 是否正确配置（防止重复映射）

---

## 五、性能

- [ ] 检查 N+1 问题：关联查询是否用了 `EntityGraph` 或 `JOIN FETCH`
- [ ] 大数据量查询是否加了分页（`Pageable`）
- [ ] 批量插入是否用了 `saveAll()` + 适当 `flush` 和 `clear`
- [ ] 索引：经常查询的字段是否加了数据库索引（`@Index` 或手动 DDL）

---

## 六、代码规范

- [ ] 实体类加 `@NoArgsConstructor`（JPA 需要）
- [ ] `@AllArgsConstructor` 谨慎使用（字段顺序变更后构造函数参数顺序会变）
- [ ] `@Builder` 和 `@NoArgsConstructor` 同时用时，加 `@AllArgsConstructor`
- [ ] `equals()` / `hashCode()` 用业务键或 ID，不要用 `@Data` 生成的全字段比较（懒加载问题）
- [ ] 日期类型统一用 `Instant` / `LocalDateTime`，不要混用 `Date`

---

## 七、测试

- [ ] 单元测试覆盖 DTO 校验边界（空值、超长、格式错误）
- [ ] 集成测试验证数据库约束（唯一索引、外键、级联删除）
- [ ] 并发测试：唯一约束在高并发下是否安全（如订单号生成）

---

> 最后更新：2026-05-31
> 有新坑随时补充。
> Analyzed by KIMI2.6.
