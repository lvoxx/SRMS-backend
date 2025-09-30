// Full CI Pipeline
def call(serviceName, serviceConfig, globalConfig) {
    pipeline {
        agent any
        stages {
            stage('Checkout') { gitUtils.checkoutRepo() }
            stage('Full Test') { script { fullTestPipeline(serviceName, serviceConfig, globalConfig) } }
            stage('Build & Docker') { buildUtils.buildService(serviceConfig, globalConfig) }
            stage('Deploy') { // Placeholder for deploy (e.g., Kubernetes)
                echo 'Deploy to env'
            }
        }
        post { always { notificationUtils.notifyBuildStatus(globalConfig, currentBuild.result) } }
    }
}