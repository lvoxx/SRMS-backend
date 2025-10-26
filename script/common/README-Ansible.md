# Ansible Quick Commands

## Installation

```bash
# Install Ansible (Ubuntu/Debian)
sudo apt update
sudo apt install ansible -y

# Install via pip
pip install ansible

# Install specific version
pip install ansible==2.10.0

# Verify installation
ansible --version
```

## Configuration

```bash
# Default config file locations (checked in order):
# 1. ANSIBLE_CONFIG environment variable
# 2. ./ansible.cfg (in current directory)
# 3. ~/.ansible.cfg (in home directory)
# 4. /etc/ansible/ansible.cfg

# View current configuration
ansible-config view

# List all config options
ansible-config list

# Dump current settings
ansible-config dump
```

### Basic ansible.cfg
```ini
[defaults]
inventory = ./inventory
host_key_checking = False
retry_files_enabled = False
gathering = smart
fact_caching = jsonfile
fact_caching_connection = /tmp/ansible_facts
fact_caching_timeout = 86400

[privilege_escalation]
become = True
become_method = sudo
become_user = root
become_ask_pass = False
```

## Inventory Management

### Static Inventory (INI format)
```ini
# inventory/hosts
[webservers]
web1.example.com
web2.example.com
web3.example.com

[databases]
db1.example.com
db2.example.com

[production:children]
webservers
databases

[production:vars]
ansible_user=ubuntu
ansible_ssh_private_key_file=~/.ssh/id_rsa
```

### Static Inventory (YAML format)
```yaml
# inventory/hosts.yml
all:
  children:
    webservers:
      hosts:
        web1.example.com:
        web2.example.com:
      vars:
        ansible_user: ubuntu
    databases:
      hosts:
        db1.example.com:
          ansible_host: 192.168.1.100
        db2.example.com:
          ansible_host: 192.168.1.101
```

### Inventory Commands
```bash
# List all hosts
ansible all --list-hosts

# List hosts in group
ansible webservers --list-hosts

# Show inventory
ansible-inventory --list
ansible-inventory --graph
ansible-inventory --host web1.example.com

# Use specific inventory file
ansible all -i inventory/production --list-hosts
```

## Ad-Hoc Commands

```bash
# Ping all hosts
ansible all -m ping

# Ping specific group
ansible webservers -m ping

# Run command on all hosts
ansible all -m command -a "uptime"
ansible all -a "uptime"  # command module is default

# Run with sudo
ansible all -m command -a "apt update" --become

# Run as specific user
ansible all -m command -a "whoami" --become-user=www-data

# Copy file
ansible all -m copy -a "src=/local/file dest=/remote/file"

# Install package
ansible all -m apt -a "name=nginx state=present" --become

# Start service
ansible all -m service -a "name=nginx state=started" --become

# Gather facts
ansible all -m setup

# Filter facts
ansible all -m setup -a "filter=ansible_distribution*"

# Check disk space
ansible all -m shell -a "df -h"

# Check memory
ansible all -m shell -a "free -m"

# Reboot servers
ansible all -m reboot --become

# Run script
ansible all -m script -a "/path/to/script.sh"
```

## Playbook Commands

```bash
# Run playbook
ansible-playbook playbook.yml

# Run with specific inventory
ansible-playbook -i inventory/production playbook.yml

# Check syntax
ansible-playbook playbook.yml --syntax-check

# Dry run (check mode)
ansible-playbook playbook.yml --check

# Dry run with diff
ansible-playbook playbook.yml --check --diff

# Run specific tags
ansible-playbook playbook.yml --tags "install,configure"

# Skip specific tags
ansible-playbook playbook.yml --skip-tags "deploy"

# Run on specific hosts
ansible-playbook playbook.yml --limit webservers
ansible-playbook playbook.yml --limit "web1.example.com,web2.example.com"

# Start at specific task
ansible-playbook playbook.yml --start-at-task="Install nginx"

# Step through playbook
ansible-playbook playbook.yml --step

# Verbose output
ansible-playbook playbook.yml -v
ansible-playbook playbook.yml -vv
ansible-playbook playbook.yml -vvv
ansible-playbook playbook.yml -vvvv

# List tasks
ansible-playbook playbook.yml --list-tasks

# List tags
ansible-playbook playbook.yml --list-tags

# List hosts
ansible-playbook playbook.yml --list-hosts
```

