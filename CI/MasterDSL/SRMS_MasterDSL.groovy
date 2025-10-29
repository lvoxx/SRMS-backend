// CI/MasterDSL/SRMS-MasterDSL.groovy - Best practice

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

println "=== SRMS Master DSL started ==="

def loadJobsFrom(String relativePath) {
    // Sử dụng WORKSPACE environment variable
    def workspace = build.getEnvironment(listener).get('WORKSPACE')
    if (!workspace) {
        workspace = new File('.').canonicalPath
    }
    
    def targetDir = new File("${workspace}/${relativePath}")
    println "🔍 Looking in: ${targetDir.absolutePath}"

    if (!targetDir.exists()) {
        println "❌ Directory not found"
        return
    }

    targetDir.eachFileMatch(~/.*\.groovy$/) { file ->
        println "🔹 Processing: ${file.name}"
        
        try {
            def jobManagement = new JenkinsJobManagement(System.out, [:], new File(workspace))
            def dslLoader = new DslScriptLoader(jobManagement)
            
            // Load shared script
            def sharedScript = readFileFromWorkspace('CI/Shared/SharedJobDSL.groovy')
            dslLoader.runScript(sharedScript)
            
            // Load job script
            def jobScript = readFileFromWorkspace("${relativePath}/${file.name}")
            dslLoader.runScript(jobScript)
            
            println "✅ Job created: ${file.name}"
        } catch (e) {
            println "❌ Failed: ${e.message}"
        }
    }
}

// === 2. Create Jenkins folders ===
println "\n=== Creating Jenkins folders... ==="
folder('SRMS') {
    description 'SRMS Project Root'
}

folder('SRMS/SpringServices') {
    description 'Spring Boot Microservices CI/CD'
}

// === 3. Load All Jobs ===
println "\n=== Loading Spring Jobs... ==="
loadJobsFrom('CI/Spring/Jobs')

println "\n🎯 Master DSL completed."