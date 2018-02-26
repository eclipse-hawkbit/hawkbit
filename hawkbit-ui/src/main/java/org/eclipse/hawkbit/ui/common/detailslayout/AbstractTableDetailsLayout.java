/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.Map;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;
import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Abstract Layout to show the entity details.
 *
 * @param <T>
 */
public abstract class AbstractTableDetailsLayout<T extends NamedEntity> extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final SpPermissionChecker permissionChecker;

    private T selectedBaseEntity;

    private Label caption;

    private Button editButton;

    private Button manageMetadataBtn;

    private TabSheet detailsTab;

    private final VerticalLayout detailsLayout;

    private final VerticalLayout descriptionLayout;

    private final VerticalLayout logLayout;

    private final VerticalLayout attributesLayout;

    private final VerticalLayout tagsLayout;

    private final ManagementUIState managementUIState;

    protected AbstractTableDetailsLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManagementUIState managementUIState) {
        this.i18n = i18n;
        this.permissionChecker = permissionChecker;
        this.managementUIState = managementUIState;
        detailsLayout = createTabLayout();
        descriptionLayout = createTabLayout();
        logLayout = createTabLayout();
        attributesLayout = createTabLayout();
        tagsLayout = createTabLayout();
        createComponents();
        buildLayout();
        eventBus.subscribe(this);
    }

    public void setSelectedBaseEntity(final T selectedBaseEntity) {
        this.selectedBaseEntity = selectedBaseEntity;
    }

    protected final VerticalLayout createTabLayout() {
        final VerticalLayout tabLayout = SPUIComponentProvider.getDetailTabLayout();
        tabLayout.addStyleName("details-layout");
        return tabLayout;
    }

    protected SpPermissionChecker getPermissionChecker() {
        return permissionChecker;
    }

    protected VaadinMessageSource getI18n() {
        return i18n;
    }

    protected T getSelectedBaseEntity() {
        return selectedBaseEntity;
    }

    /**
     * Default implementation to handle an entity event.
     * 
     * @param baseEntityEvent
     *            the event
     */
    protected void onBaseEntityEvent(final BaseUIEntityEvent<T> baseEntityEvent) {
        final BaseEntityEventType eventType = baseEntityEvent.getEventType();
        if (BaseEntityEventType.SELECTED_ENTITY == eventType || BaseEntityEventType.UPDATED_ENTITY == eventType
                || BaseEntityEventType.REMOVE_ENTITY == eventType) {
            UI.getCurrent().access(() -> populateData(baseEntityEvent.getEntity()));
        } else if (BaseEntityEventType.MINIMIZED == eventType) {
            UI.getCurrent().access(() -> setVisible(true));
        } else if (BaseEntityEventType.MAXIMIZED == eventType) {
            UI.getCurrent().access(() -> setVisible(false));
        }
    }

    protected void setName(final String headerCaption, final String value) {
        caption.setValue(HawkbitCommonUtil.getSoftwareModuleName(headerCaption, value));
    }

    /**
     * Restores the tables and tabs displayed on the view based on the selected
     * entity.
     */
    protected void restoreState() {
        populateData(getSelectedBaseEntity());
        if (onLoadIsTableMaximized()) {
            setVisible(false);
        }
    }

    protected void populateTags(final AbstractTagToken<?> tagToken) {
        getTagsLayout().removeAllComponents();
        if (getSelectedBaseEntity() == null) {
            return;
        }
        getTagsLayout().addComponent(tagToken.getTokenField());
    }

    protected void populateLog() {
        logLayout.removeAllComponents();
        logLayout.addComponent(SPUIComponentProvider.createNameValueLabel(i18n.getMessage("label.created.at"),
                SPDateTimeUtil.formatCreatedAt(selectedBaseEntity)));
        logLayout.addComponent(SPUIComponentProvider.createCreatedByLabel(i18n, selectedBaseEntity));

        logLayout.addComponent(SPUIComponentProvider.createNameValueLabel(i18n.getMessage("label.modified.date"),
                SPDateTimeUtil.formatLastModifiedAt(selectedBaseEntity)));
        logLayout.addComponent(SPUIComponentProvider.createLastModifiedByLabel(i18n, selectedBaseEntity));
    }

    protected void updateDescriptionLayout(final String description) {
        descriptionLayout.removeAllComponents();
        final Label descLabel = SPUIComponentProvider.createNameValueLabel("", description == null ? "" : description);
        /**
         * By default text will be truncated based on layout width. So removing
         * it as we need full description.
         */
        descLabel.removeStyleName("label-style");
        descLabel.setId(UIComponentIdProvider.DETAILS_DESCRIPTION_LABEL_ID);
        descriptionLayout.addComponent(descLabel);
    }

    /*
     * display Attributes details in Target details.
     */
    protected void updateAttributesLayout(final Map<String, String> attributes) {
        if (null != attributes) {
            attributesLayout.removeAllComponents();
            for (final Map.Entry<String, String> entry : attributes.entrySet()) {
                final Label conAttributeLabel = SPUIComponentProvider.createNameValueLabel(
                        entry.getKey().concat("  :  "), entry.getValue() == null ? "" : entry.getValue());
                conAttributeLabel.setDescription(entry.getKey().concat("  :  ") + entry.getValue());
                conAttributeLabel.addStyleName("label-style");
                attributesLayout.addComponent(conAttributeLabel);
            }
        }
    }

    protected VerticalLayout getLogLayout() {
        return logLayout;
    }

    protected VerticalLayout getAttributesLayout() {
        return attributesLayout;
    }

    protected VerticalLayout getDescriptionLayout() {
        return descriptionLayout;
    }

    protected VerticalLayout getDetailsLayout() {
        return detailsLayout;
    }

    protected VerticalLayout getTagsLayout() {
        return tagsLayout;
    }

    protected TabSheet getDetailsTab() {
        return detailsTab;
    }

    protected ManagementUIState getManagementUIState() {
        return managementUIState;
    }

    private void createComponents() {
        caption = createHeaderCaption();
        caption.setImmediate(true);
        caption.setContentMode(ContentMode.HTML);
        caption.setId(getDetailsHeaderCaptionId());

        editButton = SPUIComponentProvider.getButton("", "", "", null, false, FontAwesome.PENCIL_SQUARE_O,
                SPUIButtonStyleSmallNoBorder.class);
        editButton.setId(getEditButtonId());
        editButton.addClickListener(this::onEdit);
        editButton.setEnabled(false);

        manageMetadataBtn = SPUIComponentProvider.getButton("", "", "", null, false, FontAwesome.LIST_ALT,
                SPUIButtonStyleSmallNoBorder.class);
        manageMetadataBtn.setId(getEditButtonId());
        manageMetadataBtn.setDescription(i18n.getMessage("tooltip.metadata.icon"));
        manageMetadataBtn.addClickListener(this::showMetadata);
        manageMetadataBtn.setEnabled(false);

        detailsTab = SPUIComponentProvider.getDetailsTabSheet();
        detailsTab.setImmediate(true);
        detailsTab.setWidth(98, Unit.PERCENTAGE);
        detailsTab.setHeight(90, Unit.PERCENTAGE);
        detailsTab.addStyleName(SPUIStyleDefinitions.DETAILS_LAYOUT_STYLE);
        detailsTab.setId(getTabSheetId());
    }

    private void buildLayout() {
        final HorizontalLayout nameEditLayout = new HorizontalLayout();
        nameEditLayout.setWidth(100.0F, Unit.PERCENTAGE);
        nameEditLayout.addComponent(caption);
        nameEditLayout.setComponentAlignment(caption, Alignment.TOP_LEFT);
        if (hasEditPermission()) {
            nameEditLayout.addComponent(editButton);
            nameEditLayout.setComponentAlignment(editButton, Alignment.TOP_RIGHT);
            if (isMetadataIconToBeDisplayed()) {
                nameEditLayout.addComponent(manageMetadataBtn);
                nameEditLayout.setComponentAlignment(manageMetadataBtn, Alignment.TOP_RIGHT);
            }
        }
        nameEditLayout.setExpandRatio(caption, 1.0F);
        nameEditLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);

        addComponent(nameEditLayout);
        setComponentAlignment(nameEditLayout, Alignment.TOP_CENTER);
        addComponent(detailsTab);
        setComponentAlignment(nameEditLayout, Alignment.TOP_CENTER);

        setSizeFull();
        setHeightUndefined();
        addStyleName(SPUIStyleDefinitions.WIDGET_STYLE);
    }

    private Label createHeaderCaption() {
        return new LabelBuilder().name(getDefaultCaption()).buildCaptionLabel();
    }

    /**
     * If there is no data in table (i.e. no row selected), then disable the
     * edit button. If row is selected, enable edit button.
     */
    private void populateData(final T selectedBaseEntity) {
        this.selectedBaseEntity = selectedBaseEntity;
        editButton.setEnabled(selectedBaseEntity != null);
        manageMetadataBtn.setEnabled(selectedBaseEntity != null);
        if (selectedBaseEntity == null) {
            setName(getDefaultCaption(), "");
        } else {
            setName(getDefaultCaption(), getName());
        }
        populateLog();
        populateDescription();
        populateDetailsWidget();
    }

    private void populateDescription() {
        if (selectedBaseEntity != null) {
            updateDescriptionLayout(selectedBaseEntity.getDescription());
        } else {
            updateDescriptionLayout(null);
        }
    }

    protected Long getSelectedBaseEntityId() {
        return selectedBaseEntity == null ? null : selectedBaseEntity.getId();
    }

    protected abstract void populateDetailsWidget();

    protected abstract void populateMetadataDetails();

    /**
     * Default caption of header to be displayed when no data row selected in
     * table.
     * 
     * @return String
     */
    protected abstract String getDefaultCaption();

    /**
     * Click listener for edit button.
     * 
     * @param event
     */
    protected abstract void onEdit(Button.ClickEvent event);

    protected abstract String getEditButtonId();

    protected abstract boolean onLoadIsTableMaximized();

    protected abstract String getTabSheetId();

    protected abstract boolean hasEditPermission();

    protected abstract String getDetailsHeaderCaptionId();

    protected abstract String getName();

    protected abstract boolean isMetadataIconToBeDisplayed();

    protected abstract void showMetadata(Button.ClickEvent event);

}