## Playbook Examples

### Basic Playbook
```yaml
---
# playbook.yml
- name: Configure web servers
  hosts: webservers
  become: yes
  
  vars:
    http_port: 80
    max_clients: 200
  
  tasks:
    - name: Install nginx
      apt:
        name: nginx
        state: present
        update_cache: yes
    
    - name: Start nginx service
      service:
        name: nginx
        state: started
        enabled: yes
    
    - name: Copy index.html
      copy:
        src: files/index.html
        dest: /var/www/html/index.html
        owner: www-data
        group: www-data
        mode: '0644'
```

### Playbook with Handlers
```yaml
---
- name: Configure web server
  hosts: webservers
  become: yes
  
  tasks:
    - name: Install nginx
      apt:
        name: nginx
        state: present
      
    - name: Copy nginx config
      template:
        src: templates/nginx.conf.j2
        dest: /etc/nginx/nginx.conf
      notify: Restart nginx
  
  handlers:
    - name: Restart nginx
      service:
        name: nginx
        state: restarted
```

### Playbook with Variables
```yaml
---
- name: Deploy application
  hosts: webservers
  become: yes
  
  vars:
    app_name: myapp
    app_version: 1.0.0
    app_port: 8080
  
  vars_files:
    - vars/common.yml
    - vars/{{ environment }}.yml
  
  tasks:
    - name: Deploy {{ app_name }} version {{ app_version }}
      docker_container:
        name: "{{ app_name }}"
        image: "{{ app_name }}:{{ app_version }}"
        ports:
          - "{{ app_port }}:{{ app_port }}"
        state: started
```

### Playbook with Loops
```yaml
---
- name: Create users
  hosts: all
  become: yes
  
  tasks:
    - name: Create multiple users
      user:
        name: "{{ item.name }}"
        state: present
        groups: "{{ item.groups }}"
      loop:
        - { name: 'alice', groups: 'sudo,developers' }
        - { name: 'bob', groups: 'developers' }
        - { name: 'charlie', groups: 'operators' }
    
    - name: Install packages
      apt:
        name: "{{ item }}"
        state: present
      loop:
        - git
        - vim
        - curl
        - htop
```

### Playbook with Conditionals
```yaml
---
- name: OS-specific tasks
  hosts: all
  become: yes
  
  tasks:
    - name: Install Apache on Debian/Ubuntu
      apt:
        name: apache2
        state: present
      when: ansible_os_family == "Debian"
    
    - name: Install Apache on RedHat/CentOS
      yum:
        name: httpd
        state: present
      when: ansible_os_family == "RedHat"
    
    - name: Ensure service is running
      service:
        name: "{{ 'apache2' if ansible_os_family == 'Debian' else 'httpd' }}"
        state: started
      when: ansible_os_family in ['Debian', 'RedHat']
```

## Roles

### Create Role Structure
```bash
# Create role
ansible-galaxy init myrole

# Role structure
myrole/
  ├── README.md
  ├── defaults/
  │   └── main.yml
  ├── files/
  ├── handlers/
  │   └── main.yml
  ├── meta/
  │   └── main.yml
  ├── tasks/
  │   └── main.yml
  ├── templates/
  ├── tests/
  │   ├── inventory
  │   └── test.yml
  └── vars/
      └── main.yml
```

### Using Roles in Playbook
```yaml
---
- name: Configure servers with roles
  hosts: webservers
  become: yes
  
  roles:
    - common
    - nginx
    - { role: mysql, mysql_port: 3306 }
  
  # Or with tags
  roles:
    - { role: nginx, tags: ['web'] }
    - { role: mysql, tags: ['database'] }
```

