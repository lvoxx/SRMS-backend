#!/bin/bash

# Install plugins via Jenkins CLI or manually
# Assume Jenkins is running at http://localhost:8080

# Download CLI
curl -o jenkins-cli.jar http://localhost:8080/jnlpJars/jenkins-cli.jar

# Install plugins
java -jar jenkins-cli.jar -s http://localhost:8080/ install-plugin job-dsl pipeline github docker-workflow folders

# Restart Jenkins
java -jar jenkins-cli.jar -s http://localhost:8080/ safe-restart

# Add credentials manually via UI: GitHub token, Docker cred, etc.
# Create tools: Maven, JDK, etc.
echo "Setup complete. Create SeedJob manually from UI, point to SeedJob.groovy."