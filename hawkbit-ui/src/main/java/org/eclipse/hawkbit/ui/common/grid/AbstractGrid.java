/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Container.Indexed;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.ui.Grid;

/**
 * Abstract grid that offers various capabilities (aka support) to offer
 * convenient enhancements to the vaadin standard grid.
 *
 * @param <T>
 *            The container-type used by the grid
 */
public abstract class AbstractGrid<T extends Indexed> extends Grid implements RefreshableContainer {
    private static final long serialVersionUID = 1L;

    protected final VaadinMessageSource i18n;
    protected final transient EventBus.UIEventBus eventBus;
    protected final SpPermissionChecker permissionChecker;

    private transient AbstractMaximizeSupport maximizeSupport;
    private transient AbstractGeneratedPropertySupport generatedPropertySupport;
    private transient SingleSelectionSupport singleSelectionSupport;
    private transient DetailsSupport detailsSupport;

    /**
     * Constructor.
     *
     * @param i18n
     * @param eventBus
     * @param permissionChecker
     */
    protected AbstractGrid(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker) {
        this.i18n = i18n;
        this.eventBus = eventBus;
        this.permissionChecker = permissionChecker;
    }

    /**
     * Initializes the grid.
     * <p>
     *
     * <b>NOTE:</b> Sub-classes should configure the grid before calling this
     * method (this means: set all support-classes needed, and then call init).
     */
    protected void init() {
        setSizeFull();
        setImmediate(true);
        setId(getGridId());
        if (!hasSingleSelectionSupport()) {
            setSelectionMode(SelectionMode.NONE);
        }
        setColumnReorderingAllowed(true);
        addNewContainerDS();
        eventBus.subscribe(this);
    }

    /**
     * Refresh the container.
     */
    @Override
    public void refreshContainer() {
        final Indexed container = getContainerDataSource();
        if (hasGeneratedPropertySupport()
                && getGeneratedPropertySupport().getRawContainer() instanceof LazyQueryContainer) {
            ((LazyQueryContainer) getGeneratedPropertySupport().getRawContainer()).refresh();
            return;
        }

        if (container instanceof LazyQueryContainer) {
            ((LazyQueryContainer) container).refresh();
        }
    }

    /**
     * Creates a new container instance by calling the required
     * template-methods.
     * <p>
     * A new container is created on initialization as well as when container
     * content fundamentally changes (e.g. if container content depends on a
     * selection as common in master-details relations)
     */
    protected void addNewContainerDS() {
        final T container = createContainer();
        Indexed indexedContainer = container;
        if (hasGeneratedPropertySupport()) {
            indexedContainer = getGeneratedPropertySupport().decorate(container);
            setContainerDataSource(indexedContainer);
            getGeneratedPropertySupport().addGeneratedContainerProperties();
        } else {
            setContainerDataSource(indexedContainer);
        }
        addContainerProperties();

        setColumnProperties();
        setColumnHeaderNames();
        setColumnsHidable();
        addColumnRenderes();
        setColumnExpandRatio();

        setHiddenColumns();

        final CellDescriptionGenerator cellDescriptionGenerator = getDescriptionGenerator();
        if (getDescriptionGenerator() != null) {
            setCellDescriptionGenerator(cellDescriptionGenerator);
        }

        if (indexedContainer != null && indexedContainer.size() == 0) {
            setData(SPUIDefinitions.NO_DATA);
        }
    }

    /**
     * Sets the standard behavior of columns to be hidable. If implementors
     * needs other behavior they have to concern about it.
     */
    protected void setColumnsHidable() {
        // Allow column hiding
        for (final Column c : getColumns()) {
            c.setHidable(true);
        }
    }

    /**
     * Enables maximize-support for the grid by setting a MaximizeSupport
     * implementation.
     *
     * @param maximizeSupport
     *            encapsulates behavior for minimize and maximize.
     */
    protected void setMaximizeSupport(final AbstractMaximizeSupport maximizeSupport) {
        this.maximizeSupport = maximizeSupport;
    }

    /**
     * Gets the MaximizeSupport implementation describing behavior for minimize
     * and maximize.
     *
     * @return maximizeSupport that encapsulates behavior for minimize and
     *         maximize.
     */
    protected AbstractMaximizeSupport getMaximizeSupport() {
        return maximizeSupport;
    }

