// CI/Shared/DockerUtils.groovy
// Docker login, build, push

def login() {
    withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
        sh 'echo "$PASS" | docker login -u "$USER" --password-stdin'
    }
}

def buildAndPush(String serviceName, String dockerfilePath = '.') {
    def image = "${env.DOCKERHUB_USER}/${serviceName.toLowerCase()}:latest"
    dir(dockerfilePath) {
        sh "docker build -t ${image} ."
        sh "docker push ${image}"
    }
}