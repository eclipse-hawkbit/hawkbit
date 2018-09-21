#
# Copyright (c) 2018 Bosch Software Innovations GmbH and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

STACK_NAME=hawkbit

echo "/------------------------------------------------------------------------------"
echo "|  Data Clean Up START:  $(date -u) "
echo "+------------------------------------------------------------------------------"
echo "|"
echo "+-- Remove Docker Stack:"
docker stack rm $STACK_NAME
sleep 15
echo "|"
echo "+-- Remove Docker Container:"
docker container prune --force
sleep 5
echo "|"
echo "+-- Remove Docker Volumes:"
docker volume prune --force
echo "|"
echo "+-- Verify Docker Container:"
docker container ls --all
echo "|"
echo "+-- Verify Docker Volumes:"
docker volume ls
echo "|"
echo "+-- Restart Docker Stack:"
docker stack deploy -c /.sandbox/stacks/sandbox/docker-compose-stack.yml $STACK_NAME
echo "|"
# Value is based on trial and error
echo "+-- Wait for hawkBit to start (160s):"
sleep 160
echo "|"
# Device Simulator has to be restarted, since hawkBit takes too long to start
echo "+-- Restart Device Simulator:"
docker service update --force hawkbit_simulator
echo "|"
# Images used by a container are not deleted. Therefore, we run this after the stacks
# are started. Only unused images will be deleted.
echo "+-- Clean up Docker:"
docker system prune --force
echo "+------------------------------------------------------------------------------"
echo "|  END:  $(date -u) "
echo "\------------------------------------------------------------------------------"
echo ""
