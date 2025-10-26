# Jenkins Quick Commands

## Installation

### Install Jenkins on Ubuntu
```bash
# Add Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

# Install Jenkins
sudo apt-get update
sudo apt-get install jenkins -y

# Start Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins
sudo systemctl status jenkins
```

### Get Initial Admin Password
```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

## Jenkins CLI

### Download Jenkins CLI
```bash
wget http://localhost:8080/jnlpJars/jenkins-cli.jar
```

### Basic CLI Commands
```bash
# Build a job
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password build JOB_NAME

# List jobs
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password list-jobs

# Get job info
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password get-job JOB_NAME

# Create job from XML
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password create-job JOB_NAME < job-config.xml

# Delete job
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password delete-job JOB_NAME

# Console output
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password console JOB_NAME
```

## Service Management

```bash
# Start Jenkins
sudo systemctl start jenkins

# Stop Jenkins
sudo systemctl stop jenkins

# Restart Jenkins
sudo systemctl restart jenkins

# Check status
sudo systemctl status jenkins

# View logs
sudo journalctl -u jenkins -f

# Jenkins log location
sudo tail -f /var/log/jenkins/jenkins.log
```

## Configuration Files

```bash
# Jenkins home directory
cd /var/lib/jenkins

# Main configuration
sudo nano /var/lib/jenkins/config.xml

# Job configurations
cd /var/lib/jenkins/jobs/

# Plugins directory
cd /var/lib/jenkins/plugins/

# Backup Jenkins home
sudo tar -czf jenkins-backup-$(date +%Y%m%d).tar.gz /var/lib/jenkins/
```

## Plugin Management

```bash
# Install plugin via CLI
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password install-plugin PLUGIN_NAME

# List installed plugins
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password list-plugins

# Restart Jenkins safely
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password safe-restart
```

## Jenkinsfile Examples

### Basic Pipeline
```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
                sh 'mvn clean package'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing...'
                sh 'mvn test'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying...'
                sh './deploy.sh'
            }
        }
    }
}
```

### Docker Build Pipeline
```groovy
pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = "myapp:${BUILD_NUMBER}"
        REGISTRY = "docker.io/myrepo"
    }
    
    stages {
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${REGISTRY}/${DOCKER_IMAGE}")
                }
            }
        }
        stage('Push to Registry') {
            steps {
                script {
                    docker.withRegistry('https://docker.io', 'docker-credentials') {
                        docker.image("${REGISTRY}/${DOCKER_IMAGE}").push()
                    }
                }
            }
        }
    }
}
```

### Kubernetes Deploy Pipeline
```groovy
pipeline {
    agent any
    
    stages {
        stage('Deploy to K8s') {
            steps {
                script {
                    sh '''
                        kubectl apply -f deployment.yaml
                        kubectl rollout status deployment/myapp
                    '''
                }
            }
        }
    }
}
```

## Troubleshooting

```bash
# Check Jenkins port
sudo netstat -tulpn | grep 8080

# Change Jenkins port (edit config)
sudo nano /etc/default/jenkins
# Change HTTP_PORT=8080 to desired port

# Fix permission issues
sudo chown -R jenkins:jenkins /var/lib/jenkins
sudo chmod -R 755 /var/lib/jenkins

# Clear workspace
rm -rf /var/lib/jenkins/workspace/JOB_NAME/*

# Reload configuration from disk
java -jar jenkins-cli.jar -s http://localhost:8080/ -auth admin:password reload-configuration
```

## Useful API Calls

```bash
# Trigger build via API
curl -X POST http://localhost:8080/job/JOB_NAME/build --user admin:password

# Trigger build with parameters
curl -X POST http://localhost:8080/job/JOB_NAME/buildWithParameters \
  --user admin:password \
  --data param1=value1 --data param2=value2

# Get build status
curl http://localhost:8080/job/JOB_NAME/lastBuild/api/json --user admin:password

# Get queue info
curl http://localhost:8080/queue/api/json --user admin:password
```

## Security

```bash
# Generate API token (do this in Jenkins UI: User > Configure > API Token)

# Using API token instead of password
curl -X POST http://localhost:8080/job/JOB_NAME/build \
  --user username:API_TOKEN
```