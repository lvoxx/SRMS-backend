// Seed Job DSL - Main entry point
// Load configs in current context
def services = evaluate(new File("${WORKSPACE}/CI/config/ServiceConfig.groovy").text)
def globalConfig = evaluate(new File("${WORKSPACE}/CI/config/CIConfig.groovy").text)

println "Configs loaded. Services: ${services.keySet()}"

// Evaluate job generators in the same context
println "Loading job generators..."

new File("${WORKSPACE}/CI/jobs").eachFileMatch(~/.*\.groovy/) { file ->
    if (file.name != 'SeedJob.groovy') {
        println "Evaluating: ${file.name}"
        evaluate(file.text)
    }
}