### Install Roles from Galaxy
```bash
# Install role
ansible-galaxy install geerlingguy.nginx

# Install to specific path
ansible-galaxy install geerlingguy.nginx -p ./roles

# Install from requirements
ansible-galaxy install -r requirements.yml

# List installed roles
ansible-galaxy list

# Remove role
ansible-galaxy remove geerlingguy.nginx

# Install collections
ansible-galaxy collection install community.general

# List collections
ansible-galaxy collection list

# Build collection
ansible-galaxy collection build

# Publish collection
ansible-galaxy collection publish namespace-collection-1.0.0.tar.gz --token=TOKEN
```

## Templates (Jinja2)

### Basic Template Example
```jinja2
{# templates/nginx.conf.j2 #}
server {
    listen {{ http_port }};
    server_name {{ server_name }};
    
    location / {
        root {{ document_root }};
        index index.html;
    }
    
    {% if enable_ssl %}
    listen 443 ssl;
    ssl_certificate {{ ssl_cert_path }};
    ssl_certificate_key {{ ssl_key_path }};
    {% endif %}
}
```

### Using Template in Playbook
```yaml
- name: Deploy nginx config
  template:
    src: templates/nginx.conf.j2
    dest: /etc/nginx/nginx.conf
    owner: root
    group: root
    mode: '0644'
  notify: Restart nginx
```

### Template with Loops
```jinja2
{# templates/hosts.j2 #}
127.0.0.1   localhost

{% for host in groups['webservers'] %}
{{ hostvars[host]['ansible_default_ipv4']['address'] }}  {{ host }}
{% endfor %}
```

## Variables and Facts

```bash
# Display all facts
ansible hostname -m setup

# Display specific fact
ansible hostname -m setup -a "filter=ansible_distribution"

# Custom facts directory
# Place JSON/INI files in /etc/ansible/facts.d/*.fact

# Use facts in playbook
- name: Show OS distribution
  debug:
    msg: "This is {{ ansible_distribution }} {{ ansible_distribution_version }}"

# Register variable from task output
- name: Get date
  command: date
  register: current_date

- name: Show date
  debug:
    var: current_date.stdout

# Set fact
- name: Set custom fact
  set_fact:
    my_variable: "some value"

# Use host variables
- name: Show host IP
  debug:
    msg: "{{ hostvars[inventory_hostname]['ansible_default_ipv4']['address'] }}"
```

## Debugging

```bash
# Debug module in playbook
- name: Print variable
  debug:
    var: my_variable

- name: Print message
  debug:
    msg: "The value is {{ my_variable }}"

# Verbose output levels
ansible-playbook playbook.yml -v     # verbose
ansible-playbook playbook.yml -vv    # more verbose
ansible-playbook playbook.yml -vvv   # very verbose
ansible-playbook playbook.yml -vvvv  # connection debugging

# Check mode (dry run)
ansible-playbook playbook.yml --check

# Show differences
ansible-playbook playbook.yml --check --diff

# Step through tasks
ansible-playbook playbook.yml --step

# Start at specific task
ansible-playbook playbook.yml --start-at-task="Install nginx"

# Use assert module
- name: Assert condition
  assert:
    that:
      - ansible_distribution == "Ubuntu"
      - ansible_distribution_version == "20.04"
    fail_msg: "This playbook requires Ubuntu 20.04"
```

## Error Handling

