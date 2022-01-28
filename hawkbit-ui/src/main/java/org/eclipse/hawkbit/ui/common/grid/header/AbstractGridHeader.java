/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.grid.header.support.HeaderSupport;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Abstract grid header.
 */
public abstract class AbstractGridHeader extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    protected final VaadinMessageSource i18n;
    protected final SpPermissionChecker permChecker;
    protected final transient UIEventBus eventBus;

    private final transient List<HeaderSupport> headerSupports;

    /**
     * Constructor for AbstractGridHeader
     *
     * @param i18n
     *            VaadinMessageSource
     * @param permChecker
     *            SpPermissionChecker
     * @param eventBus
     *            UIEventBus
     */
    protected AbstractGridHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus) {
        this.i18n = i18n;
        this.permChecker = permChecker;
        this.eventBus = eventBus;

        this.headerSupports = new ArrayList<>();

        init();
    }

    protected void restoreCaption() {
        // empty by default for stateless header captions
    }

    protected void addHeaderSupports(final Collection<HeaderSupport> headerSupports) {
        headerSupports.forEach(this::addHeaderSupport);
    }

    protected void addHeaderSupport(final HeaderSupport headerSupport) {
        if (headerSupport != null) {
            headerSupports.add(headerSupport);
        }
    }

    protected void addHeaderSupport(final HeaderSupport headerSupport, final int index) {
        if (headerSupport != null) {
            headerSupports.add(index, headerSupport);
        }
    }

    protected void init() {
        setSpacing(false);
        setMargin(false);

        addStyleName("bordered-layout");
        addStyleName("no-border-bottom");

        setHeight("50px");
    }

    /**
     * Build header component for grid
     */
    public void buildHeader() {
        final HorizontalLayout headerComponentsLayout = new HorizontalLayout();
        headerComponentsLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        headerComponentsLayout.setSpacing(false);
        headerComponentsLayout.setMargin(false);
        headerComponentsLayout.setSizeFull();

        final Component headerCaption = getHeaderCaption();
        headerComponentsLayout.addComponent(headerCaption);
        headerComponentsLayout.setComponentAlignment(headerCaption, Alignment.TOP_LEFT);
        headerComponentsLayout.setExpandRatio(headerCaption, 0.4F);

        headerSupports.stream().filter(Objects::nonNull).forEach(headerSupport -> {
            final Component headerComponent = headerSupport.getHeaderComponent();

            headerComponentsLayout.addComponent(headerComponent);
            headerComponentsLayout.setComponentAlignment(headerComponent, Alignment.TOP_RIGHT);
            headerComponentsLayout.setExpandRatio(headerComponent, headerSupport.getExpandRation());
        });

        addComponent(headerComponentsLayout);
    }

    protected abstract Component getHeaderCaption();

    /**
     * Restore to the default header state
     */
    public void restoreState() {
        restoreCaption();
        headerSupports.stream().filter(Objects::nonNull).forEach(HeaderSupport::restoreState);
    }

    protected int getHeaderSupportsSize() {
        return headerSupports.size();
    }
}
