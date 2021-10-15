/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import java.util.Collection;
import java.util.stream.Collectors;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.exception.TargetTypeInUseException;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetTypeToProxyTargetTypeMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractTargetTypeFilterButtons;
import org.eclipse.hawkbit.ui.common.state.TagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.management.targettag.targettype.TargetTypeWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Target Type filter buttons table.
 */
public class TargetTypeFilterButtons extends AbstractTargetTypeFilterButtons {
    private static final long serialVersionUID = 1L;

    private final transient TargetTypeManagement targetTypeManagement;
    private final transient TargetTypeWindowBuilder targetTypeWindowBuilder;

    private static final Logger LOG = LoggerFactory.getLogger(TargetTypeFilterButtons.class);

    TargetTypeFilterButtons(final CommonUiDependencies uiDependencies,
                            final TargetTypeManagement targetTypeManagement, final TagFilterLayoutUiState tagFilterLayoutUiState,
                            final TargetTypeWindowBuilder targetTypeWindowBuilder) {
        super(uiDependencies, tagFilterLayoutUiState);

        this.targetTypeManagement = targetTypeManagement;
        this.targetTypeWindowBuilder = targetTypeWindowBuilder;

        init();
        setDataProvider(
                new TargetTypeDataProvider<>(targetTypeManagement, new TargetTypeToProxyTargetTypeMapper<>()));
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.TARGET_TYPE_TABLE_ID;
    }

    @Override
    protected String getMessageKeyEntityTypeSing() {
        return "caption.entity.target.type";
    }

    @Override
    protected String getMessageKeyEntityTypePlur() {
        return "caption.entity.target.types";
    }

    @Override
    protected boolean deleteFilterButtons(Collection<ProxyTargetType> filterButtonsToDelete) {
        final ProxyTargetType targetTypeToDelete = filterButtonsToDelete.iterator().next();
        return deleteTargetType(targetTypeToDelete);
    }

    @Override
    protected String getFilterButtonIdPrefix() {
        return UIComponentIdProvider.TARGET_TYPE_ID_PREFIXS;
    }

    @Override
    protected void editButtonClickListener(ProxyTargetType clickedFilter) {
        final Window updateWindow = targetTypeWindowBuilder.getWindowForUpdate(clickedFilter);

        updateWindow.setCaption(i18n.getMessage("caption.update", i18n.getMessage("caption.type")));
        UI.getCurrent().addWindow(updateWindow);
        updateWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getFilterMasterEntityType() {
        return ProxyTarget.class;
    }

    @Override
    protected EventView getView() {
        return EventView.DEPLOYMENT;
    }

    @Override
    protected boolean deleteTargetType(ProxyTargetType targetTypeToDelete) {
        try{
            targetTypeManagement.delete(targetTypeToDelete.getId());
            eventBus.publish(EventTopics.ENTITY_MODIFIED, this,
                    new EntityModifiedEventPayload(EntityModifiedEventPayload.EntityModifiedEventType.ENTITY_REMOVED, getFilterMasterEntityType(),
                            ProxyTargetType.class, targetTypeToDelete.getId()));
            return true;
        } catch (TargetTypeInUseException exception){
            LOG.trace("Target type already in use exception: {}", exception.getMessage());
            uiNotification.displayValidationError(i18n.getMessage(exception.getMessage()));
        }
        return false;
    }

    @Override
    protected Collection<Long> filterExistingTargetTypeIds(Collection<Long> targetTypeIds) {
        return targetTypeManagement.get(targetTypeIds).stream().map(Identifiable::getId).collect(Collectors.toSet());
    }

    @Override
    protected boolean isDeletionAllowed() {
        return permissionChecker.hasDeleteTargetPermission();
    }

    @Override
    protected boolean isEditAllowed() {
        return permissionChecker.hasUpdateRepositoryPermission();
    }

}
