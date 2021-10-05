/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModuleDetails;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Software module details table.
 *
 */
public class SoftwareModuleDetailsGrid extends Grid<ProxySoftwareModuleDetails>
        implements MasterEntityAwareComponent<ProxyDistributionSet> {
    private static final long serialVersionUID = 1L;

    private static final String SOFT_TYPE_NAME_ID = "typeName";
    private static final String SOFT_MODULES_ID = "softwareModules";
    private static final String SOFT_TYPE_MANDATORY_ID = "mandatory";

    private final VaadinMessageSource i18n;
    private final transient UIEventBus eventBus;
    private final UINotification uiNotification;
    private final SpPermissionChecker permissionChecker;

    private final transient DistributionSetManagement distributionSetManagement;
    private final transient SoftwareModuleManagement smManagement;
    private final transient DistributionSetTypeManagement dsTypeManagement;

    private ProxyDistributionSet masterEntity;
    private final Map<Long, Boolean> typeIdIsRendered;

    private boolean isUnassignSmAllowed;

    /**
     * Initialize software module table- to be displayed in details layout.
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param smManagement
     *            SoftwareModuleManagement
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     */
    public SoftwareModuleDetailsGrid(final CommonUiDependencies uiDependencies,
            final DistributionSetManagement distributionSetManagement, final SoftwareModuleManagement smManagement,
            final DistributionSetTypeManagement dsTypeManagement) {
        this.i18n = uiDependencies.getI18n();
        this.uiNotification = uiDependencies.getUiNotification();
        this.eventBus = uiDependencies.getEventBus();
        this.permissionChecker = uiDependencies.getPermChecker();

        this.distributionSetManagement = distributionSetManagement;
        this.smManagement = smManagement;
        this.dsTypeManagement = dsTypeManagement;

        this.typeIdIsRendered = new HashMap<>();

        init();
        setVisible(false);
    }

    // columns are being re-rendered on each tab change, so we need to reset the
    // state here
    @Override
    public void beforeClientResponse(final boolean initial) {
        super.beforeClientResponse(initial);

        typeIdIsRendered.clear();
    }

    private void init() {
        setId(UIComponentIdProvider.DS_DETAILS_MODULES_ID);
        setSizeFull();
        setHeightMode(HeightMode.UNDEFINED);

        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);

        setSelectionMode(SelectionMode.NONE);

        addColumns();
        setColumnReorderingAllowed(false);
        disableColumnSorting();
    }

    private void addColumns() {
        GridComponentBuilder.addIconColumn(this, this::buildIsMandatoryLabel, SOFT_TYPE_MANDATORY_ID, null);

        GridComponentBuilder.addColumn(this, this::buildTypeName).setId(SOFT_TYPE_NAME_ID)
                .setCaption(i18n.getMessage("header.caption.typename"));

        GridComponentBuilder.addComponentColumn(this, this::buildSoftwareModulesLayout).setId(SOFT_MODULES_ID)
                .setCaption(i18n.getMessage("header.caption.softwaremodule"));
    }

    private Label buildIsMandatoryLabel(final ProxySoftwareModuleDetails softwareModuleDetails) {
        final Label isMandatoryLabel = new Label("");

        isMandatoryLabel.setSizeFull();
        isMandatoryLabel.addStyleName(SPUIDefinitions.TEXT_STYLE);
        isMandatoryLabel.addStyleName("text-cut");

        if (softwareModuleDetails.isMandatory() && !isTypeAlreadyAdded(softwareModuleDetails.getTypeId())) {
            isMandatoryLabel.setValue("*");
            isMandatoryLabel.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        }

        return isMandatoryLabel;
    }

    // workaround for vaadin 8 grid dynamic row height bug:
    // https://github.com/vaadin/framework/issues/9355
    private boolean isTypeAlreadyAdded(final Long typeId) {
        return typeIdIsRendered.getOrDefault(typeId, false);
    }

    private String buildTypeName(final ProxySoftwareModuleDetails softwareModuleDetails) {
        if (isTypeAlreadyAdded(softwareModuleDetails.getTypeId())) {
            return "";
        }

        return softwareModuleDetails.getTypeName();
    }

    private HorizontalLayout buildSoftwareModulesLayout(final ProxySoftwareModuleDetails softwareModuleDetails) {
        if (!isTypeAlreadyAdded(softwareModuleDetails.getTypeId())) {
            typeIdIsRendered.put(softwareModuleDetails.getTypeId(), true);
        }

        final Long smId = softwareModuleDetails.getSmId();
        final String smNameVersion = softwareModuleDetails.getSmNameAndVersion();

        final HorizontalLayout smLabelWithUnassignButtonLayout = new HorizontalLayout();
        smLabelWithUnassignButtonLayout.setSpacing(false);
        smLabelWithUnassignButtonLayout.setMargin(false);
        smLabelWithUnassignButtonLayout.setSizeFull();

        if (smId != null && !StringUtils.isEmpty(smNameVersion)) {
            smLabelWithUnassignButtonLayout.addComponent(buildSmLabel(smId, smNameVersion));

            if (isUnassignSmAllowed && permissionChecker.hasUpdateRepositoryPermission() && masterEntity.getIsValid()) {
                smLabelWithUnassignButtonLayout.addComponent(buildSmUnassignButton(smId, smNameVersion));
            }
        }

        return smLabelWithUnassignButtonLayout;
    }

    private static Label buildSmLabel(final Long smId, final String smNameWithVersion) {
        final Label smLabel = new Label(smNameWithVersion);

        smLabel.setId("sm-label-" + smId);
        smLabel.setSizeFull();
        smLabel.addStyleName(SPUIDefinitions.TEXT_STYLE);
        smLabel.addStyleName("text-cut");

        return smLabel;
    }

    private Button buildSmUnassignButton(final Long smId, final String smNameAndVersion) {
        final Button unassignSoftwareModuleButton = new Button(VaadinIcons.CLOSE_SMALL);

        unassignSoftwareModuleButton.setId("sm-unassign-button-" + smId);
        unassignSoftwareModuleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        unassignSoftwareModuleButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
        unassignSoftwareModuleButton.addStyleName("button-no-border");

        unassignSoftwareModuleButton.addClickListener(event -> unassignSoftwareModule(smId, smNameAndVersion));

        return unassignSoftwareModuleButton;
    }

    private void unassignSoftwareModule(final Long smId, final String smNameAndVersion) {
        if (masterEntity == null) {
            uiNotification.displayValidationError(
                    i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_DISTRIBUTIONSET_REQUIRED));
            return;
        }

        final Long dsId = masterEntity.getId();

        if (distributionSetManagement.isInUse(dsId)) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.notification.ds.target.assigned",
                    masterEntity.getName(), masterEntity.getVersion()));
        } else {
            distributionSetManagement.unassignSoftwareModule(dsId, smId);

            eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                    EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, dsId));
            uiNotification.displaySuccess(i18n.getMessage("message.sw.unassigned", smNameAndVersion));
        }
    }

    private void disableColumnSorting() {
        for (final Column<ProxySoftwareModuleDetails, ?> c : getColumns()) {
            c.setSortable(false);
        }
    }

    @Override
    public void masterEntityChanged(final ProxyDistributionSet masterEntity) {
        this.masterEntity = masterEntity;
        typeIdIsRendered.clear();

        if (masterEntity == null) {
            setItems(Collections.emptyList());
            setVisible(false);

            return;
        }

        final Optional<DistributionSetType> dsType = dsTypeManagement.get(masterEntity.getTypeInfo().getId());

        final List<ProxySoftwareModuleDetails> items = new ArrayList<>();

        dsType.ifPresent(type -> {
            final Collection<SoftwareModule> softwareModules = getSoftwareModulesByDsId(masterEntity.getId());

            for (final SoftwareModuleType mandatoryType : type.getMandatoryModuleTypes()) {
                items.addAll(getSmDetailsByType(softwareModules, mandatoryType, true));

                typeIdIsRendered.put(mandatoryType.getId(), false);
            }

            for (final SoftwareModuleType optionalType : type.getOptionalModuleTypes()) {
                items.addAll(getSmDetailsByType(softwareModules, optionalType, false));

                typeIdIsRendered.put(optionalType.getId(), false);
            }
        });

        setItems(items);
        setVisible(true);
    }

    private Collection<SoftwareModule> getSoftwareModulesByDsId(final Long dsId) {
        return HawkbitCommonUtil.getEntitiesByPageableProvider(query -> smManagement.findByAssignedTo(query, dsId));
    }

    private static List<ProxySoftwareModuleDetails> getSmDetailsByType(final Collection<SoftwareModule> softwareModules,
            final SoftwareModuleType type, final boolean isMandatory) {
        final List<ProxySoftwareModuleDetails> smDetails = softwareModules.stream()
                .filter(sm -> sm.getType().getId().equals(type.getId()))
                .map(sm -> new ProxySoftwareModuleDetails(isMandatory, type.getId(), type.getName(), sm.getId(),
                        HawkbitCommonUtil.concatStrings(":", sm.getName(), sm.getVersion())))
                .collect(Collectors.toList());

        if (smDetails.isEmpty()) {
            return Collections
                    .singletonList(new ProxySoftwareModuleDetails(isMandatory, type.getId(), type.getName(), null, ""));
        }

        return smDetails;
    }

    /**
     * @param isUnassignSmAllowed
     *            <code>true</code> if unassigned software module is allowed,
     *            otherwise <code>false</code>
     */
    public void setUnassignSmAllowed(final boolean isUnassignSmAllowed) {
        this.isUnassignSmAllowed = isUnassignSmAllowed;
    }
}
