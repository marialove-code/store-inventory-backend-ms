# 域名切到微服务（nestparts.top）

目标：妈妈打开 `https://nestparts.top` 与以前一样用，后端走 Gateway `9080`。

```text
浏览器 → Nginx:443（nestparts.top）
         ├─ 静态页 → /www/wwwroot/inventory-ms-front
         ├─ /api/*    → 127.0.0.1:9080（Gateway）
         └─ /upload/* → 127.0.0.1:9080（Gateway → platform）
```

前端生产构建已使用 `VITE_API_BASE_URL=/api`（同源），**不必**写死 IP。

---

## 1. 本机打包前端

```bash
cd E:\Projects\store-inventory-front-ms
npm run build
```

产物在 `dist/`。上传 **dist 内全部文件** 到服务器：

```text
/www/wwwroot/inventory-ms-front/
```

（应能看到 `index.html`、`assets/` 等。）

---

## 2. 宝塔改网站（nestparts.top）

宝塔 → 网站 → `nestparts.top` → **设置** → **网站目录**：

- 运行目录 / 网站目录改为：`/www/wwwroot/inventory-ms-front`

再打开 **配置文件**，在 `server { ... }` 里保证类似下面（按你现有 SSL 段落保留证书路径，只改 root 与反代）：

```nginx
server {
    listen 80;
    listen 443 ssl http2;
    server_name nestparts.top www.nestparts.top;

    # SSL 证书行保持宝塔原有配置，不要删

    root /www/wwwroot/inventory-ms-front;
    index index.html;

    # 前端 SPA
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 微服务 Gateway
    location /api/ {
        proxy_pass http://127.0.0.1:9080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }

    # 上传/静态图片（经 Gateway → platform）
    location /upload/ {
        proxy_pass http://127.0.0.1:9080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 20m;
    }
}
```

保存后：**重载 Nginx**（宝塔点重载配置）。

> 若站点里还有旧的 `proxy_pass` 指单体 jar（如 8080），删掉或改成上面的 9080。

---

## 3. 验收

1. 打开 `https://nestparts.top` → 登录页  
2. 用原来的账号登录  
3. F12 Network：登录请求应是 `https://nestparts.top/api/...`，状态 200  
4. 门店相关页面能打开  

失败时：

```bash
ss -lntp | grep 9080
tail -n 50 /www/wwwroot/inventory-ms-backend/logs/gateway-service.log
tail -n 50 /www/wwwroot/inventory-ms-backend/logs/platform-service.log
```

---

## 4. 回滚（万一不行）

网站目录改回原来的 `/www/wwwroot/inventory-front`（或你单体前端目录），Nginx 反代改回单体端口，重载即可。
