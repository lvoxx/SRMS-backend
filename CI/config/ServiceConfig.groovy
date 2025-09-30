// Service and Module Metadata
def services = [
    SpringServices: [
        language: 'java',
        modules: [
            customer: [path: 'SpringServices/customer', testCmd: 'mvn test', dockerContext: '.'],
            order: [path: 'SpringServices/order', testCmd: 'mvn test', dockerContext: '.'],
            payment: [path: 'SpringServices/payment', testCmd: 'mvn test', dockerContext: '.']
            // Add more modules here, e.g., inventory: [path: '...']
        ],
        buildCmd: 'mvn clean package -f pom.xml',  // Root build for all modules
        dockerBuild: true
    ],
    PythonServices: [
        language: 'python',
        modules: [:],  // Empty for now, add later e.g., moduleX: [path: '...', testCmd: 'pytest']
        buildCmd: 'python setup.py build',
        dockerBuild: false  // Customize per language
    ],
    GoServices: [
        language: 'go',
        modules: [:],
        buildCmd: 'go build ./...',
        dockerBuild: true
    ],
    SRMSClient: [
        language: 'frontend',  // e.g., React/Vue
        modules: [client: [path: 'SrmsClient', testCmd: 'npm test', dockerContext: '.']],
        buildCmd: 'npm run build',
        dockerBuild: true
    ]
    // Add new services here
]

// Export
services