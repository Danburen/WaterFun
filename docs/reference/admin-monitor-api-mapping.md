# 系统监控页 - API 对接映射

> 对应前端样例: `style/waterfun_admin_monitor.html`
> 基础 URL: `http://localhost:8082/admin`（开发直连，无需 JWT）或 `http://localhost:8080/admin`（经 Gateway，需 JWT）

---

## 前置：获取应用列表

```
GET /admin/applications
```

返回示例（概览卡片状态标签）:
```json
[
  {
    "name": "waterfun-admin-service",
    "status": "UP",
    "instances": [{ "id": "..." }]
  },
  {
    "name": "waterfun-service",
    "status": "UP",
    "instances": [{ "id": "..." }]
  }
]
```

`{id}` 取 `instances[0].id`，后续所有 `/admin/applications/{id}/...` 使用此 id。

---

## Monitor Cards

| HTML 卡片 | 数据字段 | 端点 |
|---|---|---|
| CPU 使用率 | 百分比 | `GET /admin/applications/{id}/metrics/system.cpu.usage` |
| CPU 核心数 | count | `GET /admin/applications/{id}/metrics/system.cpu.count` |
| CPU 负载 | load avg | `GET /admin/applications/{id}/metrics/system.load.average.1m` |
| 内存使用 | 已用/总量 | `GET /admin/applications/{id}/metrics/system.memory.used` + `system.memory.total` |
| JVM 堆内存 | 已用/最大 | `GET /admin/applications/{id}/metrics/jvm.memory.used?tag.area=heap` + `jvm.memory.max?tag.area=heap` |
| JVM 状态标签 | 阈值判断 | 堆使用率 > 80% → 偏高（状态 → warning），> 90% → 危险 |

---

## 资源使用趋势

| 按钮 | 数据字段 | 端点 |
|---|---|---|
| CPU | 历史 CPU 使用率 | `GET /admin/applications/{id}/metrics/system.cpu.usage`（定时轮询，保留最近 N 条） |
| 内存 | 历史内存使用 | `GET /admin/applications/{id}/metrics/jvm.memory.used`（定时轮询） |
| JVM | 历史堆使用 | `GET /admin/applications/{id}/metrics/jvm.memory.used?tag.area=heap`（定时轮询） |

> 注：SBA 不提供历史趋势聚合，前端需每隔 3-5s 轮询 metrics，本地存时间序列绘制折线图。

---

## JVM 详情

| HTML 行 | 端点 |
|---|---|
| Java 版本 | `GET /admin/applications/{id}/env/java.version` |
| JVM 名称 | `GET /admin/applications/{id}/env/java.vm.name` |
| 运行时间 | `GET /admin/applications/{id}/metrics/process.uptime`（返回值转 `Xd Xh Xm` 格式） |
| 活跃线程 | `GET /admin/applications/{id}/metrics/jvm.threads.live` / `jvm.threads.daemon` |
| GC 次数 | `GET /admin/applications/{id}/metrics/jvm.gc.pause`（取 `count` 字段） |
| GC 耗时 | `GET /admin/applications/{id}/metrics/jvm.gc.pause`（取 `totalTime`、`max` 字段） |
| GC Young/Full | 需 `jvm.gc.memory.promoted` 等指标估算，或直接取 `jvm.gc.pause` 按 `tag.cause` 区分 |

---

## 磁盘使用

| HTML 行 | 端点 |
|---|---|
| 各分区信息 | `GET /admin/applications/{id}/health`（`components.diskSpace.details` 含 total/free/threshold）|
| 或直接通过 metrics | `GET /admin/applications/{id}/metrics/disk.free` + `disk.total` |

> `health` 端点的 `diskSpace` 只暴露一个分区。如果需要多分区，应使用 `metrics` 端点。

---

## 网络 IO

| HTML 行 | 端点 |
|---|---|
| 接收/发送速率 | `GET /admin/applications/{id}/metrics/network.rx.bytes` + `network.tx.bytes`（需定时采样计算速率差值） |
| 总收/总发 | 同上，取绝对值（累计值） |
| 连接数 | OS 级别指标，actuator 默认不暴露，可借助自定义 `ServerProperties` 或 `Micrometer` 补充 |

---

## 系统日志

| HTML 行 | 端点 |
|---|---|
| 日志流 | `GET /admin/applications/{id}/logfile`（返回完整日志文本，前端需自行切割行 + 解析 level） |

> SBA 的 logfile 端点直接返回原始文本。前端需要做:
> - 按行分割
> - 用正则提取时间/级别/消息
> - 定时轮询增量（可用 `Range` 头或 `?offset=` 参数，视 actuator 配置）

---

## 简化方案：自定义聚合接口

如果觉得前端逐一调 SBA API 太麻烦，可以在 admin-service 中新增一个聚合端点：

```
GET /api/admin/monitor/summary
```

一次返回所有指标，前端只刷这一个接口。数据结构可设计为：

```json
{
  "applications": [
    {
      "name": "waterfun-service",
      "status": "UP",
      "cpu": 23.5,
      "memoryUsed": "4.2 GB",
      "memoryTotal": "16 GB",
      "jvmUsed": 512,
      "jvmMax": 1024,
      "diskFree": "380 GB",
      "diskTotal": "500 GB",
      "uptime": "3d 12h 34m",
      "threads": 42,
      "gcCount": 23,
      "gcTime": "2.34s"
    }
  ]
}
```

这样可以避免前端直接依赖 SBA 的内部 API 结构，也方便后续替换监控数据源（如 Prometheus）。
