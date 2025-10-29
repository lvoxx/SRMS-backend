# 🚀 SRMS Backend – Jenkins CI/CD Pipeline

## 📋 Overview

This repository contains the complete CI/CD pipeline configuration for the **Sakura Restaurant Management System (SRMS)** backend services.  
The pipeline is implemented using **Jenkins Declarative Pipelines**, designed to be **modular**, **multi-language**, and **Docker-based**.

Currently supports:
- ✅ Java (Spring Boot microservices)
- 🔜 Python (AI services)
- 🔜 Go (lightweight microservices)

---

## 🧱 Pipeline Architecture

### Root-level Jenkinsfiles

| File | Description |
|------|--------------|
| `Jenkinsfile` | Main pipeline for building and pushing all Spring Boot services |
| `Jenkinsfile.python` | (Future) Pipeline for Python-based AI modules |
| `Jenkinsfile.go` | (Future) Pipeline for Go microservices |
| `Jenkinsfile.master` | (Optional) Master orchestrator that triggers all pipelines |

Each Jenkinsfile can run **independently** or be **orchestrated** by the master pipeline.

---

## 📁 Repository Structure

