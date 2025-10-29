// CI/MasterDSL/SRMS-MasterDSL.groovy
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

println "=== SRMS Master DSL started ==="

def discoverBasePath() {
    def workspace = getWorkspacePath()
    def currentDir = new File(workspace)
    
    println "🔍 Scanning for DSL structure from: ${currentDir.absolutePath}"
    
    // Tìm thư mục chứa CI/Spring/Jobs
    def candidates = []
    
    // Tìm trong thư mục hiện tại và các thư mục con
    def searchDirs = [currentDir]
    // Thêm các thư mục con level 1
    currentDir.eachDir { dir ->
        searchDirs << dir
    }
    
    searchDirs.each { dir ->
        def testPath = new File(dir, "CI/Spring/Jobs")
        if (testPath.exists() && testPath.isDirectory()) {
            if (dir == currentDir) {
                candidates.add(".")
            } else {
                def relativePath = currentDir.toPath().relativize(dir.toPath()).toString()
                candidates.add(relativePath)
            }
            println "✅ Found DSL structure in: ${dir.absolutePath}"
        }
    }
    
    if (candidates.isEmpty()) {
        println "⚠️  No DSL structure found, using current directory"
        return "."
    }
    
    return candidates[0]
}

def getWorkspacePath() {
    // Cách an toàn để lấy workspace path
    try {
        // Thử đọc từ biến môi trường
        def env = System.getenv()
        if (env['WORKSPACE']) {
            return env['WORKSPACE']
        }
    } catch (e) {
        // Fallback
    }
    
    // Fallback: sử dụng current directory
    return new File(".").canonicalPath
}

def loadJobsFrom(String relativePath) {
    def basePath = discoverBasePath()
    def workspace = getWorkspacePath()
    
    println "🔧 Auto-discovered base path: '${basePath}'"
    println "📁 Workspace: ${workspace}"
    
    // Xây dựng full path
    def fullPath = basePath == "." ? relativePath : "${basePath}/${relativePath}"
    def targetDir = new File(workspace, fullPath)
    
    println "📁 Looking for jobs in: ${targetDir.absolutePath}"
    println "📁 Directory exists: ${targetDir.exists()}"

    if (!targetDir.exists()) {
        println "❌ Directory not found: ${targetDir.absolutePath}"
        
        // Debug: liệt kê thư mục workspace
        def workspaceDir = new File(workspace)
        println "📂 Contents of workspace:"
        if (workspaceDir.exists()) {
            workspaceDir.eachFile { file ->
                println "   - ${file.name} (${file.isDirectory() ? 'dir' : 'file'})"
            }
        }
        return
    }

    def groovyFiles = targetDir.listFiles({ file -> file.name.endsWith('.groovy') } as FileFilter)
    if (!groovyFiles || groovyFiles.size() == 0) {
        println "ℹ️  No Groovy files found in: ${targetDir.absolutePath}"
        return
    }

    println "📄 Found ${groovyFiles.size()} Groovy files"

    // Load shared script
    def sharedScriptPath = basePath == "." ? "CI/Shared/SharedJobDSL.groovy" : "${basePath}/CI/Shared/SharedJobDSL.groovy"
    def sharedScript = null
    try {
        sharedScript = readFileFromWorkspace(sharedScriptPath)
        println "✅ SharedJobDSL loaded from: ${sharedScriptPath}"
    } catch (e) {
        println "❌ Failed to load SharedJobDSL from ${sharedScriptPath}: ${e.message}"
        return
    }

    // Process each job file
    groovyFiles.each { file ->
        println "\n🔹 Processing: ${file.name}"
        
        try {
            def jobManagement = new JenkinsJobManagement(System.out, [:], new File(workspace))
            def dslLoader = new DslScriptLoader(jobManagement)
            
            // Run shared script
            dslLoader.runScript(sharedScript)
            
            // Run job script
            def jobScriptPath = "${fullPath}/${file.name}"
            def jobScript = readFileFromWorkspace(jobScriptPath)
            dslLoader.runScript(jobScript)
            
            println "✅ Job created: ${file.name}"
        } catch (e) {
            println "❌ Failed to process ${file.name}: ${e.message}"
            e.printStackTrace()
        }
    }
}

// === Create Jenkins folders ===
println "\n=== Creating Jenkins folders... ==="
folder('SRMS') {
    description 'SRMS Project Root'
}

folder('SRMS/SpringServices') {
    description 'Spring Boot Microservices CI/CD'
}

// === Load All Jobs ===
println "\n=== Loading Spring Jobs... ==="
loadJobsFrom('CI/Spring/Jobs')

println "\n🎯 Master DSL completed successfully."