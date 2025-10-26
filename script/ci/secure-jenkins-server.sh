#!/bin/bash

# Ubuntu Server Security Script for Jenkins (Port 8080)
# Run as root: sudo bash secure_server.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Ubuntu Server Security Setup for Jenkins${NC}"
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
    useradd -m -k /etc/skel -s /bin/bash "$ADMIN_USER"
    echo "$ADMIN_USER:$ADMIN_PASS" | chpasswd
    
    # Add to sudo group
    usermod -aG sudo "$ADMIN_USER"
    
    echo -e "${GREEN}✓ Admin user $ADMIN_USER created successfully${NC}"
fi

# 1. Update system
echo -e "${YELLOW}[1/11] Updating system...${NC}"
apt update && apt upgrade -y

# 2. Configure UFW Firewall
echo -e "${YELLOW}[2/11] Configuring UFW Firewall...${NC}"
apt install -y ufw
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 8080/tcp
echo "y" | ufw enable
ufw status

# 3. Configure Jenkins to listen on port 8080
echo -e "${YELLOW}[3/11] Configuring Jenkins for port 8080...${NC}"

# Configure Jenkins systemd service
JENKINS_SERVICE="/lib/systemd/system/jenkins.service"
if [ -f "$JENKINS_SERVICE" ]; then
    mkdir -p /etc/systemd/system/jenkins.service.d/
    
    cat > /etc/systemd/system/jenkins.service.d/override.conf << 'EOF'
[Service]
Environment="JENKINS_PORT=8080"
Environment="JENKINS_LISTEN_ADDRESS=0.0.0.0"
Environment="JAVA_OPTS=-Djava.awt.headless=true"
EOF
    
    systemctl daemon-reload
    echo -e "${GREEN}✓ Jenkins service configured for port 8080${NC}"
else
    echo -e "${YELLOW}⚠ Jenkins not installed yet. Configuration will be applied when Jenkins is installed.${NC}"
fi

# Create Jenkins configuration file
mkdir -p /etc/default
cat > /etc/default/jenkins << 'EOF'
# Jenkins configuration
HTTP_PORT=8080
JENKINS_ARGS="--httpPort=8080 --httpListenAddress=0.0.0.0"
EOF

# 4. Install Fail2Ban
echo -e "${YELLOW}[4/11] Installing Fail2Ban...${NC}"
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
port = 8080
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

# 5. Secure SSH
echo -e "${YELLOW}[5/11] Securing SSH...${NC}"

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

# 6. Configure resource limits
echo -e "${YELLOW}[6/11] Configuring resource limits...${NC}"
cat >> /etc/security/limits.conf << EOF

# Resource limits
* soft nofile 65535
* hard nofile 65535
* soft nproc 65535
* hard nproc 65535
jenkins soft nofile 65535
jenkins hard nofile 65535
EOF

# 7. Install and configure AppArmor
echo -e "${YELLOW}[7/11] Configuring AppArmor...${NC}"
apt install -y apparmor apparmor-utils
systemctl enable apparmor
systemctl start apparmor

# 8. Secure Jenkins directories (if they exist)
echo -e "${YELLOW}[8/11] Securing Jenkins directories...${NC}"
if [ -d "/var/lib/jenkins" ]; then
    chown -R jenkins:jenkins /var/lib/jenkins
    chmod -R 750 /var/lib/jenkins
    echo -e "${GREEN}✓ Jenkins directory secured${NC}"
fi

if [ -d "/var/log/jenkins" ]; then
    chown -R jenkins:jenkins /var/log/jenkins
    chmod -R 750 /var/log/jenkins
fi

# 9. Disable unnecessary services
echo -e "${YELLOW}[9/11] Disabling unnecessary services...${NC}"
services_to_disable=("cups" "avahi-daemon" "bluetooth")
for service in "${services_to_disable[@]}"; do
    if systemctl list-unit-files | grep -q "$service"; then
        systemctl stop "$service" 2>/dev/null || true
        systemctl disable "$service" 2>/dev/null || true
    fi
done

# Configure automatic security updates
echo -e "${YELLOW}Configuring automatic security updates...${NC}"
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

# 10. Configure kernel parameters for security
echo -e "${YELLOW}[10/11] Configuring kernel parameters...${NC}"
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

# 11. Install monitoring tools and create backup script
echo -e "${YELLOW}[11/11] Installing monitoring tools...${NC}"
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
echo "✓ Jenkins configured for port 8080"
echo "✓ UFW Firewall configured (SSH, Jenkins:8080)"
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
echo "URL: http://$SERVER_IP:8080"
echo "Jenkins listens on port 8080"
echo ""

echo -e "\n${BLUE}SSH Access Information:${NC}"
echo "Admin user: $ADMIN_USER"
echo "SSH command: ssh $ADMIN_USER@$SERVER_IP"
echo "Root login is DISABLED"

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
echo "   - Go to http://$SERVER_IP:8080"
echo "   - Complete Jenkins setup wizard"
echo "   - Go to Manage Jenkins > Configure Global Security"
echo "   - Enable security and create admin user"
echo ""
echo "4. Update Jenkins URL:"
echo "   - Go to Manage Jenkins > System"
echo "   - Set Jenkins URL: http://$SERVER_IP:8080"
echo ""
echo "5. (Optional) Setup HTTPS with Nginx reverse proxy:"
echo "   - Install Nginx"
echo "   - Get Let's Encrypt certificate (if you have a domain)"
echo "   - Configure Nginx as reverse proxy to localhost:8080"
echo ""
echo "6. Regular maintenance:"
echo "   - Update Jenkins plugins regularly"
echo "   - Monitor logs: /var/log/jenkins/"
echo "   - Check backups: /backup/jenkins/"

echo -e "\n${RED}IMPORTANT SECURITY NOTES:${NC}"
echo "⚠  Root login is DISABLED. Use: ssh $ADMIN_USER@$SERVER_IP"
echo "⚠  Test SSH connection as $ADMIN_USER before logging out!"
echo "⚠  Jenkins backups saved at: /backup/jenkins/"
echo "⚠  Jenkins running on HTTP (port 8080) - consider adding HTTPS"
echo "⚠  Make sure to setup Jenkins authentication immediately after installation"

echo -e "\n${BLUE}Services Status:${NC}"
echo "Fail2Ban: $(systemctl is-active fail2ban)"
echo "UFW: $(ufw status | grep Status | awk '{print $2}')"
if systemctl is-active --quiet jenkins; then
    echo "Jenkins: $(systemctl is-active jenkins)"
else
    echo "Jenkins: not installed yet (run installation script)"
fi

echo -e "\n${GREEN}Test your services:${NC}"
echo "1. SSH access in a NEW terminal: ssh $ADMIN_USER@$SERVER_IP"
echo "2. Jenkins: http://$SERVER_IP:8080"

echo -e "\n${YELLOW}After confirming everything works, you can optionally reboot:${NC}"
echo "sudo reboot"