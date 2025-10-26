#!/bin/bash

# Ansible Core installation script for Ubuntu 25.04 x64
# Usage: sudo bash install-ansible-core.sh

set -e

echo "=== Starting Ansible Core Installation ==="

# Update package list
echo "Updating package list..."
apt-get update

# Install dependencies
echo "Installing dependencies..."
apt-get install -y software-properties-common python3 python3-pip python3-venv

# Add Ansible PPA repository
echo "Adding Ansible PPA repository..."
add-apt-repository --yes --update ppa:ansible/ansible

# Install Ansible Core
echo "Installing Ansible Core..."
apt-get update
apt-get install -y ansible-core

# Create configuration directory if not exists
mkdir -p /etc/ansible
if [ ! -f /etc/ansible/ansible.cfg ]; then
    echo "Creating default configuration file..."
    cat > /etc/ansible/ansible.cfg << 'EOF'
[defaults]
inventory = /etc/ansible/hosts
host_key_checking = False
retry_files_enabled = False
deprecation_warnings = False

[privilege_escalation]
become = True
become_method = sudo
become_user = root
become_ask_pass = False
EOF
fi

# Create sample inventory file
if [ ! -f /etc/ansible/hosts ]; then
    echo "Creating sample inventory file..."
    cat > /etc/ansible/hosts << 'EOF'
# Sample inventory file
# Uncomment and edit according to your environment

# [webservers]
# web1.example.com
# web2.example.com

# [databases]
# db1.example.com
# db2.example.com

[local]
localhost ansible_connection=local
EOF
fi

# Check version
echo ""
echo "=== Installation Complete ==="
ansible --version

echo ""
echo "Ansible Core has been successfully installed!"
echo "Configuration file: /etc/ansible/ansible.cfg"
echo "Inventory file: /etc/ansible/hosts"
echo "Use 'ansible --help' to see available commands."