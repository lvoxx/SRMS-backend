# Kubernetes Quick Commands

## Cluster Management

```bash
# Get cluster info
kubectl cluster-info

# View cluster nodes
kubectl get nodes
kubectl get nodes -o wide

# Describe node
kubectl describe node <node-name>

# Check cluster version
kubectl version --short

# Get all resources
kubectl get all --all-namespaces
```

## Context and Configuration

```bash
# View current context
kubectl config current-context

# List all contexts
kubectl config get-contexts

# Switch context
kubectl config use-context <context-name>

# Set namespace for current context
kubectl config set-context --current --namespace=<namespace>

# View kubeconfig
kubectl config view

# Set custom kubeconfig
export KUBECONFIG=/path/to/kubeconfig
```

## Namespace Management

```bash
# List namespaces
kubectl get namespaces
kubectl get ns

# Create namespace
kubectl create namespace <namespace>

# Delete namespace
kubectl delete namespace <namespace>

# Describe namespace
kubectl describe namespace <namespace>
```

## Pod Management

```bash
# List pods
kubectl get pods
kubectl get pods -n <namespace>
kubectl get pods --all-namespaces
kubectl get pods -o wide

# Describe pod
kubectl describe pod <pod-name>

# Get pod logs
kubectl logs <pod-name>
kubectl logs <pod-name> -f
kubectl logs <pod-name> --previous
kubectl logs <pod-name> -c <container-name>

# Execute command in pod
kubectl exec -it <pod-name> -- /bin/bash
kubectl exec -it <pod-name> -- sh
kubectl exec <pod-name> -- ls /app

# Copy files to/from pod
kubectl cp <pod-name>:/path/to/file ./local-file
kubectl cp ./local-file <pod-name>:/path/to/file

# Delete pod
kubectl delete pod <pod-name>
kubectl delete pod <pod-name> --force --grace-period=0

# Port forward
kubectl port-forward <pod-name> 8080:80
kubectl port-forward pod/<pod-name> 8080:80

# Get pod YAML
kubectl get pod <pod-name> -o yaml
kubectl get pod <pod-name> -o json
```

## Deployment Management

```bash
# List deployments
kubectl get deployments
kubectl get deploy

# Create deployment
kubectl create deployment <name> --image=<image>

# Scale deployment
kubectl scale deployment <name> --replicas=3

# Update image
kubectl set image deployment/<name> <container-name>=<new-image>

# Rollout status
kubectl rollout status deployment/<name>

# Rollout history
kubectl rollout history deployment/<name>

# Rollback deployment
kubectl rollout undo deployment/<name>
kubectl rollout undo deployment/<name> --to-revision=2

# Restart deployment
kubectl rollout restart deployment/<name>

# Delete deployment
kubectl delete deployment <name>

# Edit deployment
kubectl edit deployment <name>
```

## Service Management

```bash
# List services
kubectl get services
kubectl get svc

# Describe service
kubectl describe service <service-name>

# Create service
kubectl expose deployment <deployment-name> --port=80 --target-port=8080

# Delete service
kubectl delete service <service-name>

# Get service endpoints
kubectl get endpoints <service-name>
```

## ConfigMap and Secret

```bash
# Create ConfigMap from literal
kubectl create configmap <name> --from-literal=key1=value1 --from-literal=key2=value2

# Create ConfigMap from file
kubectl create configmap <name> --from-file=config.properties

# List ConfigMaps
kubectl get configmaps
kubectl get cm

# Describe ConfigMap
kubectl describe configmap <name>

# Create Secret from literal
kubectl create secret generic <name> --from-literal=password=mypassword

# Create Secret from file
kubectl create secret generic <name> --from-file=ssh-privatekey=~/.ssh/id_rsa

# Create Docker registry secret
kubectl create secret docker-registry <name> \
  --docker-server=<server> \
  --docker-username=<username> \
  --docker-password=<password> \
  --docker-email=<email>

# List secrets
kubectl get secrets

# Describe secret
kubectl describe secret <name>

# Decode secret
kubectl get secret <name> -o jsonpath="{.data.password}" | base64 --decode
```

## StatefulSet and DaemonSet

```bash
# List StatefulSets
kubectl get statefulsets
kubectl get sts

# Scale StatefulSet
kubectl scale statefulset <name> --replicas=3

# Delete StatefulSet
kubectl delete statefulset <name>

# List DaemonSets
kubectl get daemonsets
kubectl get ds

# Delete DaemonSet
kubectl delete daemonset <name>
```

## Ingress

```bash
# List ingresses
kubectl get ingress
kubectl get ing

# Describe ingress
kubectl describe ingress <name>

# Create ingress
kubectl create ingress <name> --rule="host/path=service:port"

# Delete ingress
kubectl delete ingress <name>
```

## Persistent Volumes

