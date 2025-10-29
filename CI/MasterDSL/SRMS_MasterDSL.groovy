// CI/MasterDSL/SRMS-MasterDSL.groovy
// Job DSL Seed Script – DO NOT use pipeline syntax here

// === 1. Load Shared Utilities (from checked-out repo) ===
def sharedFiles = [
    'CommonEnv.groovy',
    'GitUtils.groovy',
    'MavenUtils.groovy',
    'DockerUtils.groovy',
    'BaseJobTemplate.groovy'
]

sharedFiles.each { fileName ->
    def path = "CI/Shared/${fileName}"
    def script = readFileFromWorkspace(path)
    evaluate(script)
    println "Loaded shared: ${path}"
}

// === 2. Helper: Load all .groovy jobs from a folder ===
def loadJobsFrom(String relativePath) {
    def dir = new File("${WORKSPACE}/${relativePath}")
    if (!dir.isDirectory()) {
        println "WARNING: Job directory not found: ${relativePath}"
        return
    }

    dir.eachFileMatch(~/.*\.groovy$/) { file ->
        def scriptPath = "${relativePath}/${file.name}"
        println "Processing job: ${scriptPath}"
        def script = readFileFromWorkspace(scriptPath)
        new javaposse.jobdsl.dsl.DslScriptLoader(
            new javaposse.jobdsl.plugin.JenkinsJobManagement(System.out, [:], new File('.'))
        ).runScript(script)
    }
}

// === 3. Create Folder Structure ===
folder('SRMS') {
    description 'Root folder for SRMS project'
}

folder('SRMS/SpringServices') {
    description 'CI/CD pipelines for Spring Boot microservices'
}

// === 4. Load Spring Jobs ===
loadJobsFrom('CI/Spring/Jobs')

// Future:
// loadJobsFrom('CI/Python/Jobs')
// loadJobsFrom('CI/Go/Jobs')

println "Master DSL completed successfully."