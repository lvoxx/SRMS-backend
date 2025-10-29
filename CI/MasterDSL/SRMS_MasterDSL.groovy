// CI/MasterDSL/SRMS-MasterDSL.groovy
// Seed job – ONLY ONE file executed by Jenkins

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

// ----------------------------------------------------------
// 1. Checkout repository (đảm bảo CI/ tồn tại trong workspace)
// ----------------------------------------------------------
def repoUrl = 'https://github.com/lvoxx/SRMS-backend.git'
def branch   = 'main'   // có thể param nếu muốn

checkout([
    $class: 'GitSCM',
    branches: [[name: "*/${branch}"]],
    userRemoteConfigs: [[
        url: repoUrl,
        credentialsId: 'github-token'
    ]],
    extensions: [
        [$class: 'CleanCheckout'],
        [$class: 'RelativeTargetDirectory', relativeTargetDir: '']
    ]
])

// ----------------------------------------------------------
// 2. Helper: load *.groovy from a folder inside the repo
// ----------------------------------------------------------
def loadJobsFrom(String relativePath) {
    def dir = new File("${WORKSPACE}/${relativePath}")
    if (!dir.isDirectory()) {
        println "WARNING: Directory not found: ${relativePath}"
        return
    }

    dir.eachFileMatch(~/.*\.groovy$/) { file ->
        println "Loading job DSL: ${relativePath}/${file.name}"
        // Dùng readFileFromWorkspace để tránh phụ thuộc filesystem
        def script = readFileFromWorkspace("${relativePath}/${file.name}")
        new DslScriptLoader(new JenkinsJobManagement(System.out, [:], new File('.'))).runScript(script)
    }
}

// ----------------------------------------------------------
// 3. Load **shared** utilities (đảm bảo chúng có trong context)
// ----------------------------------------------------------
def sharedPath = 'CI/Shared'
[
    'CommonEnv.groovy',
    'GitUtils.groovy',
    'MavenUtils.groovy',
    'DockerUtils.groovy',
    'BaseJobTemplate.groovy'
].each { file ->
    def script = readFileFromWorkspace("${sharedPath}/${file}")
    evaluate(script)                     // đưa hàm vào binding hiện tại
}

// ----------------------------------------------------------
// 4. Create folder hierarchy
// ----------------------------------------------------------
folder('SRMS') {
    description 'Root folder for SRMS project'
}
folder('SRMS/SpringServices') {
    description 'CI/CD pipelines for Spring Boot microservices'
}

// ----------------------------------------------------------
// 5. Load Spring jobs (và các ngôn ngữ khác sau này)
// ----------------------------------------------------------
loadJobsFrom('CI/Spring/Jobs')

// Future:
// loadJobsFrom('CI/Python/Jobs')
// loadJobsFrom('CI/Go/Jobs')

println "Master DSL execution completed."