```bash
# List PersistentVolumes
kubectl get pv

# List PersistentVolumeClaims
kubectl get pvc

# Describe PVC
kubectl describe pvc <pvc-name>

# Delete PVC
kubectl delete pvc <pvc-name>
```

## Resource Monitoring

```bash
# Top nodes (requires metrics-server)
kubectl top nodes

# Top pods
kubectl top pods
kubectl top pods -n <namespace>

# Top pods with containers
kubectl top pods --containers

# Watch resources
kubectl get pods -w
kubectl get pods --watch
```

## Labels and Selectors

```bash
# Show labels
kubectl get pods --show-labels

# Add label
kubectl label pods <pod-name> environment=production

# Update label
kubectl label pods <pod-name> environment=staging --overwrite

# Remove label
kubectl label pods <pod-name> environment-

# Select by label
kubectl get pods -l environment=production
kubectl get pods -l 'environment in (production,staging)'
kubectl get pods -l environment!=production

# Delete by label
kubectl delete pods -l environment=testing
```

## Annotations

```bash
# Add annotation
kubectl annotate pod <pod-name> description="My description"

# Remove annotation
kubectl annotate pod <pod-name> description-
```

## Apply and Manage Resources

```bash
# Apply configuration
kubectl apply -f deployment.yaml
kubectl apply -f ./configs/
kubectl apply -f https://example.com/deployment.yaml

# Create from file
kubectl create -f deployment.yaml

# Delete from file
kubectl delete -f deployment.yaml

# Replace resource
kubectl replace -f deployment.yaml

# Diff before apply
kubectl diff -f deployment.yaml

# Dry run
kubectl apply -f deployment.yaml --dry-run=client
kubectl apply -f deployment.yaml --dry-run=server
```

## Jobs and CronJobs

```bash
# Create job
kubectl create job <name> --image=<image>

# List jobs
kubectl get jobs

# Delete job
kubectl delete job <name>

# List CronJobs
kubectl get cronjobs
kubectl get cj

# Create CronJob
kubectl create cronjob <name> --image=<image> --schedule="*/5 * * * *"

# Delete CronJob
kubectl delete cronjob <name>
```

## RBAC

```bash
# List service accounts
kubectl get serviceaccounts
kubectl get sa

# Create service account
kubectl create serviceaccount <name>

# List roles
kubectl get roles

# List cluster roles
kubectl get clusterroles

# List role bindings
kubectl get rolebindings

# List cluster role bindings
kubectl get clusterrolebindings

# Create role binding
kubectl create rolebinding <name> --role=<role> --serviceaccount=<namespace>:<sa-name>

# Check permissions
kubectl auth can-i create pods
kubectl auth can-i create pods --as=system:serviceaccount:<namespace>:<sa-name>
```

## Events and Troubleshooting

```bash
# Get events
kubectl get events
kubectl get events --sort-by='.lastTimestamp'
kubectl get events --field-selector type=Warning

# Describe for troubleshooting
kubectl describe pod <pod-name>
kubectl describe node <node-name>

# Check pod restarts
kubectl get pods --field-selector=status.phase=Running

# Get pod by status
kubectl get pods --field-selector=status.phase!=Running

# Debug pod
kubectl debug <pod-name> -it --image=busybox

# Run temporary pod for debugging
kubectl run debug-pod --rm -it --image=busybox -- sh

# Cordon node (mark unschedulable)
kubectl cordon <node-name>

# Uncordon node
kubectl uncordon <node-name>

# Drain node
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data
```

## Resource Quota and Limits

```bash
# List resource quotas
kubectl get resourcequotas
kubectl get quota

# Describe quota
kubectl describe quota <name>

# List limit ranges
kubectl get limitranges
kubectl get limits
```

## Advanced Operations

```bash
# Patch resource
kubectl patch deployment <name> -p '{"spec":{"replicas":3}}'

# Set resources
kubectl set resources deployment <name> --limits=cpu=200m,memory=512Mi --requests=cpu=100m,memory=256Mi

# Set env variable
kubectl set env deployment/<name> ENV_VAR=value

# Create from stdin
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: test-pod
spec:
  containers:
  - name: test
    image: nginx
EOF

# Export resource to file
kubectl get deployment <name> -o yaml > deployment.yaml

# Get API resources
kubectl api-resources

# Get API versions
kubectl api-versions

# Explain resource
kubectl explain pod
kubectl explain pod.spec.containers
```

## Cleanup Commands

```bash
# Delete all pods in namespace
kubectl delete pods --all -n <namespace>

# Delete all resources in namespace
kubectl delete all --all -n <namespace>

# Force delete pod
kubectl delete pod <pod-name> --force --grace-period=0

# Delete completed jobs
kubectl delete jobs --field-selector status.successful=1

# Delete evicted pods
kubectl get pods | grep Evicted | awk '{print $1}' | xargs kubectl delete pod
```