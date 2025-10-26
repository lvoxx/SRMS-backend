# 🧩 SRMS Backend — CI/CD Environment Overview

This document explains the structure and purpose of the **CI/CD system** used in the SRMS Backend project.  
Both `ci/` and `cd/` folders contain ready-to-use automation scripts — **do not rewrite them manually**.  
All DevOps and developers should follow these standards for consistency and reliability.

---

## 📂 1. `ci/` — Continuous Integration (Jenkins)

**Purpose:**  
Handles build, test, and integration automation using **Jenkins**.

- Designed for **a single central machine** (the main Jenkins server).  
- Can optionally include multiple **agent nodes** to distribute builds.  
- The scripts inside `ci/`:
  - Install and secure Jenkins
  - Set up Java, required plugins, and system hardening
  - Configure admin user and service management

**Deployment rule:**
> Every CI node (main or agent) must use the provided scripts from this folder.  
> Do **not** rewrite or modify installation steps manually.

---

## 📂 2. `cd/` — Continuous Deployment (ArgoCD)

**Purpose:**  
Automates the **deployment pipeline** using **ArgoCD (GitOps)**.

- Can run on **one or two separate machines**, as long as ArgoCD is installed.  
- ArgoCD should be **linked directly to the `main` branch** of this Git repository.  
- Scripts inside `cd/` handle:
  - Installing and configuring ArgoCD
  - Registering applications or clusters
  - Setting up automatic synchronization and rollback

**Deployment rule:**
> Each CD environment must execute the setup scripts located inside `cd/README.md`.  
> Scripts are already standardized to hook with the project’s main Git branch.

---

## 🧾 Summary Table

| Folder | Purpose | Notes |
|---------|----------|-------|
| `ci/` | Jenkins CI environment | Usually 1 main node + optional agents |
| `cd/` | ArgoCD GitOps deployment | 1–2 separate nodes with ArgoCD integration |
| `common/` | Shared scripts and helpers | Can be imported by both CI and CD |
| `devops/` | Templates and infra definitions | Optional: Docker, Kubernetes, Terraform, etc. |

---

## ✅ Deployment Guidelines

- Always use the provided scripts — **do not reimplement setup logic**.  
- Only modify CI/CD logic with approval from the **core DevOps team**.  
- Jenkins (CI) and ArgoCD (CD) are automatically linked to the **`main` branch**.  
- Once a new commit is merged:
  1. **CI** runs build, test, and image packaging.  
  2. **CD** triggers ArgoCD to deploy updates automatically.

---

**Author:** Lvoxx  
**Project:** SRMS Backend  
**Version:** Infrastructure v1.2  
**License:** Internal Use Only
