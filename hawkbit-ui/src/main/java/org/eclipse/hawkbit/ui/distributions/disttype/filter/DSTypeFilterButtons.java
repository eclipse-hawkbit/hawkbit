/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype.filter;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.event.DistributionSetTypeFilterHeaderEvent;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.disttype.UpdateDistributionSetTypeLayout;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
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
 * Distribution Set Type filter buttons.
 */
public class DSTypeFilterButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 1L;

    private final ManageDistUIState manageDistUIState;

    private final DistributionsViewClientCriterion distributionsViewClientCriterion;

    private final transient EntityFactory entityFactory;

    private final SpPermissionChecker permChecker;

    private final UINotification uiNotification;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final transient DistributionSetManagement distributionSetManagement;

    private final transient DistributionSetTypeManagement distributionSetTypeManagement;

    private final transient SystemManagement systemManagement;

    /**
     * Constructor
     * 
     * @param eventBus
     *            UIEventBus
     * @param manageDistUIState
     *            ManageDistUIState
     * @param distributionsViewClientCriterion
     *            DistributionsViewClientCriterion
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param systemManagement
     *            SystemManagement
     */
    public DSTypeFilterButtons(final UIEventBus eventBus, final ManageDistUIState manageDistUIState,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final DistributionSetTypeManagement distributionSetTypeManagement, final VaadinMessageSource i18n,
            final EntityFactory entityFactory, final SpPermissionChecker permChecker,
            final UINotification uiNotification, final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetManagement distributionSetManagement, final SystemManagement systemManagement) {
        super(eventBus, new DSTypeFilterButtonClick(eventBus, manageDistUIState, distributionSetTypeManagement), i18n);
        this.manageDistUIState = manageDistUIState;
        this.distributionsViewClientCriterion = distributionsViewClientCriterion;
        this.entityFactory = entityFactory;
        this.permChecker = permChecker;
        this.uiNotification = uiNotification;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.systemManagement = systemManagement;
    }

    @Override
    protected String getButtonsTableId() {
        return UIComponentIdProvider.DISTRIBUTION_SET_TYPE_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        return HawkbitCommonUtil.createLazyQueryContainer(new BeanQueryFactory<>(DistributionSetTypeBeanQuery.class));
    }

    @Override
    protected boolean isClickedByDefault(final String typeName) {
        return manageDistUIState.getManageDistFilters().getClickedDistSetType() != null
                && manageDistUIState.getManageDistFilters().getClickedDistSetType().getName().equals(typeName);
    }

    @Override
    protected String createButtonId(final String name) {
        return UIComponentIdProvider.DS_TYPE_FILTER_BTN_ID + name;
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
    protected String getButtonWrapperData() {
        return null;
    }

    @Override
    protected String getButttonWrapperIdPrefix() {
        return SPUIDefinitions.DISTRIBUTION_SET_TYPE_ID_PREFIXS;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionSetTypeEvent event) {
        if (event.getDistributionSetTypeEnum() == DistributionSetTypeEvent.DistributionSetTypeEnum.ADD_DIST_SET_TYPE
                || event.getDistributionSetTypeEnum() == DistributionSetTypeEvent.DistributionSetTypeEnum.UPDATE_DIST_SET_TYPE) {
            refreshTable();
        }
        if (event
                .getDistributionSetTypeEnum() == DistributionSetTypeEvent.DistributionSetTypeEnum.UPDATE_DIST_SET_TYPE) {
            getEventBus().publish(this, new DistributionSetTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR));
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.SAVED_DELETE_DIST_SET_TYPES) {
            refreshTable();
        }
    }

    @Override
    protected void addEditButtonClickListener(final ClickEvent event) {
        new UpdateDistributionSetTypeLayout(getI18n(), entityFactory, getEventBus(), permChecker, uiNotification,
                softwareModuleTypeManagement, distributionSetTypeManagement, distributionSetManagement,
                getEntityId(event), getCloseListenerForEditAndDeleteTag(
                        new DistributionSetTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR)));
    }

    @Override
    protected void addDeleteButtonClickListener(final ClickEvent event) {
        openConfirmationWindowForDeletion(getEntityId(event), getI18n().getMessage("caption.entity.distribution.type"),
                new DistributionSetTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR));
    }

    @Override
    protected void deleteEntity(final String entityToDelete) {
        final Optional<DistributionSetType> distTypeToDelete = distributionSetTypeManagement.getByName(entityToDelete);
        distTypeToDelete.ifPresent(tag -> {
            if (tag.equals(manageDistUIState.getManageDistFilters().getClickedDistSetType())) {
                uiNotification.displayValidationError(getI18n().getMessage("message.tag.delete", entityToDelete));
                removeUpdateAndDeleteColumn();
            } else if (isDefaultDsType(entityToDelete)) {
                uiNotification.displayValidationError(getI18n().getMessage("message.cannot.delete.default.dstype"));
                removeUpdateAndDeleteColumn();
            } else {
                distributionSetTypeManagement.delete(distTypeToDelete.get().getId());
                getEventBus().publish(this, SaveActionWindowEvent.SAVED_DELETE_DIST_SET_TYPES);
                uiNotification.displaySuccess(getI18n().getMessage("message.delete.success", entityToDelete));
            }
        });
    }

    private boolean isDefaultDsType(final String dsTypeName) {
        return getCurrentDistributionSetType() != null && getCurrentDistributionSetType().getName().equals(dsTypeName);
    }

    private DistributionSetType getCurrentDistributionSetType() {
        return systemManagement.getTenantMetadata().getDefaultDsType();
    }
}
