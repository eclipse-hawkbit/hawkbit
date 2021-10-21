/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import com.google.common.collect.Sets;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToProxyTypeMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Layout for the distribution sets select grids for managing Target
 * Types on the Distributions View.
 */
public class TargetTypeDsTypeSelectLayout extends CustomField<Set<ProxyType>> {
    private static final long serialVersionUID = 1L;

    private static final int MAX_DS_TYPE_QUERY = 500;

    private final VaadinMessageSource i18n;

    private final transient TypeToProxyTypeMapper<DistributionSetType> dsTypeToProxyTypeMapper;

    private DsTypeSelectedGrid selectedGrid;
    private DsTypeSourceGrid sourceGrid;

    private final List<ProxyType> allDsTypes;
    private Set<ProxyType> selectedDsTypes;

    private final HorizontalLayout layout;

    /**
     * Constructor
     *
     * @param i18n
     *            VaadinMessageSource
     * @param distributionSetTypeManagement
     *            distributionSetTypeManagement
     */
    public TargetTypeDsTypeSelectLayout(final VaadinMessageSource i18n,
                                        final DistributionSetTypeManagement distributionSetTypeManagement) {
        this.i18n = i18n;
        this.dsTypeToProxyTypeMapper = new TypeToProxyTypeMapper<>();

        this.allDsTypes = distributionSetTypeManagement.findAll(PageRequest.of(0, MAX_DS_TYPE_QUERY))
                .map(dsTypeToProxyTypeMapper::map).getContent();
        this.selectedDsTypes = new HashSet<>();


        this.layout = new HorizontalLayout();
        this.layout.setSpacing(false);
        this.layout.setMargin(false);
        this.layout.setSizeFull();
        this.layout.setWidth("400px");

        buildLayout();
    }

    private void buildLayout() {
        final VerticalLayout selectButtonLayout = new VerticalLayout();
        selectButtonLayout.setSpacing(false);
        selectButtonLayout.setMargin(false);

        final Button selectButton = SPUIComponentProvider.getButton(UIComponentIdProvider.SELECT_DIST_TYPE, "", "",
                "arrow-button", true, VaadinIcons.FORWARD, SPUIButtonStyleNoBorder.class);
        selectButton.addClickListener(event -> addDsTypeToSelectedGrid());

        final Button unSelectButton = SPUIComponentProvider.getButton(UIComponentIdProvider.UNSELECT_DIST_TYPE, "", "", "arrow-button",
                true, VaadinIcons.BACKWARDS, SPUIButtonStyleNoBorder.class);
        unSelectButton.addClickListener(event -> removeDsTypeFromSelectedGrid());

        selectButtonLayout.addComponent(selectButton);
        selectButtonLayout.addComponent(unSelectButton);
        selectButtonLayout.setComponentAlignment(selectButton, Alignment.MIDDLE_CENTER);
        selectButtonLayout.setComponentAlignment(unSelectButton, Alignment.MIDDLE_CENTER);

        sourceGrid = buildSourceGrid();
        selectedGrid = buildSelectedGrid();

        layout.addComponent(sourceGrid);
        layout.addComponent(selectButtonLayout);
        layout.addComponent(selectedGrid);
        layout.setComponentAlignment(sourceGrid, Alignment.MIDDLE_LEFT);
        layout.setComponentAlignment(selectButtonLayout, Alignment.MIDDLE_CENTER);
        layout.setComponentAlignment(selectedGrid, Alignment.MIDDLE_RIGHT);
        layout.setExpandRatio(sourceGrid, 0.45F);
        layout.setExpandRatio(selectButtonLayout, 0.07F);
        layout.setExpandRatio(selectedGrid, 0.48F);
    }

    private void addDsTypeToSelectedGrid() {
        final Set<ProxyType> selectedSourceDsTypes = sourceGrid.getSelectedItems();
        if (CollectionUtils.isEmpty(selectedSourceDsTypes)) {
            return;
        }

        setValue(Sets.union(selectedDsTypes, selectedSourceDsTypes).immutableCopy());
    }

    private void removeDsTypeFromSelectedGrid() {
        final Set<ProxyType> selectedSelectedDsTypes = selectedGrid.getSelectedItems();
        if (CollectionUtils.isEmpty(selectedSelectedDsTypes)) {
            return;
        }

        setValue(Sets.difference(selectedDsTypes, selectedSelectedDsTypes).immutableCopy());
    }

    private DsTypeSourceGrid buildSourceGrid() {
        final DsTypeSourceGrid grid = new DsTypeSourceGrid(i18n);
        grid.setItems(allDsTypes);

        if (!CollectionUtils.isEmpty(allDsTypes)) {
            grid.select(allDsTypes.get(0));
        }

        return grid;
    }

    private DsTypeSelectedGrid buildSelectedGrid() {
        final DsTypeSelectedGrid dsTypeSelectedGrid = new DsTypeSelectedGrid(i18n);
        dsTypeSelectedGrid.setItems(selectedDsTypes);
        return dsTypeSelectedGrid;
    }

    @Override
    public Set<ProxyType> getValue() {
        return selectedDsTypes;
    }

    @Override
    protected Component initContent() {
        return layout;
    }

    @Override
    protected void doSetValue(final Set<ProxyType> value) {
        if (value == null) {
            return;
        }

        selectedDsTypes = value;

        selectedGrid.setItems(selectedDsTypes);
        sourceGrid.setItems(getSourceDsTypes());
    }

    private List<ProxyType> getSourceDsTypes() {
        final Set<Long> selectedDsTypeIds = selectedDsTypes.stream().map(ProxyIdentifiableEntity::getId)
                .collect(Collectors.toSet());
        return allDsTypes.stream().filter(dsType -> !selectedDsTypeIds.contains(dsType.getId()))
                .collect(Collectors.toList());
    }
}
