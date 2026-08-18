# 面料查询系统

基于 RuoYi-Vue 3.9.2 深度定制的面辅料档案与库存管理系统。后端采用 Spring Boot 4.0.6、Java 17、MyBatis 和 H2 文件数据库，前端采用 Vue 3、Vite 6 与 Element Plus。

当前仓库为可部署的干净架构版本：不包含实时数据库、上传图片、备份、日志、生产密码和临时公网地址；首次部署时由脚本创建空业务库。

## 核心功能

- 面料档案：分类、供应商、多行成分、报价换算、多图、年度编号。
- 辅料档案：独立供应商、图片、状态筛选及 `B-YYNNNN` 编号。
- 库存管理：库存台账、入库单、领用单、锁定、发料、退回、冲销和流水追溯。
- 账号权限：管理员、面辅料主管、录入员、查询员分级授权。
- 基础资料：面料分类、成分代码、面料供应商和辅料供应商。
- 单机运行：H2 嵌入式数据库，无需安装 MySQL、Redis。
- Windows 部署：Caddy 静态托管与反向代理、Cloudflare Tunnel、开机任务、看门狗和每日备份。

## 工程结构

| 目录 | 作用 |
|---|---|
| `ruoyi-admin` | Spring Boot 启动模块、Controller、配置和 H2 初始化 SQL |
| `ruoyi-common` | 通用工具和面辅料/库存实体 |
| `ruoyi-system` | Mapper、Service、业务规则与 MyBatis XML |
| `ruoyi-framework` | 安全、异常、限流和 Web 基础设施 |
| `ruoyi-quartz` | 定时任务模块 |
| `ruoyi-generator` | 代码生成模块 |
| `ruoyi-ui` | Vue 3 前端、页面和 API 封装 |
| `deploy/windows` | Windows 初始化、启动、隧道、备份和计划任务脚本 |
| `docs` | 使用说明和库存模块设计文档 |

## 主要数据关系

```text
面料档案 fabric
├─ fabric_composition       多行成分
├─ fabric_image             多张实物图
├─ fabric_supplier          面料供应商
└─ inventory_stock          一个档案可关联多个库存批次

辅料档案 fabric_accessory
├─ fabric_accessory_image
├─ fabric_accessory_supplier
└─ inventory_stock

inventory_stock
├─ inventory_inbound_detail       入库明细
├─ inventory_requisition_detail   领用明细
├─ inventory_stock_image          库存图片
└─ inventory_movement             不可直接修改的库存流水
```

被库存引用的面料、辅料和库存批次不能直接删除。已过账业务通过冲销、取消或退回形成反向流水，保留完整审计链。

## 本地开发

环境要求：JDK 17、Maven 3.9+、Node.js 20+、npm 10+。

```powershell
git clone https://github.com/SeaCat666/ruoyi-fabric-query.git D:\ruoyi-fabric-query
Set-Location D:\ruoyi-fabric-query

mvn clean install -DskipTests
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\windows\initialize-database.ps1

$env:FABRIC_DB_PATH = 'D:/ruoyi-fabric-query/ruoyi-admin/data/fabric'
java -jar .\ruoyi-admin\target\ruoyi-admin.jar --spring.profiles.active=druid
```

另开一个 PowerShell：

```powershell
Set-Location D:\ruoyi-fabric-query\ruoyi-ui
npm install
npm run dev
```

开发前端默认访问 `http://localhost:80`，后端为 `http://localhost:8080`。

新数据库初始账号均使用密码 `admin123`：

- `admin`：系统管理员
- `fabric_manager`：面辅料主管
- `fabric_entry`：面辅料录入员
- `fabric_view`：面辅料查询员

首次登录后必须立即修改管理员及测试账号密码。

## Windows 长期运行

完整步骤见 [deploy/windows/README.md](deploy/windows/README.md)。最短流程：

```powershell
Set-Location D:\ruoyi-fabric-query
mvn clean install -DskipTests
Set-Location .\ruoyi-ui
npm install
npm run build:prod
Set-Location ..

powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\windows\install-runtime-tools.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\windows\initialize-database.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\windows\start-public-stack.ps1
```

## 安全与仓库边界

以下内容通过 `.gitignore` 排除，不应提交到 GitHub：

- `ruoyi-admin/data`：实时 H2 数据库；
- `runtime`、`logs`、`backups`：运行日志、PID、临时公网地址与备份；
- `tools/*.exe`：第三方运行程序，由安装脚本下载；
- 上传文件和本机环境变量；
- CSV 测试数据与本机截图。

生产 JWT 密钥通过用户环境变量 `FABRIC_TOKEN_SECRET` 注入，禁止写入配置文件。

## 文档

- [开发部署与架构说明](docs/开发部署与架构说明.md)
- [库存管理模块设计](docs/库存管理模块设计.md)
- `docs\面料查询系统使用说明.docx`

## 开源说明

本项目基于 [RuoYi-Vue](https://github.com/yangzongzhuan/RuoYi-Vue) 定制，遵循仓库中的 MIT License。第三方组件与运行程序遵循各自许可证。
