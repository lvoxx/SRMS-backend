# ArgoCD Quick Commands

## Installation

### Install ArgoCD on Kubernetes
```bash
# Create namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for pods to be ready
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s
```

### Access ArgoCD

```bash
# Port forward to access UI
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Get admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo

# Change admin password
argocd account update-password
```

### Install ArgoCD CLI

```bash
# Linux
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
rm argocd-linux-amd64

# Mac
brew install argocd

# Verify installation
argocd version
```

## CLI Login

```bash
# Login to ArgoCD
argocd login localhost:8080 --username admin --password <password> --insecure

# Login with SSO
argocd login argocd.example.com --sso

# Logout
argocd logout localhost:8080
```

## Application Management

### Create Application
```bash
# Create app from Git repo
argocd app create myapp \
  --repo https://github.com/myorg/myrepo.git \
  --path kubernetes \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace default

# Create app from Helm chart
argocd app create myapp \
  --repo https://github.com/myorg/helm-charts.git \
  --path charts/myapp \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace default \
  --helm-set image.tag=v1.0.0
```

### List Applications
```bash
# List all applications
argocd app list

# List with output format
argocd app list -o wide
argocd app list -o json
```

### Get Application Details
```bash
# Get app info
argocd app get myapp

# Get app in YAML format
argocd app get myapp -o yaml

# Show app history
argocd app history myapp
```

### Sync Application
```bash
# Sync app
argocd app sync myapp

# Sync with prune (remove resources not in Git)
argocd app sync myapp --prune

# Sync specific resource
argocd app sync myapp --resource Deployment:myapp

# Force sync
argocd app sync myapp --force

# Async sync
argocd app sync myapp --async
```

### Application Status
```bash
# Get sync status
argocd app get myapp --refresh

# Wait for app to be synced
argocd app wait myapp --sync

# Wait for app to be healthy
argocd app wait myapp --health
```

### Delete Application
```bash
# Delete app (keeps resources in cluster)
argocd app delete myapp

# Delete app and cascade delete resources
argocd app delete myapp --cascade

# Delete without confirmation
argocd app delete myapp -y
```

### Rollback
```bash
# Rollback to previous version
argocd app rollback myapp

# Rollback to specific revision
argocd app rollback myapp 5

# List history for rollback
argocd app history myapp
```

## Repository Management

```bash
# Add Git repository
argocd repo add https://github.com/myorg/myrepo.git --username myuser --password mypass

# Add Git repo with SSH
argocd repo add git@github.com:myorg/myrepo.git --ssh-private-key-path ~/.ssh/id_rsa

# List repositories
argocd repo list

# Remove repository
argocd repo rm https://github.com/myorg/myrepo.git
```

## Cluster Management

```bash
# Add cluster
argocd cluster add CONTEXT_NAME

# List clusters
argocd cluster list

# Get cluster info
argocd cluster get https://kubernetes.default.svc

# Remove cluster
argocd cluster rm https://kubernetes.default.svc
```

## Project Management

```bash
# Create project
argocd proj create myproject \
  --description "My Project" \
  --src https://github.com/myorg/* \
  --dest https://kubernetes.default.svc,default

# List projects
argocd proj list

# Get project details
argocd proj get myproject

# Delete project
argocd proj delete myproject
```

## Application Set Examples

### Create ApplicationSet
```yaml
# applicationset.yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: myapp-set
  namespace: argocd
spec:
  generators:
  - list:
      elements:
      - cluster: dev
        namespace: dev
      - cluster: staging
        namespace: staging
      - cluster: prod
        namespace: prod
  template:
    metadata:
      name: 'myapp-{{cluster}}'
    spec:
      project: default
      source:
        repoURL: https://github.com/myorg/myrepo.git
        targetRevision: HEAD
        path: kubernetes/{{cluster}}
      destination:
        server: https://kubernetes.default.svc
        namespace: '{{namespace}}'
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
```

```bash
# Apply ApplicationSet
kubectl apply -f applicationset.yaml
```

## Sync Policies

### Auto-sync Configuration
```bash
# Enable auto-sync
argocd app set myapp --sync-policy automated

# Enable auto-sync with prune
argocd app set myapp --sync-policy automated --auto-prune

# Enable self-heal
argocd app set myapp --self-heal

# Disable auto-sync
argocd app set myapp --sync-policy none
```

## ArgoCD Notifications

```bash
# Install notifications
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj-labs/argocd-notifications/stable/manifests/install.yaml

# Configure Slack notifications (edit configmap)
kubectl edit configmap argocd-notifications-cm -n argocd
```

## Troubleshooting

```bash
# View ArgoCD logs
kubectl logs -n argocd deployment/argocd-server -f
kubectl logs -n argocd deployment/argocd-application-controller -f
kubectl logs -n argocd deployment/argocd-repo-server -f

# Refresh app (force fetch from Git)
argocd app get myapp --refresh --hard-refresh

# View diff before sync
argocd app diff myapp

# View sync status
argocd app get myapp --show-operation

# Terminate sync operation
argocd app terminate-op myapp

# Restart ArgoCD components
kubectl rollout restart deployment/argocd-server -n argocd
kubectl rollout restart deployment/argocd-repo-server -n argocd
kubectl rollout restart deployment/argocd-application-controller -n argocd
```

## Useful kubectl Commands for ArgoCD

```bash
# Check ArgoCD pods
kubectl get pods -n argocd

# Check ArgoCD applications as CRDs
kubectl get applications -n argocd

# Edit application directly
kubectl edit application myapp -n argocd

# Delete application CRD
kubectl delete application myapp -n argocd

# View application events
kubectl describe application myapp -n argocd
```

## UI Access with Ingress

```yaml
# argocd-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-ingress
  namespace: argocd
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
spec:
  ingressClassName: nginx
  rules:
  - host: argocd.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: argocd-server
            port:
              number: 443
  tls:
  - hosts:
    - argocd.example.com
    secretName: argocd-tls
```

```bash
# Apply ingress
kubectl apply -f argocd-ingress.yaml
```

## Backup and Restore

```bash
# Backup all applications
argocd app list -o yaml > argocd-apps-backup.yaml

# Backup ArgoCD settings
kubectl get configmap argocd-cm -n argocd -o yaml > argocd-cm-backup.yaml
kubectl get secret argocd-secret -n argocd -o yaml > argocd-secret-backup.yaml

# Restore applications
kubectl apply -f argocd-apps-backup.yaml
```