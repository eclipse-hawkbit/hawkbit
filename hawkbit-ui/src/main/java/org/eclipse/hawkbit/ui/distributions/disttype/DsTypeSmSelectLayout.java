/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToProxyTypeMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Layout for the software modules select grids for managing Distribution Set
 * Types on the Distributions View.
 */
public class DsTypeSmSelectLayout extends CustomField<Set<ProxyType>> {
    private static final long serialVersionUID = 1L;

    private static final int MAX_SM_TYPE_QUERY = 500;

    private final VaadinMessageSource i18n;

    private final transient TypeToProxyTypeMapper<SoftwareModuleType> smTypeToProxyTypeMapper;

    private SmTypeSelectedGrid selectedGrid;
    private SmTypeSourceGrid sourceGrid;

    private final List<ProxyType> allSmTypes;
    private Set<ProxyType> selectedSmTypes;

    private final HorizontalLayout layout;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public DsTypeSmSelectLayout(final VaadinMessageSource i18n,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        this.i18n = i18n;
        this.smTypeToProxyTypeMapper = new TypeToProxyTypeMapper<>();

        this.allSmTypes = softwareModuleTypeManagement.findAll(PageRequest.of(0, MAX_SM_TYPE_QUERY))
                .map(smTypeToProxyTypeMapper::map).getContent();
        this.selectedSmTypes = new HashSet<>();

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
        selectButton.addClickListener(event -> addSmTypeToSelectedGrid());

        final Button unSelectButton = SPUIComponentProvider.getButton("unselect-dist-type", "", "", "arrow-button",
                true, VaadinIcons.BACKWARDS, SPUIButtonStyleNoBorder.class);
        unSelectButton.addClickListener(event -> removeSmTypeFromSelectedGrid());

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

    private void addSmTypeToSelectedGrid() {
        final Set<ProxyType> selectedSourceSmTypes = sourceGrid.getSelectedItems();
        if (CollectionUtils.isEmpty(selectedSourceSmTypes)) {
            return;
        }

        setValue(Sets.union(selectedSmTypes, selectedSourceSmTypes).immutableCopy());
    }

    private void removeSmTypeFromSelectedGrid() {
        final Set<ProxyType> selectedSelectedSmTypes = selectedGrid.getSelectedItems();
        if (CollectionUtils.isEmpty(selectedSelectedSmTypes)) {
            return;
        }

        setValue(Sets.difference(selectedSmTypes, selectedSelectedSmTypes).immutableCopy());
    }

    private SmTypeSourceGrid buildSourceGrid() {
        final SmTypeSourceGrid grid = new SmTypeSourceGrid(i18n);
        grid.setItems(allSmTypes);

        if (!CollectionUtils.isEmpty(allSmTypes)) {
            grid.select(allSmTypes.get(0));
        }

        return grid;
    }

    private SmTypeSelectedGrid buildSelectedGrid() {
        // we fire value change event to validate binder in case of mandatory
        // property change because binder itself does not track modified
        // software module types within collection
        final SmTypeSelectedGrid smTypeSelectedGrid = new SmTypeSelectedGrid(i18n,
                () -> fireEvent(createValueChange(selectedSmTypes, false)));
        smTypeSelectedGrid.setItems(selectedSmTypes);

        return smTypeSelectedGrid;
    }

    @Override
    public Set<ProxyType> getValue() {
        return selectedSmTypes;
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

        selectedSmTypes = value;

        selectedGrid.setItems(selectedSmTypes);
        sourceGrid.setItems(getSourceSmTypes());
    }

    private List<ProxyType> getSourceSmTypes() {
        final Set<Long> selectedSmTypeIds = selectedSmTypes.stream().map(ProxyIdentifiableEntity::getId)
                .collect(Collectors.toSet());
        return allSmTypes.stream().filter(smType -> !selectedSmTypeIds.contains(smType.getId()))
                .collect(Collectors.toList());
    }
}
