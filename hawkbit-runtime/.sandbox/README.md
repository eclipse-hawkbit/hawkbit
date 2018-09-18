hawkBit Sandbox
===

## Try out the update server in our hawkBit sandbox

- try out Management UI https://hawkbit.eclipse.org/UI (username: admin, passwd: admin)
- try out Management API https://hawkbit.eclipse.org/rest/v1/targets (don't forget basic auth header; username: admin, passwd: admin)
- try out DDI API https://hawkbit.eclipse.org/DEFAULT/controller/v1/MYTESTDEVICE (authentication disabled)


## Disclaimer

The sandbox is a shared installation that will be reset from time to time. Therefore, it is not allowed to upload
any personal data. 


## Sandbox Setup

**1. File Structure** 

Copy the files to the respective location on the VM. 

```
/
+-stacks
|  +-sandbox
|  |   +-docker-compose-stack.yml
|  +-proxy
|      +-docker-compose-stack.yml  
+- opt
   +-crontab
       +-hawkbit-sandbox-cleanup-task.sh      
```

**2. Weekly Reset**

Reset the Sandbox once a week with a cron-job and log its output.

```
$ sudo crontab -e
0 0 * * 0 /opt/crontab/hawkbit-sandbox-cleanup-task.sh >> /var/log/hawkbit.log 2>&1
```

**3. Nginx Reverse Proxy**

Start the stack for the Nginx reverse proxy with Let's Encrypt support. Thanks to JrCs for providing the Docker container
 and instructions with his [Docker-Letsencrypt-Nginx-Companion](https://github.com/JrCs/docker-letsencrypt-nginx-proxy-companion).

```
$ cd /stacks/proxy/
$ docker stack deploy -c docker-compose-stack.yml proxy
```

**4. hawkBit**

Start the hawkBit stack.

```
$ cd /stacks/sandbox/
$ docker stack deploy -c docker-compose-stack.yml hawkbit
```