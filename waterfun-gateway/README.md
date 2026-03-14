# waterfun-gateway (minimal starter)

This module is the unified API entry for WaterFun.

## What this version includes

- Route forwarding to user/admin/notify services
- JWT authentication for non-public endpoints
- Public whitelist for login/register/refresh/health
- Simple in-memory auth rate limiting
- `X-Request-Id` tracing header

## Defaults

- Gateway port: `8080`
- User service: `http://localhost:8081`
- Admin service: `http://localhost:8082`
- Notify service: `http://localhost:8083`

Override with environment variables:

- `WATERFUN_GATEWAY_PORT`
- `WATERFUN_USER_SERVICE_URI`
- `WATERFUN_ADMIN_SERVICE_URI`
- `WATERFUN_NOTIFY_SERVICE_URI`
- `WATERFUN_JWT_SECRET`

## Notes

- Login/register are still owned by user service. Gateway only forwards `/api/auth/**`.
- The current rate limiter is intentionally simple for learning and local dev.
- For production, replace in-memory limiter with Redis-based limiter.

