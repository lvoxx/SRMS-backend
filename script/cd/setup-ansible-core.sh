#!/bin/bash
set -e

# =========================
# ⚙️ Ansible Community Installer
# =========================

GREEN="\e[32m"
YELLOW="\e[33m"
BLUE="\e[36m"
RED="\e[31m"
RESET="\e[0m"

echo -e "${BLUE}🚀 Starting Ansible Community installation...${RESET}"

# --- Update & install dependencies ---
echo -e "${YELLOW}🔄 Updating system packages...${RESET}"
sudo apt update -y && sudo apt upgrade -y

echo -e "${YELLOW}📦 Installing dependencies...${RESET}"
sudo apt install -y software-properties-common python3 python3-pip python3-venv sshpass

# --- Add official Ansible PPA ---
echo -e "${BLUE}🧩 Adding Ansible PPA...${RESET}"
sudo add-apt-repository --yes --update ppa:ansible/ansible

# --- Install Ansible ---
echo -e "${BLUE}📥 Installing Ansible...${RESET}"
sudo apt install -y ansible

# --- Verify installation ---
if command -v ansible >/dev/null 2>&1; then
  echo -e "${GREEN}✅ Ansible installed successfully!${RESET}"
  ansible --version
else
  echo -e "${RED}❌ Installation failed.${RESET}"
  exit 1
fi
