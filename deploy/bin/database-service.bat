@echo off
net start MySQL
cd /d E:\Program Files\Redis\
redis-server.exe redis.conf
echo redis service start successfully
pause
