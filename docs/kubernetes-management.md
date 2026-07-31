# Kubernetes & ArgoCD Management Guide

This document explains how to completely tear down your Kubernetes environment to save RAM, CPU, and disk space on your Mac, and how to spin it back up when you are ready to work again.

## 🛑 How to Tear Down (Save Resources)

When you are done working and want your Mac's resources back, you have two options depending on how much you want to delete.

### Option 1: Stop the Microservices (Keeps ArgoCD)
This will delete all your microservices, databases, and message brokers, freeing up a massive amount of RAM and CPU.

1. Go to your **ArgoCD Dashboard** (`https://localhost:8080`).
2. Click the **Delete** button on the `distributedms-app`.
3. Type the application name to confirm. 
*Note: ArgoCD will delete all the pods, but ArgoCD itself will remain running in the background (using very little resources).*

### Option 2: The "Nuke" Option (Maximum Resource Savings)
This deletes your microservices AND completely uninstalls ArgoCD from your cluster.

Run these commands in your terminal:
```bash
# 1. Delete all your microservices and infrastructure
kubectl delete -k k8s/overlays/dev

# 2. Completely uninstall ArgoCD
kubectl delete namespace argocd
```

### 🧹 Freeing Up Disk Space (Safer Docker Cleanup)
If you want to free up disk space but **KEEP** the microservice images you just built so you don't have to rebuild them later, run this safe command:
```bash
docker image prune
```
*(This only deletes temporary/dangling junk images, but protects your tagged microservices).*

If you ever want to completely wipe **everything** on Docker (including your project images), you would use `docker system prune -a`, but only do that if you are okay with rebuilding!

---

## 🟢 How to Spin Back Up

When you are ready to start coding again, here is how you bring your entire GitOps environment back to life in just a few commands.

### 1. Re-build your Docker Images (If you deleted them)
```bash
docker compose build
```

### 2. Re-install ArgoCD (If you used the Nuke Option)
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml --server-side --force-conflicts
```

### 3. Re-deploy the Application
```bash
kubectl apply -f k8s/argocd/app.yaml
```

### 4. Access the Dashboard
To get the new ArgoCD admin password:
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```
Port-forward the UI (if you want to view it on `localhost:8080`):
```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```
Login with username `admin` and the password generated above. ArgoCD will automatically read your GitHub repository and spin all your microservices back up!