    /**
     * Checks whether maximize-support is enabled.
     *
     * @return <code>true</code> if maximize-support is enabled, otherwise
     *         <code>false</code>
     */
    protected boolean hasMaximizeSupport() {
        return maximizeSupport != null;
    }

    /**
     * Enables support for generated properties. This implies that the
     * standard-container has to be decorated and the generators have to be
     * registered for the generated (aka virtual) properties.
     *
     * @param generatedPropertySupport
     *            that encapsulates behavior for generated properties
     */
    protected void setGeneratedPropertySupport(final AbstractGeneratedPropertySupport generatedPropertySupport) {
        this.generatedPropertySupport = generatedPropertySupport;
    }

    /**
     * Gets the GeneratedPropertySupport implementation describing generated
     * properties by registering their generators and attaching them to a
     * wrapper-container.
     *
     * @return generatedPropertySupport that encapsulates registration of
     *         generated properties.
     */
    protected AbstractGeneratedPropertySupport getGeneratedPropertySupport() {
        return generatedPropertySupport;
    }

    /**
     * Checks whether support for generated properties is enabled.
     *
     * @return <code>true</code> if support for generated properties is enabled,
     *         otherwise <code>false</code>
     */
    protected boolean hasGeneratedPropertySupport() {
        return generatedPropertySupport != null;
    }

    /**
     * Enables single-selection-support for the grid by setting
     * SingleSelectionSupport configuration.
     *
     * @param singleSelectionSupport
     *            encapsulates behavior for single-selection and offers some
     *            convenient functionality.
     */
    protected void setSingleSelectionSupport(final SingleSelectionSupport singleSelectionSupport) {
        this.singleSelectionSupport = singleSelectionSupport;
    }

    /**
     * Gets the SingleSelectionSupport implementation configuring
     * single-selection.
     *
     * @return singleSelectionSupport that configures single-selection.
     */
    protected SingleSelectionSupport getSingleSelectionSupport() {
        return singleSelectionSupport;
    }

    /**
     * Checks whether single-selection-support is enabled.
     *
     * @return <code>true</code> if single-selection-support is enabled,
     *         otherwise <code>false</code>
     */
    protected boolean hasSingleSelectionSupport() {
        return singleSelectionSupport != null;
    }

    /**
     * Enables details-support for the grid by setting DetailsSupport
     * configuration. If details-support is enabled, the grid handles
     * details-data that depends on a master-selection.
     *
     * @param detailsSupport
     *            encapsulates behavior for changes of master-selection.
     */
    protected void setDetailsSupport(final DetailsSupport detailsSupport) {
        this.detailsSupport = detailsSupport;
    }

    /**
     * Gets the DetailsSupport implementation configuring master-details
     * relation.
     *
     * @return detailsSupport that configures master-details relation.
     */
    public DetailsSupport getDetailsSupport() {
        return detailsSupport;
    }

    /**
     * Checks whether details-support is enabled.
     *
     * @return <code>true</code> if details-support is enabled, otherwise
     *         <code>false</code>
     */
    public boolean hasDetailsSupport() {
        return detailsSupport != null;
    }

    /**
     * Template method invoked by {@link this#addNewContainerDS()} for creating
     * a container instance.
     *
     * @return new container instance used by the grid.
     */
    protected abstract T createContainer();

    /**
     * Template method invoked by {@link this#addNewContainerDS()} for adding
     * properties to the container (usually by invoing
     * {@link Container#addContainerProperty(Object, Class, Object))})
     */
    protected abstract void addContainerProperties();

    /**
     * Template method invoked by {@link this#addNewContainerDS()} for setting
     * the expand ratio of the columns.
     */
    protected abstract void setColumnExpandRatio();

    /**
     * Template method invoked by {@link this#addNewContainerDS()} for setting
     * the column names.
     */
    protected abstract void setColumnHeaderNames();

    /**
     * Template method invoked by {@link this#addNewContainerDS()} for setting
     * the column properties to the grid.
     */
    protected abstract void setColumnProperties();

    /**
     * Template method invoked by {@link this#addNewContainerDS()} for adding
     * special column renderers if needed.
     */
    protected abstract void addColumnRenderes();

    /**
     * Template method invoked by {@link this#addNewContainerDS()} that hides
     * columns. If a column is hidable and hidden, it can be made visible via
     * grid column menu.
     */
    protected abstract void setHiddenColumns();

    /**
     * Template method invoked by {@link this#addNewContainerDS()} for adding a
     * CellDescriptionGenerator to the grid.
     */
    protected abstract CellDescriptionGenerator getDescriptionGenerator();

