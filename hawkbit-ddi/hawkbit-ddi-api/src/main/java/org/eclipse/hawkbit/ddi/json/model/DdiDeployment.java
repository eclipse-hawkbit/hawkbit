/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Detailed update action information.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiDeployment {

    @Schema(description = """
            Handling for the download part of the provisioning process ('skip': do not download yet, 'attempt': server asks
            to download, 'forced': server requests immediate download)""")
    private final HandlingType download;

    @Schema(description = """
            Handling for the update part of the provisioning process ('skip': do not update yet,
            'attempt': server asks to update, 'forced': server requests immediate update)""")
    private final HandlingType update;

    @NotNull
    @Schema(description = "Software chunks of an update. In server mapped by Software Module")
    private final List<DdiChunk> chunks;

    @Schema(description = """
            Separation of download and installation by defining a maintenance window for the installation. Status shows if
            currently in a window""")
    private final DdiMaintenanceWindowStatus maintenanceWindow;

    /**
     * Constructor.
     *
     * @param download handling type
     * @param update handling type
     * @param chunks to handle.
     * @param maintenanceWindow specifying whether there is a maintenance schedule associated.
     *         If it is, the value is either 'available' (i.e. the maintenance window is now available as per defined schedule
     *         and the update can progress) or 'unavailable' (implying that maintenance window is not available now and update should not
     *         be attempted). If there is no maintenance schedule defined, the parameter is null.
     */
    @JsonCreator
    public DdiDeployment(
            @JsonProperty("download") final HandlingType download,
            @JsonProperty("update") final HandlingType update,
            @JsonProperty(value = "chunks", required = true) final List<DdiChunk> chunks,
            @JsonProperty("maintenanceWindow") final DdiMaintenanceWindowStatus maintenanceWindow) {
        this.download = download;
        this.update = update;
        this.chunks = chunks;
        this.maintenanceWindow = maintenanceWindow;
    }

    public List<DdiChunk> getChunks() {
        if (chunks == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(chunks);
    }

    /**
     * The handling type for the update action.
     */
    public enum HandlingType {

        /**
         * Not necessary for the command.
         */
        SKIP("skip"),

        /**
         * Try to execute (local applications may intervene by SP control API).
         */
        ATTEMPT("attempt"),

        /**
         * Execution independent of local intervention attempts.
         */
        FORCED("forced");

        private String name;

        HandlingType(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

    /**
     * Status of the maintenance window for action.
     */
    public enum DdiMaintenanceWindowStatus {
        /**
         * A window is currently available, target can go ahead with
         * installation.
         */
        AVAILABLE("available"),

        /**
         * A window is not available, target should wait and skip the
         * installation.
         */
        UNAVAILABLE("unavailable");

        private String status;

        DdiMaintenanceWindowStatus(final String status) {
            this.status = status;
        }

        /**
         * @return status of maintenance window.
         */
        @JsonValue
        public String getStatus() {
            return this.status;
        }
    }
}