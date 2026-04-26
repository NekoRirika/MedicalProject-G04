# 胸有成影部署指南
## 系统架构

```
the-medical-project-g04/
├── src/
│   ├── ai/                    # AI模型训练模块
│   ├── backend/               # Spring Boot后端服务
│   └── frontend/              # Vue 3前端应用
```

## 环境要求

### 基础环境
- **操作系统**: Windows/Linux/macOS
- **Java**: JDK 17+
- **Node.js**: v18+ 
- **Python**: 3.9+
- **MySQL**: 8.0+
- **Redis**: 6.0+ 
- **Kafka**: 4.2+
- **MinIO**: 7.1.1+

### 硬件要求
- **开发环境**: 8GB RAM, 50GB 磁盘空间
- **AI模型训练**: 建议使用GPU 
  - 最低: 4GB GPU显存
  - 推荐: 8GB+ GPU显存

## 部署步骤

### 一、数据库部署

1. **安装MySQL 8.0+**
   - 下载: https://dev.mysql.com/downloads/mysql/
   - 安装过程中设置root密码

2. **创建数据库**
   ```bash
   cd src/backend
   mysql -u root -p < init.sql
   ```

3. **初始化数据**
   ```bash
   mysql -u root -p chest_imaging < init_model.sql < insert_users.sql < insert_medical_case.sql < create_indexes.sql
   ```

### 二、Redis部署

1. **安装Redis**
   - Windows: 下载 https://github.com/microsoftarchive/redis/releases 中的 `Redis-x64-3.2.100.msi`(安装时注意你的6379端口是否有没有被其他应用占用)
   - Linux: `sudo apt install redis-server` 或 `sudo yum install redis`

2. **启动Redis服务**
   ```bash
   redis-server redis.windows.conf
   ```

3. **验证安装**
   ```bash
   redis-cli ping
   # 应返回 PONG
   ```

4. **配置连接**
   
   编辑 `src/backend/src/main/resources/application.properties`:
   ```properties
   spring.data.redis.host=localhost
   spring.data.redis.port=6380（如果说你的Redis端口是6380，就修改这里端口）
   ```

### 三、Kafka部署

1. **下载Kafka**
   - 访问: https://kafka.apache.org/downloads
   - 下载二进制版本: 最新的Kafka版本
   

2. **解压到本地**


3. **启动Kafka**
   打开**新的PowerShell窗口**：
   ```powershell
   cd D:\kafka
   .\bin\windows\kafka-server-start.bat .\config\server.properties
   具体问AI 不同版本有差异 我的是以上这样
   ```
   **保持此窗口打开**，Kafka会持续运行。



6. **配置连接**
   
   编辑 `src/backend/src/main/resources/application.properties`:
   ```properties
   spring.kafka.bootstrap-servers=localhost:9092
   ```

### 四、MinIO部署

1. **下载MinIO**
   ```powershell
   # 创建MinIO目录
   mkdir D:\minio
   cd D:\minio
   
   # 下载MinIO服务器（Windows版本）
   Invoke-WebRequest -Uri "https://dl.min.io/server/minio/release/windows-amd64/minio.exe" -OutFile "minio.exe"
   ```

2. **创建数据存储目录**
   ```powershell
   mkdir D:\minio\data
   ```

3. **启动MinIO**
   ```powershell
   cd D:\minio
   .\minio.exe server D:\minio\data --console-address ":9001"
   ```

4. **访问MinIO控制台**（可选）
   - 浏览器打开: http://localhost:9001
   - 用户名: `minioadmin`
   - 密码: `minioadmin`

5. **验证MinIO**
   - 浏览器访问: http://localhost:9000/minio/health/live
   - 返回 `OK` 表示正常

6. **配置连接**
   
   编辑 `src/backend/src/main/resources/application.properties`:
   ```properties
   minio.endpoint=http://localhost:9000
   minio.access-key=minioadmin
   minio.secret-key=minioadmin
   minio.bucket-name=medical-images
   ```

### 五、后端服务部署

1. **配置数据库连接**
   
   编辑 `src/backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/chest_imaging?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=你的数据库密码
   ```

2. **安装依赖**
   ```bash
   cd src/backend
   mvn clean install
   ```

3. **运行后端服务**
   ```bash
   mvn spring-boot:run
   ```

4. **验证服务**
   - 访问: http://localhost:8080/
   - 默认端口: 8080
   - API前缀: /api
   - 健康检查: http://localhost:8080/api/actuator/health
   - 系统状态: http://localhost:8080/api/system/health

### 六、AI模型部署

1. **安装Python依赖**
   ```bash
   cd src/ai
   pip install -r requirements.txt
   ```

2. **模型加载**
   - 把ai开发师给的model文件夹保存在 `src/ai/` 目录
   - 在 `src/ai/model/model/` 中有两个模型文件
   - 系统启动时会自动加载模型到内存
   - 模型文件格式: `.pth和.pt`

3. **启动AI推理服务**
   ```bash
   cd src/ai
   python ai_service.py
   ```

