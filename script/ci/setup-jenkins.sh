#!/bin/bash

# Script to install and configure Jenkins with Java 21, Maven, and Docker on Ubuntu
# Enhanced with colored and formatted logs

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Log functions
log_info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}
log_success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}
log_error() {
    echo -e "${RED}[ERROR] $1${NC}"
    exit 1
}
log_warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

# Check if script is run with sudo
if [ "$EUID" -ne 0 ]; then
    log_error "Please run this script as root (use sudo)"
fi

echo -e "\n${BLUE}======================================${NC}"
echo -e "${BLUE} Starting Jenkins Installation Script ${NC}"
echo -e "${BLUE}======================================${NC}\n"

# Step 1: Update system
log_info "Updating system packages..."
apt update && apt upgrade -y || log_error "Failed to update system packages"

# Step 2: Install Java 21
echo -e "\n${BLUE}=== Installing Java 21 ===${NC}"
log_info "Installing OpenJDK 21..."
apt install openjdk-21-jdk -y || log_error "Failed to install OpenJDK 21"

# Set JAVA_HOME permanently
JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
log_info "Setting JAVA_HOME to $JAVA_HOME"
if ! grep -q "JAVA_HOME" /etc/environment; then
    echo "JAVA_HOME=\"$JAVA_HOME\"" >> /etc/environment
else
    sed -i "s|JAVA_HOME=.*|JAVA_HOME=\"$JAVA_HOME\"|" /etc/environment
fi
source /etc/environment
java -version && log_success "Java 21 installed and JAVA_HOME set" || log_error "Java 21 installation verification failed"

# Step 3: Install Maven
echo -e "\n${BLUE}=== Installing Maven ===${NC}"
log_info "Installing Maven..."
apt install maven -y || log_error "Failed to install Maven"

# Set MVN_HOME permanently
MVN_HOME="/usr/share/maven"
log_info "Setting MVN_HOME to $MVN_HOME"
if ! grep -q "MVN_HOME" /etc/environment; then
    echo "MVN_HOME=\"$MVN_HOME\"" >> /etc/environment
else
    sed -i "s|MVN_HOME=.*|MVN_HOME=\"$MVN_HOME\"|" /etc/environment
fi
source /etc/environment
mvn -version && log_success "Maven installed and MVN_HOME set" || log_error "Maven installation verification failed"

# Step 4: Install Docker
echo -e "\n${BLUE}=== Installing Docker ===${NC}"
log_info "Installing Docker prerequisites..."
apt install apt-transport-https ca-certificates curl software-properties-common -y || log_error "Failed to install Docker prerequisites"

log_info "Adding Docker GPG key..."
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg || log_error "Failed to add Docker GPG key"

log_info "Adding Docker repository..."
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt update || log_error "Failed to update APT with Docker repository"

log_info "Installing Docker..."
apt install docker-ce docker-ce-cli containerd.io -y || log_error "Failed to install Docker"

systemctl start docker && systemctl enable docker && log_success "Docker started and enabled" || log_error "Failed to start Docker"
docker --version && log_success "Docker installed successfully"

# Add current user and Jenkins user to Docker group
log_info "Adding users to Docker group..."
usermod -aG docker $SUDO_USER || log_warning "Failed to add current user to Docker group"
usermod -aG docker jenkins || log_warning "Jenkins user not yet created, skipping Docker group addition"

# Step 5: Install Jenkins
echo -e "\n${BLUE}=== Installing Jenkins ===${NC}"
log_info "Adding Jenkins GPG key..."
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | gpg --dearmor -o /usr/share/keyrings/jenkins-keyring.asc || log_error "Failed to add Jenkins GPG key"

log_info "Adding Jenkins repository..."
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian binary/" | tee /etc/apt/sources.list.d/jenkins.list || log_error "Failed to add Jenkins repository"
apt update || log_error "Failed to update APT with Jenkins repository"

log_info "Installing Jenkins..."
apt install jenkins -y || log_error "Failed to install Jenkins"

# Start and enable Jenkins
systemctl start jenkins && systemctl enable jenkins && log_success "Jenkins started and enabled" || log_error "Failed to start Jenkins"

# Step 6: Configure firewall (if UFW is installed)
echo -e "\n${BLUE}=== Configuring Firewall ===${NC}"
if command -v ufw >/dev/null; then
    ufw allow 8080 && log_success "Port 8080 opened for Jenkins" || log_warning "Failed to open port 8080"
else
    log_warning "UFW not installed, skipping firewall configuration"
fi

# Step 7: Display Jenkins initial admin password
echo -e "\n${BLUE}======================================${NC}"
echo -e "${GREEN} Jenkins Installation Completed! ${NC}"
echo -e "${BLUE}======================================${NC}"
log_info "Access Jenkins at http://<server-ip>:8080"
log_info "Initial admin password:"
if [ -f /var/lib/jenkins/secrets/initialAdminPassword ]; then
    cat /var/lib/jenkins/secrets/initialAdminPassword
else
    log_warning "Initial admin password file not found. Check /var/lib/jenkins/secrets/initialAdminPassword later."
fi

echo -e "\n${BLUE}=== Next Steps ===${NC}"
echo -e "${GREEN}1. Open http://<server-ip>:8080 in your browser.${NC}"
echo -e "${GREEN}2. Use the initial admin password above to log in.${NC}"
echo -e "${GREEN}3. Install recommended plugins and create an admin user.${NC}"
echo -e "${GREEN}4. Configure JDK 21 and Maven in 'Manage Jenkins' > 'Global Tool Configuration'.${NC}"
echo -e "${GREEN}5. Add Docker support via the Docker Pipeline plugin.${NC}"