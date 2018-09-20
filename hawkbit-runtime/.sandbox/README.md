hawkBit Sandbox
===

## Try out the update server in our hawkBit sandbox

- try out Management UI https://hawkbit.eclipse.org (username: demo, passwd: demo)
- try out Management API https://hawkbit.eclipse.org/rest/v1/targets (don't forget basic auth header; username: demo, passwd: demo)
- try out DDI API https://hawkbit.eclipse.org/DEFAULT/controller/v1/MYTESTDEVICE 


## Sandbox Setup

**1. File Structure** 

Copy the files to the respective location on the VM. 

```
/
+.sandbox
  |
  +-stacks
  |  +-sandbox
  |  |   +-docker-compose-stack.yml
  |  +-proxy
  |      +-docker-compose-stack.yml  
  +- scripts
     +-intialize-cronjobs.sh
     +-sandbox-cleanup.sh 
```

**2. Initialize Cronjobs**

Reset the Sandbox once a week with a cron-job and log its output.

```
$ sudo /.sandbox/scripts/initialize-cronjobs.sh
```


**3. Nginx Reverse Proxy**

Start the stack for the Nginx reverse proxy with Let's Encrypt support. Thanks to JrCs for providing the Docker container
 and instructions with his [Docker-Letsencrypt-Nginx-Companion](https://github.com/JrCs/docker-letsencrypt-nginx-proxy-companion).

```
$ docker stack deploy -c /.sandbox/stacks/sandbox/docker-compose-stack.yml proxy
```

**4. hawkBit**

Start the hawkBit stack.

```
$ docker stack deploy -c /.sandbox/stacks/sandbox/docker-compose-stack.yml hawkbit
```