4. **验证服务**
   - 访问: http://localhost:8000/docs
   - 默认端口: 8000

5. **启用AI服务**
   
   编辑 `src/backend/src/main/resources/application.properties`:
   ```properties
   ai.service.enabled=true
   ```

### 七、前端应用部署

1. **安装依赖**
   ```bash
   cd src/frontend
   npm install
   ```

2. **配置API地址**
   
   编辑 `.env.development`:
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api
   ```
   
   如果后端部署在远程服务器:
   ```env
   VITE_API_BASE_URL=https://your-server-domain.com/api
   ```

3. **开发模式运行**
   ```bash
   npm run dev
   ```
   
   访问: http://localhost:5173

4. **生产环境构建**
   ```bash
   npm run build
   npm run preview
   ```

## 性能监控

### Spring Boot Actuator

应用已集成Spring Boot Actuator，提供以下端点：

- **健康检查**: http://localhost:8080/api/actuator/health
- **指标查询**: http://localhost:8080/api/actuator/metrics
- **Prometheus指标**: http://localhost:8080/api/actuator/prometheus

### Prometheus + Grafana（可选）

1. **安装Prometheus**
   - 下载: https://prometheus.io/download/
   - 配置 `prometheus.yml`:
     ```yaml
     scrape_configs:
       - job_name: 'spring-boot'
         metrics_path: '/api/actuator/prometheus'
         static_configs:
           - targets: ['localhost:8080']
     ```

2. **安装Grafana**
   - 下载: https://grafana.com/grafana/download
   - 导入Spring Boot仪表盘（Dashboard ID: 11378）

## 常见问题

### 1. 数据库连接失败
- 检查MySQL服务是否启动
- 验证用户名密码是否正确
- 确认数据库 `chest_imaging` 是否已创建

### 2. 端口占用
- 修改后端端口: `server.port=8080`
- 修改前端端口: `vite.config.js` 中配置 `server.port`
- Kafka默认端口: 9092
- MinIO默认端口: 9000（API）、9001（控制台）

### 3. AI模型加载失败
- 检查PyTorch是否正确安装
- 验证CUDA版本与PyTorch兼容性
- 确认模型文件路径正确

### 4. DICOM文件
- 确认文件格式为DICOM 3.0
- 有从kaggle官网上下载的DCM文件的话放在uploads目录下
- 路径为'src/backend/uploads/stage_2_test_images/'

### 5. Kafka连接失败
- 确保Zookeeper已启动（可能不需要zookeeper）
- 确保Kafka已启动
- 检查 `bootstrap.servers` 配置是否正确
- Windows防火墙是否拦截9092端口

### 6. MinIO连接失败
- 确保MinIO服务已启动
- 检查endpoint、access-key、secret-key配置
- 浏览器访问 http://localhost:9001 确认控制台可访问
- Windows防火墙是否拦截9000/9001端口

### 7. Kafka主题未创建
应用启动时会自动创建以下主题：
- `detection-request-topic` (3分区)
- `detection-result-topic` (3分区)
- `detection-dead-letter-topic` (1分区)

如果自动创建失败，可手动创建：
```powershell
cd D:\kafka
.\bin\windows\kafka-topics.bat --create --topic detection-request-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic detection-result-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic detection-dead-letter-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

## 快速启动脚本（Windows）

创建 `start-services.bat` 文件：

```batch
@echo off
echo Starting Zookeeper...
start "Zookeeper" cmd /k "cd D:\kafka && .\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties"

timeout /t 5

echo Starting Kafka...
start "Kafka" cmd /k "cd D:\kafka && .\bin\windows\kafka-server-start.bat .\config\server.properties"

timeout /t 5

echo Starting MinIO...
start "MinIO" cmd /k "cd D:\minio && .\minio.exe server D:\minio\data --console-address :9001"

timeout /t 5

echo Starting Redis...
start "Redis" cmd /k "redis-server"

timeout /t 3

echo All services started!
pause
```

## 部署验证清单

- [ ] MySQL服务正常运行
- [ ] 数据库 `chest_imaging` 已创建
- [ ] 所有表结构已创建
- [ ] Redis服务正常运行 (`redis-cli ping` 返回 PONG)
- [ ] Zookeeper服务正常运行
- [ ] Kafka服务正常运行 (localhost:9092)
- [ ] MinIO服务正常运行 (localhost:9000)
- [ ] MinIO控制台可访问 (localhost:9001)
- [ ] 后端服务可访问 (http://localhost:8080/api)
- [ ] 健康检查正常 (http://localhost:8080/api/actuator/health)
- [ ] 前端应用可访问 (http://localhost:5173)
- [ ] AI模型已加载到内存
- [ ] AI推理服务已启动 (http://localhost:8000/docs)
- [ ] 可以成功上传DICOM文件
- [ ] 可以发起AI检测请求
- [ ] Kafka消息队列正常工作
- [ ] MinIO文件存储正常工作
