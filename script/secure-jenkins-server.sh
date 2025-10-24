#!/bin/bash

# Ubuntu Server Security Script for Jenkins with Nginx Reverse Proxy
# Run as root: sudo bash secure_jenkins_server.sh

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

# Ask for domain configuration
echo -e "${BLUE}Nginx Reverse Proxy Configuration${NC}"
read -p "Enter domain name for Jenkins (e.g., jenkins.lvoxxserver.com): " DOMAIN_NAME
read -p "Do you want to install SSL certificate with Let's Encrypt? (y/n): " INSTALL_SSL

if [ -z "$DOMAIN_NAME" ]; then
    echo -e "${RED}Domain name cannot be empty!${NC}"
    exit 1
fi

# 1. Update system
echo -e "${YELLOW}[1/14] Updating system...${NC}"
apt update && apt upgrade -y

# 2. Install Nginx
echo -e "${YELLOW}[2/14] Installing Nginx...${NC}"
apt install -y nginx

# 3. Configure UFW Firewall
echo -e "${YELLOW}[3/14] Configuring UFW Firewall...${NC}"
apt install -y ufw
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 'Nginx Full'
echo "y" | ufw enable
ufw status

# 4. Configure Nginx reverse proxy for Jenkins
echo -e "${YELLOW}[4/14] Configuring Nginx Reverse Proxy...${NC}"

cat > /etc/nginx/sites-available/jenkins << EOF
upstream jenkins {
    keepalive 32;
    server 127.0.0.1:8080;
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN_NAME;

    # Let's Encrypt validation
    location ^~ /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $DOMAIN_NAME;

    # SSL certificates (will be configured by Let's Encrypt or self-signed)
    ssl_certificate /etc/ssl/certs/jenkins-selfsigned.crt;
    ssl_certificate_key /etc/ssl/private/jenkins-selfsigned.key;

    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logging
    access_log /var/log/nginx/jenkins.access.log;
    error_log /var/log/nginx/jenkins.error.log;

    # Max upload size
    client_max_body_size 100M;

    location / {
        proxy_pass http://jenkins;
        proxy_redirect off;

        proxy_set_header Host \$host:\$server_port;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Port \$server_port;

        # Jenkins specific settings
        proxy_http_version 1.1;
        proxy_request_buffering off;
        proxy_buffering off;
        
        # WebSocket support for Jenkins
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";

        # Timeouts
        proxy_connect_timeout 90;
        proxy_send_timeout 90;
        proxy_read_timeout 90;
    }
}
EOF

# Create temporary self-signed certificate
echo -e "${YELLOW}Creating self-signed SSL certificate...${NC}"
mkdir -p /etc/ssl/private
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/ssl/private/jenkins-selfsigned.key \
    -out /etc/ssl/certs/jenkins-selfsigned.crt \
    -subj "/C=VN/ST=CanTho/L=CanTho/O=Jenkins/CN=$DOMAIN_NAME"

chmod 600 /etc/ssl/private/jenkins-selfsigned.key

# Enable site
ln -sf /etc/nginx/sites-available/jenkins /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Test nginx configuration
nginx -t

# 5. Configure Jenkins for reverse proxy
echo -e "${YELLOW}[5/14] Configuring Jenkins for reverse proxy...${NC}"

# Update Jenkins configuration
JENKINS_CONFIG="/etc/default/jenkins"
if [ -f "$JENKINS_CONFIG" ]; then
    # Backup original config
    cp $JENKINS_CONFIG ${JENKINS_CONFIG}.backup
    
    # Add Jenkins arguments
    if ! grep -q "JENKINS_ARGS.*--prefix" $JENKINS_CONFIG; then
        sed -i 's/JENKINS_ARGS=""/JENKINS_ARGS="--prefix=\/jenkins"/' $JENKINS_CONFIG
    fi
    
    echo -e "${GREEN}✓ Jenkins config updated${NC}"
fi

# Configure Jenkins systemd service
JENKINS_SERVICE="/lib/systemd/system/jenkins.service"
if [ -f "$JENKINS_SERVICE" ]; then
    mkdir -p /etc/systemd/system/jenkins.service.d/
    
    cat > /etc/systemd/system/jenkins.service.d/override.conf << EOF
[Service]
Environment="JENKINS_ARGS=--httpListenAddress=127.0.0.1"
EOF
    
    systemctl daemon-reload
    echo -e "${GREEN}✓ Jenkins service configured to listen on localhost only${NC}"
