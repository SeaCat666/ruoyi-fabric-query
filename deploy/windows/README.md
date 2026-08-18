# Windows 部署说明

本文用于在另一台 Windows 电脑上，从 GitHub 拉取代码并运行完整的面辅料档案与库存管理系统。示例目录为 `D:\ruoyi-fabric-query`，换到其他盘符也可以，脚本会自动识别项目目录。

## 一、准备环境

安装以下软件并加入 `PATH`：

- Git
- Java 17
- Maven 3.9+
- Node.js 20+

确认版本：

```powershell
git --version
java -version
mvn -version
node --version
npm --version
```

## 二、拉取与构建

```powershell
Set-Location D:\
git clone https://github.com/SeaCat666/ruoyi-fabric-query.git
Set-Location D:\ruoyi-fabric-query

mvn clean install -DskipTests

Set-Location D:\ruoyi-fabric-query\ruoyi-ui
npm install
npm run build:prod
```

## 三、首次初始化

生成并保存 JWT 密钥。此值只保存在电脑环境变量中，不要写进 Git：

```powershell
$tokenBytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($tokenBytes)
$tokenSecret = [Convert]::ToBase64String($tokenBytes)
[Environment]::SetEnvironmentVariable('FABRIC_TOKEN_SECRET', $tokenSecret, 'User')
```

可选环境变量：

```powershell
[Environment]::SetEnvironmentVariable('FABRIC_UPLOAD_PATH', 'D:\ruoyi\uploadPath', 'User')
[Environment]::SetEnvironmentVariable('FABRIC_QUICK_TUNNEL_ENABLED', 'true', 'User')
```

重新打开 PowerShell，让用户环境变量生效。然后安装运行工具并初始化空数据库：

```powershell
Set-Location D:\ruoyi-fabric-query
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\install-runtime-tools.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\initialize-database.ps1"
```

初始化脚本只在数据库不存在时执行，不会覆盖已有数据。数据库默认位于 `D:\ruoyi-fabric-query\ruoyi-admin\data\fabric.mv.db`，上传文件默认位于 `D:\ruoyi\uploadPath`。

## 四、启动与访问

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\start-public-stack.ps1"
```

- 本机入口：`http://127.0.0.1:8088`
- 后端接口仅监听：`127.0.0.1:8080`
- 临时公网地址：运行以下命令查看

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\show-public-url.ps1"
```

Quick Tunnel 地址在进程或电脑重启后可能变化。当前地址会保存到 `D:\ruoyi-fabric-query\runtime\quick-tunnel-url.txt`。

首次初始化的管理员账号为 `admin`，初始密码为 `admin123`。第一次登录后应立即修改密码，并为实际员工分别创建账号和分配角色。

停止服务：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\stop-public-stack.ps1"
```

## 五、设置开机和定时任务

使用“以管理员身份运行”的 PowerShell 执行：

```powershell
Set-Location D:\ruoyi-fabric-query
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\install-scheduled-tasks.ps1"
```

脚本创建三个任务：

- `\FabricQuery\StartProduction`：登录 Windows 后启动系统
- `\FabricQuery\WatchProduction`：每 5 分钟检查并恢复异常进程
- `\FabricQuery\DailyBackup`：每天 02:00 备份数据库和上传文件

运行日志位于 `D:\ruoyi-fabric-query\runtime\logs`，备份位于 `D:\ruoyi-fabric-query\backups`。这些运行数据均被 Git 忽略，不会上传到 GitHub。

手工备份：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\backup-data.ps1"
```

建议定期把整个 `backups` 目录复制到另一块硬盘或公司网盘，防止电脑硬盘损坏。

## 六、更新代码

更新前先备份并停止服务：

```powershell
Set-Location D:\ruoyi-fabric-query
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\backup-data.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\stop-public-stack.ps1"
git pull
mvn clean install -DskipTests

Set-Location .\ruoyi-ui
npm install
npm run build:prod

Set-Location ..
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\deploy\windows\start-public-stack.ps1"
```

不要使用 `npm run dev` 作为长期运行方式。正式运行由 Caddy 提供前端静态文件，并把 `/prod-api` 转发给 Spring Boot。

## 七、数据边界

当前使用 H2 嵌入式文件数据库，适合公司内部少量人员、以查询和普通录入为主的单机部署。必须保证同一份生产数据库同时只由一个后端实例连接。若以后需要多台服务器或出现高频并发写入，再迁移到 PostgreSQL 或 MySQL。

Git 仓库只包含源码、空库初始化 SQL、部署脚本和文档，不包含生产数据库、上传图片、备份、日志、运行密钥或员工密码。
