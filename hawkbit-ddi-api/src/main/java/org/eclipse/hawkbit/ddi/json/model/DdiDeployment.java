/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Detailed update action information.
 */
public class DdiDeployment {

    private HandlingType download;

    private HandlingType update;

    @JsonProperty("chunks")
    @NotNull
    private List<DdiChunk> chunks;

    /**
     * Constructor.
     */
    public DdiDeployment() {
        // needed for json create.
    }

    /**
     * Constructor.
     *
     * @param download
     *            handling type
     * @param update
     *            handling type
     * @param chunks
     *            to handle.
     */
    public DdiDeployment(final HandlingType download, final HandlingType update, final List<DdiChunk> chunks) {
        this.download = download;
        this.update = update;
        this.chunks = chunks;
    }

    public HandlingType getDownload() {
        return download;
    }

    public HandlingType getUpdate() {
        return update;
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

    @Override
    public String toString() {
        return "Deployment [download=" + download + ", update=" + update + ", chunks=" + chunks + "]";
    }

}
