/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import java.util.List;

import org.eclipse.hawkbit.repository.model.Action;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Detailed {@link UpdateAction} information.
 *
 */
public class Deployment {

    private final HandlingType download;

    private final HandlingType update;

    private final List<Chunk> chunks;

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
    public Deployment(final HandlingType download, final HandlingType update, final List<Chunk> chunks) {
        super();
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

    public List<Chunk> getChunks() {
        return chunks;
    }

    /**
     * The handling type for the update {@link Action}.
     *
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

        private HandlingType(final String name) {
            this.name = name;
        }

        /**
         * @return the name
         */
        @JsonValue
        public String getName() {
            return name;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Deployment [download=" + download + ", update=" + update + ", chunks=" + chunks + "]";
    }

}
