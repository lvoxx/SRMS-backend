// CI/MasterDSL/SRMS-MasterDSL.groovy
// Job DSL Seed Job – Final working version

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

// === 1. Load SharedJobDSL class (NOT evaluate!) ===
def sharedScript = readFileFromWorkspace('CI/Shared/SharedJobDSL.groovy')

// Tạo GroovyClassLoader để compile class
def classLoader = new GroovyClassLoader(this.class.classLoader)
def sharedClass = classLoader.parseClass(sharedScript)

// Đưa class vào binding (để job files dùng được)
binding.setVariable('SharedJobDSL', sharedClass)

// === 2. Helper: Load and run job DSL scripts ===
def loadJobsFrom(String relativePath) {
    def dir = new File("${WORKSPACE}/${relativePath}")
    if (!dir.isDirectory()) {
        println "WARNING: Directory not found: ${relativePath}"
        return
    }

    dir.eachFileMatch(~/.*\.groovy$/) { file ->
        def scriptPath = "${relativePath}/${file.name}"
        println "Processing job: ${scriptPath}"
        def script = readFileFromWorkspace(scriptPath)

        def jobManagement = new JenkinsJobManagement(System.out, [:], new File('.'))
        new DslScriptLoader(jobManagement).runScript(script)
    }
}

// === 3. Create Jenkins Folders ===
folder('SRMS') {
    description 'SRMS Project Root'
}

folder('SRMS/SpringServices') {
    description 'Spring Boot Microservices CI/CD'
}

// === 4. Load All Jobs ===
loadJobsFrom('CI/Spring/Jobs')

println "Master DSL completed successfully."