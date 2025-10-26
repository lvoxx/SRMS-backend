#!/bin/bash

# Ubuntu Server Security Script for Jenkins with HTTPS (Direct Port 443)
# Run as root: sudo bash secure_server.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Ubuntu Server Security Setup for Jenkins${NC}"
echo -e "${GREEN}with HTTPS (Self-Signed Certificate)${NC}"
echo -e "${GREEN}================================================${NC}\n"

# Check root privileges
if [[ $EUID -ne 0 ]]; then
   echo -e "${RED}This script must be run as root!${NC}" 
   exit 1
fi

# Get server IP
SERVER_IP=$(hostname -I | awk '{print $1}')
echo -e "${BLUE}Detected server IP: ${GREEN}${SERVER_IP}${NC}\n"

# Create admin user
echo -e "${BLUE}Creating admin user...${NC}"
read -p "Enter admin username (default: admin): " ADMIN_USER
ADMIN_USER=${ADMIN_USER:-admin}

if id "$ADMIN_USER" &>/dev/null; then
    echo -e "${YELLOW}User $ADMIN_USER already exists. Skipping creation.${NC}"
else
    read -s -p "Enter password for $ADMIN_USER: " ADMIN_PASS
    echo
    read -s -p "Confirm password: " ADMIN_PASS_CONFIRM
    echo
    
    if [ "$ADMIN_PASS" != "$ADMIN_PASS_CONFIRM" ]; then
        echo -e "${RED}Passwords do not match!${NC}"
        exit 1
    fi
    
    # Create user with home directory and bash shell
    useradd -m -s /bin/bash "$ADMIN_USER"
    echo "$ADMIN_USER:$ADMIN_PASS" | chpasswd
    
    # Add to sudo group
    usermod -aG sudo "$ADMIN_USER"
    
    echo -e "${GREEN}✓ Admin user $ADMIN_USER created successfully${NC}"
fi

# 1. Update system
echo -e "${YELLOW}[1/13] Updating system...${NC}"
apt update && apt upgrade -y

# 2. Install Java if not present (required for keytool)
echo -e "${YELLOW}[2/13] Checking Java installation...${NC}"
if ! command -v java &> /dev/null; then
    apt install -y openjdk-17-jdk
fi

# 3. Generate SSL certificate and keystore for Jenkins
echo -e "${YELLOW}[3/13] Generating SSL certificate and keystore...${NC}"
JENKINS_HOME="/var/lib/jenkins"
KEYSTORE_DIR="$JENKINS_HOME/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/jenkins.jks"
KEYSTORE_PASS="changeit"

mkdir -p "$KEYSTORE_DIR"

# Generate self-signed certificate in JKS format
if [ ! -f "$KEYSTORE_FILE" ]; then
    keytool -genkeypair -alias jenkins -keyalg RSA -keysize 4096 \
        -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" \
        -dname "CN=$SERVER_IP, OU=Jenkins, O=Jenkins, L=CanTho, ST=CanTho, C=VN" \
        -validity 365 -ext san=ip:"$SERVER_IP"
    
    echo -e "${GREEN}✓ SSL keystore created${NC}"
else
    echo -e "${YELLOW}⚠ Keystore already exists, skipping generation${NC}"
fi

# Also generate PEM format for backup/reference
openssl req -x509 -nodes -days 365 -newkey rsa:4096 \
    -keyout "$KEYSTORE_DIR/jenkins.key" \
    -out "$KEYSTORE_DIR/jenkins.crt" \
    -subj "/C=VN/ST=CanTho/L=CanTho/O=Jenkins/CN=$SERVER_IP"