    /**
     * Gets id of the grid.
     *
     * @return id of the grid
     */
    protected abstract String getGridId();

    /**
     * Resets the default row of the header. This means the current default row
     * is removed and replaced with a newly created one.
     *
     * @return the new and clean header row.
     */
    protected HeaderRow resetHeaderDefaultRow() {
        getHeader().removeRow(getHeader().getDefaultRow());
        final HeaderRow newHeaderRow = getHeader().appendRow();
        getHeader().setDefaultRow(newHeaderRow);
        return newHeaderRow;
    }

    /**
     * Support for master-details relation for grid. This means that grid
     * content (=details) is updated as soon as master-data changes.
     */
    public class DetailsSupport {

        private Long master;

        /**
         * Set selected master-data as member of this grid-support (as all
         * presented grid-data is related to this master-data) and re-calculate
         * grid-container-content.
         *
         * @param master
         *            id of selected action
         */
        public void populateMasterDataAndRecalculateContainer(final Long master) {
            this.master = master;
            recalculateContainer();
            populateSelection();
        }

        /**
         * Set selected master-data as member of this grid-support (as all
         * presented grid-data is related to this master-data) and re-create
         * grid-container.
         *
         * @param master
         *            id of selected action
         */
        public void populateMasterDataAndRecreateContainer(final Long master) {
            this.master = master;
            recreateContainer();
            populateSelection();
        }

        /**
         * Propagates the selection if needed.
         *
         */
        public void populateSelection() {
            if (!hasSingleSelectionSupport()) {
                return;
            }

            if (master == null) {
                getSingleSelectionSupport().clearSelection();
                return;
            }
            getSingleSelectionSupport().selectFirstRow();
        }

        /**
         * Gets the master-data id.
         *
         * @return master-data id
         */
        public Long getMasterDataId() {
            return master;
        }

        /**
         * Invalidates container-data (but reused container) and refreshes it
         * with new details-data for the new selected master-data.
         */
        private void recalculateContainer() {
            clearSortOrder();
            refreshContainer();
        }

        /**
         * Invalidates container and replace it with a fresh instance for the
         * new selected master-data.
         */
        private void recreateContainer() {
            removeAllColumns();
            clearSortOrder();
            addNewContainerDS();
        }
    }

    /**
     * Via implementations of this support capability an expand-mode is provided
     * that maximizes the grid size.
     */
    protected abstract class AbstractMaximizeSupport {

        /**
         * Renews the content for maximized layout.
         */
        public void createMaximizedContent() {
            setMaximizedColumnProperties();
            setMaximizedHiddenColumns();
            setMaximizedHeaders();
            setMaximizedColumnExpandRatio();
        }

        /**
         * Renews the content for minimized layout.
         */
        public void createMinimizedContent() {
            setColumnProperties();
            setHiddenColumns();
            setColumnExpandRatio();
        }

        /**
         * Sets the column properties for maximized-state.
         */
        protected abstract void setMaximizedColumnProperties();

        /**
         * Sets the hidden columns for maximized-state.
         */
        protected abstract void setMaximizedHiddenColumns();

        /**
         * Sets additional headers for maximized-state.
         */
        protected abstract void setMaximizedHeaders();

        /**
         * Sets column expand ratio for maximized-state.
         */
        protected abstract void setMaximizedColumnExpandRatio();
    }

    /**
     * Grids that are used in conjunction with
     * {@link GeneratedPropertyContainer}, might use
     * {@link AbstractGeneratedPropertySupport} to get type-save access to the
     * raw container as well as the decorated container.
     */
    protected abstract class AbstractGeneratedPropertySupport {

        /**
         * Gives type-save access to the wrapper container the grid works on.
         * This wrapper container attaches generated properties that are not
         * part of the raw container that encapsulates the database access.
         *
         * @return decorated container that includes the generated properties as
         *         well as the native properties.
         */
        public abstract GeneratedPropertyContainer getDecoratedContainer();

        /**
         * Gives type-save access to the wrapped container that binds to the
         * database. The grid does not work directly with this container but
         * with a container that wraps and decorates it with generated
         * properties.
         *
         * @return raw container that gives access to the native properties.
         */
        public abstract T getRawContainer();

        /**
         * Adds the generated properties to the decorated container. Each
         * generated property has to be associated with a property generator
         * that is capable to calculate the value of the generated (aka virtual)
         * property.
         *
         * @return decorated container that includes the generated properties
         */
        protected abstract GeneratedPropertyContainer addGeneratedContainerProperties();

