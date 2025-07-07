/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.mgmt.json.model.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.hateoas.RepresentationModel;

/**
 * A json annotated rest model for a tenant configuration value to RESTful API
 * representation.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
        **properties**:
        * **rollout.approval.enabled** - Boolean, The configuration key 'rollout.approval.enabled' defines if approval mode for Rollout Management is enabled.
        * **repository.actions.autoclose.enabled** - Boolean, The configuration key 'repository.actions.autoclose.enabled' defines if autoclose running actions with new Distribution Set assignment is enabled.
        * **user.confirmation.flow.enabled** - Boolean, The configuration key 'user.confirmation.flow.enabled' defines if confirmation is required when distribution set is assigned to target.
        * **authentication.gatewaytoken.enabled** - Boolean, The configuration key 'authentication.gatewaytoken.enabled' defines if the authentication mode 'gateway security token' is enabled.
        * **action.cleanup.enabled** - Boolean, The configuration key 'action.cleanup.enabled' defines if automatic cleanup of deployment actions is enabled.
        * **action.cleanup.actionExpiry** - Long, The configuration key 'action.cleanup.actionExpiry' defines the expiry time in milliseconds that needs to elapse before an action may be cleaned up.
        * **authentication.header.enabled** - Boolean, The configuration key 'authentication.header.enabled' defines if the authentication mode 'authority header' is enabled.
        * **maintenanceWindowPollCount** - Integer, The configuration key 'maintenanceWindowPollCount' defines the polling interval so that controller tries to poll at least these many times between the last polling and before start of maintenance window. The polling interval is bounded by configured pollingTime and minPollingTime. The polling interval is modified as per following scheme: pollingTime(@time=t) = (maintenanceWindowStartTime - t)/maintenanceWindowPollCount.
        * **authentication.targettoken.enabled** - Boolean, The configuration key 'authentication.targettoken.enabled' defines if the authentication mode 'target security token' is enabled.
        * **minPollingTime** - String, The configuration key 'minPollingTime' defines the smallest time interval permitted between two poll requests of a target.
        * **pollingTime** - String, The configuration key 'pollingTime' defines the time interval between two poll requests of a target.
        * **pollingOverdueTime** - String, The configuration key 'pollingOverdueTime' defines the period of time after the SP server will recognize a target, which is not performing pull requests anymore.
        * **authentication.header.authority** - String, The configuration key 'authentication.header.authority' defines the name of the 'authority header'.
        * **authentication.gatewaytoken.key** - String, The configuration key 'authentication.gatewaytoken.key' defines the key of the gateway security token.
        * **action.cleanup.actionStatus** - String, The configuration key 'action.cleanup.actionStatus' defines the list of action status that should be taken into account for the cleanup.
        * **multi.assignments.enabled** - Boolean, The configuration key 'multi.assignments.enabled' defines if multiple distribution sets can be assigned to the same targets.
        * **batch.assignments.enabled** - Boolean, The configuration key 'batch.assignments.enabled' defines if distribution set can be assigned to multiple targets in a single batch message.
        * **implicit.lock.enabled** - Boolean (true by default), The configuration key 'implicit.lock.enabled' defines if distribution set and their software modules shall be implicitly locked when assigned to target, rollout or target filter.
        """, example = """
        {
          "value" : "",
          "global" : true,
          "_links" : {
            "self" : {
              "href" : "https://management-api.host.com/rest/v1/system/configs/authentication.gatewaytoken.key"
            }
          }
        }""")
public class MgmtSystemTenantConfigurationValue extends RepresentationModel<MgmtSystemTenantConfigurationValue> {

    @JsonInclude
    @Schema(description = "Current value of of configuration parameter", example = "true")
    private Object value;

    @JsonInclude
    @Schema(description = "true - if the current value is the global configuration value, false - if there is a " +
            "tenant specific value configured", example = "true")
    private boolean global = true;

    @Schema(description = "Entity was last modified at (timestamp UTC in milliseconds)", example = "1623085150")
    @EqualsAndHashCode.Exclude
    private Long lastModifiedAt;

    @Schema(description = "Entity was last modified by (User, AMQP-Controller, anonymous etc.)",
            example = "example user")
    private String lastModifiedBy;

    @Schema(description = "Entity was originally created at (timestamp UTC in milliseconds)", example = "1523085150")
    private Long createdAt;

    @Schema(description = "Entity was originally created by (User, AMQP-Controller, anonymous etc.)",
            example = "example user")
    private String createdBy;
}