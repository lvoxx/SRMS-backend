// Build and Docker Pipeline
def call(serviceName, serviceConfig, globalConfig) {
    pipeline {
        agent any
        stages {
            stage('Checkout') { gitUtils.checkoutRepo() }
            stage('Build & Docker') { buildUtils.buildService(serviceConfig, globalConfig) }
        }
        post { always { notificationUtils.notifyBuildStatus(globalConfig, currentBuild.result) } }
    }
}