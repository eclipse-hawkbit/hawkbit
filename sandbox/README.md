hawkBit Sandbox
===

We offer a sandbox installation that is free for everyone to try out hawkBit's [Management UI](https://eclipse.org/hawkbit/ui/), 
[Management API](https://eclipse.org/hawkbit/apis/management_api/), and [Direct Device Integration API](https://eclipse.org/hawkbit/apis/ddi_api/).

* URL: [https://hawkbit.eclipse.org/](https://hawkbit.eclipse.org/)
* Bugzilla: [Bugzilla ID: 535953](https://bugs.eclipse.org/bugs/show_bug.cgi?id=535953)


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