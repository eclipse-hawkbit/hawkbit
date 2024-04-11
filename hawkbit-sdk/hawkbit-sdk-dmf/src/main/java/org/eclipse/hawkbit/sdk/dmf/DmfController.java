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
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.sdk.dmf.amqp.DmfSenderService;

import java.util.Collections;
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
    private final DmfSenderService dmfSenderService;

    // configuration
    private final boolean downloadAuthenticationEnabled;

    @Getter @Setter
    private volatile Map<String, String> attributes = Collections.emptyMap();

    private volatile Long currentActionId;

    private volatile Long lastActionId;

    /**
     * Creates a new device instance.
     *
     * @param tenant the tenant of the device belongs to
     * @param controller the controller
     */
    public DmfController(
            final Tenant tenant, final Controller controller,
            final UpdateHandler updateHandler,
            final DmfSenderService dmfSenderService) {
        this.tenantId = tenant.getTenantId();
        downloadAuthenticationEnabled = tenant.isDownloadAuthenticationEnabled();
        this.controllerId = controller.getControllerId();
        this.updateHandler = updateHandler == null ? UpdateHandler.SKIP : updateHandler;
        this.dmfSenderService = dmfSenderService;
    }

    public void connect() {
        log.debug(LOG_PREFIX + "Connecting/Polling ...", getTenantId(), getControllerId());
        dmfSenderService.createOrUpdateThing(getTenantId(), getControllerId());
        log.debug(LOG_PREFIX + "Done. Create thing sent.", getTenantId(), getControllerId());
    }

    public void poll() {
        log.debug(LOG_PREFIX + "Polling ..", getTenantId(), getControllerId());
        dmfSenderService.createOrUpdateThing(getTenantId(), getControllerId());
        log.debug(LOG_PREFIX + "Done. Create thing sent.", getTenantId(), getControllerId());
    }

    public void stop() {
        lastActionId = null;
        currentActionId = null;
    }

    public void processUpdate(final EventTopic actionType, final DmfDownloadAndUpdateRequest updateRequest) {
        updateHandler.getUpdateProcessor(this, actionType, updateRequest);
    }

    public void sendFeedback(final UpdateStatus updateStatus) {
        dmfSenderService.sendFeedback(tenantId, currentActionId, updateStatus);
    }
}