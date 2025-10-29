// CI/MasterDSL/SRMS-MasterDSL.groovy
// Master DSL – Load shared helpers and build all job DSLs

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

println "=== SRMS Master DSL started ==="
println "Workspace location: ${new File('.').canonicalPath}"

// === 1. Helper: Load and run job DSL scripts ===
def loadJobsFrom(String relativePath) {
    def workspace = new File(".").canonicalPath
    def targetDir = new File("${workspace}/${relativePath}")

    println "🔍 Looking for job scripts in: ${targetDir.absolutePath}"
    println "📁 Directory exists: ${targetDir.exists()}, Is directory: ${targetDir.isDirectory()}"

    if (!targetDir.exists()) {
        println "❌ Directory not found: ${targetDir.absolutePath}"
        return
    }
    
    if (!targetDir.isDirectory()) {
        println "❌ Path is not a directory: ${targetDir.absolutePath}"
        return
    }

    def groovyFiles = targetDir.listFiles({ file -> file.name.endsWith('.groovy') } as FileFilter)
    if (!groovyFiles) {
        println "ℹ️  No Groovy files found in: ${targetDir.absolutePath}"
        return
    }

    println "📄 Found ${groovyFiles.size()} Groovy files"

    // Load SharedJobDSL once
    def sharedScript = null
    try {
        sharedScript = readFileFromWorkspace('CI/Shared/SharedJobDSL.groovy')
        println "✅ SharedJobDSL loaded successfully"
    } catch (e) {
        println "❌ Failed to load SharedJobDSL: ${e.message}"
        return
    }

    groovyFiles.each { file ->
        println "\n🔹 Processing job: ${file.name}"

        try {
            def jobManagement = new JenkinsJobManagement(System.out, [:], new File('.'))
            def dslLoader = new DslScriptLoader(jobManagement)

            // Run shared script first
            dslLoader.runScript(sharedScript)
            
            // Run job-specific script
            def jobScriptPath = "${relativePath}/${file.name}"
            def jobScript = readFileFromWorkspace(jobScriptPath)
            dslLoader.runScript(jobScript)
            println "✅ Job created: ${file.name}"
            
        } catch (e) {
            println "❌ Failed to process ${file.name}: ${e.message}"
            e.printStackTrace()
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