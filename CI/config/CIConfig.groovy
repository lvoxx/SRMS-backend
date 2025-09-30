// Global CI/CD Configuration
def jenkinsTools = [
    maven: 'Maven3',  // Tool name in Jenkins
    jdk: 'JDK17',
    python: 'Python3',
    go: 'Go1.21',
    sonar: 'SonarQubeScanner'  // For future integration
]

def credentials = [
    github: 'github-token',
    dockerRegistry: 'docker-hub-cred',
    sonar: 'sonar-token',
    slack: 'slack-webhook'
]

def dockerRegistry = 'your-docker-repo/srms'  // e.g., docker.io/lvoxx/srms
def notificationChannels = ['email', 'slack']  // Configurable

// Export as map for import
[
    tools: jenkinsTools,
    creds: credentials,
    docker: dockerRegistry,
    notify: notificationChannels
]