fi

# 6. Install Fail2Ban
echo -e "${YELLOW}[6/14] Installing Fail2Ban...${NC}"
apt install -y fail2ban
systemctl enable fail2ban
systemctl start fail2ban

# Configure Fail2Ban for SSH, Nginx and Jenkins
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

[nginx-http-auth]
enabled = true
port = http,https
logpath = /var/log/nginx/error.log

[nginx-limit-req]
enabled = true
port = http,https
logpath = /var/log/nginx/error.log

[nginx-botsearch]
enabled = true
port = http,https
logpath = /var/log/nginx/access.log
maxretry = 2

[jenkins]
enabled = true
port = http,https
filter = jenkins
logpath = /var/log/jenkins/jenkins.log
maxretry = 5
EOF

# Create filter for Jenkins
cat > /etc/fail2ban/filter.d/jenkins.conf << EOF
[Definition]
failregex = ^.*Failed login attempt.*from <HOST>.*$
            ^.*Invalid login attempt.*<HOST>.*$
            ^.*WARNING.*o\.e\.j\.s\.h\.ContextHandler.*<HOST>.*$
ignoreregex =
EOF

systemctl restart fail2ban

# 7. Secure SSH
echo -e "${YELLOW}[7/14] Securing SSH...${NC}"
sed -i 's/#PermitRootLogin yes/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/PermitRootLogin yes/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config
sed -i 's/#PubkeyAuthentication yes/PubkeyAuthentication yes/' /etc/ssh/sshd_config

# Add SSH security configurations
cat >> /etc/ssh/sshd_config << EOF

# Security configurations
Protocol 2
MaxAuthTries 3
MaxSessions 5
ClientAliveInterval 300
ClientAliveCountMax 2
PermitEmptyPasswords no
X11Forwarding no
EOF

systemctl restart sshd

# 8. Configure resource limits
echo -e "${YELLOW}[8/14] Configuring resource limits...${NC}"
cat >> /etc/security/limits.conf << EOF

# Resource limits
* soft nofile 65535
* hard nofile 65535
* soft nproc 65535
* hard nproc 65535
jenkins soft nofile 65535
jenkins hard nofile 65535
nginx soft nofile 65535
nginx hard nofile 65535
EOF

# 9. Install and configure AppArmor
echo -e "${YELLOW}[9/14] Configuring AppArmor...${NC}"
apt install -y apparmor apparmor-utils
systemctl enable apparmor
systemctl start apparmor

# 10. Secure Jenkins directories
echo -e "${YELLOW}[10/14] Securing Jenkins directories...${NC}"
if [ -d "/var/lib/jenkins" ]; then
    chown -R jenkins:jenkins /var/lib/jenkins
    chmod -R 750 /var/lib/jenkins
    echo -e "${GREEN}✓ Jenkins directory secured${NC}"
fi

if [ -d "/var/log/jenkins" ]; then
    chown -R jenkins:jenkins /var/log/jenkins
    chmod -R 750 /var/log/jenkins
fi

# 11. Disable unnecessary services
echo -e "${YELLOW}[11/14] Disabling unnecessary services...${NC}"
services_to_disable=("cups" "avahi-daemon" "bluetooth")
for service in "${services_to_disable[@]}"; do
    if systemctl list-unit-files | grep -q "$service"; then
        systemctl stop "$service" 2>/dev/null || true
        systemctl disable "$service" 2>/dev/null || true
    fi
done

# 12. Configure automatic security updates
echo -e "${YELLOW}[12/14] Configuring automatic security updates...${NC}"
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

# 13. Install monitoring tools
echo -e "${YELLOW}[13/14] Installing monitoring tools...${NC}"
apt install -y htop iotop iftop net-tools

# 14. Configure kernel parameters for security
echo -e "${YELLOW}[14/14] Configuring kernel parameters...${NC}"
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

# Create Jenkins backup script
echo -e "${YELLOW}Creating Jenkins backup script...${NC}"
cat > /usr/local/bin/backup_jenkins.sh << 'EOFBACKUP'
#!/bin/bash
BACKUP_DIR="/backup/jenkins"
JENKINS_HOME="/var/lib/jenkins"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR
tar -czf $BACKUP_DIR/jenkins_backup_$DATE.tar.gz $JENKINS_HOME

