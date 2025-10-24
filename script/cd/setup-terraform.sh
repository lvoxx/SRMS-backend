#!/bin/bash
set -e

# =========================
# 🌍 Terraform Installer
# Tested on Ubuntu 20.04+ 
# =========================

# --- Color setup ---
GREEN="\e[32m"
YELLOW="\e[33m"
BLUE="\e[36m"
RED="\e[31m"
RESET="\e[0m"

echo -e "${BLUE}🚀 Starting Terraform installation...${RESET}"

# --- Update packages ---
echo -e "${YELLOW}🔄 Updating package list...${RESET}"
sudo apt-get update -y

# --- Install dependencies ---
echo -e "${YELLOW}📦 Installing required packages...${RESET}"
sudo apt-get install -y gnupg software-properties-common curl lsb-release

# --- Add HashiCorp GPG key ---
echo -e "${BLUE}🔑 Adding HashiCorp GPG key...${RESET}"
curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg

# --- Add HashiCorp repo ---
DISTRO=$(lsb_release -cs)
echo -e "${YELLOW}🧩 Adding HashiCorp repo for ${DISTRO}...${RESET}"
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com ${DISTRO} main" | sudo tee /etc/apt/sources.list.d/hashicorp.list

# --- Install Terraform ---
echo -e "${BLUE}📥 Installing Terraform...${RESET}"
sudo apt-get update -y
sudo apt-get install -y terraform

# --- Verify installation ---
if command -v terraform >/dev/null 2>&1; then
  echo -e "${GREEN}✅ Terraform installed successfully!${RESET}"
  terraform -version
else
  echo -e "${RED}❌ Terraform installation failed.${RESET}"
  exit 1
fi
