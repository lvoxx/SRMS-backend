// Dashboard Generator
def serviceConfig = load('CI/config/ServiceConfig.groovy')

serviceConfig.keySet().each { serviceName ->
    folder("SRMS/${serviceName}")
    
    // Test View
    listView("SRMS/${serviceName}/Test") {
        jobs { regex("SRMS/${serviceName}/Test/.*") }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
    
    // Build View
    listView("SRMS/${serviceName}/Build") {
        jobs { regex("SRMS/${serviceName}/Build/.*") }
        columns { /* similar columns */ }
    }
    
    // Deploy View
    listView("SRMS/${serviceName}/Deploy") {
        jobs { regex("SRMS/${serviceName}/Deploy/.*") }
        columns { /* similar columns */ }
    }
}