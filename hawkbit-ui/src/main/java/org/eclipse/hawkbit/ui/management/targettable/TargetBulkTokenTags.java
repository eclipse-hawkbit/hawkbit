/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTagToken;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTagEvent;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.UI;

/**
 * Target tag layout in bulk upload popup.
 * 
 * 
 *
 *
 */
@SpringComponent
@ViewScope
public class TargetBulkTokenTags extends AbstractTagToken {

    /**
    * 
    */
    private static final long serialVersionUID = 4159616629565523717L;
    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient TagManagement tagManagement;

    @Autowired
    private SpPermissionChecker checker;

    private UI ui;

    private final List<String> assignedTagNames = new ArrayList<>();

    @PostConstruct
    protected void init() {
        super.init();
        ui = UI.getCurrent();
        eventBus.subscribe(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.tagdetails.TargetTagToken#assignTag(java.
     * lang.String)
     */
    @Override
    protected void assignTag(final String tagNameSelected) {
        assignedTagNames.add(tagNameSelected);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.tagdetails.TargetTagToken#unassignTag(java.
     * lang.String)
     */
    @Override
    protected void unassignTag(final String tagName) {
        assignedTagNames.remove(tagName);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.tagdetails.AbstractTagToken#getTagStyleName
     * ()
     */
    @Override
    protected String getTagStyleName() {
        return "target-tag-";
    }

    @Override
    protected String getTokenInputPrompt() {
        return i18n.get("combo.type.tag.name");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.tagdetails.AbstractTagToken#
     * hasUpdatePermission()
     */
    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasCreateTargetPermission();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTableEvent targetTableEvent) {
        if (targetTableEvent.getTargetComponentEvent() == TargetComponentEvent.SELECTED_TARGET
                && targetTableEvent.getTarget() != null) {
            ui.access(() ->
            /**
             * targetTableEvent.getTarget() is null when table has no data.
             */
            repopulateToken());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTagEvent event) {
        ui.access(() -> {
            if (event.getTargetTagComponentEvent() == TargetTagEvent.TargetTagComponentEvent.ADD_TARGETTAG) {
                setContainerPropertValues(event.getTargetTag().getId(), event.getTargetTag().getName(), event
                        .getTargetTag().getColour());
            } else if (event.getTargetTagComponentEvent() == TargetTagEvent.TargetTagComponentEvent.DELETE_TARGETTAG) {
                final Long deletedTagId = getTagIdByTagName(event.getTargetTagName());
                removeTagFromCombo(deletedTagId);
            } else if (event.getTargetTagComponentEvent() == TargetTagEvent.TargetTagComponentEvent.ASSIGNED) {
                final Long newlyAssignedTagId = getTagIdByTagName(event.getTargetTagName());
                addNewToken(newlyAssignedTagId);
            } else if (event.getTargetTagComponentEvent() == TargetTagEvent.TargetTagComponentEvent.UNASSIGNED) {
                final Long newlyUnAssignedTagId = getTagIdByTagName(event.getTargetTagName());
                removeTokenItem(newlyUnAssignedTagId, event.getTargetTagName());
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.tagdetails.AbstractTagToken#
     * displayAlreadyAssignedTags()
     */
    @Override
    protected void displayAlreadyAssignedTags() {
        container.removeAllItems();
        for (final TargetTag tag : tagManagement.findAllTargetTags()) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.tagdetails.AbstractTagToken#
     * populateContainer()
     */
    @Override
    protected void populateContainer() {
        container.removeAllItems();
        for (final TargetTag tag : tagManagement.findAllTargetTags()) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }

    }

    /**
     * @return the assignedTagNames
     */
    public List<String> getAssignedTagNames() {
        return assignedTagNames;
    }
}
