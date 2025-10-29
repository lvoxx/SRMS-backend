// CI/MasterDSL/SRMS-MasterDSL.groovy
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

println "=== SRMS Master DSL started ==="

def discoverBasePath() {
    def workspace = getWorkspacePath()
    def currentDir = new File(workspace)
    
    // Tìm thư mục chứa CI/Spring/Jobs
    def candidates = []
    
    // Search in current directory and parents
    def searchDirs = [currentDir]
    searchDirs.addAll(currentDir.parentFile.listFiles()?.findAll { it.isDirectory() } ?: [])
    
    searchDirs.each { dir ->
        def testPath = new File(dir, "CI/Spring/Jobs")
        if (testPath.exists()) {
            def relativePath = currentDir.toPath().relativize(dir.toPath()).toString()
            candidates.add(relativePath ?: ".")
            println "✅ Found DSL structure in: ${dir.absolutePath}"
        }
    }
    
    return candidates ? candidates[0] : "."
}

def loadJobsFrom(String relativePath) {
    def basePath = discoverBasePath()
    def workspace = getWorkspacePath()
    
    println "🔧 Auto-discovered base path: ${basePath}"
    
    def fullPath = basePath == "." ? relativePath : "${basePath}/${relativePath}"
    def targetDir = new File("${workspace}/${fullPath}")
    
    println "📁 Target directory: ${targetDir.absolutePath}"

    if (!targetDir.exists()) {
        println "❌ Directory not found"
        return
    }

    targetDir.eachFileMatch(~/.*\.groovy$/) { file ->
        println "🔹 Processing: ${file.name}"
        
        try {
            def jobManagement = new JenkinsJobManagement(System.out, [:], new File(workspace))
            def dslLoader = new DslScriptLoader(jobManagement)
            
            // Load scripts với base path auto-discovered
            def sharedScript = readFileFromWorkspace("${basePath}/CI/Shared/SharedJobDSL.groovy")
            dslLoader.runScript(sharedScript)
            
            def jobScript = readFileFromWorkspace("${fullPath}/${file.name}")
            dslLoader.runScript(jobScript)
            
            println "✅ Job created: ${file.name}"
        } catch (e) {
            println "❌ Failed: ${e.message}"
        }
    }
}

def getWorkspacePath() {
    return build.getEnvironment(listener).get('WORKSPACE')
}

// Create folders
folder('SRMS') { description 'SRMS Project Root' }
folder('SRMS/SpringServices') { description 'Spring Boot Microservices CI/CD' }

// Load jobs
println "=== Loading Spring Jobs... ==="
loadJobsFrom('CI/Spring/Jobs')

println "🎯 Master DSL completed."