/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.criteria;

import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_AREA_CONFIG;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_AREA_CONFIG_COUNT;

import java.util.Arrays;
import java.util.Set;

import com.google.common.collect.Sets;
import com.vaadin.event.dd.acceptcriteria.Or;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;

/**
 * Server part for the client-side accept criterion
 * <code>ViewClientCriterion</code>.<br>
 * This class is intended to be sub-classed. Each sub-class takes over
 * responsibility of a UI view.<br>
 * NOTE: Only the server part of the criterion has to be sub-classed, not the
 * client part.<br>
 * This class represents a composite for
 * <code>ServerViewComponentClientCriterion</code> elements. To find out if the
 * current drop location is a valid drop target for the current drag source, the
 * list of elements is iterated through to retrieve:
 * <ol>
 * <li>a <code>ServerViewComponentClientCriterion</code> responsible for the
 * drag source (the one that has a matching drag source id-prefix)</li>
 * <li>if the drop location is a valid drop target for the responsible
 * <code>ServerViewComponentClientCriterion</code> (by testing the valid drop
 * target id-prefixes)</li>
 * </ol>
 */
public class ServerViewClientCriterion extends Or {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 3208301105751198826L;

    private final Set<String> dropAreaHints;

    /**
     * Constructor for the server part of the client-side accept criterion.
     *
     * @param criteria
     *            elements the composite consists of.
     */
    public ServerViewClientCriterion(ServerViewComponentClientCriterion... criteria) {
        super(criteria);
        dropAreaHints = Sets.newHashSet();
        for (ServerViewComponentClientCriterion criterion : criteria) {
            dropAreaHints.addAll(Arrays.asList(criterion.getValidDropAreaIds()));
        }
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        int dropAreaStylesConfigCount = 0;
        for (String dropAreaEntry : dropAreaHints) {
            target.addAttribute(DROP_AREA_CONFIG + dropAreaStylesConfigCount, dropAreaEntry);
            dropAreaStylesConfigCount++;
        }
        target.addAttribute(DROP_AREA_CONFIG_COUNT, dropAreaStylesConfigCount);

        super.paintContent(target);
    }

    @Override
    protected String getIdentifier() {
        return ServerViewClientCriterion.class.getCanonicalName();
    }

}
