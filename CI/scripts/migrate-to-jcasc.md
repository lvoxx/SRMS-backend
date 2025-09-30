# Migrate to Jenkins Configuration as Code (JCasC)

1. Install JCasC plugin.
2. Create `jenkins.yaml` with configs:
   - Tools: maven, jdk.
   - Credentials: github-token, etc.
   - Plugins: list all required.
3. Point Jenkins to YAML file via env var: `CASC_JENKINS_CONFIG=/path/to/jenkins.yaml`.
4. Restart Jenkins. All configs (tools, creds) will be auto-applied.
5. Migrate shared/ to global Shared Lib in JCasC.

More details: https://jenkins.io/projects/jcasc/