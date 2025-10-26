# Linux Quick Commands

## System Information

```bash
# OS information
uname -a
cat /etc/os-release
lsb_release -a
hostnamectl

# Kernel version
uname -r

# Hostname
hostname
hostnamectl set-hostname newhostname

# CPU information
lscpu
cat /proc/cpuinfo
nproc  # number of cores

# Memory information
free -h
cat /proc/meminfo
vmstat

# Disk information
df -h
df -i  # inodes
lsblk
fdisk -l

# System uptime
uptime
w

# Load average
cat /proc/loadavg

# Hardware information
lshw
dmidecode
```

## User Management

```bash
# Create user
useradd username
useradd -m -s /bin/bash username  # with home and shell

# Create user with specific UID
useradd -u 1500 username

# Set password
passwd username

# Delete user
userdel username
userdel -r username  # remove home directory

# Modify user
usermod -aG sudo username  # add to group
usermod -s /bin/bash username  # change shell
usermod -l newname oldname  # rename

# Lock/unlock user
usermod -L username  # lock
usermod -U username  # unlock

# List users
cat /etc/passwd
cut -d: -f1 /etc/passwd

# Current user info
whoami
id
groups

# Switch user
su - username
sudo -u username command

# List logged in users
who
w
last
```

## Group Management

```bash
# Create group
groupadd groupname

# Delete group
groupdel groupname

# Add user to group
usermod -aG groupname username
gpasswd -a username groupname

# Remove user from group
gpasswd -d username groupname

# List groups
cat /etc/group
groups username

# Change primary group
usermod -g groupname username
```

## File and Directory Operations

```bash
# List files
ls -la
ls -lh  # human readable
ls -lt  # sort by time
ls -lS  # sort by size

# Create directory
mkdir dirname
mkdir -p parent/child/grandchild  # create parents

# Remove directory
rmdir dirname  # empty only
rm -rf dirname  # force recursive

# Copy files
cp source dest
cp -r source dest  # recursive
cp -p source dest  # preserve attributes

# Move/rename
mv source dest

# Remove files
rm file
rm -f file  # force
rm -i file  # interactive

# Find files
find /path -name "*.txt"
find /path -type f -mtime -7  # modified in last 7 days
find /path -type f -size +100M  # larger than 100MB
find /path -type f -user username
find /path -type f -perm 755

# Find and delete
find /path -name "*.log" -delete
find /path -type f -name "*.tmp" -exec rm {} \;

# Locate files (requires updatedb)
locate filename
sudo updatedb

# Which command
which python3
whereis python3
```

## File Permissions

```bash
# Change permissions
chmod 755 file
chmod u+x file  # add execute for user
chmod g-w file  # remove write for group
chmod o=r file  # set read only for others
chmod -R 755 directory  # recursive

# Change ownership
chown user:group file
chown -R user:group directory
chown user file  # only user
chgrp group file  # only group

# View permissions
ls -l file
stat file

# Special permissions
chmod u+s file  # setuid
chmod g+s file  # setgid
chmod +t directory  # sticky bit

# ACL (Access Control Lists)
setfacl -m u:username:rwx file
getfacl file
setfacl -b file  # remove ACL
```

## Process Management

```bash
# List processes
ps aux
ps -ef
ps aux | grep nginx

# Process tree
pstree
ps auxf

# Top processes
top
htop
atop

# Kill process
kill PID
kill -9 PID  # force kill
killall processname
pkill processname

# Kill by pattern
pkill -f "pattern"

# Process priority
nice -n 10 command  # start with priority
renice -n 5 -p PID  # change priority

# Background/foreground jobs
command &  # background
jobs  # list jobs
fg %1  # foreground job 1
bg %1  # background job 1
nohup command &  # persist after logout

# Monitor process
watch -n 1 'ps aux | grep nginx'
```

## Network Commands

```bash
# Network interfaces
ip addr show
ip a
ifconfig  # legacy

# Routing table
ip route show
route -n
netstat -rn

# Network statistics
netstat -tuln  # listening ports
netstat -tuln | grep :80
ss -tuln  # modern alternative
ss -tulnp  # with process info

# Test connectivity
ping google.com
ping -c 4 google.com  # 4 packets

# DNS lookup
nslookup google.com
dig google.com
host google.com

# Trace route
traceroute google.com
tracepath google.com
mtr google.com  # better traceroute

# Check port
telnet hostname 80
nc -zv hostname 80  # netcat
curl -v telnet://hostname:80

# Download files
wget https://example.com/file
curl -O https://example.com/file
curl -L -o file https://example.com/file

# Network configuration
ip link set eth0 up
ip link set eth0 down
ip addr add 192.168.1.100/24 dev eth0
ip addr del 192.168.1.100/24 dev eth0

# Firewall (iptables)
iptables -L
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -D INPUT -p tcp --dport 80 -j ACCEPT
iptables-save > /etc/iptables/rules.v4

# Firewall (ufw)
ufw status
ufw enable
ufw allow 22/tcp
ufw allow 80/tcp
ufw deny 23/tcp
ufw delete allow 80/tcp
```

