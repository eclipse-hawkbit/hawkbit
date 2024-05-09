/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.sdk.dmf.amqp.DmfSender;

import java.util.HashMap;
import java.util.Map;

/**
 * Class representing DMF device twin connecting to hawkBit via DMF.
 */
@Slf4j
@Getter
public class DmfController {

    private static final String LOG_PREFIX = "[{}:{}] ";

    private static final String DEPLOYMENT_BASE_LINK = "deploymentBase";
    private static final String CONFIRMATION_BASE_LINK = "confirmationBase";

    private final String tenantId;
    private final String controllerId;
    private final UpdateHandler updateHandler;
    private final DmfSender dmfSender;

    // configuration
    private final boolean downloadAuthenticationEnabled;

    @Getter @Setter
    private volatile Map<String, String> attributes = new HashMap<>();

    @Setter
    private volatile Long currentActionId;

    private volatile Long lastActionId;

    /**
     * Creates a new device instance.
     *
     * @param tenant the tenant of the device belongs to
     * @param controller the controller
     */
    DmfController(
            final Tenant tenant, final Controller controller,
            final UpdateHandler updateHandler,
            final DmfSender dmfSender) {
        this.tenantId = tenant.getTenantId();
        downloadAuthenticationEnabled = tenant.isDownloadAuthenticationEnabled();
        this.controllerId = controller.getControllerId();
        this.updateHandler = updateHandler == null ? UpdateHandler.SKIP : updateHandler;
        this.dmfSender = dmfSender;
    }

    public void connect() {
        log.debug(LOG_PREFIX + "Connecting/Polling ...", getTenantId(), getControllerId());
        dmfSender.createOrUpdateThing(getTenantId(), getControllerId());
        log.debug(LOG_PREFIX + "Done. Create thing sent.", getTenantId(), getControllerId());
    }

    public void poll() {
        log.debug(LOG_PREFIX + "Polling ..", getTenantId(), getControllerId());
        dmfSender.createOrUpdateThing(getTenantId(), getControllerId());
        log.debug(LOG_PREFIX + "Done. Create thing sent.", getTenantId(), getControllerId());
    }

    public void stop() {
        lastActionId = null;
        currentActionId = null;
    }

    public void processUpdate(final EventTopic actionType, final DmfDownloadAndUpdateRequest updateRequest) {
        log.info("Processing update for {} actionId : {}", controllerId, updateRequest.getActionId());
        updateHandler.getUpdateProcessor(this, actionType, updateRequest).run();
        updateRequest.getSoftwareModules().forEach(sm -> {
            updateAttributes(sm.getModuleId().toString(), sm.getModuleVersion());
        });
    }

    public void sendFeedback(final UpdateStatus updateStatus) {
        log.info("Sending {} for controller : {} action : {}", updateStatus.status(), controllerId, currentActionId);
        dmfSender.sendFeedback(tenantId, currentActionId, updateStatus);
    }

    public void sendUpdateAttributes() {
        log.info("Sending UPDATE_ATTRIBUTES for controller {}", controllerId);
        dmfSender.updateAttributes(tenantId, controllerId, DmfUpdateMode.MERGE, attributes);
    }

    public void updateAttributes(final String key, final String value) {
        this.attributes.put(key, value);
    }
}