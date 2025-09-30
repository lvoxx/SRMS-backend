pipeline {
    agent any
    triggers {
        githubPush()
    }
    stages {
        stage('Trigger Seed Job') {
            steps {
                checkout scm
                build job: 'SRMS/SeedJob', wait: false  // Trigger Seed Job asynchronously
            }
        }
    }
}