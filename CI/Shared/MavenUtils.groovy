// CI/Shared/MavenUtils.groovy
// Maven build and test wrappers

def buildModule(String modulePath = '.') {
    sh "mvn -B -Dmaven.repo.local=.m2 clean package -DskipTests -pl ${modulePath} -am"
}

def runTests() {
    sh 'mvn -B -Dmaven.repo.local=.m2 clean test'
}

def archiveArtifacts(String moduleName) {
    archiveArtifacts artifacts: "${modulePath}/target/*.jar", fingerprint: true
}