## Package Management

### Debian/Ubuntu (APT)
```bash
# Update package list
apt update
apt-get update

# Upgrade packages
apt upgrade
apt full-upgrade
apt-get dist-upgrade

# Install package
apt install package
apt install package1 package2

# Remove package
apt remove package
apt purge package  # remove config files
apt autoremove  # remove unused dependencies

# Search package
apt search keyword
apt-cache search keyword

# Show package info
apt show package
apt-cache show package

# List installed packages
apt list --installed
dpkg -l

# Download package without install
apt download package

# Clean cache
apt clean
apt autoclean
```

### RedHat/CentOS (YUM/DNF)
```bash
# Update packages
yum update
dnf update

# Install package
yum install package
dnf install package

# Remove package
yum remove package
dnf remove package

# Search package
yum search keyword
dnf search keyword

# Show package info
yum info package
dnf info package

# List installed
yum list installed
dnf list installed

# Clean cache
yum clean all
dnf clean all
```

## Service Management (systemd)

```bash
# Start service
systemctl start nginx
systemctl start nginx.service

# Stop service
systemctl stop nginx

# Restart service
systemctl restart nginx

# Reload service
systemctl reload nginx

# Enable on boot
systemctl enable nginx

# Disable on boot
systemctl disable nginx

# Check status
systemctl status nginx
systemctl is-active nginx
systemctl is-enabled nginx

# List all services
systemctl list-units --type=service
systemctl list-units --type=service --state=running

# View logs
journalctl -u nginx
journalctl -u nginx -f  # follow
journalctl -u nginx --since "1 hour ago"
journalctl -u nginx --since "2024-01-01" --until "2024-01-02"

# Reload systemd daemon
systemctl daemon-reload

# Mask service (prevent start)
systemctl mask nginx
systemctl unmask nginx
```

## Disk Management

```bash
# Disk usage
du -sh /path
du -sh *  # size of each item
du -h --max-depth=1

# Find large files
find / -type f -size +1G
du -ah / | sort -rh | head -n 20

# Disk space
df -h
df -i  # inodes

# Mount filesystem
mount /dev/sdb1 /mnt
mount -t ext4 /dev/sdb1 /mnt

# Unmount
umount /mnt

# Check mount points
mount | grep sdb
cat /proc/mounts

# Edit fstab for auto-mount
vi /etc/fstab
# /dev/sdb1  /mnt  ext4  defaults  0  2

# Format partition
mkfs.ext4 /dev/sdb1
mkfs.xfs /dev/sdb1

# Check filesystem
fsck /dev/sdb1
e2fsck /dev/sdb1

# Partition management
fdisk /dev/sdb
parted /dev/sdb

# LVM operations
pvdisplay  # physical volumes
vgdisplay  # volume groups
lvdisplay  # logical volumes
lvcreate -L 10G -n mylv myvg
lvextend -L +5G /dev/myvg/mylv
resize2fs /dev/myvg/mylv
```

## Log Management

```bash
# View system logs
tail -f /var/log/syslog
tail -f /var/log/messages

# View authentication logs
tail -f /var/log/auth.log
tail -f /var/log/secure

# View logs with journalctl
journalctl -f
journalctl -b  # current boot
journalctl -p err  # errors only
journalctl --since "1 hour ago"
journalctl --since "2024-01-01 00:00:00"
journalctl -u nginx.service

# Disk usage by logs
journalctl --disk-usage

# Rotate logs
logrotate -f /etc/logrotate.conf

# Clear journal logs
journalctl --vacuum-time=7d  # keep 7 days
journalctl --vacuum-size=1G  # keep 1GB
```

## Cron Jobs

```bash
# Edit crontab
crontab -e

# List crontab
crontab -l

# Remove crontab
crontab -r

# Edit crontab for user
crontab -u username -e

# Crontab syntax
# * * * * * command
# │ │ │ │ │
# │ │ │ │ └─── day of week (0-7, 0 and 7 are Sunday)
# │ │ │ └───── month (1-12)
# │ │ └─────── day of month (1-31)
# │ └───────── hour (0-23)
# └─────────── minute (0-59)

# Examples
0 2 * * * /path/to/backup.sh  # daily at 2 AM
*/15 * * * * /path/to/script.sh  # every 15 minutes
0 0 * * 0 /path/to/weekly.sh  # weekly on Sunday
@reboot /path/to/startup.sh  # at reboot

# System cron directories
ls /etc/cron.daily/
ls /etc/cron.weekly/
ls /etc/cron.monthly/
```

