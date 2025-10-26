#!/bin/bash

# Ansible Semaphore UI installation script for Ubuntu 25.04 x64
# Usage: sudo bash install-ansible-semaphore.sh

set -e

echo "=== Starting Ansible Semaphore UI Installation ==="

# Update package list
echo "Updating package list..."
apt-get update

# Install dependencies
echo "Installing dependencies..."
apt-get install -y wget git ansible mysql-server

# Start MySQL
echo "Starting MySQL service..."
systemctl start mysql
systemctl enable mysql

# Create database and user for Semaphore
echo "Creating database for Semaphore..."
mysql -e "CREATE DATABASE IF NOT EXISTS semaphore;"
mysql -e "CREATE USER IF NOT EXISTS 'semaphore'@'localhost' IDENTIFIED BY 'semaphore_password';"
mysql -e "GRANT ALL PRIVILEGES ON semaphore.* TO 'semaphore'@'localhost';"
mysql -e "FLUSH PRIVILEGES;"

# Download Semaphore
SEMAPHORE_VERSION=$(curl -s https://api.github.com/repos/semaphoreui/semaphore/releases/latest | grep '"tag_name"' | sed -E 's/.*"v([^"]+)".*/\1/')
echo "Downloading Semaphore version ${SEMAPHORE_VERSION}..."

wget -O /tmp/semaphore_${SEMAPHORE_VERSION}_linux_amd64.deb \
    https://github.com/semaphoreui/semaphore/releases/download/v${SEMAPHORE_VERSION}/semaphore_${SEMAPHORE_VERSION}_linux_amd64.deb

# Install Semaphore
echo "Installing Semaphore..."
dpkg -i /tmp/semaphore_${SEMAPHORE_VERSION}_linux_amd64.deb || apt-get install -f -y

# Create configuration directory
mkdir -p /etc/semaphore
mkdir -p /var/lib/semaphore

# Create configuration file
echo "Creating configuration file..."
cat > /etc/semaphore/config.json << 'EOF'
{
  "mysql": {
    "host": "127.0.0.1:3306",
    "user": "semaphore",
    "pass": "semaphore_password",
    "name": "semaphore"
  },
  "port": ":3000",
  "tmp_path": "/tmp/semaphore",
  "cookie_hash": "CHANGE_THIS_TO_RANDOM_STRING",
  "cookie_encryption": "CHANGE_THIS_TO_RANDOM_STRING",
  "access_key_encryption": "CHANGE_THIS_TO_RANDOM_STRING",
  "email_secure": false,
  "web_host": "http://localhost:3000"
}
EOF

# Create systemd service
echo "Creating systemd service..."
cat > /etc/systemd/system/semaphore.service << 'EOF'
[Unit]
Description=Semaphore Ansible UI
Documentation=https://github.com/semaphoreui/semaphore
After=network.target mysql.service

[Service]
Type=simple
ExecStart=/usr/bin/semaphore service --config /etc/semaphore/config.json
User=root
Group=root
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd and start service
echo "Starting Semaphore service..."
systemctl daemon-reload
systemctl enable semaphore
systemctl start semaphore

# Wait for service to start
sleep 5

# Check status
systemctl status semaphore --no-pager || true

echo ""
echo "=== Installation Complete ==="
echo ""
echo "Semaphore UI has been successfully installed!"
echo ""
echo "Access information:"
echo "  URL: http://localhost:3000"
echo "  Or: http://$(hostname -I | awk '{print $1}'):3000"
echo ""
echo "On first access, you will need to create an admin account."
echo ""
echo "SECURITY NOTES:"
echo "  1. Change MySQL password in /etc/semaphore/config.json"
echo "  2. Replace cookie_hash, cookie_encryption, and access_key_encryption values"
echo "  3. Configure firewall if necessary"
echo ""
echo "Service management:"
echo "  - Start: sudo systemctl start semaphore"
echo "  - Stop: sudo systemctl stop semaphore"
echo "  - Restart: sudo systemctl restart semaphore"
echo "  - View logs: sudo journalctl -u semaphore -f"