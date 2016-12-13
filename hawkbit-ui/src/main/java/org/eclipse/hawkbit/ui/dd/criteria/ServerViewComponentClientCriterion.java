/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.criteria;

import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DRAG_SOURCE;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_AREA;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_AREA_COUNT;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_TARGET;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_TARGET_COUNT;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.ClientSideCriterion;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;

/**
 * Server part for the client-side accept criterion
 * <code>ViewComponentClientCriterion</code>. Specifies valid drop targets (and
 * the associated drop areas) for a drag source.
 */
public class ServerViewComponentClientCriterion extends ClientSideCriterion {

    private static final long serialVersionUID = -2744174296987361607L;

    private final String dragSourceIdPrefix;

    private final String[] validDropTargetIdPrefixes;

    private final String[] validDropAreaIds;

    /**
     * Constructor for the accept criterion using a builder.
     *
     * @param builder
     *            the associated builder for convenient use.
     */
    protected ServerViewComponentClientCriterion(Builder builder) {
        this.dragSourceIdPrefix = builder.dragSourceIdPrefix;
        this.validDropTargetIdPrefixes = builder.validDropTargetIdPrefixes;
        this.validDropAreaIds = builder.validDropAreaIds;
    }

    /**
     * Constructor for the accept criterion with direct parameter use.
     *
     * @param dragSourceIdPrefix
     *            specifies the drag source (as id-prefix) the accept criterion
     *            is responsible for. A current drag source matches the
     *            configured prefix if its id starts with this prefix.
     * @param validDropTargetIdPrefixes
     *            specifies the valid drop targets the drag source is valid for.
     *            The current drop target (the one the mouse is over) accepts
     *            the drag source if it starts with one of these prefixes.
     * @param validDropAreaIds
     *            these ids are used to highlight the drop areas valid for the
     *            configured drag source (prefix) - if it matches the current
     *            current drag source.
     */
    public ServerViewComponentClientCriterion(String dragSourceIdPrefix, String[] validDropTargetIdPrefixes,
            String[] validDropAreaIds) {
        this.dragSourceIdPrefix = dragSourceIdPrefix;
        this.validDropTargetIdPrefixes = validDropTargetIdPrefixes;
        this.validDropAreaIds = validDropAreaIds;
    }

    /**
     * Gets the drag source (as id-prefix) the accept criterion is responsible
     * for.
     *
     * @return drag source the accept criterion is responsible for (as
     *         id-prefix)
     */
    String getDragSourcePrefix() {
        return dragSourceIdPrefix;
    }

    /**
     * Gets id of valid drop areas used to highlight drop hints.
     *
     * @return drop area ids to be highlighted if the configured drag source is
     *         active.
     */
    String[] getValidDropAreaIds() {
        return validDropAreaIds;
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(target);

        target.addAttribute(DRAG_SOURCE, dragSourceIdPrefix);

        int countDropTarget = 0;
        for (String prefix : validDropTargetIdPrefixes) {
            target.addAttribute(DROP_TARGET + countDropTarget, prefix);
            countDropTarget++;
        }
        target.addAttribute(DROP_TARGET_COUNT, countDropTarget);

        int countDropAreas = 0;
        for (String dropArea : validDropAreaIds) {
            target.addAttribute(DROP_AREA + countDropAreas, dropArea);
            countDropAreas++;
        }
        target.addAttribute(DROP_AREA_COUNT, countDropAreas);
    }

    @Override
    public boolean accept(DragAndDropEvent dragEvent) {
        String dragSourceId = dragEvent.getTransferable().getSourceComponent().getId();
        // double-check if this is the right handler:
        if (!dragSourceId.startsWith(getDragSourcePrefix())) {
            return false;
        }

        final String dropTargetId = dragEvent.getTargetDetails().getTarget().getId();
        for (String cId : validDropTargetIdPrefixes) {
            if (dropTargetId.startsWith(cId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected String getIdentifier() {
        // extending classes use client side implementation from this class
        return ServerViewComponentClientCriterion.class.getCanonicalName();
    }

    /**
     * Creates a builder to build
     * <code>ServerViewComponentClientCriterion</code> objects in a convenient
     * way.
     *
     * @return builder
     */
    public static Builder createBuilder() {
        return new Builder();
    }

    /**
     * Builds <code>ServerViewComponentClientCriterion</code> objects in a
     * convenient way.
     */
    public static class Builder {

        private String dragSourceIdPrefix;
        private String[] validDropTargetIdPrefixes;
        private String[] validDropAreaIds;

        /**
         * Configures the id-prefix of the drag-source the accept criterion is
         * responsible for.
         *
         * @param dragSourceIdPrefix
         *            the id-prefix of the drag source the criterion is
         *            responsible for.
         * @return builder
         */
        public Builder dragSourceIdPrefix(String dragSourceIdPrefix) {
            this.dragSourceIdPrefix = dragSourceIdPrefix;
            return this;
        }

        /**
         * Configures the prefixes of drop-target ids used by the accept
         * criterion to match valid drop-targets.
         *
         * @param validDropTargetIdPrefixes
         *            id-prefixes of drop targets valid for the configured drag
         *            source (id-prefix).
         * @return builder
         */
        public Builder dropTargetIdPrefixes(String... validDropTargetIdPrefixes) {
            this.validDropTargetIdPrefixes = validDropTargetIdPrefixes;
            return this;
        }

        /**
         * Configures the drop area ids used to be highlighted when a drag
         * operation starts for the associated drag source.
         *
         * @param validDropAreaIds
         * @return builder
         */
        public Builder dropAreaIds(String... validDropAreaIds) {
            this.validDropAreaIds = validDropAreaIds;
            return this;
        }

        /**
         * Builds the previously configured accept criterion.
         *
         * @return accept criterion
         */
        public ServerViewComponentClientCriterion build() {
            return new ServerViewComponentClientCriterion(this);
        }
    }
}