# Set permissions
chown -R jenkins:jenkins "$KEYSTORE_DIR" 2>/dev/null || true
chmod 700 "$KEYSTORE_DIR"
chmod 600 "$KEYSTORE_DIR"/* 2>/dev/null || true

# 4. Configure UFW Firewall
echo -e "${YELLOW}[4/13] Configuring UFW Firewall...${NC}"
apt install -y ufw
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 443/tcp
ufw allow 80/tcp
echo "y" | ufw enable
ufw status

# 5. Configure Jenkins to listen on port 443 with HTTPS
echo -e "${YELLOW}[5/13] Configuring Jenkins for HTTPS on port 443...${NC}"

# Allow Jenkins to bind to privileged ports (below 1024)
if command -v setcap &> /dev/null && [ -f "/usr/bin/java" ]; then
    setcap 'cap_net_bind_service=+ep' /usr/bin/java
    echo -e "${GREEN}✓ Java permitted to bind to port 443${NC}"
fi

# Configure Jenkins systemd service
JENKINS_SERVICE="/lib/systemd/system/jenkins.service"
if [ -f "$JENKINS_SERVICE" ]; then
    mkdir -p /etc/systemd/system/jenkins.service.d/
    
    cat > /etc/systemd/system/jenkins.service.d/override.conf << EOF
[Service]
Environment="JENKINS_PORT=-1"
Environment="JENKINS_HTTPS_PORT=443"
Environment="JENKINS_HTTPS_KEYSTORE=$KEYSTORE_FILE"
Environment="JENKINS_HTTPS_KEYSTORE_PASSWORD=$KEYSTORE_PASS"
Environment="JENKINS_HTTPS_LISTEN_ADDRESS=0.0.0.0"
Environment="JAVA_OPTS=-Djava.awt.headless=true -Djavax.net.ssl.trustStore=$KEYSTORE_FILE -Djavax.net.ssl.trustStorePassword=$KEYSTORE_PASS"
AmbientCapabilities=CAP_NET_BIND_SERVICE
EOF
    
    systemctl daemon-reload
    echo -e "${GREEN}✓ Jenkins service configured for HTTPS on port 443${NC}"
else
    echo -e "${YELLOW}⚠ Jenkins not installed yet. Configuration will be applied when Jenkins is installed.${NC}"
fi

# Create Jenkins configuration file
mkdir -p /etc/default
cat > /etc/default/jenkins << EOF
# Jenkins HTTPS configuration
HTTP_PORT=-1
HTTPS_PORT=443
HTTPS_LISTEN_ADDRESS=0.0.0.0
HTTPS_KEYSTORE=$KEYSTORE_FILE
HTTPS_KEYSTORE_PASSWORD=$KEYSTORE_PASS
JENKINS_ARGS="--httpPort=-1 --httpsPort=443 --httpsListenAddress=0.0.0.0 --httpsKeyStore=$KEYSTORE_FILE --httpsKeyStorePassword=$KEYSTORE_PASS"
EOF

# 6. Setup HTTP to HTTPS redirect (optional simple redirect page)
echo -e "${YELLOW}[6/13] Setting up HTTP to HTTPS redirect...${NC}"

# Create a simple redirect script
cat > /usr/local/bin/http-redirect.sh << 'EOFRED'
#!/bin/bash
while true; do
    echo -e "HTTP/1.1 301 Moved Permanently\r\nLocation: https://$SERVER_IP\r\nConnection: close\r\n\r\n" | nc -l -p 80 -q 1
done
EOFRED

chmod +x /usr/local/bin/http-redirect.sh

# Create systemd service for HTTP redirect
cat > /etc/systemd/system/http-redirect.service << EOF
[Unit]
Description=HTTP to HTTPS Redirect
After=network.target

[Service]
Type=simple
Environment="SERVER_IP=$SERVER_IP"
ExecStart=/usr/local/bin/http-redirect.sh
Restart=always
User=nobody
Group=nogroup

[Install]
WantedBy=multi-user.target
EOF

# Install netcat if not present
apt install -y netcat-openbsd

systemctl daemon-reload
systemctl enable http-redirect.service
systemctl start http-redirect.service

echo -e "${GREEN}✓ HTTP to HTTPS redirect configured${NC}"

# 7. Install Fail2Ban
echo -e "${YELLOW}[7/13] Installing Fail2Ban...${NC}"
apt install -y fail2ban
systemctl enable fail2ban
systemctl start fail2ban

# Configure Fail2Ban for SSH and Jenkins
cat > /etc/fail2ban/jail.local << EOF
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5
destemail = root@localhost
sendername = Fail2Ban

[sshd]
enabled = true
port = ssh
logpath = %(sshd_log)s
maxretry = 3
bantime = 7200

[jenkins]
enabled = true
port = 443
filter = jenkins
logpath = /var/log/jenkins/jenkins.log
maxretry = 5
EOF

# Create filter for Jenkins
cat > /etc/fail2ban/filter.d/jenkins.conf << 'EOF'
[Definition]
failregex = ^.*Failed login attempt.*from <HOST>.*$
            ^.*Invalid login attempt.*<HOST>.*$
            ^.*WARNING.*o\.e\.j\.s\.h\.ContextHandler.*<HOST>.*$
ignoreregex =
EOF

systemctl restart fail2ban

# 8. Secure SSH
echo -e "${YELLOW}[8/13] Securing SSH...${NC}"

# Backup original SSH config
cp /etc/ssh/sshd_config /etc/ssh/sshd_config.backup

# Configure SSH security
cat > /etc/ssh/sshd_config.d/99-security.conf << EOF
# Disable root login
PermitRootLogin no

# Enable public key authentication
PubkeyAuthentication yes

# Keep password authentication for now (disable after setting up SSH keys)
PasswordAuthentication yes

# Security settings
Protocol 2
MaxAuthTries 3
MaxSessions 5
ClientAliveInterval 300
ClientAliveCountMax 2
PermitEmptyPasswords no
X11Forwarding no

# Allow only specific user
AllowUsers $ADMIN_USER
EOF

systemctl restart sshd
echo -e "${GREEN}✓ SSH secured. Only user '$ADMIN_USER' can login${NC}"

# 9. Configure resource limits
echo -e "${YELLOW}[9/13] Configuring resource limits...${NC}"
cat >> /etc/security/limits.conf << EOF

# Resource limits
* soft nofile 65535
* hard nofile 65535
* soft nproc 65535
* hard nproc 65535
jenkins soft nofile 65535
jenkins hard nofile 65535
EOF

# 10. Install and configure AppArmor
echo -e "${YELLOW}[10/13] Configuring AppArmor...${NC}"
apt install -y apparmor apparmor-utils
systemctl enable apparmor
systemctl start apparmor

# 11. Secure Jenkins directories (if they exist)
echo -e "${YELLOW}[11/13] Securing Jenkins directories...${NC}"
if [ -d "/var/lib/jenkins" ]; then
    chown -R jenkins:jenkins /var/lib/jenkins
    chmod -R 750 /var/lib/jenkins
    echo -e "${GREEN}✓ Jenkins directory secured${NC}"
fi

if [ -d "/var/log/jenkins" ]; then
    chown -R jenkins:jenkins /var/log/jenkins
    chmod -R 750 /var/log/jenkins
fi

# 12. Disable unnecessary services
echo -e "${YELLOW}[12/13] Disabling unnecessary services...${NC}"
services_to_disable=("cups" "avahi-daemon" "bluetooth")
for service in "${services_to_disable[@]}"; do
    if systemctl list-unit-files | grep -q "$service"; then
        systemctl stop "$service" 2>/dev/null || true
        systemctl disable "$service" 2>/dev/null || true
    fi
done

# Configure automatic security updates
apt install -y unattended-upgrades
dpkg-reconfigure -plow unattended-upgrades

cat > /etc/apt/apt.conf.d/50unattended-upgrades << EOF
Unattended-Upgrade::Allowed-Origins {
    "\${distro_id}:\${distro_codename}-security";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::MinimalSteps "true";
Unattended-Upgrade::Remove-Unused-Kernel-Packages "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
EOF

# 13. Configure kernel parameters for security
echo -e "${YELLOW}[13/13] Configuring kernel parameters...${NC}"
cat >> /etc/sysctl.conf << EOF

# Security configurations
net.ipv4.conf.default.rp_filter=1
net.ipv4.conf.all.rp_filter=1
net.ipv4.tcp_syncookies=1
net.ipv4.conf.all.accept_redirects=0
net.ipv6.conf.all.accept_redirects=0
net.ipv4.conf.all.send_redirects=0
net.ipv4.conf.all.accept_source_route=0
net.ipv6.conf.all.accept_source_route=0
net.ipv4.conf.all.log_martians=1
net.ipv4.icmp_echo_ignore_broadcasts=1
net.ipv4.icmp_ignore_bogus_error_responses=1
kernel.dmesg_restrict=1
EOF

sysctl -p

# Install monitoring tools
echo -e "${YELLOW}Installing monitoring tools...${NC}"
apt install -y htop iotop iftop net-tools

# Create Jenkins backup script
echo -e "${YELLOW}Creating Jenkins backup script...${NC}"
cat > /usr/local/bin/backup_jenkins.sh << 'EOFBACKUP'
#!/bin/bash
BACKUP_DIR="/backup/jenkins"
JENKINS_HOME="/var/lib/jenkins"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR
tar -czf $BACKUP_DIR/jenkins_backup_$DATE.tar.gz $JENKINS_HOME 2>/dev/null

# Delete backups older than 7 days
find $BACKUP_DIR -name "jenkins_backup_*.tar.gz" -mtime +7 -delete 2>/dev/null

echo "Backup completed: jenkins_backup_$DATE.tar.gz"
EOFBACKUP

chmod +x /usr/local/bin/backup_jenkins.sh

# Create cronjob for daily backup at 2 AM
(crontab -l 2>/dev/null; echo "0 2 * * * /usr/local/bin/backup_jenkins.sh") | crontab -

# Setup SSH keys directory for admin user
mkdir -p /home/$ADMIN_USER/.ssh
chmod 700 /home/$ADMIN_USER/.ssh
touch /home/$ADMIN_USER/.ssh/authorized_keys
chmod 600 /home/$ADMIN_USER/.ssh/authorized_keys
chown -R $ADMIN_USER:$ADMIN_USER /home/$ADMIN_USER/.ssh

# Restart Jenkins if it's running
echo -e "${YELLOW}Restarting services...${NC}"
if systemctl is-active --quiet jenkins; then
    systemctl restart jenkins
    echo -e "${GREEN}✓ Jenkins restarted${NC}"
fi

echo -e "\n${GREEN}================================================${NC}"
echo -e "${GREEN}Security Configuration Complete!${NC}"
echo -e "${GREEN}================================================${NC}\n"

echo -e "${YELLOW}Summary of implemented measures:${NC}"
echo "✓ Created admin user: $ADMIN_USER (member of sudo group)"
echo "✓ System updated"
echo "✓ Self-signed SSL certificate generated"
echo "✓ Jenkins configured for HTTPS on port 443"
echo "✓ HTTP (port 80) redirects to HTTPS"
echo "✓ UFW Firewall configured (SSH, HTTP, HTTPS)"
echo "✓ Fail2Ban protection (SSH, Jenkins)"
echo "✓ SSH root login disabled"
echo "✓ SSH access restricted to: $ADMIN_USER"
echo "✓ Resource limits configured"
echo "✓ AppArmor enabled"
echo "✓ Jenkins directories secured"
echo "✓ Unnecessary services disabled"
echo "✓ Automatic security updates enabled"
echo "✓ Monitoring tools installed"
echo "✓ Kernel security hardened"
echo "✓ Automatic Jenkins backup configured"

echo -e "\n${BLUE}Jenkins Access Information:${NC}"
echo "HTTPS URL: https://$SERVER_IP"
echo "Jenkins listens on port 443 (HTTPS)"
echo "HTTP (port 80) automatically redirects to HTTPS"
echo ""
echo -e "${YELLOW}SSL Certificate Information:${NC}"
echo "Keystore location: $KEYSTORE_FILE"
echo "Keystore password: $KEYSTORE_PASS"
echo "Certificate files: $KEYSTORE_DIR/jenkins.crt and jenkins.key"

echo -e "\n${BLUE}SSH Access Information:${NC}"
echo "Admin user: $ADMIN_USER"
echo "SSH command: ssh $ADMIN_USER@$SERVER_IP"
echo "Root login is DISABLED"

echo -e "\n${YELLOW}IMPORTANT - Certificate Warning:${NC}"
echo "⚠  You will see a certificate warning in your browser"
echo "⚠  This is NORMAL for self-signed certificates"
echo "⚠  Click 'Advanced' and 'Proceed to site' (safe in this case)"
echo ""
echo "To avoid this warning, you need to:"
echo "1. Use a real domain name (not IP address)"
echo "2. Get a trusted certificate from Let's Encrypt or a CA"

echo -e "\n${YELLOW}Next steps you should take:${NC}"
echo "1. Setup SSH key for $ADMIN_USER:"
echo "   - On your local machine: ssh-copy-id $ADMIN_USER@$SERVER_IP"
echo "   - Or manually add your public key to: /home/$ADMIN_USER/.ssh/authorized_keys"
echo ""
echo "2. After setting up SSH key, disable password authentication:"
echo "   - Edit: /etc/ssh/sshd_config.d/99-security.conf"
echo "   - Change: PasswordAuthentication no"
echo "   - Restart SSH: sudo systemctl restart sshd"
echo ""
echo "3. Configure Jenkins Security:"
echo "   - Go to https://$SERVER_IP"
echo "   - Accept certificate warning"
echo "   - Complete Jenkins setup wizard"
echo "   - Go to Manage Jenkins > Configure Global Security"
echo "   - Enable security and create admin user"
echo ""
echo "4. Update Jenkins URL:"
echo "   - Go to Manage Jenkins > System"
echo "   - Set Jenkins URL: https://$SERVER_IP"
echo ""
echo "5. (Optional) Get trusted SSL certificate:"
echo "   - Get a domain name and point it to: $SERVER_IP"
echo "   - Install certbot: sudo apt install certbot"
echo "   - Get Let's Encrypt certificate"
echo "   - Import into Jenkins keystore"
echo ""
echo "6. Regular maintenance:"
echo "   - Update Jenkins plugins regularly"
echo "   - Monitor logs: /var/log/jenkins/"
echo "   - Check backups: /backup/jenkins/"
echo "   - Renew SSL certificate annually"

echo -e "\n${RED}IMPORTANT SECURITY NOTES:${NC}"
echo "⚠  Root login is DISABLED. Use: ssh $ADMIN_USER@$SERVER_IP"
echo "⚠  Test SSH connection as $ADMIN_USER before logging out!"
echo "⚠  Jenkins backups saved at: /backup/jenkins/"
echo "⚠  SSL keystore password: $KEYSTORE_PASS (change in production!)"
echo "⚠  Certificate valid for 365 days, remember to renew!"

echo -e "\n${BLUE}Services Status:${NC}"
echo "Fail2Ban: $(systemctl is-active fail2ban)"
echo "UFW: $(ufw status | grep Status | awk '{print $2}')"
echo "HTTP Redirect: $(systemctl is-active http-redirect)"
if systemctl is-active --quiet jenkins; then
    echo "Jenkins: $(systemctl is-active jenkins)"
else
    echo "Jenkins: not installed yet (run installation script)"
fi

echo -e "\n${GREEN}Test your services:${NC}"
echo "1. SSH access in a NEW terminal: ssh $ADMIN_USER@$SERVER_IP"
echo "2. Jenkins HTTPS: https://$SERVER_IP"
echo "3. HTTP redirect: http://$SERVER_IP (should redirect to HTTPS)"

echo -e "\n${YELLOW}After confirming everything works, you can optionally reboot:${NC}"
echo "sudo reboot"