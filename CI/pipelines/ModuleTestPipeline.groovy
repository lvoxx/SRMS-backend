// Module Test Pipeline Template
def call(moduleName, moduleConfig, globalConfig) {
    pipeline {
        agent any
        stages {
            stage('Checkout') { gitUtils.checkoutRepo() }
            stage('Test Module') { testUtils.runModuleTest(moduleConfig, globalConfig) }
        }
        post {
            always { notificationUtils.notifyBuildStatus(globalConfig, currentBuild.result) }
        }
    }
}