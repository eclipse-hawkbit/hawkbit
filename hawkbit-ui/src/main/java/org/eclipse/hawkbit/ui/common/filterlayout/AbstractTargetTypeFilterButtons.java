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
import java.util.Collection;
import java.util.Map;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtonClickBehaviour.ClickBehaviourType;
import org.eclipse.hawkbit.ui.common.state.TagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUITagButtonStyle;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.util.CollectionUtils;

/**
 * Class for defining the type filter buttons.
 */
public abstract class AbstractTargetTypeFilterButtons extends AbstractFilterButtons<ProxyTargetType, Void> {
    private static final long serialVersionUID = 1L;

    private final TagFilterLayoutUiState tagFilterLayoutUiState;

    protected final UINotification uiNotification;
    private final Button noTargetTypeButton;

    private final TargetTypeFilterButtonClick targetTypeFilterButtonClick;

    /**
     * Constructor for AbstractTargetTypeFilterButtons
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param tagFilterLayoutUiState
     *            TagFilterLayoutUiState
     */
    protected AbstractTargetTypeFilterButtons(final CommonUiDependencies uiDependencies,
                                              final TagFilterLayoutUiState tagFilterLayoutUiState) {
        super(uiDependencies.getEventBus(), uiDependencies.getI18n(), uiDependencies.getUiNotification(),
                uiDependencies.getPermChecker());

        this.uiNotification = uiDependencies.getUiNotification();
        this.tagFilterLayoutUiState = tagFilterLayoutUiState;
        this.noTargetTypeButton = buildNoTargetTypeButton();
        this.targetTypeFilterButtonClick = new TargetTypeFilterButtonClick(this::onFilterChangedEvent);
    }

    private Button buildNoTargetTypeButton() {
        final Button noTargetType = SPUIComponentProvider.getButton(
                getFilterButtonIdPrefix() + "." + SPUIDefinitions.NO_TARGET_TYPE_BUTTON_ID,
                i18n.getMessage(UIMessageIdProvider.LABEL_NO_TARGET_TYPE),
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_CLICK_TO_FILTER), "button-no-tag", false, null,
                SPUITagButtonStyle.class);

        final ProxyTargetType proxyTargetType = new ProxyTargetType();
        proxyTargetType.setNoTargetType(true);

        return noTargetType;
    }

    @Override
    protected TargetTypeFilterButtonClick getFilterButtonClickBehaviour(){
        return targetTypeFilterButtonClick;
    }

    private void onFilterChangedEvent(final ProxyTargetType targetType,
                                      final ClickBehaviourType clickType) {
        getDataCommunicator().reset();
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
     * Target type deletion operation.
     * 
     * @param targetTypeToDelete
     *            target type to delete
     *
     * @return true if target type is deleted, in error case false.
     */
    protected abstract boolean deleteTargetType(final ProxyTargetType targetTypeToDelete);

    /**
     * @return Button component of no target type
     */
    public Button getNoTargetTypeButton() {
        return noTargetTypeButton;
    }

    @Override
    public void restoreState() {
        final Map<Long, String> targetTypesToRestore = tagFilterLayoutUiState.getClickedTagIdsWithName();

        if (!CollectionUtils.isEmpty(targetTypesToRestore)) {
            removeNonExistingTargetTypes(targetTypesToRestore);
        }

        if (tagFilterLayoutUiState.isNoTagClicked()) {
            getNoTargetTypeButton().addStyleName(SPUIStyleDefinitions.SP_NO_TAG_BTN_CLICKED_STYLE);
        }
    }

    private void removeNonExistingTargetTypes(final Map<Long, String> targetTypeIdsWithName) {
        final Collection<Long> targetTypeIds = targetTypeIdsWithName.keySet();
        final Collection<Long> existingTargetTypeIds = filterExistingTargetTypeIds(targetTypeIds);
        if (targetTypeIds.size() != existingTargetTypeIds.size()) {
            targetTypeIds.retainAll(existingTargetTypeIds);
        }

    }

    /**
     * Filters out non-existant target type by ids.
     *
     * @param targetTypeIds
     *            provided target type ids
     * @return filtered list of existing target type ids
     */
    protected abstract Collection<Long> filterExistingTargetTypeIds(final Collection<Long> targetTypeIds);

}
