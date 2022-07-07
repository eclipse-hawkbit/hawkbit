/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;

/**
 * If footer support is enabled, the footer is placed below the component
 */
public abstract class AbstractFooterSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFooterSupport.class);

    protected final VaadinMessageSource i18n;
    private final UINotification notification;

    protected final Label countLabel;

    private final ExecutorService countExecutor;
    private Future<?> currentCountCalculation;
    private Future<?> currentCountDetailsCalculation;

    protected AbstractFooterSupport(final VaadinMessageSource i18n, final UINotification notification) {
        this.i18n = i18n;
        this.notification = notification;

        this.countLabel = new Label();
        this.countExecutor = Executors.newSingleThreadExecutor();

        init();
    }

    /**
     * Init footer message label, can be overriden to adapt label styling.
     *
     */
    protected void init() {
        countLabel.setId(UIComponentIdProvider.COUNT_LABEL);
        countLabel.addStyleName(SPUIStyleDefinitions.SP_LABEL_MESSAGE_STYLE);

        countLabel.addDetachListener(e -> {
            abortCurrentCountCalculation();
            abortCurrentDetailsCountCalculation();
        });
    }

    /**
     * Creates a sub-layout for the footer.
     *
     * @return the footer sub-layout.
     */
    public Layout createFooterMessageComponent() {
        final HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setMargin(false);
        footerLayout.setSpacing(false);
        footerLayout.setWidth(100, Unit.PERCENTAGE);

        footerLayout.addComponent(countLabel);

        return footerLayout;
    }

    /**
     * Calculates count asynchronously and updated the count label.
     *
     * @param countValueUpdater
     *                          callback to update count value
     * @param countUiUpdater
     *                          callback to update count label in UI
     */
    protected void updateCountAsynchronously(final Runnable countValueUpdater, final Runnable countUiUpdater) {
        abortCurrentCountCalculation();
        countLabel.setCaption(i18n.getMessage("label.calculating"));

        currentCountCalculation = submitAsynchronousCountUpdate(countValueUpdater, countUiUpdater);
    }

    private Future<?> submitAsynchronousCountUpdate(final Runnable countValueUpdater, final Runnable countUiUpdater) {
        final UI ui = UI.getCurrent();
        final SecurityContext securityContext = SecurityContextHolder.getContext();

        return countExecutor.submit(() -> {
            try {
                LOG.trace("Started calculating count asynchronously");
                SecurityContextHolder.setContext(securityContext);
                countValueUpdater.run();

                LOG.trace("Finished calculating count asynchronously, updating UI");
                ui.access(countUiUpdater);
            } catch (final Exception ex) {
                LOG.error("Error occurred during asynchronous count calculation", ex);
                ui.access(() -> notification
                        .displayValidationError(i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_COUNT_FAILED)));
            }
        });
    }

    /**
     * Calculates count details asynchronously and updated the details count
     * label.
     *
     * @param countDetailsValueUpdater
     *                                 callback to update details count value
     * @param countDetailsUiUpdater
     *                                 callback to update details count label in UI
     */
    protected void updateCountDetailsAsynchronously(final Runnable countDetailsValueUpdater,
            final Runnable countDetailsUiUpdater) {
        abortCurrentDetailsCountCalculation();
        countLabel.setValue(i18n.getMessage("label.calculating"));

        currentCountDetailsCalculation = submitAsynchronousCountUpdate(countDetailsValueUpdater, countDetailsUiUpdater);
    }

    private void abortCurrentCountCalculation() {
        if (currentCountCalculation != null && !currentCountCalculation.isCancelled()) {
            currentCountCalculation.cancel(true);
            currentCountCalculation = null;
        }
    }

    private void abortCurrentDetailsCountCalculation() {
        if (currentCountDetailsCalculation != null && !currentCountDetailsCalculation.isCancelled()) {
            currentCountDetailsCalculation.cancel(true);
            currentCountDetailsCalculation = null;
        }
    }

}