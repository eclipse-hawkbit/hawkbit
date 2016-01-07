package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.model.helper.PollConfigurationHelper;
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
        implements ConfigurationGroup, ConfigurationElement.ConfigurationGroupChangeListener {

    private static final long serialVersionUID = 1L;

    @Autowired
    private I18N i18n;

    @Autowired
    PollConfigurationHelper pollConfigurationHelper;

    @Autowired
    private DurationConfigField fieldPollingTime;

    @Autowired
    private DurationConfigField fieldPollingOverdueTime;

    private Duration tenantPollingTime;
    private Duration tenantPollingOverdueTime;

    /**
     * Initialize Authentication Configuration layout.
     */
    @PostConstruct
    public void init() {

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();
        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setMargin(true);
        // vLayout.setSizeFull();

        final Label headerDisSetType = new Label(i18n.get("configuration.polling.title"));
        headerDisSetType.addStyleName("config-panel-header");
        vLayout.addComponent(headerDisSetType);

        tenantPollingTime = pollConfigurationHelper.getTenantPollTimeIntervall();
        fieldPollingTime.setInitValues(i18n.get("configuration.polling.time"), tenantPollingTime,
                pollConfigurationHelper.getGlobalPollTimeInterval());
        fieldPollingTime.setAllowedRange(pollConfigurationHelper.getMinimumPollingInterval(),
                pollConfigurationHelper.getMaximumPollingInterval());
        fieldPollingTime.addChangeListener(this);

        vLayout.addComponent(fieldPollingTime);

        tenantPollingOverdueTime = pollConfigurationHelper.getTenantOverduePollTimeIntervall();

        fieldPollingOverdueTime.setInitValues(i18n.get("configuration.polling.overduetime"), tenantPollingOverdueTime,
                pollConfigurationHelper.getGlobalOverduePollTimeInterval());
        fieldPollingOverdueTime.setAllowedRange(pollConfigurationHelper.getMinimumPollingInterval(),
                pollConfigurationHelper.getMaximumPollingInterval());
        fieldPollingOverdueTime.addChangeListener(this);

        vLayout.addComponent(fieldPollingOverdueTime);

        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    @Override
    public void save() {
        // make sure values are only saved, when the value has been changed

        if (!compareDurations(tenantPollingTime, fieldPollingTime.getValue())) {
            tenantPollingTime = fieldPollingTime.getValue();
            pollConfigurationHelper.setTenantPollTimeIntervall(fieldPollingTime.getValue());
        }

        if (!compareDurations(tenantPollingOverdueTime, fieldPollingOverdueTime.getValue())) {
            tenantPollingOverdueTime = fieldPollingOverdueTime.getValue();
            pollConfigurationHelper.setTenantOverduePollTimeIntervall(fieldPollingOverdueTime.getValue());
        }
    }

    @Override
    public void undo() {
        fieldPollingTime.setValue(tenantPollingTime);
        fieldPollingOverdueTime.setValue(tenantPollingOverdueTime);
    }

    @Override
    public boolean isUserInputValid() {
        return fieldPollingTime.isUserInputValid() && fieldPollingOverdueTime.isUserInputValid();
    }

    @Override
    public void configurationChanged() {
        notifyConfigurationChanged();
    }

    private boolean compareDurations(Duration d1, Duration d2) {
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
