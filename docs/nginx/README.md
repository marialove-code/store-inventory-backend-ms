# Nginx 域名配置（nestparts.top）

| 文件 | 用途 |
|------|------|
| `nginx-nestparts-monolith.conf` | 单体时代全文备份 + 注释（有用/需改/单体专用） |
| `nginx-nestparts-ms.conf` | 微服务版本：前端 `inventory-ms-front` + API → Gateway `9080` |

## 切到微服务时最少改两处

1. `root` → `/www/wwwroot/inventory-ms-front`
2. `location /api` 的 `proxy_pass` → `http://127.0.0.1:9080`

`/upload/` 继续 `alias /opt/upload/;` 即可（与单体相同）。

改完后重载 Nginx，并确认 Gateway 已监听 9080。
