/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype.filter;

import java.util.EnumSet;
import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.smtype.UpdateSoftwareModuleTypeLayout;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.common.event.SoftwareModuleTypeFilterHeaderEvent;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.dd.criteria.UploadViewClientCriterion;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Button.ClickEvent;

/**
 * Software module type filter buttons.
 *
 */
public class SMTypeFilterButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 1L;

    private final ArtifactUploadState artifactUploadState;

    private final UploadViewClientCriterion uploadViewClientCriterion;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final transient EntityFactory entityFactory;

    private final SpPermissionChecker permChecker;

    private final UINotification uiNotification;

    /**
     * Constructor
     * 
     * @param eventBus
     *            UIEventBus
     * @param artifactUploadState
     *            ArtifactUploadState
     * @param uploadViewClientCriterion
     *            UploadViewClientCriterion
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     */
    public SMTypeFilterButtons(final UIEventBus eventBus, final ArtifactUploadState artifactUploadState,
            final UploadViewClientCriterion uploadViewClientCriterion,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement, final VaadinMessageSource i18n,
            final EntityFactory entityFactory, final SpPermissionChecker permChecker,
            final UINotification uiNotification) {
        super(eventBus, new SMTypeFilterButtonClick(eventBus, artifactUploadState, softwareModuleTypeManagement), i18n);
        this.artifactUploadState = artifactUploadState;
        this.uploadViewClientCriterion = uploadViewClientCriterion;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.entityFactory = entityFactory;
        this.permChecker = permChecker;
        this.uiNotification = uiNotification;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleTypeEvent event) {
        if (event.getSoftwareModuleType() != null
                && EnumSet.allOf(SoftwareModuleTypeEnum.class).contains(event.getSoftwareModuleTypeEnum())) {
            refreshTable();
        }

        if (isUpdate(event)) {
            getEventBus().publish(this, new SoftwareModuleTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR));
        }
    }

    private static boolean isUpdate(final SoftwareModuleTypeEvent event) {
        return event.getSoftwareModuleTypeEnum() == SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.DELETED_ALL_SOFTWARE_TYPE) {
            refreshTable();
        }
    }

    @Override
    protected String getButtonsTableId() {
        return UIComponentIdProvider.SW_MODULE_TYPE_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        return HawkbitCommonUtil.createLazyQueryContainer(new BeanQueryFactory<>(SoftwareModuleTypeBeanQuery.class));
    }

    @Override
    protected boolean isClickedByDefault(final String typeName) {
        return artifactUploadState.getSoftwareModuleFilters().getSoftwareModuleType()
                .map(type -> type.getName().equals(typeName)).orElse(false);
    }

    @Override
    protected String createButtonId(final String name) {
        return UIComponentIdProvider.SM_TYPE_FILTER_BTN_ID + name;
    }

    @Override
    protected DropHandler getFilterButtonDropHandler() {
        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return uploadViewClientCriterion;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                /* Not required */
            }
        };
    }

    @Override
    protected String getButttonWrapperIdPrefix() {
        return UIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX;
    }

    @Override
    protected String getButtonWrapperData() {
        return null;
    }

    @Override
    protected void addEditButtonClickListener(final ClickEvent event) {
        new UpdateSoftwareModuleTypeLayout(getI18n(), entityFactory, getEventBus(), permChecker, uiNotification,
                softwareModuleTypeManagement, getEntityId(event), getCloseListenerForEditAndDeleteTag(
                        new SoftwareModuleTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR)));
    }

    @Override
    protected void addDeleteButtonClickListener(final ClickEvent event) {
        openConfirmationWindowForDeletion(getEntityId(event),
                getI18n().getMessage("caption.entity.software.module.type"),
                new SoftwareModuleTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR));
    }

    @Override
    protected void deleteEntity(final String entityToDelete) {
        final Optional<SoftwareModuleType> swmTypeToDelete = softwareModuleTypeManagement.getByName(entityToDelete);
        swmTypeToDelete.ifPresent(tag -> {
            if (artifactUploadState.getSoftwareModuleFilters().getSoftwareModuleType().equals(swmTypeToDelete)) {
                uiNotification.displayValidationError(getI18n().getMessage("message.tag.delete", entityToDelete));
                removeUpdateAndDeleteColumn();
            } else {
                softwareModuleTypeManagement.delete(swmTypeToDelete.get().getId());
                getEventBus().publish(this, UploadArtifactUIEvent.DELETED_ALL_SOFTWARE_TYPE);
                uiNotification.displaySuccess(getI18n().getMessage("message.delete.success", entityToDelete));
            }
        });
    }

}
