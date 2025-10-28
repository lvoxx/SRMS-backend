#!/bin/bash
# ---------------------------------------------------------
# ⚙️ Jenkins Worker Node Setup Script
# Installs Java 21 + Maven + Docker on Ubuntu
# Connect manually to Jenkins master using agent.jar
# ---------------------------------------------------------

set -e

# 🎨 Màu sắc
GREEN="\e[32m"
YELLOW="\e[33m"
BLUE="\e[36m"
RED="\e[31m"
RESET="\e[0m"

function info()   { echo -e "${BLUE}➜ $1${RESET}"; }
function ok()     { echo -e "${GREEN}✔ $1${RESET}"; }
function warn()   { echo -e "${YELLOW}⚠ $1${RESET}"; }
function error()  { echo -e "${RED}✖ $1${RESET}" >&2; }

# ---------------------------------------------------------
# Bắt đầu
# ---------------------------------------------------------
info "Starting Jenkins worker node setup..."

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

DISTRO=$(lsb_release -cs)
if [[ "$DISTRO" == "plucky" || "$DISTRO" == "noble" ]]; then
  DISTRO="jammy"
fi
echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $DISTRO main" | sudo tee /etc/apt/sources.list.d/adoptium.list

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
# Cài Docker (để build image hoặc chạy container)
# ---------------------------------------------------------
info "Installing Docker..."
sudo apt remove -y docker docker-engine docker.io containerd runc || true
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
# Thiết lập biến môi trường (JAVA_HOME / MVN_HOME)
# ---------------------------------------------------------
info "Setting JAVA_HOME and MVN_HOME globally..."

sudo sed -i '/JAVA_HOME/d' /etc/environment || true
sudo sed -i '/MVN_HOME/d' /etc/environment || true
sudo sed -i '/M2_HOME/d' /etc/environment || true

echo "JAVA_HOME=$JAVA_HOME_PATH" | sudo tee -a /etc/environment
echo "MVN_HOME=$MVN_HOME_PATH" | sudo tee -a /etc/environment
ok "Environment variables set."

# ---------------------------------------------------------
# Tạo user riêng cho Jenkins agent (tùy chọn)
# ---------------------------------------------------------
if ! id "jenkins" &>/dev/null; then
  info "Creating Jenkins user..."
  sudo useradd -m -s /bin/bash jenkins
  sudo usermod -aG docker jenkins
  ok "User 'jenkins' created and added to docker group."
else
  warn "User 'jenkins' already exists. Adding to docker group..."
  sudo usermod -aG docker jenkins
fi

# ---------------------------------------------------------
# Chuẩn bị thư mục làm việc cho agent
# ---------------------------------------------------------
sudo mkdir -p /opt/jenkins
sudo chown -R jenkins:jenkins /opt/jenkins
sudo chmod -R 755 /opt/jenkins
sudo chown -R jenkins:jenkins /opt/jenkins/remoting
sudo chmod -R 755 /opt/jenkins/remoting
ok "Workspace ready at /opt/jenkins."

# ---------------------------------------------------------
# Hướng dẫn kết nối agent tới master
# ---------------------------------------------------------
echo ""
info "✅ Jenkins Worker Node setup completed successfully!"
echo ""
echo -e "${YELLOW}Next steps:${RESET}"
echo -e "1. Log in as Jenkins user: ${BLUE}sudo su - jenkins${RESET}"
echo -e "2. Download agent.jar from your Jenkins master (e.g.):"
echo -e "   ${GREEN}wget http://<jenkins-master>:8080/jnlpJars/agent.jar${RESET}"
echo -e "3. Connect to master using the secret key provided in node config:"
echo -e "   ${GREEN}java -jar agent.jar -jnlpUrl http://<jenkins-master>:8080/computer/<node-name>/slave-agent.jnlp -secret <SECRET>${RESET}"
echo ""