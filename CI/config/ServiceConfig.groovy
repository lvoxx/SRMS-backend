// Service and Module Metadata for SRMS
def services = [
    SpringServices: [
        language: 'java',
        modules: [
            contactor: [path: 'SpringServices/contactor', testCmd: 'mvn test -f SpringServices/contactor', dockerContext: 'SpringServices/contactor'],
            customer: [path: 'SpringServices/customer', testCmd: 'mvn test -f SpringServices/customer', dockerContext: 'SpringServices/customer'],
            order: [path: 'SpringServices/order', testCmd: 'mvn test -f SpringServices/order', dockerContext: 'SpringServices/order'],
            payment: [path: 'SpringServices/payment', testCmd: 'mvn test -f SpringServices/payment', dockerContext: 'SpringServices/payment'],
            kitchen: [path: 'SpringServices/kitchen', testCmd: 'mvn test -f SpringServices/kitchen', dockerContext: 'SpringServices/kitchen'],
            warehouse: [path: 'SpringServices/warehouse', testCmd: 'mvn test -f SpringServices/warehouse', dockerContext: 'SpringServices/warehouse'],
            dashboard: [path: 'SpringServices/dashboard', testCmd: 'mvn test -f SpringServices/dashboard', dockerContext: 'SpringServices/dashboard'],
            reporting: [path: 'SpringServices/reporting', testCmd: 'mvn test -f SpringServices/reporting', dockerContext: 'SpringServices/reporting'],
            notification: [path: 'SpringServices/notification', testCmd: 'mvn test -f SpringServices/notification', dockerContext: 'SpringServices/notification']
        ],
        buildCmd: 'mvn clean package -f SpringServices/pom.xml',
        dockerBuild: true
    ]
    // Add PythonServices, GoServices, etc. here as needed
]

// Export
services