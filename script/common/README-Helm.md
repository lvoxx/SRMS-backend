# Show all chart information
helm show all bitnami/nginx

# Show README
helm show readme bitnami/nginx
```

## Dependencies

```bash
# Update dependencies
helm dependency update mychart/

# Build dependencies
helm dependency build mychart/

# List dependencies
helm dependency list mychart/
```

## Working with Values

### values.yaml Example
```yaml
replicaCount: 2

image:
  repository: nginx
  tag: "1.21"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: true
  className: nginx
  hosts:
    - host: example.com
      paths:
        - path: /
          pathType: Prefix

resources:
  limits:
    cpu: 100m
    memory: 128Mi
  requests:
    cpu: 100m
    memory: 128Mi
```

### Override Values
```bash
# Override with file
helm install myapp ./mychart/ -f prod-values.yaml

# Override multiple files (last wins)
helm install myapp ./mychart/ -f values.yaml -f prod-values.yaml

# Override with command line
helm install myapp ./mychart/ --set image.tag=1.22

# Override nested values
helm install myapp ./mychart/ --set ingress.hosts[0].host=newhost.com

# Override with JSON
helm install myapp ./mychart/ --set-json 'ingress.hosts=[{"host":"example.com"}]'
```

## Helm Diff Plugin

```bash
# Install diff plugin
helm plugin install https://github.com/databus23/helm-diff

# Show diff before upgrade
helm diff upgrade myapp bitnami/nginx -f values.yaml

# Show diff with color
helm diff upgrade myapp bitnami/nginx --color
```

## Helm Secrets Plugin

```bash
# Install secrets plugin
helm plugin install https://github.com/jkroepke/helm-secrets

# Encrypt values file
helm secrets enc secrets.yaml

# Decrypt values file
helm secrets dec secrets.yaml

# Install with encrypted values
helm secrets install myapp ./mychart/ -f secrets.yaml

# Upgrade with encrypted values
helm secrets upgrade myapp ./mychart/ -f secrets.yaml
```

## Testing

```bash
# Test release
helm test myapp

# Test with logs
helm test myapp --logs
```

## Hooks

### Pre-install Hook Example
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: pre-install-job
  annotations:
    "helm.sh/hook": pre-install
    "helm.sh/hook-weight": "-5"
    "helm.sh/hook-delete-policy": hook-succeeded
spec:
  template:
    spec:
      containers:
      - name: pre-install
        image: busybox
        command: ['sh', '-c', 'echo Pre-install hook']
      restartPolicy: Never
```

### Available Hooks
- pre-install
- post-install
- pre-delete
- post-delete
- pre-upgrade
- post-upgrade
- pre-rollback
- post-rollback
- test

## Advanced Commands

```bash
# Get release information
helm get all myapp

# Export release values
helm get values myapp -o yaml > current-values.yaml

# Pull chart without installing
helm pull bitnami/nginx
helm pull bitnami/nginx --untar
helm pull bitnami/nginx --version 13.2.0

# Show chart dependencies
helm show chart bitnami/nginx | grep dependencies -A 10

# Verify chart
helm verify mychart-1.0.0.tgz

# Index charts (for chart repository)
helm repo index .

# Serve local chart repository
helm serve --address 0.0.0.0:8879 --repo-path ./charts
```

## Debugging

```bash
# Debug template rendering
helm install myapp ./mychart/ --debug --dry-run

# Get manifest of installed release
helm get manifest myapp

# Get hooks of release
helm get hooks myapp

# Verbose output
helm install myapp ./mychart/ -v=5

# Get all release info for debugging
helm get all myapp -o yaml
```

## Environment Variables

```bash
# Set Kubernetes context
export KUBECONFIG=/path/to/kubeconfig

# Set Helm cache
export HELM_CACHE_HOME=/tmp/helm/cache

# Set Helm config
export HELM_CONFIG_HOME=/tmp/helm/config

# Set Helm data
export HELM_DATA_HOME=/tmp/helm/data

# Disable color output
export HELM_NO_COLOR=1
```

## Chart Best Practices

### Chart.yaml Example
```yaml
apiVersion: v2
name: mychart
description: A Helm chart for Kubernetes
type: application
version: 1.0.0
appVersion: "1.0"
keywords:
  - nginx
  - web
home: https://example.com
sources:
  - https://github.com/example/mychart
maintainers:
  - name: John Doe
    email: john@example.com
dependencies:
  - name: redis
    version: "17.x.x"
    repository: https://charts.bitnami.com/bitnami
```

