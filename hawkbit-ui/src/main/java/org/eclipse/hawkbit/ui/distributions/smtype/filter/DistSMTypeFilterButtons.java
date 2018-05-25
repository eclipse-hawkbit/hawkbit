/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtype.filter;

import static org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum.ADD_SOFTWARE_MODULE_TYPE;
import static org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_NAME;
import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.SW_MODULE_TYPE_TABLE_ID;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.artifacts.smtype.UpdateSoftwareModuleTypeLayout;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.common.event.SoftwareModuleTypeFilterHeaderEvent;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Button.ClickEvent;

/**
 * Software Module Type filter buttons.
 */
public class DistSMTypeFilterButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 1L;

    private final ManageDistUIState manageDistUIState;

    private final DistributionsViewClientCriterion distributionsViewClientCriterion;

    private final transient EntityFactory entityFactory;

    private final SpPermissionChecker permChecker;

    private final UINotification uiNotification;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    /**
     * Constructor
     * 
     * @param eventBus
     *            UIEventBus
     * @param manageDistUIState
     *            ManageDistUIState
     * @param distributionsViewClientCriterion
     *            DistributionsViewClientCriterion
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
    public DistSMTypeFilterButtons(final UIEventBus eventBus, final ManageDistUIState manageDistUIState,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement, final VaadinMessageSource i18n,
            final EntityFactory entityFactory, final SpPermissionChecker permChecker,
            final UINotification uiNotification) {
        super(eventBus, new DistSMTypeFilterButtonClick(eventBus, manageDistUIState, softwareModuleTypeManagement),
                i18n);
        this.manageDistUIState = manageDistUIState;
        this.distributionsViewClientCriterion = distributionsViewClientCriterion;
        this.entityFactory = entityFactory;
        this.permChecker = permChecker;
        this.uiNotification = uiNotification;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    protected String getButtonsTableId() {
        return SW_MODULE_TYPE_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        final BeanQueryFactory<SoftwareModuleTypeBeanQuery> typeQF = new BeanQueryFactory<>(
                SoftwareModuleTypeBeanQuery.class);
        typeQF.setQueryConfiguration(Collections.emptyMap());
        return new LazyQueryContainer(new LazyQueryDefinition(true, 20, VAR_NAME), typeQF);
    }

    @Override
    protected String getButtonWrapperData() {
        return null;
    }

    @Override
    protected boolean isClickedByDefault(final String typeName) {
        return manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType()
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
                return distributionsViewClientCriterion;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                /* Not required */
            }
        };
    }

    @Override
    protected String getButttonWrapperIdPrefix() {
        return SPUIDefinitions.SOFTWARE_MODULE_TAG_ID_PREFIXS;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleTypeEvent event) {
        if (isCreateOrUpdate(event) && event.getSoftwareModuleType() != null) {
            refreshTable();
        }
        if (isUpdate(event)) {
            getEventBus().publish(this, new SoftwareModuleTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR));
        }
    }

    private static boolean isUpdate(final SoftwareModuleTypeEvent event) {
        return event.getSoftwareModuleTypeEnum() == SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE;
    }

    private static boolean isCreateOrUpdate(final SoftwareModuleTypeEvent event) {
        return event.getSoftwareModuleTypeEnum() == ADD_SOFTWARE_MODULE_TYPE
                || event.getSoftwareModuleTypeEnum() == UPDATE_SOFTWARE_MODULE_TYPE;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.SAVED_DELETE_SW_MODULE_TYPES) {
            refreshTable();
        }
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
            if (manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType().equals(swmTypeToDelete)) {
                uiNotification.displayValidationError(getI18n().getMessage("message.tag.delete", entityToDelete));
                removeUpdateAndDeleteColumn();
            } else {
                softwareModuleTypeManagement.delete(swmTypeToDelete.get().getId());
                getEventBus().publish(this, SaveActionWindowEvent.SAVED_DELETE_SW_MODULE_TYPES);
                uiNotification.displaySuccess(getI18n().getMessage("message.delete.success", entityToDelete));
            }
        });
    }
}
