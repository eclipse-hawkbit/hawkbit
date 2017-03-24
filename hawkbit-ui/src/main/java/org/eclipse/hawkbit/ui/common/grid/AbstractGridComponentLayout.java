/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid;

import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

/**
 * Abstract grid layout class which builds layout with grid {@link AbstractGrid}
 * and grid header {@link DefaultGridHeader}.
 */
public abstract class AbstractGridComponentLayout extends VerticalLayout {
    private static final long serialVersionUID = -3766179797384539821L;

    protected final transient EventBus.UIEventBus eventBus;
    protected final VaadinMessageSource i18n;

    private AbstractOrderedLayout gridHeader;
    private Grid grid;

    private transient AbstractFooterSupport footerSupport;

    /**
     * Constructor.
     *
     * @param i18n
     * @param deploymentManagement
     * @param eventBus
     * @param notification
     * @param managementUIState
     */
    public AbstractGridComponentLayout(final VaadinMessageSource i18n, final UIEventBus eventBus) {
        super();
        this.i18n = i18n;
        this.eventBus = eventBus;
    }

    /**
     * Initializes this layout that presents a header and a grid.
     */
    protected void init() {
        this.gridHeader = createGridHeader();
        this.grid = createGrid();
        buildLayout();
        setSizeFull();
        setImmediate(true);
        eventBus.subscribe(this);
    }

    /**
     * Layouts header, grid and optional footer.
     */
    protected void buildLayout() {
        setSizeFull();
        setSpacing(true);
        setMargin(false);
        setStyleName("group");
        final VerticalLayout gridHeaderLayout = new VerticalLayout();
        gridHeaderLayout.setSizeFull();
        gridHeaderLayout.setSpacing(false);
        gridHeaderLayout.setMargin(false);

        gridHeaderLayout.setStyleName("table-layout");
        gridHeaderLayout.addComponent(gridHeader);

        gridHeaderLayout.setComponentAlignment(gridHeader, Alignment.TOP_CENTER);
        gridHeaderLayout.addComponent(grid);
        gridHeaderLayout.setComponentAlignment(grid, Alignment.TOP_CENTER);
        gridHeaderLayout.setExpandRatio(grid, 1.0F);

        addComponent(gridHeaderLayout);
        setComponentAlignment(gridHeaderLayout, Alignment.TOP_CENTER);
        setExpandRatio(gridHeaderLayout, 1.0F);
        if (hasFooterSupport()) {
            final Layout footerLayout = getFooterSupport().createFooterMessageComponent();
            addComponent(footerLayout);
            setComponentAlignment(footerLayout, Alignment.BOTTOM_CENTER);
        }

    }

    /**
     * Registers the selection of this grid as master for another grid that
     * displays the details.
     *
     * @param details
     *            the details of another grid the selection of this grid should
     *            be registered for as master.
     */
    public void registerDetails(final AbstractGrid<?>.DetailsSupport details) {
        grid.addSelectionListener(event -> {
            final Long masterId = (Long) event.getSelected().stream().findFirst().orElse(null);
            details.populateMasterDataAndRecalculateContainer(masterId);
        });
    }

    /**
     * Gets the grid instance displayed and owned by the layout.
     *
     * @return grid instance displayed and owned by the layout.
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Gets the grid-header instance displayed and owned by the layout.
     *
     * @return grid-header instance displayed and owned by the layout.
     */
    public AbstractOrderedLayout getHeader() {
        return gridHeader;
    }

    /**
     * Creates the grid-header instance the layout is responsible for.
     *
     * @return newly created grid-header instance displayed and owned by the
     *         layout.
     */
    public abstract AbstractOrderedLayout createGridHeader();

    /**
     * Creates the grid instance the layout is responsible for.
     *
     * @return newly created grid instance displayed and owned by the layout.
     */
    public abstract Grid createGrid();

    /**
     * Enables footer-support for the grid by setting a FooterSupport
     * implementation.
     *
     * @param footerSupport
     *            encapsulates footer layout.
     */
    public void setFooterSupport(final AbstractFooterSupport footerSupport) {
        this.footerSupport = footerSupport;
    }

    /**
     * Gets the FooterSupport implementation describing footer layout.
     *
     * @return footerSupport that encapsulates footer layout.
     */
    public AbstractFooterSupport getFooterSupport() {
        return footerSupport;
    }

    /**
     * Checks whether footer-support is enabled.
     *
     * @return <code>true</code> if footer-support is enabled, otherwise
     *         <code>false</code>
     */
    public boolean hasFooterSupport() {
        return footerSupport != null;
    }

    /**
     * If footer support is enabled, the footer is placed below the component
     */
    public abstract class AbstractFooterSupport {

        /**
         * Creates a sub-layout for the footer.
         *
         * @return the footer sub-layout.
         */
        private Layout createFooterMessageComponent() {
            final HorizontalLayout footerLayout = new HorizontalLayout();
            footerLayout.addComponent(getFooterMessageLabel());
            footerLayout.setStyleName(SPUIStyleDefinitions.FOOTER_LAYOUT);
            footerLayout.setWidth(100, Unit.PERCENTAGE);
            return footerLayout;
        }

        /**
         * Get the count message label.
         *
         * @return count message
         */
        protected abstract Label getFooterMessageLabel();
    }
}
