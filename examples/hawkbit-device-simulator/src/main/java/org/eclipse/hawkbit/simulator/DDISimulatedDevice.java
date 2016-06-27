/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import org.eclipse.hawkbit.simulator.http.ControllerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * A simulated device using the DDI API of the hawkBit update server.
 *
 */
public class DDISimulatedDevice extends AbstractSimulatedDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(DDISimulatedDevice.class);

    private final ControllerResource controllerResource;

    private final DeviceSimulatorUpdater deviceUpdater;

    private volatile boolean removed;
    private volatile Long currentActionId;

    /**
     * @param id
     *            the ID of the device
     * @param tenant
     *            the tenant of the simulated device
     * @param pollDelaySec
     *            the delay of the poll interval in sec
     * @param controllerResource
     *            the http controller resource
     * @param deviceUpdater
     *            the service to update devices
     */
    public DDISimulatedDevice(final String id, final String tenant, final int pollDelaySec,
            final ControllerResource controllerResource, final DeviceSimulatorUpdater deviceUpdater) {
        super(id, tenant, Protocol.DDI_HTTP, pollDelaySec);
        this.controllerResource = controllerResource;
        this.deviceUpdater = deviceUpdater;
    }

    @Override
    public void clean() {
        super.clean();
        removed = true;
    }

    /**
     * Polls the base URL for the DDI API interface.
     */
    @Override
    public void poll() {
        if (!removed) {
            final String basePollJson = controllerResource.get(getTenant(), getId());
            try {
                final String href = JsonPath.parse(basePollJson).read("_links.deploymentBase.href");
                final long actionId = Long.parseLong(href.substring(href.lastIndexOf('/') + 1, href.indexOf('?')));
                if (currentActionId == null) {
                    final String deploymentJson = controllerResource.getDeployment(getTenant(), getId(), actionId);
                    final String swVersion = JsonPath.parse(deploymentJson).read("deployment.chunks[0].version");
                    currentActionId = actionId;
                    startDdiUpdate(actionId, swVersion);
                }
            } catch (final PathNotFoundException e) {
                // href might not be in the json response, so ignore
                // exception here.
                LOGGER.trace("Response does not contain a deploymentbase href link, ignoring.", e);
            }

        }
    }

    private void startDdiUpdate(final long actionId, final String swVersion) {
        deviceUpdater.startUpdate(getTenant(), getId(), actionId, swVersion, null, null, (device, actionId1) -> {
            switch (device.getUpdateStatus().getResponseStatus()) {
            case SUCCESSFUL:
                controllerResource.postSuccessFeedback(getTenant(), getId(), actionId1);
                break;
            case ERROR:
                controllerResource.postErrorFeedback(getTenant(), getId(), actionId1);
                break;
            default:
                throw new IllegalStateException("simulated device has an unknown response status + "
                        + device.getUpdateStatus().getResponseStatus());
            }
            currentActionId = null;
        });
    }
}
