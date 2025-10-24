#!/bin/bash
set -e

# =========================
# 🌐 Ansible Semaphore Web UI Installer
# =========================

GREEN="\e[32m"
YELLOW="\e[33m"
BLUE="\e[36m"
RED="\e[31m"
RESET="\e[0m"

echo -e "${BLUE}🚀 Installing Ansible Semaphore Web UI...${RESET}"

# --- Install dependencies ---
echo -e "${YELLOW}📦 Installing dependencies...${RESET}"
sudo apt update -y
sudo apt install -y wget tar systemctl sqlite3

# --- Download latest Semaphore ---
echo -e "${BLUE}📥 Downloading Semaphore...${RESET}"
VERSION=$(curl -s https://api.github.com/repos/ansible-semaphore/semaphore/releases/latest | grep tag_name | cut -d '"' -f 4)
wget https://github.com/ansible-semaphore/semaphore/releases/download/${VERSION}/semaphore_${VERSION#v}_linux_amd64.tar.gz

tar -xzf semaphore_${VERSION#v}_linux_amd64.tar.gz
sudo mv semaphore /usr/local/bin/semaphore

# --- Setup config ---
echo -e "${YELLOW}⚙️ Setting up config directory...${RESET}"
sudo mkdir -p /etc/semaphore
sudo mkdir -p /var/lib/semaphore

# --- Initialize database (SQLite) ---
echo -e "${BLUE}🧱 Initializing database...${RESET}"
sudo semaphore setup --config /etc/semaphore/config.json --db /var/lib/semaphore/semaphore.db <<EOF
y
sqlite3
/var/lib/semaphore/semaphore.db
127.0.0.1
admin
admin
admin@example.com
EOF

# --- Create systemd service ---
echo -e "${YELLOW}🧩 Creating systemd service...${RESET}"
sudo bash -c 'cat > /etc/systemd/system/semaphore.service <<EOL
[Unit]
Description=Ansible Semaphore Web UI
After=network.target

[Service]
ExecStart=/usr/local/bin/semaphore server --config /etc/semaphore/config.json
Restart=always
User=root

[Install]
WantedBy=multi-user.target
EOL'

# --- Enable & start service ---
sudo systemctl daemon-reload
sudo systemctl enable semaphore
sudo systemctl start semaphore

# --- Verify status ---
if systemctl is-active --quiet semaphore; then
  echo -e "${GREEN}✅ Semaphore Web UI is running!${RESET}"
  echo -e "${BLUE}🌍 Access it at: http://<your-server-ip>:3000${RESET}"
else
  echo -e "${RED}❌ Semaphore failed to start.${RESET}"
  sudo journalctl -u semaphore --no-pager -n 20
fi
