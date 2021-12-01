/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import com.vaadin.ui.Button;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtonClickBehaviour.ClickBehaviourType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUITagButtonStyle;
import org.eclipse.hawkbit.ui.management.targettag.filter.TargetTagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;

import java.util.Collection;

/**
 * Class for defining the target type filter buttons.
 */
public abstract class AbstractTargetTypeFilterButtons extends AbstractFilterButtons<ProxyTargetType, Void> {
    private static final long serialVersionUID = 1L;

    private final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState;
    protected final UINotification uiNotification;
    private final TargetTypeFilterButtonClick targetTypeFilterButtonClick;
    private final transient TargetTypeManagement targetTypeManagement;
    private final Button noTargetTypeButton;
    private boolean preNoTargetTypeBtnState;

    /**
     * Constructor for AbstractTargetTypeFilterButtons
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTagFilterLayoutUiState
     *          {@link TargetTagFilterLayoutUiState}
     * @param targetTypeManagement
     *          TargetTypeManagement
     */
    protected AbstractTargetTypeFilterButtons(final CommonUiDependencies uiDependencies,
                                              final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState,
                                              final TargetTypeManagement targetTypeManagement) {
        super(uiDependencies.getEventBus(), uiDependencies.getI18n(), uiDependencies.getUiNotification(),
                uiDependencies.getPermChecker());

        this.preNoTargetTypeBtnState = false;
        this.uiNotification = uiDependencies.getUiNotification();
        this.targetTagFilterLayoutUiState = targetTagFilterLayoutUiState;
        this.targetTypeManagement = targetTypeManagement;
        this.noTargetTypeButton = buildNoTargetTypeButton();
        this.targetTypeFilterButtonClick = new TargetTypeFilterButtonClick(this::onFilterChangedEvent);
    }

    @Override
    protected TargetTypeFilterButtonClick getFilterButtonClickBehaviour() {
        return targetTypeFilterButtonClick;
    }

    private Button buildNoTargetTypeButton() {
        final Button noTargetType = SPUIComponentProvider.getButton(
                getFilterButtonIdPrefix() + "." + SPUIDefinitions.NO_TARGET_TYPE_BUTTON_ID,
                i18n.getMessage(UIMessageIdProvider.LABEL_NO_TARGET_TYPE),
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_CLICK_TO_FILTER), "button-no-tag", false, null,
                SPUITagButtonStyle.class);

        final ProxyTargetType proxyTargetType = new ProxyTargetType();
        proxyTargetType.setNoTargetType(true);

        noTargetType.addClickListener(event -> getFilterButtonClickBehaviour().processFilterClick(proxyTargetType));