```yaml
---
- name: Error handling example
  hosts: all
  
  tasks:
    - name: Task that might fail
      command: /bin/false
      ignore_errors: yes
    
    - name: Task with retry
      uri:
        url: http://example.com
        status_code: 200
      register: result
      until: result.status == 200
      retries: 5
      delay: 10
    
    - name: Task with custom failure condition
      command: some_command
      register: result
      failed_when: "'ERROR' in result.stderr"
    
    - name: Task with changed condition
      command: echo "no change"
      changed_when: false
    
    - name: Block with rescue
      block:
        - name: Risky task
          command: /might/fail
      rescue:
        - name: Handle failure
          debug:
            msg: "Task failed, running recovery"
      always:
        - name: Always run this
          debug:
            msg: "This runs regardless of success or failure"
```

## Dynamic Inventory

### AWS EC2 Dynamic Inventory
```bash
# Install boto3
pip install boto3

# Use AWS EC2 plugin
# Create aws_ec2.yml
plugin: aws_ec2
regions:
  - us-east-1
  - us-west-2
filters:
  tag:Environment: production
keyed_groups:
  - key: tags.Name
    prefix: name
  - key: tags.Environment
    prefix: env

# Use dynamic inventory
ansible-inventory -i aws_ec2.yml --graph
ansible-playbook -i aws_ec2.yml playbook.yml
```

### Custom Dynamic Inventory Script
```python
#!/usr/bin/env python3
# inventory.py
import json

inventory = {
    "webservers": {
        "hosts": ["web1.example.com", "web2.example.com"],
        "vars": {
            "ansible_user": "ubuntu"
        }
    },
    "_meta": {
        "hostvars": {
            "web1.example.com": {
                "ansible_host": "192.168.1.10"
            },
            "web2.example.com": {
                "ansible_host": "192.168.1.11"
            }
        }
    }
}

print(json.dumps(inventory))
```

```bash
# Make executable
chmod +x inventory.py

# Use custom inventory
ansible-playbook -i inventory.py playbook.yml
```

## Performance Optimization

```ini
# ansible.cfg
[defaults]
# Use multiple forks (parallel execution)
forks = 20

# Pipelining (faster execution)
pipelining = True

# SSH connection optimization
[ssh_connection]
ssh_args = -o ControlMaster=auto -o ControlPersist=60s
control_path = /tmp/ansible-ssh-%%h-%%p-%%r

# Fact caching
[defaults]
gathering = smart
fact_caching = jsonfile
fact_caching_connection = /tmp/ansible_facts
fact_caching_timeout = 86400
```

```yaml
# Disable fact gathering when not needed
- name: Playbook without facts
  hosts: all
  gather_facts: no
  
  tasks:
    - name: Task without facts
      command: echo "Hello"

# Use async for long-running tasks
- name: Long running task
  command: /usr/bin/long_task
  async: 3600
  poll: 0
  register: long_task

- name: Check on async task
  async_status:
    jid: "{{ long_task.ansible_job_id }}"
  register: job_result
  until: job_result.finished
  retries: 30
  delay: 10
```

## Best Practices

```yaml
# Use meaningful names
- name: Install and configure web server
  apt:
    name: nginx
    state: present

# Use tags for selective execution
- name: Install packages
  apt:
    name: "{{ item }}"
    state: present
  loop: "{{ packages }}"
  tags:
    - install
    - packages

# Use handlers for service restarts
handlers:
  - name: Restart nginx
    service:
      name: nginx
      state: restarted

# Use variables for reusability
vars:
  app_port: 8080
  app_name: myapp

# Use roles for organization
roles:
  - common
  - webserver
  - database

# Use vault for sensitive data
# Store passwords, keys in vault-encrypted files

# Use check mode before applying
ansible-playbook playbook.yml --check --diff

# Use version control
# Keep playbooks, roles, inventory in Git

# Document your playbooks
# Add comments and descriptions
```

## Modules Reference

