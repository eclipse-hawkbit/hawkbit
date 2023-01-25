/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyKeyValueDetails;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyNamedEntity;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract Layout to show the entity details.
 *
 * @param <T>
 *            Generic type of ProxyNamedEntity
 */
public abstract class AbstractGridDetailsLayout<T extends ProxyNamedEntity> extends TabSheet
        implements MasterEntityAwareComponent<T> {
    private static final long serialVersionUID = 1L;

    protected final VaadinMessageSource i18n;

    protected final Binder<T> binder;

    protected final KeyValueDetailsComponent entityDetails;
    protected final TextArea entityDescription;
    protected final KeyValueDetailsComponent logDetails;

    private final transient Collection<Entry<String, Component>> detailsComponents;

    /**
     * Constructor for AbstractGridDetailsLayout
     *
     * @param i18n
     *            VaadinMessageSource
     */
    protected AbstractGridDetailsLayout(final VaadinMessageSource i18n) {
        this.i18n = i18n;

        this.binder = new Binder<>();

        this.entityDetails = buildEntityDetails();
        this.entityDescription = buildEntityDescription();
        this.logDetails = buildLogDetails();

        this.detailsComponents = new ArrayList<>();

        init();
    }

    private void init() {
        setWidth(98, Unit.PERCENTAGE);
        setHeight(90, Unit.PERCENTAGE);

        addStyleName(ValoTheme.TABSHEET_COMPACT_TABBAR);
        addStyleName(ValoTheme.TABSHEET_FRAMED);
        addStyleName(SPUIStyleDefinitions.DETAILS_LAYOUT_STYLE);

        setId(getTabSheetId());
    }

    protected abstract String getTabSheetId();

    private KeyValueDetailsComponent buildEntityDetails() {
        final KeyValueDetailsComponent details = new KeyValueDetailsComponent();

        binder.forField(details).bind(this::getEntityDetails, null);

        return details;
    }

    protected abstract List<ProxyKeyValueDetails> getEntityDetails(final T entity);

    private TextArea buildEntityDescription() {
        final TextArea description = new TextArea();

        description.setId(getDetailsDescriptionId());
        description.setReadOnly(true);
        description.setWordWrap(true);
        description.setSizeFull();
        description.setStyleName(ValoTheme.TEXTAREA_BORDERLESS);
        description.addStyleName(ValoTheme.TEXTAREA_SMALL);
        description.addStyleName("details-description");

        binder.forField(description).bind(ProxyNamedEntity::getDescription, null);

        return description;
    }

    protected abstract String getDetailsDescriptionId();

    private KeyValueDetailsComponent buildLogDetails() {
        final KeyValueDetailsComponent logs = new KeyValueDetailsComponent();
        final String idPrefix = getLogLabelIdPrefix();

        binder.forField(logs)
                .bind(entity -> Arrays.asList(new ProxyKeyValueDetails(idPrefix + UIComponentIdProvider.CREATEDAT_ID,
                        i18n.getMessage("label.created.at"), SPDateTimeUtil.getFormattedDate(entity.getCreatedAt())),
                        new ProxyKeyValueDetails(idPrefix + UIComponentIdProvider.CREATEDBY_ID,
                                i18n.getMessage("label.created.by"),
                                entity.getCreatedBy() != null
                                        ? UserDetailsFormatter.loadAndFormatUsername(entity.getCreatedBy())
                                        : ""),
                        new ProxyKeyValueDetails(idPrefix + UIComponentIdProvider.MODIFIEDAT_ID,
                                i18n.getMessage("label.modified.date"),
                                SPDateTimeUtil.getFormattedDate(entity.getLastModifiedAt())),
                        new ProxyKeyValueDetails(idPrefix + UIComponentIdProvider.MODIFIEDBY_ID,
                                i18n.getMessage("label.modified.by"),
                                entity.getCreatedBy() != null
                                        ? UserDetailsFormatter.loadAndFormatUsername(entity.getLastModifiedBy())
                                        : "")),
                        null);

        return logs;
    }

    protected abstract String getLogLabelIdPrefix();

    protected void addDetailsComponents(final Collection<Entry<String, Component>> detailsComponents) {
        this.detailsComponents.addAll(detailsComponents);
    }

    protected void addDetailsComponent(final Entry<String, Component> detailsComponent) {
        this.detailsComponents.add(detailsComponent);
    }

    /**
     * Build grid details
     */
    public void buildDetails() {
        detailsComponents.forEach(detailsComponentEntry -> {
            final String detailsComponentCaption = detailsComponentEntry.getKey();
            final Component detailsComponent = detailsComponentEntry.getValue();

            addTab(buildTabWrapperDetailsLayout(detailsComponent), detailsComponentCaption);
        });
    }

    protected static VerticalLayout buildTabWrapperDetailsLayout(final Component detailsComponent) {
        final VerticalLayout tabWrapperDetailsLayout = new VerticalLayout();
        tabWrapperDetailsLayout.setSpacing(false);
        tabWrapperDetailsLayout.setMargin(false);
        tabWrapperDetailsLayout.setStyleName("details-layout");

        tabWrapperDetailsLayout.addComponent(detailsComponent);

        return tabWrapperDetailsLayout;
    }

    @Override
    public void masterEntityChanged(final T entity) {
        binder.setBean(entity);
    }
}