        noTargetType.addStyleName("filter-drop-hint-layout");
        return noTargetType;
    }

    /**
     * @return the noTargetType Button component
     */
    public Button getNoTargetTypeButton() {
        return noTargetTypeButton;
    }

    private void onFilterChangedEvent(ProxyTargetType proxyTargetType, ClickBehaviourType clickType) {
        getDataCommunicator().reset();

        final boolean isNoTargetTypeActive = proxyTargetType.isNoTargetType() && clickType == ClickBehaviourType.CLICKED;

        if (isNoTargetTypeActive) {
            getNoTargetTypeButton().addStyleName(SPUIStyleDefinitions.SP_NO_TAG_BTN_CLICKED_STYLE);
        } else {
            getNoTargetTypeButton().removeStyleName(SPUIStyleDefinitions.SP_NO_TAG_BTN_CLICKED_STYLE);
        }

        if (preNoTargetTypeBtnState != isNoTargetTypeActive){
            publishNoTargetTypeChangedEvent(isNoTargetTypeActive);
        }

        final Long targetTypeId = ClickBehaviourType.CLICKED == clickType ? proxyTargetType.getId()
                : null;
        publishFilterChangedEvent(targetTypeId);
        preNoTargetTypeBtnState = isNoTargetTypeActive;
    }

    private void publishNoTargetTypeChangedEvent(final boolean isNoTargetTypeActivated) {
        eventBus.publish(EventTopics.FILTER_CHANGED, this, new FilterChangedEventPayload<>(ProxyTarget.class,
                FilterType.NO_TARGET_TYPE, isNoTargetTypeActivated, EventView.DEPLOYMENT));
        targetTagFilterLayoutUiState.setNoTargetTypeClicked(isNoTargetTypeActivated);
    }

    private void publishFilterChangedEvent(final Long targetTypeId) {
        eventBus.publish(EventTopics.FILTER_CHANGED, this, new FilterChangedEventPayload<>(ProxyTarget.class,
                FilterType.TARGET_TYPE, targetTypeId, EventView.DEPLOYMENT));

        targetTagFilterLayoutUiState.setClickedTargetTypeFilterId(targetTypeId);
    }

    /**
     * Provides type of the master entity.
     * 
     * @return type of the master entity
     */
    protected abstract Class<? extends ProxyIdentifiableEntity> getFilterMasterEntityType();

    /**
     * Provides event view filter.
     * 
     * @return event view filter.
     */
    protected abstract EventView getView();


    /**
     * Type deletion operation.
     *
     * @param typeToDelete
     *            target type to delete
     * @return true if delete target type has no exception
     */
    protected abstract boolean deleteTargetType(final ProxyTargetType typeToDelete);

    @Override
    public void restoreState() {
        final Long targetFilterTypeIdToRestore = targetTagFilterLayoutUiState.getClickedTargetTypeFilterId();

        if (targetFilterTypeIdToRestore != null) {
            if (targetTypeExists(targetFilterTypeIdToRestore)) {
                targetTypeFilterButtonClick
                        .setPreviouslyClickedFilterId(targetTagFilterLayoutUiState.getClickedTargetTypeFilterId());
            } else {
                targetTagFilterLayoutUiState.setClickedTargetTypeFilterId(null);
            }
        }

        if (targetTagFilterLayoutUiState.isNoTargetTypeClicked()) {
            getNoTargetTypeButton().addStyleName(SPUIStyleDefinitions.SP_NO_TAG_BTN_CLICKED_STYLE);
        }
    }

    /**
     * Reset filter on target type updated
     *
     * @param updatedTargetTypeIds
     *          Collections of updated target type Ids
     */
    public void resetFilterOnTargetTypeUpdated(Collection<Long> updatedTargetTypeIds) {
        if (isClickedTargetTypeInIds(updatedTargetTypeIds)) {
            publishFilterChangedEvent(targetTypeFilterButtonClick.getPreviouslyClickedFilterId());
        }
    }

    /**
     * Reset filter on target type deleted
     *
     * @param deletedTargetTargetTypeIds
     *          Collections of updated target type Ids
     */
    public void resetFilterOnTargetTypeDeleted(final Collection<Long> deletedTargetTargetTypeIds) {
        if (isClickedTargetTypeInIds(deletedTargetTargetTypeIds)) {
            targetTypeFilterButtonClick.setPreviouslyClickedFilterId(null);
            publishFilterChangedEvent(null);
        }
    }

    private boolean isClickedTargetTypeInIds(final Collection<Long> targetTypeIds) {
        final Long clickedTargetTypeId = targetTypeFilterButtonClick.getPreviouslyClickedFilterId();
        return clickedTargetTypeId != null && targetTypeIds.contains(clickedTargetTypeId);
    }

    /**
     * Reevaluate filter
     */
    public void reevaluateFilter() {
        final Long clickedTargetTypeId = targetTypeFilterButtonClick.getPreviouslyClickedFilterId();

        if (clickedTargetTypeId != null && !targetTypeExists(clickedTargetTypeId)) {
            targetTypeFilterButtonClick.setPreviouslyClickedFilterId(null);
            publishFilterChangedEvent(null);
        }
    }

    private boolean targetTypeExists(Long targetTypeId) {
        return targetTypeManagement.get(targetTypeId).isPresent();
    }

    /**
     * Remove applied target type filter
     */
    public void clearAppliedTargetTypeFilter() {
        if (targetTagFilterLayoutUiState.isNoTargetTypeClicked()) {
            targetTagFilterLayoutUiState.setNoTargetTypeClicked(false);
            getNoTargetTypeButton().removeStyleName(SPUIStyleDefinitions.SP_NO_TAG_BTN_CLICKED_STYLE);
        }

        if (targetTypeFilterButtonClick.getPreviouslyClickedFilterId() != null) {
            targetTypeFilterButtonClick.setPreviouslyClickedFilterId(null);
            targetTagFilterLayoutUiState.setClickedTargetTypeFilterId(null);
        }
    }
}
