# Opsbox Utility Plugin

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/opsbox-utility-plugin.svg)](https://plugins.jenkins.io/opsbox-utility-plugin/)
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.414+-blue.svg)](https://jenkins.io/)

Jenkins插件，提供两个实用功能：
- **Job Build Name Parameter**: 从其他Jenkins任务中选择构建名称作为参数
- **Git Branch Environment Variables**: 为Git分支参数自动添加环境变量

## 快速开始

### 安装
1. Jenkins → 管理Jenkins → 插件管理
2. 搜索 "Opsbox Utility Plugin" 并安装
3. 重启Jenkins

### 功能1: Job Build Name Parameter

在任务配置中添加参数：
```groovy
pipeline {
    agent any
    parameters {
        jobBuildNameParam(
            name: 'BUILD_NAME',
            jobName: 'upstream-job',
            countLimit: 5,
            description: '选择上游任务的构建名称'
        )
    }
    stages {
        stage('Deploy') {
            steps {
                echo "部署构建: ${params.BUILD_NAME}"
            }
        }
    }
}
```

**配置选项**:
- `name`: 参数名称
- `jobName`: 源任务名称（支持文件夹路径如 `folder/job`）
- `countLimit`: 显示的最大构建数量（默认5）
- `description`: 参数描述

### 功能2: Git Branch Environment Variables

配合 [List Git Branches Parameter](https://plugins.jenkins.io/list-git-branches-parameter/) 插件使用：

```groovy
pipeline {
    agent any
    parameters {
        listGitBranches(
            name: 'BRANCH',
            remoteURL: 'https://github.com/user/repo.git',
            credentialsId: 'git-credentials'
        )
    }
    stages {
        stage('Build') {
            steps {
                echo "分支: ${params.BRANCH}"
                echo "仓库: ${env.PARAMS__BRANCH__REMOTE_URL}"
                echo "凭证: ${env.PARAMS__BRANCH__CREDENTIALS_ID}"
            }
        }
    }
}
```

**自动生成的环境变量**:
- `PARAMS__{PARAM_NAME}__REMOTE_URL`: Git仓库URL
- `PARAMS__{PARAM_NAME}__CREDENTIALS_ID`: Git凭证ID
- `{PARAM_NAME}`: 清理后的分支名称

## 系统要求

- Jenkins 2.414+
- Java 11+
- List Git Branches Parameter Plugin（Git功能需要）

## 开发

```bash
# 构建
mvn clean package

# 测试
mvn test

# 开发模式运行
mvn hpi:run
```

## 常见问题

**Q: 如何处理文件夹中的任务？**
A: 使用完整路径，如 `folder1/folder2/job-name`

**Q: 看不到构建名称选项？**
A: 确保源任务存在且有成功的构建记录

**Q: 环境变量未设置？**
A: 确保安装了List Git Branches Parameter插件且配置正确

## 支持

- [GitHub Issues](https://github.com/jenkinsci/opsbox-utility-plugin/issues)
- [插件文档](https://plugins.jenkins.io/opsbox-utility-plugin/)

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

---

作者: **Seanly Liu** - [seanly.me@gmail.com](mailto:seanly.me@gmail.com) 