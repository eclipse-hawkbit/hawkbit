/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract header for master entity aware grids.
 *
 * @param <T>
 *            Generic type
 */
public abstract class AbstractMasterAwareGridHeader<T> extends AbstractGridHeader
        implements MasterEntityAwareComponent<T> {
    private static final long serialVersionUID = 1L;

    private final Label entityDetailsCaption;
    private final Label masterEntityDetailsCaption;

    /**
     * Constructor for AbstractMasterAwareGridHeader
     *
     * @param i18n
     *            VaadinMessageSource
     * @param permChecker
     *            SpPermissionChecker
     * @param eventBus
     *            UIEventBus
     */
    protected AbstractMasterAwareGridHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus) {
        super(i18n, permChecker, eventBus);

        this.entityDetailsCaption = buildEntityDetailsCaption();
        this.masterEntityDetailsCaption = buildMasterEntityDetailsCaption();
    }

    private Label buildEntityDetailsCaption() {
        final Label caption = new Label(i18n.getMessage(getEntityDetailsCaptionMsgKey()));

        caption.addStyleName(ValoTheme.LABEL_SMALL);
        caption.addStyleName(ValoTheme.LABEL_BOLD);

        return caption;
    }

    protected abstract String getEntityDetailsCaptionMsgKey();

    private Label buildMasterEntityDetailsCaption() {
        final Label caption = new Label();

        caption.setId(getMasterEntityDetailsCaptionId());
        caption.setWidthFull();
        caption.addStyleName(ValoTheme.LABEL_SMALL);
        caption.addStyleName(ValoTheme.LABEL_BOLD);
        caption.addStyleName("text-bold");
        caption.addStyleName("text-cut");

        return caption;
    }

    protected abstract String getMasterEntityDetailsCaptionId();

    @Override
    protected Component getHeaderCaption() {
        final HorizontalLayout masterAwareCaptionLayout = new HorizontalLayout();
        masterAwareCaptionLayout.setMargin(false);
        masterAwareCaptionLayout.setSpacing(true);
        masterAwareCaptionLayout.setSizeFull();
        masterAwareCaptionLayout.addStyleName("header-caption");

        masterAwareCaptionLayout.addComponent(entityDetailsCaption);
        masterAwareCaptionLayout.setComponentAlignment(entityDetailsCaption, Alignment.TOP_LEFT);
        masterAwareCaptionLayout.setExpandRatio(entityDetailsCaption, 0.0F);

        masterAwareCaptionLayout.addComponent(masterEntityDetailsCaption);
        masterAwareCaptionLayout.setComponentAlignment(masterEntityDetailsCaption, Alignment.TOP_LEFT);
        masterAwareCaptionLayout.setExpandRatio(masterEntityDetailsCaption, 1.0F);

        return masterAwareCaptionLayout;
    }

    @Override
    public void masterEntityChanged(final T masterEntity) {
        final String masterEntityName = masterEntity != null ? getMasterEntityName(masterEntity) : "";

        if (StringUtils.hasText(masterEntityName)) {
            entityDetailsCaption.setValue(i18n.getMessage(getEntityDetailsCaptionOfMsgKey()));
            masterEntityDetailsCaption.setValue(masterEntityName);
        } else {
            entityDetailsCaption.setValue(i18n.getMessage(getEntityDetailsCaptionMsgKey()));
            masterEntityDetailsCaption.setValue("");
        }
    }

    protected abstract String getMasterEntityName(final T masterEntity);

    protected abstract String getEntityDetailsCaptionOfMsgKey();
}
