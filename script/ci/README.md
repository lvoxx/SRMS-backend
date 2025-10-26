# 🧩 SRMS Backend — Secure Jenkins Server Setup Guide

This guide helps developers quickly deploy or update the **secure Jenkins environment** for the **SRMS Backend** project on a fresh or existing Linux server (Ubuntu-based).

---

## ⚙️ 1. System Preparation

Update and clean up your system before installation:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt autoremove -y
````

---

## 🧱 2. Initial Jenkins Setup

This command will:

* Ensure `git` is installed
* Clone the **SRMS-backend** repository (if not present)
* Pull the latest changes
* Run the setup script at `script/ci/setup-jenkins.sh`

> 💡 Safe to re-run — it will update or reconfigure automatically.

```bash
bash -c 'set -e; REPO="https://github.com/lvoxx/SRMS-backend.git"; DIR="SRMS-backend"; if ! command -v git &>/dev/null; then echo "Installing git..."; sudo apt-get update -qq && sudo apt-get install -y git; fi; if [ ! -d "$DIR/.git" ]; then git clone --filter=blob:none --sparse "$REPO" "$DIR"; fi; cd "$DIR"; git pull --force origin main; git sparse-checkout set script; bash ./script/ci/setup-jenkins.sh'
```

---

## 🔄 3. Secure Jenkins Re-Setup or Update

Use this command if Jenkins is **already installed**, and you just want to **update** the repository and rerun the security setup script (`secure-jenkins-server.sh`).

```bash
bash -c 'set -e; REPO_DIR="SRMS-backend"; GREEN="\e[32m"; BLUE="\e[36m"; YELLOW="\e[33m"; RESET="\e[0m"; echo -e "${BLUE}🔄 Updating repository...${RESET}"; cd "$REPO_DIR" && git fetch origin main && LOCAL=$(git rev-parse HEAD) && REMOTE=$(git rev-parse origin/main); if [ "$LOCAL" != "$REMOTE" ]; then echo -e "${YELLOW}📥 New update detected. Pulling changes...${RESET}"; git reset --hard origin/main; else echo -e "${GREEN}✅ Already up to date.${RESET}"; fi; SCRIPT="./script/ci/secure-jenkins-server.sh"; if [ -f "$SCRIPT" ]; then echo -e "${BLUE}🚀 Running Jenkins secure setup script...${RESET}"; chmod +x "$SCRIPT" && bash "$SCRIPT"; else echo -e "${YELLOW}⚠️ Script not found at $SCRIPT${RESET}"; fi'
```

---

## 🧾 Notes

* Make sure to run all commands as a **sudo-capable user**.
* You can run both setup commands multiple times — they are **idempotent**.
* The setup will handle:

  * Jenkins installation & hardening
  * Admin user creation
  * Java 21 (Temurin) installation
  * Firewall & service security configuration

---

**Author:** Lvoxx </br>
**Repository:** [SRMS-backend](https://github.com/lvoxx/SRMS-backend) </br>
**License:** Internal use only 