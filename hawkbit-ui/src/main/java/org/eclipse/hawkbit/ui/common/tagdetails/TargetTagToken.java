/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagAssigmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTagEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.UI;

/**
 * Implementation of Target tag token.
 *
 *
 */
@SpringComponent
@ViewScope
public class TargetTagToken extends AbstractTagToken {

    private static final long serialVersionUID = 7124887018280196721L;

    // To Be Done : have to set this value based on view???
    private static final Boolean NOTAGS_SELECTED = Boolean.FALSE;
    @Autowired
    private SpPermissionChecker spChecker;

    @Autowired
    private I18N i18n;

    @Autowired
    private UINotification uinotification;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient TagManagement tagManagement;

    @Autowired
    private transient TargetManagement targetManagement;

    private Target selectedTarget;

    private UI ui;

    @PostConstruct
    protected void init() {
        super.init();
        ui = UI.getCurrent();
        eventBus.subscribe(this);
    }

    @Override
    protected String getTagStyleName() {
        return "target-tag-";
    }

    @Override
    protected String getTokenInputPrompt() {
        return i18n.get("combo.type.tag.name");
    }

    @Override
    protected void assignTag(final String tagNameSelected) {
        if (tagNameSelected != null) {
            final TargetTagAssigmentResult result = toggleAssignment(tagNameSelected);
            if (result.getAssigned() >= 1 && NOTAGS_SELECTED) {
                eventBus.publish(this, ManagementUIEvent.ASSIGN_TARGET_TAG);
            }
        } else {
            uinotification.displayValidationError(i18n.get("message.error.missing.tagname"));
        }
    }

    private TargetTagAssigmentResult toggleAssignment(final String tagNameSelected) {
        final Set<String> targetList = new HashSet<String>();
        targetList.add(selectedTarget.getControllerId());
        final TargetTagAssigmentResult result = targetManagement.toggleTagAssignment(targetList, tagNameSelected);
        uinotification.displaySuccess(HawkbitCommonUtil.getTargetTagAssigmentMsg(tagNameSelected, result, i18n));
        return result;
    }

    @Override
    protected void unassignTag(final String tagName) {
        final TargetTagAssigmentResult result = toggleAssignment(tagName);
        if (result.getUnassigned() >= 1 && (isClickedTagListEmpty() || getClickedTagList().contains(tagName))) {
            eventBus.publish(this, ManagementUIEvent.UNASSIGN_TARGET_TAG);
        }
    }

    private Boolean isClickedTagListEmpty() {
        if (getClickedTagList() == null || getClickedTagList() != null && !getClickedTagList().isEmpty()) {
            return true;
        }
        return false;
    }

    /* To Be Done : this implementation will vary in views */
    private List<String> getClickedTagList() {
        return new ArrayList<String>();
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return spChecker.hasUpdateTargetPermission();
    }

    @Override
    protected void displayAlreadyAssignedTags() {
        removePreviouslyAddedTokens();
        if (selectedTarget != null) {
            for (final TargetTag tag : selectedTarget.getTags()) {
                addNewToken(tag.getId());
            }
        }
    }

    @Override
    protected void populateContainer() {
        container.removeAllItems();
        for (final TargetTag tag : tagManagement.findAllTargetTags()) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTableEvent targetTableEvent) {
        if (targetTableEvent.getTargetComponentEvent() == TargetComponentEvent.SELECTED_TARGET
                && targetTableEvent.getTarget() != null) {
            ui.access(() -> {
                /**
                 * targetTableEvent.getTarget() is null when table has no data.
                 */
                selectedTarget = targetTableEvent.getTarget();
                repopulateToken();
            });
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTagEvent event) {
        ui.access(() -> {
            if (event.getTargetTagComponentEvent() == TargetTagEvent.TargetTagComponentEvent.ADD_TARGETTAG) {
                setContainerPropertValues(event.getTargetTag().getId(), event.getTargetTag().getName(),
                        event.getTargetTag().getColour());
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

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

}
