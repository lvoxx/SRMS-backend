#!/bin/bash

# Terraform installation script for Ubuntu 25.04 x64
# Usage: sudo bash install-terraform.sh

set -e

echo "=== Starting Terraform Installation ==="

# Update package list
echo "Updating package list..."
apt-get update

# Install required dependencies
echo "Installing dependencies..."
apt-get install -y gnupg software-properties-common curl

# Add HashiCorp GPG key
echo "Adding HashiCorp GPG key..."
wget -O- https://apt.releases.hashicorp.com/gpg | \
gpg --dearmor | \
tee /usr/share/keyrings/hashicorp-archive-keyring.gpg > /dev/null

# Verify fingerprint
gpg --no-default-keyring \
--keyring /usr/share/keyrings/hashicorp-archive-keyring.gpg \
--fingerprint

# Add HashiCorp repository
echo "Adding HashiCorp repository..."
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] \
https://apt.releases.hashicorp.com $(lsb_release -cs) main" | \
tee /etc/apt/sources.list.d/hashicorp.list

# Update and install Terraform
echo "Installing Terraform..."
apt-get update
apt-get install -y terraform

# Check version
echo ""
echo "=== Installation Complete ==="
terraform version

echo ""
echo "Terraform has been successfully installed!"
echo "Use 'terraform --help' to see available commands."