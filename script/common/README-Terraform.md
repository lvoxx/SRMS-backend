# Terraform Quick Commands

## Installation

```bash
# Install Terraform (Linux)
wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
unzip terraform_1.6.0_linux_amd64.zip
sudo mv terraform /usr/local/bin/
terraform version

# Install via package manager (Ubuntu/Debian)
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform

# Install (Mac)
brew install terraform

# Verify installation
terraform -version
```

## Basic Commands

```bash
# Initialize Terraform
terraform init

# Initialize and upgrade providers
terraform init -upgrade

# Initialize without backend configuration
terraform init -backend=false

# Validate configuration
terraform validate

# Format configuration files
terraform fmt
terraform fmt -recursive

# Check formatting without making changes
terraform fmt -check

# Show execution plan
terraform plan

# Show plan with specific variables
terraform plan -var="instance_type=t2.micro"

# Show plan and save to file
terraform plan -out=tfplan

# Apply changes
terraform apply

# Apply without confirmation prompt
terraform apply -auto-approve

# Apply saved plan
terraform apply tfplan

# Apply with specific variables
terraform apply -var="instance_type=t2.micro"

# Apply with variable file
terraform apply -var-file="prod.tfvars"

# Destroy infrastructure
terraform destroy

# Destroy without confirmation
terraform destroy -auto-approve

# Destroy specific resource
terraform destroy -target=aws_instance.example
```

## State Management

```bash
# List resources in state
terraform state list

# Show specific resource
terraform state show aws_instance.example

# Move resource in state
terraform state mv aws_instance.old aws_instance.new

# Remove resource from state (doesn't destroy)
terraform state rm aws_instance.example

# Pull state and output to file
terraform state pull > terraform.tfstate

# Push state from file
terraform state push terraform.tfstate

# Replace provider in state
terraform state replace-provider registry.terraform.io/hashicorp/aws registry.terraform.io/custom/aws

# Import existing resource
terraform import aws_instance.example i-1234567890abcdef0

# Refresh state
terraform refresh
```

## Workspace Management

```bash
# List workspaces
terraform workspace list

# Show current workspace
terraform workspace show

# Create new workspace
terraform workspace new dev

# Select workspace
terraform workspace select dev

# Delete workspace
terraform workspace delete dev
```

## Output and Variables

```bash
# Show outputs
terraform output

# Show specific output
terraform output instance_ip

# Show output in JSON
terraform output -json

# Show output in raw format
terraform output -raw private_key

# Set variable from command line
terraform apply -var="region=us-west-2"

# Set multiple variables
terraform apply -var="region=us-west-2" -var="instance_type=t2.micro"

# Use variable file
terraform apply -var-file="terraform.tfvars"
terraform apply -var-file="prod.tfvars"
```

## Graph and Visualization

```bash
# Generate dependency graph
terraform graph

# Generate graph in DOT format and convert to image
terraform graph | dot -Tpng > graph.png

# Show providers
terraform providers

# Show provider schema
terraform providers schema -json
```

## Console and Testing

```bash
# Open Terraform console
terraform console

# Example console commands:
# > var.region
# > aws_instance.example.id
# > join(",", var.availability_zones)

# Validate and test expressions
echo "var.region" | terraform console
```

## Configuration Examples

### Basic Provider Configuration
```hcl
# provider.tf
terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}
```

### Variables
```hcl
# variables.tf
variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default = {
    Environment = "dev"
    Project     = "myapp"
  }
}
```

### terraform.tfvars
```hcl
region        = "us-west-2"
instance_type = "t3.medium"
tags = {
  Environment = "production"
  Project     = "myapp"
}
```

### Outputs
```hcl
# outputs.tf
output "instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.example.id
}

output "instance_public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_instance.example.public_ip
}

output "instance_private_ip" {
  description = "Private IP of the EC2 instance"
  value       = aws_instance.example.private_ip
  sensitive   = true
}
```

### Data Sources
```hcl
# data.tf
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64-server-*"]
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}
```

### Resources
```hcl
# main.tf
resource "aws_instance" "example" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.instance_type
  
  tags = merge(
    var.tags,
    {
      Name = "example-instance"
    }
  )
}

resource "aws_security_group" "example" {
  name        = "example-sg"
  description = "Example security group"

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = var.tags
}
```

## Backend Configuration