# Delete backups older than 7 days
find $BACKUP_DIR -name "jenkins_backup_*.tar.gz" -mtime +7 -delete

echo "Backup completed: jenkins_backup_$DATE.tar.gz"
EOFBACKUP

chmod +x /usr/local/bin/backup_jenkins.sh

# Create cronjob for daily backup at 2 AM
(crontab -l 2>/dev/null; echo "0 2 * * * /usr/local/bin/backup_jenkins.sh") | crontab -

# Restart Jenkins and Nginx
echo -e "${YELLOW}Restarting services...${NC}"
if systemctl is-active --quiet jenkins; then
    systemctl restart jenkins
fi
systemctl restart nginx

# Install Let's Encrypt if user chose to
if [[ $INSTALL_SSL == "y" || $INSTALL_SSL == "Y" ]]; then
    echo -e "${YELLOW}Installing Let's Encrypt SSL...${NC}"
    apt install -y certbot python3-certbot-nginx
    
    echo -e "${BLUE}Run the following command to install SSL certificate:${NC}"
    echo -e "${GREEN}certbot --nginx -d $DOMAIN_NAME${NC}"
    echo ""
    echo -e "${YELLOW}Note: Make sure domain $DOMAIN_NAME points to this server's IP!${NC}"
    echo ""
    read -p "Do you want to run certbot now? (y/n): " RUN_CERTBOT
    
    if [[ $RUN_CERTBOT == "y" || $RUN_CERTBOT == "Y" ]]; then
        certbot --nginx -d $DOMAIN_NAME
    fi
fi

echo -e "\n${GREEN}================================================${NC}"
echo -e "${GREEN}Security Configuration Complete!${NC}"
echo -e "${GREEN}================================================${NC}\n"

echo -e "${YELLOW}Summary of implemented measures:${NC}"
echo "✓ System update"
echo "✓ Nginx Reverse Proxy installation and configuration"
echo "✓ UFW Firewall configuration (SSH, HTTP, HTTPS)"
echo "✓ SSL certificate configuration"
echo "✓ Fail2Ban installation with SSH, Nginx and Jenkins protection"
echo "✓ Disabled root login via SSH"
echo "✓ SSH security hardening"
echo "✓ System resource limits configuration"
echo "✓ AppArmor enabled"
echo "✓ Jenkins directories secured"
echo "✓ Disabled unnecessary services"
echo "✓ Automatic security updates configured"
echo "✓ Monitoring tools installed"
echo "✓ Kernel security parameters hardened"
echo "✓ Automatic Jenkins backup script created"

echo -e "\n${BLUE}Jenkins Access Information:${NC}"
echo "URL: https://$DOMAIN_NAME"
echo "Jenkins listens only on localhost:8080"
echo "All traffic goes through Nginx reverse proxy"

echo -e "\n${YELLOW}Next steps you should take:${NC}"
echo "1. Configure Jenkins Security Realm and Authorization"
echo "   - Go to Manage Jenkins > Configure Global Security"
echo "   - Enable security and select Jenkins' own user database"
echo "2. Update Jenkins URL in configuration"
echo "   - Go to Manage Jenkins > System"
echo "   - Set Jenkins URL: https://$DOMAIN_NAME"
echo "3. Check Jenkins plugins and update regularly"
echo "4. Create dedicated SSH user and disable password authentication (use SSH keys)"
echo "5. Configure Cloud VM security groups/firewall rules"
echo "6. Set up monitoring and alerting"
echo "7. Configure automatic backup for Nginx configs"

echo -e "\n${RED}IMPORTANT NOTES:${NC}"
echo "- Make sure domain $DOMAIN_NAME points to this server's IP"
echo "- If Let's Encrypt not installed, run: certbot --nginx -d $DOMAIN_NAME"
echo "- Make sure you have SSH key before disabling password authentication"
echo "- Test SSH connection from another terminal before logging out"
echo "- Jenkins backups are saved at /backup/jenkins/"
echo "- Port 8080 now only listens on localhost, not accessible from outside"

echo -e "\n${BLUE}Services Status Check:${NC}"
echo "Nginx: $(systemctl is-active nginx)"
echo "Jenkins: $(systemctl is-active jenkins)"
echo "Fail2Ban: $(systemctl is-active fail2ban)"
echo "UFW: $(ufw status | grep Status | awk '{print $2}')"

echo -e "\n${GREEN}Reboot server to apply some changes:${NC}"
echo "sudo reboot"