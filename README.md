# Opsbox Utility Plugin

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/opsbox-utility-plugin.svg)](https://plugins.jenkins.io/opsbox-utility-plugin/)
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.414+-blue.svg)](https://jenkins.io/)

A Jenkins plugin providing two utility features:
- **Job Build Name Parameter**: Select build names from other Jenkins jobs as parameters
- **Git Branch Environment Variables**: Automatically add environment variables for Git branch parameters

## Quick Start

### Installation
1. Jenkins → Manage Jenkins → Plugin Manager
2. Search for "Opsbox Utility Plugin" and install
3. Restart Jenkins

### Feature 1: Job Build Name Parameter

Add parameter to job configuration:
```groovy
pipeline {
    agent any
    parameters {
        jobBuildNameParam(
            name: 'BUILD_NAME',
            jobName: 'upstream-job',
            countLimit: 5,
            description: 'Select build name from upstream job'
        )
    }
    stages {
        stage('Deploy') {
            steps {
                echo "Deploying build: ${params.BUILD_NAME}"
            }
        }
    }
}
```

**Configuration Options**:
- `name`: Parameter name
- `jobName`: Source job name (supports folder paths like `folder/job`)
- `countLimit`: Maximum number of builds to show (default: 5)
- `description`: Parameter description

### Feature 2: Git Branch Environment Variables

Works with [List Git Branches Parameter](https://plugins.jenkins.io/list-git-branches-parameter/) plugin:

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
                echo "Branch: ${params.BRANCH}"
                echo "Repository: ${env.PARAMS__BRANCH__REMOTE_URL}"
                echo "Credentials: ${env.PARAMS__BRANCH__CREDENTIALS_ID}"
            }
        }
    }
}
```

**Auto-generated Environment Variables**:
- `PARAMS__{PARAM_NAME}__REMOTE_URL`: Git repository URL
- `PARAMS__{PARAM_NAME}__CREDENTIALS_ID`: Git credentials ID
- `{PARAM_NAME}`: Clean branch name

## Requirements

- Jenkins 2.414+
- Java 11+
- List Git Branches Parameter Plugin (for Git features)

## Development

```bash
# Build
mvn clean package

# Test
mvn test

# Run in development mode
mvn hpi:run
```

## FAQ

**Q: How to handle jobs in folders?**
A: Use full path, e.g., `folder1/folder2/job-name`

**Q: Can't see build name options?**
A: Ensure source job exists and has successful build records

**Q: Environment variables not set?**
A: Make sure List Git Branches Parameter plugin is installed and configured correctly

## Support

- [GitHub Issues](https://github.com/jenkinsci/opsbox-utility-plugin/issues)
- [Plugin Documentation](https://plugins.jenkins.io/opsbox-utility-plugin/)

## License

MIT License - see [LICENSE](LICENSE) file for details

---

Author: **Seanly Liu** - [seanly.me@gmail.com](mailto:seanly.me@gmail.com) 