// Build Utilities
def buildService(serviceConfig, config) {
    stage('Build') {
        sh serviceConfig.buildCmd
    }
    if (serviceConfig.dockerBuild) {
        stage('Docker Build & Push') {
            docker.build("${config.docker}:${env.BUILD_ID}", serviceConfig.modules.collect { it.value.dockerContext }.join(' '))
            docker.withRegistry('https://index.docker.io/v1/', config.creds.dockerRegistry) {
                docker.image("${config.docker}:${env.BUILD_ID}").push()
            }
        }
    }
}