```bash
# Package management
ansible all -m apt -a "name=nginx state=present"
ansible all -m yum -a "name=httpd state=present"
ansible all -m package -a "name=vim state=present"

# Service management
ansible all -m service -a "name=nginx state=started enabled=yes"
ansible all -m systemd -a "name=nginx state=restarted daemon_reload=yes"

# File operations
ansible all -m file -a "path=/tmp/test state=directory mode=0755"
ansible all -m copy -a "src=/local/file dest=/remote/file"
ansible all -m fetch -a "src=/remote/file dest=/local/dir"
ansible all -m template -a "src=template.j2 dest=/remote/file"

# User management
ansible all -m user -a "name=john state=present groups=sudo"
ansible all -m group -a "name=developers state=present"

# Command execution
ansible all -m command -a "uptime"
ansible all -m shell -a "ps aux | grep nginx"
ansible all -m script -a "/path/to/script.sh"

# Git operations
ansible all -m git -a "repo=https://github.com/user/repo.git dest=/opt/app version=main"

# Docker operations
ansible all -m docker_container -a "name=webapp image=nginx:latest state=started"
ansible all -m docker_image -a "name=myapp:latest source=build build_path=/path/to/dockerfile"

# Cloud modules
ansible all -m ec2 -a "instance_type=t2.micro image=ami-123456 region=us-east-1"
ansible all -m azure_rm_virtualmachine -a "name=myvm resource_group=mygroup"

# Database modules
ansible all -m mysql_db -a "name=mydb state=present"
ansible all -m postgresql_user -a "name=myuser password=secret"
```

## Troubleshooting

```bash
# Test connection
ansible all -m ping

# Check connectivity with verbose
ansible all -m ping -vvv

# Test sudo access
ansible all -m command -a "whoami" --become

# Check Python version
ansible all -m command -a "python3 --version"

# Verify inventory
ansible-inventory --list
ansible-inventory --graph

# Test specific host
ansible web1.example.com -m ping

# Check SSH connection manually
ssh -i ~/.ssh/id_rsa ubuntu@web1.example.com

# Run with different user
ansible all -m ping -u ubuntu

# Run with SSH password
ansible all -m ping --ask-pass

# Run with sudo password
ansible all -m command -a "whoami" --become --ask-become-pass

# Clear fact cache
rm -rf /tmp/ansible_facts/*

# Check Python interpreter
ansible all -m setup -a "filter=ansible_python*"

# Test playbook syntax
ansible-playbook playbook.yml --syntax-check

# Dry run playbook
ansible-playbook playbook.yml --check

# View task details
ansible-playbook playbook.yml --list-tasks
ansible-playbook playbook.yml -vvv
```

# Install specific version
ansible-galaxy install geerlingguy.nginx,3.1.4

# Install from requirements file
ansible-galaxy install -r requirements.yml

# List installed roles
ansible-galaxy list

# Remove role
ansible-galaxy remove geerlingguy.nginx
```

### requirements.yml
```yaml
---
# requirements.yml
roles:
  - name: geerlingguy.nginx
    version: 3.1.4
  
  - src: https://github.com/user/repo
    name: custom_role
    version: master

collections:
  - name: community.general
    version: 5.0.0
  - name: ansible.posix
```

## Vault (Encryption)

```bash
# Create encrypted file
ansible-vault create secrets.yml

# Encrypt existing file
ansible-vault encrypt vars.yml

# Decrypt file
ansible-vault decrypt secrets.yml

# View encrypted file
ansible-vault view secrets.yml

# Edit encrypted file
ansible-vault edit secrets.yml

# Rekey (change password)
ansible-vault rekey secrets.yml

# Encrypt string
ansible-vault encrypt_string 'mypassword' --name 'db_password'

# Run playbook with vault
ansible-playbook playbook.yml --ask-vault-pass

# Run with vault password file
ansible-playbook playbook.yml --vault-password-file ~/.vault_pass

# Use environment variable
export ANSIBLE_VAULT_PASSWORD_FILE=~/.vault_pass
ansible-playbook playbook.yml
```

## Ansible Galaxy

```bash
# Search for roles
ansible-galaxy search nginx

# Get role info
ansible-galaxy info geerlingguy.nginx

# Install role
ansible-galaxy install geerlingguy.nginx