        /**
         * Decorates the raw-container by wrapping it.
         *
         * @param container
         *            raw-container to be wrapped.
         * @return decorated container.
         */
        protected GeneratedPropertyContainer decorate(final T container) {
            return new GeneratedPropertyContainer(container);
        }
    }

    /**
     * Support for single selection on the grid.
     */
    protected class SingleSelectionSupport {

        public SingleSelectionSupport() {
            enable();
        }

        public final void enable() {
            setSelectionMode(SelectionMode.SINGLE);
        }

        public final void disable() {
            setSelectionMode(SelectionMode.NONE);
        }

        /**
         * Selects the first row if available and enabled.
         */
        public void selectFirstRow() {
            if (!isSingleSelectionModel()) {
                return;
            }

            final Indexed container = getContainerDataSource();
            final int size = container.size();
            if (size > 0) {
                refreshRows(getContainerDataSource().firstItemId());
                getSingleSelectionModel().select(getContainerDataSource().firstItemId());
            } else {
                getSingleSelectionModel().select(null);
            }
        }

        private boolean isSingleSelectionModel() {
            return getSelectionModel() instanceof SelectionModel.Single;
        }

        /**
         * Clears the selection.
         */
        public void clearSelection() {
            if (!isSingleSelectionModel()) {
                return;
            }
            getSingleSelectionModel().select(null);
        }

        private SelectionModel.Single getSingleSelectionModel() {
            return (SelectionModel.Single) getSelectionModel();
        }
    }

    /**
     * CellStyleGenerator that concerns about alignment in the grid cells.
     */
    protected static class AlignCellStyleGenerator implements CellStyleGenerator {
        private static final long serialVersionUID = 5573570647129792429L;

        private final String[] left;
        private final String[] center;
        private final String[] right;

        /**
         * Constructor.
         *
         * @param left
         *            list of propertyIds that should be left-aligned
         * @param center
         *            list of propertyIds that should be center-aligned
         * @param right
         *            list of propertyIds that should be right-aligned
         */
        public AlignCellStyleGenerator(final String[] left, final String[] center, final String[] right) {
            this.left = left;
            this.center = center;
            this.right = right;
        }

        @Override
        public String getStyle(final CellReference cellReference) {

            if (center != null
                    && Arrays.stream(center).anyMatch(o -> Objects.equals(o, cellReference.getPropertyId()))) {
                return "centeralign";
            } else if (right != null
                    && Arrays.stream(right).anyMatch(o -> Objects.equals(o, cellReference.getPropertyId()))) {
                return "rightalign";
            } else if (left != null
                    && Arrays.stream(left).anyMatch(o -> Objects.equals(o, cellReference.getPropertyId()))) {
                return "leftalign";
            }
            return null;
        }
    }

    /**
     * Adds a tooltip to the 'Date and time' column in detailed format.
     */
    public static class ModifiedTimeTooltipGenerator implements CellDescriptionGenerator {
        private static final long serialVersionUID = -6617911967167729195L;

        private final String datePropertyId;

        /**
         * Constructor.
         *
         * @param datePropertyId
         */
        public ModifiedTimeTooltipGenerator(final String datePropertyId) {
            this.datePropertyId = datePropertyId;
        }

        @Override
        public String getDescription(final CellReference cell) {
            if (!datePropertyId.equals(cell.getPropertyId())) {
                return null;
            }
            final Long timestamp = (Long) cell.getItem().getItemProperty(datePropertyId).getValue();
            return SPDateTimeUtil.getFormattedDate(timestamp);
        }
    }

    /**
     * Converter that gets time-data as input of type <code>Long</code> and
     * converts to a formatted date string.
     */
    public class LongToFormattedDateStringConverter implements Converter<String, Long> {
        private static final long serialVersionUID = 1247513913478717845L;

        @Override
        public Long convertToModel(final String value, final Class<? extends Long> targetType, final Locale locale) {
            // not needed
            return null;
        }

        @Override
        public String convertToPresentation(final Long value, final Class<? extends String> targetType,
                final Locale locale) {
            return SPDateTimeUtil.getFormattedDate(value, SPUIDefinitions.LAST_QUERY_DATE_FORMAT_SHORT);
        }

        @Override
        public Class<Long> getModelType() {
            return Long.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }
}
