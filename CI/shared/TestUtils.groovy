// Test Utilities
def runModuleTest(moduleConfig, config) {
    stage('Test') {
        dir(moduleConfig.path) {
            sh moduleConfig.testCmd
        }
    }
    // Future: Add SonarQube
    if (config.tools.sonar) {
        stage('Sonar Scan') {
            withSonarQubeEnv('SonarServer') {
                sh "${config.tools.sonar} -DprojectKey=${moduleConfig.path}"
            }
        }
    }
}