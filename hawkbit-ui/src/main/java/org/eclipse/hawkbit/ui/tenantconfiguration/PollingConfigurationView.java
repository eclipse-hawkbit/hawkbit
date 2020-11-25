/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigPolling;
import org.eclipse.hawkbit.ui.tenantconfiguration.polling.DurationConfigField;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * View to configure the polling interval and the overdue time.
 */
public class PollingConfigurationView extends BaseConfigurationView<ProxySystemConfigPolling> {

    private static final long serialVersionUID = 1L;

    private static final ZoneId ZONEID_UTC = ZoneId.of("+0");

    private final VaadinMessageSource i18n;
    private final ControllerPollProperties controllerPollProperties;

    PollingConfigurationView(final VaadinMessageSource i18n, final ControllerPollProperties controllerPollProperties,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        super(tenantConfigurationManagement);
        this.controllerPollProperties = controllerPollProperties;
        this.i18n = i18n;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        init();
    }

    private void init() {

        final Duration minDuration = DurationHelper
                .formattedStringToDuration(controllerPollProperties.getMinPollingTime());
        final Duration maxDuration = DurationHelper
                .formattedStringToDuration(controllerPollProperties.getMaxPollingTime());
        final Duration globalPollTime = DurationHelper.formattedStringToDuration(getTenantConfigurationManagement()
                .getGlobalConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class));
        final Duration globalOverdueTime = DurationHelper.formattedStringToDuration(getTenantConfigurationManagement()
                .getGlobalConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class));

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();
        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setSpacing(false);
        vLayout.setMargin(true);

        final Label headerDisSetType = new Label(i18n.getMessage("configuration.polling.title"));
        headerDisSetType.addStyleName("config-panel-header");
        vLayout.addComponent(headerDisSetType);

        DurationConfigField fieldPollTime = DurationConfigField
                .builder(UIComponentIdProvider.SYSTEM_CONFIGURATION_POLLING, i18n)
                .caption(i18n.getMessage("configuration.polling.time"))
                .checkBoxTooltip(i18n.getMessage("configuration.polling.custom.value")).range(minDuration, maxDuration)
                .globalDuration(globalPollTime).build();

        getBinder().forField(fieldPollTime.getCheckBox()).bind(ProxySystemConfigPolling::isPollingTime,
                ProxySystemConfigPolling::setPollingTime);

        getBinder().forField(fieldPollTime.getDurationField())
                .withConverter(PollingConfigurationView::localDateTimeToDuration,
                        PollingConfigurationView::durationToLocalDateTime)
                .bind(ProxySystemConfigPolling::getPollingTimeDuration,
                        ProxySystemConfigPolling::setPollingTimeDuration);

        vLayout.addComponent(fieldPollTime);

        DurationConfigField fieldPollingOverdueTime = DurationConfigField
                .builder(UIComponentIdProvider.SYSTEM_CONFIGURATION_OVERDUE, i18n)
                .caption(i18n.getMessage("configuration.polling.overduetime"))
                .checkBoxTooltip(i18n.getMessage("configuration.polling.custom.value")).range(minDuration, maxDuration)
                .globalDuration(globalOverdueTime).build();
        getBinder().forField(fieldPollingOverdueTime.getCheckBox()).bind(ProxySystemConfigPolling::isPollingOverdue,
                ProxySystemConfigPolling::setPollingOverdue);

        getBinder().forField(fieldPollingOverdueTime.getDurationField())
                .withConverter(PollingConfigurationView::localDateTimeToDuration,
                        PollingConfigurationView::durationToLocalDateTime)
                .bind(ProxySystemConfigPolling::getPollingOverdueDuration,
                        ProxySystemConfigPolling::setPollingOverdueDuration);

        vLayout.addComponent(fieldPollingOverdueTime);

        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    private static Duration localDateTimeToDuration(final LocalDateTime date) {
        final LocalTime endExclusive = LocalDateTime.ofInstant(date.toInstant(ZoneOffset.UTC), ZONEID_UTC)
                .toLocalTime();
        return Duration.between(LocalTime.MIDNIGHT, LocalTime.from(endExclusive));
    }

    private static LocalDateTime durationToLocalDateTime(final Duration duration) {
        final LocalTime lt = LocalTime.ofNanoOfDay(duration.toNanos());
        final Date date = Date.from(lt.atDate(LocalDate.now(ZONEID_UTC)).atZone(ZONEID_UTC).toInstant());
        return LocalDateTime.ofInstant(date.toInstant(), ZONEID_UTC);
    }

    @Override
    protected ProxySystemConfigPolling populateSystemConfig() {
        ProxySystemConfigPolling configBean = new ProxySystemConfigPolling();
        final TenantConfigurationValue<String> pollingTimeConfValue = getTenantConfigurationManagement()
                .getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class);
        configBean.setPollingTime(!pollingTimeConfValue.isGlobal());
        configBean.setPollingTimeDuration(DurationHelper.formattedStringToDuration(pollingTimeConfValue.getValue()));

        final TenantConfigurationValue<String> overdueTimeConfValue = getTenantConfigurationManagement()
                .getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class);
        configBean.setPollingOverdue(!overdueTimeConfValue.isGlobal());
        configBean.setPollingOverdueDuration(DurationHelper.formattedStringToDuration(overdueTimeConfValue.getValue()));

        return configBean;
    }

    @Override
    public void save() {
        if (getBinderBean().isPollingTime()) {
            getTenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.POLLING_TIME_INTERVAL,
                    DurationHelper.durationToFormattedString(getBinderBean().getPollingTimeDuration()));
        } else {
            getTenantConfigurationManagement().deleteConfiguration(TenantConfigurationKey.POLLING_TIME_INTERVAL);
        }

        if (getBinderBean().isPollingOverdue()) {
            getTenantConfigurationManagement().addOrUpdateConfiguration(
                    TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL,
                    DurationHelper.durationToFormattedString(getBinderBean().getPollingOverdueDuration()));
        } else {
            getTenantConfigurationManagement()
                    .deleteConfiguration(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL);
        }
    }
}
