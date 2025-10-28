# 🧩 SRMS Backend — Secure Jenkins Server & Worker Setup Guide

This guide helps developers quickly deploy or update the **secure Jenkins environment** for the **SRMS Backend** project — including both **controller (master)** and **worker node** setup — on a fresh or existing Ubuntu-based Linux server.

---

## ⚙️ 1. System Preparation

Update and clean up your system before installation:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt autoremove -y
````

---

## 🧱 2. Jenkins Controller Setup

This command will:

* Ensure `git` is installed
* Clone the **SRMS-backend** repository (if not already present)
* Pull the latest changes
* Run the setup script at `script/ci/setup-jenkins-controller.sh`

> 💡 Safe to re-run — it will automatically update or reconfigure as needed.

```bash
bash -c 'set -e; REPO="https://github.com/lvoxx/SRMS-backend.git"; DIR="SRMS-backend"; if ! command -v git &>/dev/null; then echo "Installing git..."; sudo apt-get update -qq && sudo apt-get install -y git; fi; if [ ! -d "$DIR/.git" ]; then git clone --filter=blob:none --sparse "$REPO" "$DIR"; fi; cd "$DIR"; git pull --force origin main; git sparse-checkout set script; bash ./script/ci/setup-jenkins-controller.sh'
```

---

## 🧩 3. Jenkins Worker Node Setup

Use this on **remote build agents (workers)** that connect to your Jenkins master.

This script will:

* Install **Java 21**, **Maven**, and **Docker**
* Create a dedicated `jenkins` user (if not already present)
* Prepare the `/opt/jenkins` workspace
* Set up environment variables for builds

> 💡 The worker node does **not** install Jenkins itself — it only runs the `agent.jar` to connect back to your controller.

```bash
bash -c 'set -e; REPO="https://github.com/lvoxx/SRMS-backend.git"; DIR="SRMS-backend"; if ! command -v git &>/dev/null; then echo "Installing git..."; sudo apt-get update -qq && sudo apt-get install -y git; fi; if [ ! -d "$DIR/.git" ]; then git clone --filter=blob:none --sparse "$REPO" "$DIR"; fi; cd "$DIR"; git pull --force origin main; git sparse-checkout set script; bash ./script/ci/setup-jenkins-worker.sh'
```

After installation completes:

1. Log in as the Jenkins user:

   ```bash
   sudo su - jenkins
   ```
2. Download the agent jar:

   ```bash
   wget http://<jenkins-master>:8080/jnlpJars/agent.jar
   ```
3. Connect the worker to your Jenkins master:

   ```bash
   java -jar agent.jar -jnlpUrl http://<jenkins-master>:8080/computer/<node-name>/slave-agent.jnlp -secret <SECRET>
   ```

---

## 🔄 4. Secure Jenkins Re-Setup or Update

Use this if Jenkins is **already installed**, and you only want to **update** the repository and re-run the security hardening script (`secure-jenkins-server.sh`):

```bash
bash -c 'set -e; REPO_DIR="SRMS-backend"; GREEN="\e[32m"; BLUE="\e[36m"; YELLOW="\e[33m"; RESET="\e[0m"; echo -e "${BLUE}🔄 Updating repository...${RESET}"; cd "$REPO_DIR" && git fetch origin main && LOCAL=$(git rev-parse HEAD) && REMOTE=$(git rev-parse origin/main); if [ "$LOCAL" != "$REMOTE" ]; then echo -e "${YELLOW}📥 New update detected. Pulling changes...${RESET}"; git reset --hard origin/main; else echo -e "${GREEN}✅ Already up to date.${RESET}"; fi; SCRIPT="./script/ci/secure-jenkins-server.sh"; if [ -f "$SCRIPT" ]; then echo -e "${BLUE}🚀 Running Jenkins secure setup script...${RESET}"; chmod +x "$SCRIPT" && bash "$SCRIPT"; else echo -e "${YELLOW}⚠️ Script not found at $SCRIPT${RESET}"; fi'
```

---

## 🧾 Notes

* Run all commands as a **sudo-capable user**.
* All setup scripts are **idempotent** — safe to re-run multiple times.
* The setup will handle:

  * Jenkins installation & hardening (controller)
  * Java 21 (Temurin) installation
  * Maven & Docker setup
  * Worker node environment configuration
  * Firewall & service security hardening

---

**Author:** Lvoxx
**Repository:** [SRMS-backend](https://github.com/lvoxx/SRMS-backend)
**License:** Internal use only

