// ======================
// Seed Job DSL - Main entry point
// ======================

// Load configs
def services = evaluate(new File("${WORKSPACE}/CI/config/ServiceConfig.groovy").text)
def globalConfig = evaluate(new File("${WORKSPACE}/CI/config/CIConfig.groovy").text)

println "Configs loaded. Services: ${services.keySet()}"
println "Loading job generators..."

// Use Binding to share variables between scripts
def binding = new Binding()
binding.setVariable('services', services)
binding.setVariable('globalConfig', globalConfig)
binding.setVariable('WORKSPACE', WORKSPACE)

def shell = new GroovyShell(binding)

// Evaluate each generator file under CI/jobs
new File("${WORKSPACE}/CI/jobs").eachFileMatch(~/.*\.groovy/) { file ->
    if (file.name != 'SeedJob.groovy') {
        println "Evaluating: ${file.name}"
        shell.evaluate(file)
    }
}

println "All job generators executed successfully."
