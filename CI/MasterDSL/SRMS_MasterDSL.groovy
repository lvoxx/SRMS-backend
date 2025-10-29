// CI/MasterDSL/SRMS-MasterDSL.groovy
// Master DSL – Load shared helpers and build all job DSLs

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

println "=== SRMS Master DSL started ==="

// === 1. Helper: Load and run job DSL scripts ===
def loadJobsFrom(String relativePath) {
    def workspace = new File(".").canonicalPath
    def targetDir = new File("${workspace}/${relativePath}")

    println "🔍 Looking for job scripts in: ${targetDir.absolutePath}"

    if (!targetDir.exists() || !targetDir.isDirectory()) {
        println "⚠️  Directory not found: ${relativePath}"
        return
    }

    targetDir.eachFileMatch(~/.*\.groovy$/) { file ->
        println "\n🔹 Processing job: ${file.name}"

        def jobManagement = new JenkinsJobManagement(System.out, [:], new File('.'))
        def dslLoader = new DslScriptLoader(jobManagement)

        try {
            def sharedScript = readFileFromWorkspace('CI/Shared/SharedJobDSL.groovy')
            dslLoader.runScript(sharedScript)
            println "✅ SharedJobDSL loaded successfully."
        } catch (e) {
            println "❌ Failed to load SharedJobDSL: ${e.message}"
            return
        }

        try {
            def jobScript = readFileFromWorkspace("${relativePath}/${file.name}")
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

// === 3. Load All Jobs ===
println "\n=== Loading Spring Jobs... ==="
loadJobsFrom('CI/Spring/Jobs')

println "\n🎯 Master DSL completed successfully."
