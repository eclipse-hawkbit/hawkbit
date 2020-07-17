/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.providers.ActionStatusMsgDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyActionStatus;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMessage;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.MasterEntitySupport;
import org.eclipse.hawkbit.ui.common.grid.support.SelectionSupport;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.provider.Query;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This grid presents the messages for a selected action-status.
 */
public class ActionStatusMsgGrid extends AbstractGrid<ProxyMessage, Long> {
    private static final long serialVersionUID = 1L;

    private static final String MSG_ID = "id";
    private static final String VALUE_ID = "msgValue";

    private final transient MasterEntitySupport<ProxyActionStatus> masterEntitySupport;

    private final Registration itemClickListenerRegistration;

    /**
     * Constructor.
     *
     * @param i18n
     * @param eventBus
     */
    protected ActionStatusMsgGrid(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final DeploymentManagement deploymentManagement) {
        super(i18n, eventBus, null);

        setSelectionSupport(new SelectionSupport<ProxyMessage>(this));
        getSelectionSupport().enableSingleSelection();

        addStyleName(SPUIStyleDefinitions.ACTION_HISTORY_MESSAGE_GRID);

        setDetailsGenerator(this::generateDetails);

        this.itemClickListenerRegistration = addItemClickListener(event -> {
            final ProxyMessage msg = event.getItem();
            setDetailsVisible(msg, !isDetailsVisible(msg));
        });

        setFilterSupport(new FilterSupport<>(
                new ActionStatusMsgDataProvider(deploymentManagement, i18n.getMessage("message.no.available")),
                this::hideAllDetails));
        initFilterMappings();

        this.masterEntitySupport = new MasterEntitySupport<>(getFilterSupport());

        init();
    }

    private void hideAllDetails() {
        getDataProvider().fetch(new Query<>()).forEach(msg -> setDetailsVisible(msg, false));
    }

    private void initFilterMappings() {
        getFilterSupport().<Long> addMapping(FilterType.MASTER,
                (filter, masterFilter) -> getFilterSupport().setFilter(masterFilter));
    }

    private Component generateDetails(final ProxyMessage msg) {
        final TextArea textArea = new TextArea();

        textArea.addStyleName(ValoTheme.TEXTAREA_BORDERLESS);
        textArea.addStyleName(ValoTheme.TEXTAREA_TINY);
        textArea.addStyleName("inline-icon");

        textArea.setHeight(120, Unit.PIXELS);
        textArea.setWidth(100, Unit.PERCENTAGE);

        textArea.setValue(msg.getMessage());
        textArea.setReadOnly(Boolean.TRUE);

        return textArea;
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.ACTION_HISTORY_MESSAGE_GRID_ID;
    }

    @Override
    public void addColumns() {
        GridComponentBuilder.addColumn(this, ProxyMessage::getId).setId(MSG_ID).setCaption("##").setExpandRatio(0)
                .setHidable(false).setHidden(false).setMinimumWidthFromContent(true);

        GridComponentBuilder.addColumn(this, ProxyMessage::getMessage).setId(VALUE_ID)
                .setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_MESSAGES)).setHidable(false)
                .setHidden(false);

        setFrozenColumnCount(2);
    }

    /**
     * @return Master entity support
     */
    public MasterEntitySupport<ProxyActionStatus> getMasterEntitySupport() {
        return masterEntitySupport;
    }

    @PreDestroy
    void destroy() {
        itemClickListenerRegistration.remove();
    }
}