## SSH Operations

```bash
# Connect to server
ssh user@hostname
ssh user@hostname -p 2222  # custom port

# Copy SSH key
ssh-copy-id user@hostname
cat ~/.ssh/id_rsa.pub | ssh user@hostname "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"

# Generate SSH key
ssh-keygen -t rsa -b 4096
ssh-keygen -t ed25519

# SCP (secure copy)
scp localfile user@hostname:/remote/path
scp user@hostname:/remote/file /local/path
scp -r localdir user@hostname:/remote/path  # recursive

# SFTP
sftp user@hostname

# SSH tunnel
ssh -L 8080:localhost:80 user@hostname  # local port forward
ssh -R 8080:localhost:80 user@hostname  # remote port forward
ssh -D 8080 user@hostname  # dynamic port forward (SOCKS proxy)

# SSH config
vi ~/.ssh/config
# Host myserver
#     HostName 192.168.1.100
#     User ubuntu
#     Port 22
#     IdentityFile ~/.ssh/id_rsa

# Then connect with
ssh myserver

# SSH agent
eval $(ssh-agent)
ssh-add ~/.ssh/id_rsa
```

## Compression and Archives

```bash
# Tar
tar -czf archive.tar.gz /path/to/directory  # create gzip
tar -cjf archive.tar.bz2 /path/to/directory  # create bzip2
tar -xzf archive.tar.gz  # extract gzip
tar -xjf archive.tar.bz2  # extract bzip2
tar -tzf archive.tar.gz  # list contents
tar -xzf archive.tar.gz -C /destination  # extract to location

# Gzip
gzip file
gunzip file.gz
gzip -d file.gz

# Bzip2
bzip2 file
bunzip2 file.bz2

# Zip
zip archive.zip file1 file2
zip -r archive.zip directory
unzip archive.zip
unzip -l archive.zip  # list contents

# 7zip
7z a archive.7z file
7z x archive.7z
7z l archive.7z  # list contents
```

## Text Processing

```bash
# View files
cat file
less file
more file
head file
head -n 20 file
tail file
tail -n 20 file
tail -f file  # follow

# Search in files
grep "pattern" file
grep -r "pattern" /path  # recursive
grep -i "pattern" file  # case insensitive
grep -v "pattern" file  # invert match
grep -n "pattern" file  # line numbers
grep -A 5 "pattern" file  # 5 lines after
grep -B 5 "pattern" file  # 5 lines before

# Replace text
sed 's/old/new/g' file
sed -i 's/old/new/g' file  # in-place
sed -i.bak 's/old/new/g' file  # backup

# AWK
awk '{print $1}' file  # first column
awk -F: '{print $1}' /etc/passwd  # custom delimiter
awk 'NR==5' file  # 5th line

# Sort
sort file
sort -r file  # reverse
sort -n file  # numeric
sort -k2 file  # by 2nd column
sort -u file  # unique

# Unique lines
uniq file
sort file | uniq  # remove duplicates
uniq -c file  # count occurrences

# Word count
wc file
wc -l file  # lines
wc -w file  # words
wc -c file  # bytes

# Cut columns
cut -d: -f1 /etc/passwd  # 1st field
cut -c1-10 file  # characters 1-10

# Compare files
diff file1 file2
comm file1 file2
```

## System Monitoring

```bash
# CPU usage
top
htop
mpstat 1  # per second

# Memory usage
free -h
vmstat 1

# Disk I/O
iostat 1
iotop

# Network monitoring
iftop
nethogs
nload

# System calls
strace command
strace -p PID

# Library calls
ltrace command

# Open files
lsof
lsof -i :80  # port 80
lsof -u username  # by user
lsof -p PID  # by process

# Resource limits
ulimit -a
ulimit -n 4096  # set file descriptors
```

## Performance Tuning

```bash
# Check system limits
cat /proc/sys/fs/file-max
cat /proc/sys/net/core/somaxconn

# Set limits temporarily
sysctl -w net.ipv4.ip_forward=1
sysctl -w fs.file-max=100000

# Set limits permanently
vi /etc/sysctl.conf
# fs.file-max = 100000
# net.ipv4.ip_forward = 1
sysctl -p  # apply changes

# User limits
vi /etc/security/limits.conf
# username soft nofile 4096
# username hard nofile 8192

# Check open file limit
ulimit -n

# System load
uptime
w
tload
```

I'll create the final README file for Docker to complete the set: