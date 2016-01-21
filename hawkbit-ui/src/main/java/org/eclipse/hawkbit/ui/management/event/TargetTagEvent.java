/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import org.eclipse.hawkbit.repository.model.TargetTag;

/**
 *
 *
 *
 */
public class TargetTagEvent {

    private TargetTagComponentEvent targetTagComponentEvent;

    private TargetTag targetTag;

    private String targetTagName;

    /**
     *
     *
     */
    public enum TargetTagComponentEvent {

        ADD_TARGETTAG, EDIT_TARGETTAG, DELETE_TARGETTAG, ASSIGNED, UNASSIGNED
    }

    /**
     * @param targetTagComponentEvent
     * @param targetTag
     */
    public TargetTagEvent(final TargetTagComponentEvent targetTagComponentEvent, final TargetTag targetTag) {
        this.targetTagComponentEvent = targetTagComponentEvent;
        this.targetTag = targetTag;

    }

    /**
     * @param targetTagComponentEvent
     * @param tagName
     */
    public TargetTagEvent(final TargetTagComponentEvent targetTagComponentEvent, final String tagName) {
        this.targetTagComponentEvent = targetTagComponentEvent;
        this.targetTagName = tagName;

    }

    public TargetTag getTargetTag() {
        return targetTag;
    }

    public void setTargetTag(final TargetTag targetTag) {
        this.targetTag = targetTag;

    }

    public TargetTagComponentEvent getTargetTagComponentEvent() {
        return targetTagComponentEvent;
    }

    public void setTargetTagComponentEvent(final TargetTagComponentEvent targetTagComponentEvent) {
        this.targetTagComponentEvent = targetTagComponentEvent;
    }

    public String getTargetTagName() {
        return targetTagName;
    }

}
