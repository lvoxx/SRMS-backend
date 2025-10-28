#!/bin/bash
# ---------------------------------------------------------
# 🧰 Jenkins Server Setup Script
# Installs Jenkins + Java 21 + Maven + Docker on Ubuntu
# ---------------------------------------------------------

set -e

# 🎨 Màu sắc
GREEN="\e[32m"
YELLOW="\e[33m"
BLUE="\e[36m"
RED="\e[31m"
RESET="\e[0m"

# ✅ Hàm in log
function info()   { echo -e "${BLUE}➜ $1${RESET}"; }
function ok()     { echo -e "${GREEN}✔ $1${RESET}"; }
function warn()   { echo -e "${YELLOW}⚠ $1${RESET}"; }
function error()  { echo -e "${RED}✖ $1${RESET}" >&2; }

# ---------------------------------------------------------
# Bắt đầu
# ---------------------------------------------------------
info "Starting Jenkins server setup..."

# ---------------------------------------------------------
# Cập nhật hệ thống
# ---------------------------------------------------------
info "Updating package list..."
sudo apt update -y && sudo apt upgrade -y
ok "System updated."

# ---------------------------------------------------------
# Cài Java 21 (Temurin)
# ---------------------------------------------------------
info "Installing Java 21..."
sudo apt install -y wget apt-transport-https gpg
wget -O- https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /usr/share/keyrings/adoptium.asc > /dev/null
# echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list 
# README: Uncommand above for Adoptium (Ubuntu 22.04 LTS), below is for “Plucky” (Ubuntu 24.10)
# START
DISTRO=$(lsb_release -cs)
if [[ "$DISTRO" == "plucky" || "$DISTRO" == "noble" ]]; then
  DISTRO="jammy"
fi
echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $DISTRO main" | sudo tee /etc/apt/sources.list.d/adoptium.list
# END
sudo apt update -y
sudo apt install -y temurin-21-jdk
ok "Java 21 installed."

JAVA_HOME_PATH=$(dirname $(dirname $(readlink -f $(which java))))

# ---------------------------------------------------------
# Cài Maven
# ---------------------------------------------------------
info "Installing Maven..."
sudo apt install -y maven
MVN_HOME_PATH=$(dirname $(dirname $(readlink -f $(which mvn))))
ok "Maven installed."

# ---------------------------------------------------------
# Cài Docker
# ---------------------------------------------------------
info "Installing Docker..."
sudo apt remove -y docker docker-engine docker.io containerd runc || true
sudo apt update -y
sudo apt install -y ca-certificates curl gnupg lsb-release

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update -y
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
ok "Docker installed and running."

# ---------------------------------------------------------
# Cài Jenkins
# ---------------------------------------------------------
info "Installing Jenkins LTS..."
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update -y
sudo apt install -y jenkins
sudo systemctl enable jenkins
sudo systemctl start jenkins
ok "Jenkins installed and started."

# ---------------------------------------------------------
# Thiết lập JAVA_HOME & MVN_HOME vĩnh viễn
# ---------------------------------------------------------
info "Setting JAVA_HOME and MVN_HOME globally..."

sudo sed -i '/JAVA_HOME/d' /etc/environment || true
sudo sed -i '/MVN_HOME/d' /etc/environment || true
sudo sed -i '/M2_HOME/d' /etc/environment || true

echo "JAVA_HOME=$JAVA_HOME_PATH" | sudo tee -a /etc/environment
echo "MVN_HOME=$MVN_HOME_PATH" | sudo tee -a /etc/environment

ok "Environment variables set."

# ---------------------------------------------------------
# Thêm user Jenkins vào nhóm Docker
# ---------------------------------------------------------
info "Adding Jenkins user to Docker group..."
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
ok "Jenkins can now run Docker containers."

# ---------------------------------------------------------
# Hoàn tất
# ---------------------------------------------------------
info "Cleaning up..."
sudo apt autoremove -y

ok "✅ Jenkins server setup completed successfully!"
echo -e "${YELLOW}Access Jenkins at: http://$(hostname -I | awk '{print $1}'):8080${RESET}"
echo -e "${BLUE}Initial admin password:${RESET}"
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
echo
