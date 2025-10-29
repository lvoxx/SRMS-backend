// CI/MasterDSL/SRMS-MasterDSL.groovy
// Single entry point for Jenkins Job DSL seed job
// Creates folder hierarchy and dynamically loads all job definitions

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

// === CONFIGURATION ===
def repoRoot = 'CI'
def sharedPath = "${repoRoot}/Shared"
def springJobsPath = "${repoRoot}/Spring/Jobs"

// Load shared utilities into context
evaluate(new File("${sharedPath}/CommonEnv.groovy"))
evaluate(new File("${sharedPath}/GitUtils.groovy"))
evaluate(new File("${sharedPath}/MavenUtils.groovy"))
evaluate(new File("${sharedPath}/DockerUtils.groovy"))
evaluate(new File("${sharedPath}/BaseJobTemplate.groovy"))

// === HELPER: Load all .groovy files from a directory ===
def loadJobsFrom(String folderPath) {
    def jobDir = new File(folderPath)
    if (!jobDir.exists() || !jobDir.isDirectory()) {
        println "Warning: Job directory not found: ${folderPath}"
        return
    }

    jobDir.eachFileMatch(~/.*\.groovy$/) { file ->
        println "Loading job DSL: ${file.name}"
        evaluate(file)
    }
}

// === CREATE JENKINS FOLDER STRUCTURE ===
folder('SRMS') {
    description 'Root folder for SRMS project'
}

folder('SRMS/SpringServices') {
    description 'CI/CD pipelines for Spring Boot microservices'
}

// === LOAD SPRING JOBS ===
loadJobsFrom(springJobsPath)

// === FUTURE EXPANSION HOOKS ===
// loadJobsFrom("${repoRoot}/Python/Jobs")
// loadJobsFrom("${repoRoot}/Go/Jobs")

println "Master DSL execution completed. All jobs loaded."