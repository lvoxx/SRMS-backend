// Seed Job DSL - Main entry point
println "[START] Loading configs..."

// Load configs - DON'T use 'def' so variables are in global scope
serviceConfigFile = new File("${WORKSPACE}/CI/config/ServiceConfig.groovy")
services = evaluate(serviceConfigFile.text)

ciconfigFile = new File("${WORKSPACE}/CI/config/CIConfig.groovy")  
globalConfig = evaluate(ciconfigFile.text)

println "[FINISHED] Configs loaded. Services: ${services.keySet()}"
//-------------------------------------------------------------------------------------
println "[START] Pre-creating top-level folders..."
folder('SRMS')  {
    description("SRMS Build-Test-Deploy")
}
folder("SRMS/Test") {
    description("Root-level Jenkins test job for SRMS Backend project")
}
services.keySet().each { serviceName ->
    folder("SRMS/${serviceName}")
    folder("SRMS/${serviceName}/Build")
    folder("SRMS/${serviceName}/Deploy")
}

println "[FINISHED] Top-level folders created."
//-------------------------------------------------------------------------------------
// Load generators - they will have access to 'services' and 'globalConfig' variables
println "[START] Evaluating job generators..."

// Note: Do NOT declare 'def' for these variables - they need to be in global scope
generatorFiles = [
    "${WORKSPACE}/CI/jobs/TestJobsGenerator.groovy", 
    "${WORKSPACE}/CI/jobs/PipelineJobsGenerator.groovy",
    "${WORKSPACE}/CI/jobs/DashboardGenerator.groovy"
]

generatorFiles.each { file ->
    println "Evaluating: ${file}"
    
    // Create a GroovyShell with proper binding and delegate
    def binding = new Binding([
        'services': services,
        'globalConfig': globalConfig,
        'WORKSPACE': WORKSPACE
    ])
    
    def shell = new GroovyShell(this.class.classLoader, binding)
    def script = shell.parse(new File(file))
    
    // Set delegate to 'this' so DSL methods are accessible
    script.metaClass.methodMissing = { String name, args ->
        this."$name"(*args)
    }
    
    script.metaClass.propertyMissing = { String name ->
        this."$name"
    }
    
    script.run()
}
println "[FINISHED] Job generators evaluated."
//-------------------------------------------------------------------------------------