### Template Helpers (_helpers.tpl)
```yaml
{{/*
Expand the name of the chart.
*/}}
{{- define "mychart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "mychart.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "mychart.labels" -}}
helm.sh/chart: {{ include "mychart.chart" . }}
{{ include "mychart.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
```

## CI/CD Integration

### GitLab CI Example
```yaml
deploy:
  stage: deploy
  script:
    - helm upgrade --install myapp ./mychart/ 
        --namespace production 
        --create-namespace 
        --values values-prod.yaml
        --wait
        --timeout 5m
  only:
    - main
```

### GitHub Actions Example
```yaml
- name: Deploy Helm Chart
  run: |
    helm upgrade --install myapp ./mychart/ \
      --namespace production \
      --create-namespace \
      --values values-prod.yaml \
      --wait \
      --timeout 5m
```

## Troubleshooting

```bash
# Check release status
helm status myapp

# Get full history
helm history myapp --max 20

# Inspect failed release
helm get manifest myapp | kubectl apply --dry-run=server -f -

# Check template syntax
helm template myapp ./mychart/ --debug

# Force delete release
helm delete myapp --no-hooks

# Clean up failed releases
helm list --failed
helm delete <failed-release> --no-hooks

# Check Helm version compatibility
helm version --client
helm version --server
``` Helm Quick Commands

## Installation

```bash
# Install Helm (Linux)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Install Helm (Mac)
brew install helm

# Verify installation
helm version
```

## Repository Management

```bash
# Add repository
helm repo add stable https://charts.helm.sh/stable
helm repo add bitnami https://charts.bitnami.com/bitnami

# List repositories
helm repo list

# Update repositories
helm repo update

# Remove repository
helm repo remove stable

# Search charts in repo
helm search repo nginx
helm search repo stable/

# Search Helm Hub
helm search hub wordpress
```

## Chart Management

```bash
# Install chart
helm install myrelease stable/nginx

# Install with custom name
helm install myapp bitnami/nginx

# Install with values file
helm install myapp bitnami/nginx -f values.yaml

# Install with set values
helm install myapp bitnami/nginx --set service.type=NodePort --set replicaCount=3

# Install in specific namespace
helm install myapp bitnami/nginx --namespace production --create-namespace

# Dry run (test before install)
helm install myapp bitnami/nginx --dry-run --debug

# Install specific version
helm install myapp bitnami/nginx --version 13.2.0
```

## Release Management

```bash
# List releases
helm list
helm list --all-namespaces
helm list -n <namespace>

# Get release status
helm status myapp
helm status myapp -n <namespace>

# Get release values
helm get values myapp
helm get values myapp --all

# Get release manifest
helm get manifest myapp

# Get release notes
helm get notes myapp

# Get release history
helm history myapp
```

## Upgrade and Rollback

```bash
# Upgrade release
helm upgrade myapp bitnami/nginx

# Upgrade with new values
helm upgrade myapp bitnami/nginx -f new-values.yaml

# Upgrade with set values
helm upgrade myapp bitnami/nginx --set replicaCount=5

# Upgrade or install (if not exists)
helm upgrade --install myapp bitnami/nginx

# Force upgrade
helm upgrade myapp bitnami/nginx --force

# Rollback to previous version
helm rollback myapp

# Rollback to specific revision
helm rollback myapp 2

# Rollback with cleanup
helm rollback myapp --cleanup-on-fail
```

## Uninstall

```bash
# Uninstall release
helm uninstall myapp

# Uninstall from namespace
helm uninstall myapp -n <namespace>

# Uninstall and keep history
helm uninstall myapp --keep-history
```

## Creating Charts

```bash
# Create new chart
helm create mychart

# Validate chart
helm lint mychart/

# Package chart
helm package mychart/

# Package with version
helm package mychart/ --version 1.0.0

# Install from local chart
helm install myapp ./mychart/

# Install from packaged chart
helm install myapp mychart-1.0.0.tgz
```

## Chart Structure
```
mychart/
  Chart.yaml          # Chart metadata
  values.yaml         # Default configuration values
  charts/             # Dependent charts
  templates/          # Kubernetes manifests
    deployment.yaml
    service.yaml
    ingress.yaml
    _helpers.tpl      # Template helpers
  .helmignore        # Files to ignore
```

## Template Commands

```bash
# Render templates locally
helm template myapp ./mychart/

# Render with values
helm template myapp ./mychart/ -f values.yaml

# Render specific template
helm template myapp ./mychart/ -s templates/deployment.yaml

# Show computed values
helm show values bitnami/nginx

# Show chart info
helm show chart bitnami/nginx

#