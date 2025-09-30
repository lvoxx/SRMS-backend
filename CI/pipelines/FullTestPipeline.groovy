// Full Test Pipeline
def call(serviceName, serviceConfig, globalConfig) {
    pipeline {
        agent any
        stages {
            stage('Checkout') { gitUtils.checkoutRepo() }
            stage('Full Test') {
                parallel {
                    serviceConfig.modules.each { moduleName, moduleConfig ->
                        stage(moduleName) { testUtils.runModuleTest(moduleConfig, globalConfig) }
                    }
                }
            }
        }
        post { always { notificationUtils.notifyBuildStatus(globalConfig, currentBuild.result) } }
    }
}