### S3 Backend
```hcl
# backend.tf
terraform {
  backend "s3" {
    bucket         = "my-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-lock"
  }
}
```

### Initialize with backend config
```bash
terraform init -backend-config="bucket=my-terraform-state" \
               -backend-config="key=prod/terraform.tfstate" \
               -backend-config="region=us-east-1"
```

### Migrate to new backend
```bash
# Update backend configuration in backend.tf
terraform init -migrate-state
```

## Modules

### Using Modules
```hcl
# main.tf
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  version = "5.0.0"

  name = "my-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["us-east-1a", "us-east-1b"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24"]

  enable_nat_gateway = true
  enable_vpn_gateway = false

  tags = var.tags
}

# Reference module outputs
resource "aws_instance" "example" {
  subnet_id = module.vpc.private_subnets[0]
  # ...
}
```

### Local Module
```hcl
module "local_module" {
  source = "./modules/my-module"
  
  variable1 = "value1"
  variable2 = "value2"
}
```

### Module Commands
```bash
# Initialize and download modules
terraform init

# Update modules
terraform get -update

# Show module tree
terraform providers
```

## Taint and Replace

```bash
# Mark resource for recreation (Terraform < 0.15.2)
terraform taint aws_instance.example

# Untaint resource
terraform untaint aws_instance.example

# Replace resource (Terraform >= 0.15.2)
terraform apply -replace="aws_instance.example"

# Plan with replace
terraform plan -replace="aws_instance.example"
```

## Debugging

```bash
# Enable debug logging
export TF_LOG=DEBUG
terraform apply

# Log levels: TRACE, DEBUG, INFO, WARN, ERROR
export TF_LOG=TRACE

# Log to file
export TF_LOG_PATH=./terraform.log

# Disable logging
unset TF_LOG
unset TF_LOG_PATH

# Verbose output
terraform apply -parallelism=1

# Show detailed crash log
cat crash.log
```

## Advanced Commands

```bash
# Lock state manually
terraform force-unlock <lock-id>

# Show Terraform version
terraform version

# Show current providers
terraform providers

# Generate provider schema
terraform providers schema -json > schema.json

# Upgrade provider versions
terraform init -upgrade

# Get plugin directory
terraform version -json | jq -r .provider_selections

# Reconfigure backend
terraform init -reconfigure

# Copy state from backend
terraform init -backend=false
terraform state pull > local.tfstate
```

## CI/CD Integration

### GitLab CI Example
```yaml
terraform:
  stage: deploy
  image: hashicorp/terraform:latest
  before_script:
    - terraform init
  script:
    - terraform validate
    - terraform plan -out=tfplan
    - terraform apply -auto-approve tfplan
  only:
    - main
```

### GitHub Actions Example
```yaml
- name: Terraform Init
  run: terraform init

- name: Terraform Validate
  run: terraform validate

- name: Terraform Plan
  run: terraform plan -out=tfplan

- name: Terraform Apply
  if: github.ref == 'refs/heads/main'
  run: terraform apply -auto-approve tfplan
```

## Best Practices

```bash
# Use consistent formatting
terraform fmt -recursive

# Validate before apply
terraform validate && terraform plan

# Use workspaces for environments
terraform workspace new production
terraform workspace select production

# Use remote state for team collaboration
# Configure S3 or Terraform Cloud backend

# Lock state during operations (automatic with remote backends)

# Use version constraints for providers and modules
# In terraform block: required_version = "~> 1.0"

# Store sensitive data in variables, not in code
# Use terraform.tfvars (add to .gitignore)

# Use data sources instead of hardcoding values

# Use count or for_each for multiple similar resources

# Use locals for computed values
locals {
  common_tags = {
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
```

## Troubleshooting

```bash
# State is locked
terraform force-unlock <lock-id>

# State file corrupted
terraform state pull > backup.tfstate
# Fix state manually
terraform state push fixed.tfstate

# Provider issues
rm -rf .terraform/
terraform init

# Refresh state to match reality
terraform refresh

# Show what would be destroyed
terraform plan -destroy

# Target specific resource
terraform plan -target=aws_instance.example
terraform apply -target=aws_instance.example

# Ignore specific resource during apply
terraform apply -target="!aws_instance.example"

# Check state for inconsistencies
terraform state list
terraform state show <resource>

# Re-import resource if lost from state
terraform import aws_instance.example i-1234567890abcdef0
```