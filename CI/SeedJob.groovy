// Seed Job DSL - Main entry point
println "Loading configs..."

// Load configs - DON'T use 'def' so variables are in global scope
serviceConfigFile = new File("${WORKSPACE}/CI/config/ServiceConfig.groovy")
services = evaluate(serviceConfigFile.text)

ciconfigFile = new File("${WORKSPACE}/CI/config/CIConfig.groovy")  
globalConfig = evaluate(ciconfigFile.text)

println "Configs loaded. Services: ${services.keySet()}"

// Load generators - they will have access to 'services' and 'globalConfig' variables
println "Evaluating job generators..."

// Note: Do NOT declare 'def' for these variables - they need to be in global scope
generatorFiles = [
    "${WORKSPACE}/CI/jobs/PipelineJobsGenerator.groovy",
    "${WORKSPACE}/CI/jobs/TestJobsGenerator.groovy", 
    "${WORKSPACE}/CI/jobs/DashboardGenerator.groovy"
]

generatorFiles.each { file ->
    println "Evaluating: ${file}"
    evaluate(new File(file).text)
}