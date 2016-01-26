package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.helper.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.tenantconfiguration.polling.DurationConfigField;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * View to configure the polling interval and the overdue time.
 * 
 * @author Fabian Nonnenmacher
 *
 */
@SpringComponent
@ViewScope
public class PollingConfigurationView extends BaseConfigurationView
        implements ConfigurationGroup, ConfigurationItem.ConfigurationItemChangeListener {

    private static final long serialVersionUID = 1L;

    @Autowired
    private I18N i18n;

    @Autowired
    private ControllerPollProperties controllerPollProperties;

    @Autowired
    private transient SystemManagement systemManagement;

    private DurationConfigField fieldPollTime = null;
    private DurationConfigField fieldPollingOverdueTime = null;

    private Duration minDuration;
    private Duration maxDuration;
    private Duration globalPollTime;
    private Duration globalOverdueTime;

    private Duration tenantPollTime = null;
    private Duration tenantOverdueTime = null;

    private final DurationHelper durationHelper = new DurationHelper();

    /**
     * Initialize Authentication Configuration layout.
     */
    @PostConstruct
    public void init() {

        minDuration = durationHelper.formattedStringToDuration(controllerPollProperties.getMinPollingTime());
        maxDuration = durationHelper.formattedStringToDuration(controllerPollProperties.getMaxPollingTime());
        globalPollTime = durationHelper.formattedStringToDuration(controllerPollProperties.getPollingTime());
        globalOverdueTime = durationHelper.formattedStringToDuration(controllerPollProperties.getPollingOverdueTime());

        final TenantConfigurationValue<String> pollTimeConfValue = systemManagement
                .getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class);
        if (!pollTimeConfValue.isGlobal()) {
            tenantPollTime = durationHelper.formattedStringToDuration(pollTimeConfValue.getValue());
        }

        final TenantConfigurationValue<String> overdueTimeConfValue = systemManagement
                .getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class);
        if (!overdueTimeConfValue.isGlobal()) {
            tenantOverdueTime = durationHelper.formattedStringToDuration(overdueTimeConfValue.getValue());
        }

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();
        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setMargin(true);

        final Label headerDisSetType = new Label(i18n.get("configuration.polling.title"));
        headerDisSetType.addStyleName("config-panel-header");
        vLayout.addComponent(headerDisSetType);

        fieldPollTime = DurationConfigField.builder().caption(i18n.get("configuration.polling.time"))
                .checkBoxLabel(i18n.get("configuration.polling.custom.value")).range(minDuration, maxDuration)
                .globalDuration(globalPollTime).tenantDuration(tenantPollTime).build();
        fieldPollTime.addChangeListener(this);
        vLayout.addComponent(fieldPollTime);

        fieldPollingOverdueTime = DurationConfigField.builder().caption(i18n.get("configuration.polling.overduetime"))
                .checkBoxLabel(i18n.get("configuration.polling.custom.value")).range(minDuration, maxDuration)
                .globalDuration(globalPollTime).tenantDuration(tenantOverdueTime).build();
        fieldPollingOverdueTime.addChangeListener(this);
        vLayout.addComponent(fieldPollingOverdueTime);

        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    @Override
    public void save() {
        // make sure values are only saved, when the value has been changed

        if (!compareDurations(tenantPollTime, fieldPollTime.getValue())) {
            tenantPollTime = fieldPollTime.getValue();
            systemManagement.addOrUpdateConfiguration(
                    new TenantConfiguration(TenantConfigurationKey.POLLING_TIME_INTERVAL.getKeyName(),
                            durationHelper.durationToFormattedString(tenantPollTime)));
        }

        if (!compareDurations(tenantOverdueTime, fieldPollingOverdueTime.getValue())) {
            tenantOverdueTime = fieldPollingOverdueTime.getValue();
            systemManagement.addOrUpdateConfiguration(
                    new TenantConfiguration(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL.getKeyName(),
                            durationHelper.durationToFormattedString(tenantOverdueTime)));
        }
    }

    @Override
    public void undo() {
        fieldPollTime.setValue(tenantPollTime);
        fieldPollingOverdueTime.setValue(tenantOverdueTime);
    }

    @Override
    public boolean isUserInputValid() {
        return fieldPollTime.isUserInputValid() && fieldPollingOverdueTime.isUserInputValid();
    }

    @Override
    public void configurationHasChanged() {
        notifyConfigurationChanged();
    }

    private boolean compareDurations(final Duration d1, final Duration d2) {
        if (d1 == null && d2 == null) {
            return true;
        }

        if (d1 != null) {
            return d1.equals(d2);
        }

        // d1 == null, d2 != null
        return false;
    }
}
