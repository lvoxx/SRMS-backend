// CI/MasterDSL/SRMS-MasterDSL.groovy
// Master DSL – Load shared helpers and build all job DSLs

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

println "=== SRMS Master DSL started ==="

// === 1. Helper: Load and run job DSL scripts ===
def loadJobsFrom(String relativePath) {
    def workspace = System.getenv('WORKSPACE') ?: new File('.').absolutePath
    def dir = new File("${workspace}/${relativePath}")

    if (!dir.isDirectory()) {
        println "⚠️  Directory not found: ${relativePath}"
        return
    }

    dir.eachFileMatch(~/.*\.groovy$/) { file ->
        def scriptPath = "${relativePath}/${file.name}"
        println "\n🔹 Processing job: ${scriptPath}"

        def jobManagement = new JenkinsJobManagement(System.out, [:], new File('.'))
        def dslLoader = new DslScriptLoader(jobManagement)

        // === Load SharedJobDSL first (so it’s visible to the job scripts) ===
        try {
            def sharedScript = readFileFromWorkspace('CI/Shared/SharedJobDSL.groovy')
            def shell = new GroovyShell(new GroovyClassLoader())
            shell.evaluate(sharedScript) // Load class vào namespace
            println "SharedJobDSL class evaluated."
        } catch (e) {
            println "❌ Failed to load SharedJobDSL: ${e.message}"
            return
        }

        // === Now run the actual job DSL file ===
        try {
            def jobScript = readFileFromWorkspace(scriptPath)
            dslLoader.runScript(jobScript)
            println "✅ Job created: ${file.name}"
        } catch (e) {
            println "❌ Failed to process ${file.name}: ${e.message}"
        }
    }
}

// === 2. Create Jenkins folders ===
folder('SRMS') {
    description 'SRMS Project Root'
}

folder('SRMS/SpringServices') {
    description 'Spring Boot Microservices CI/CD'
}

// === Load All Jobs ===
println "\n=== Loading Spring Jobs... ==="
loadJobsFrom('/workspace/SRMS-DSL/CI/Spring/Jobs')

println "\n🎯 Master DSL completed successfully."
