package org.eclipse.hawkbit.ui.tenantconfiguration;

import static org.eclipse.hawkbit.repository.model.helper.PollConfigurationHelper.EXPECTED_POLLING_TIME_FORMAT;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.helper.PollConfigurationHelper;
import org.eclipse.hawkbit.ui.tenantconfiguration.polling.DurationField;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Validator;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Field;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
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
        implements ConfigurationGroup, Field.ValueChangeListener {

    private static final long serialVersionUID = 1L;

    @Autowired
    private transient SystemManagement systemManagement;

    @Autowired
    private I18N i18n;

    @Autowired
    PollConfigurationHelper pollConfigurationHelper;

    final private DurationField fieldPollingTime = new DurationField();
    final private DurationField fieldPollingOverdueTime = new DurationField();

    /**
     * Initialize Authentication Configuration layout.
     */
    @PostConstruct
    public void init() {

        Validator correctFormatValidator = new Validator() {
            private static final long serialVersionUID = 1L;

            @Override
            public void validate(Object value) throws InvalidValueException {
                if (!(value instanceof String) || !((String) value).matches(EXPECTED_POLLING_TIME_FORMAT)) {
                    throw new InvalidValueException("Not in HH:MM:SS Format.");
                }
            }
        };

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();

        rootPanel.addStyleName("config-panel");

        // TODO Better Layout than Vertical Layout - maybe a table layout?
        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setMargin(true);
        vLayout.setSizeFull();

        final Label headerDisSetType = new Label(i18n.get("configuration.polling.title"));
        headerDisSetType.addStyleName("config-panel-header");
        vLayout.addComponent(headerDisSetType);

        final Label labelPollingTime = new Label(i18n.get("configuration.polling.time"));
        vLayout.addComponent(labelPollingTime);

        vLayout.addComponent(fieldPollingTime);

        final Label labelPollingOverdueTime = new Label(i18n.get("configuration.polling.overduetime"));
        vLayout.addComponent(labelPollingOverdueTime);

        vLayout.addComponent(fieldPollingOverdueTime);

        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.data.Property.ValueChangeListener#valueChange(com.vaadin.data.
     * Property.ValueChangeEvent)
     * 
     * This method is called when a value of a textField changes. When the value
     * is not in the correct format, but has valid data, this method will change
     * the value to the correct format
     */
    @Override
    public void valueChange(ValueChangeEvent event) {

        notifyConfigurationChanged();

        if (event.getProperty() instanceof TextField) {
            TextField textfield = (TextField) event.getProperty();

            String value = textfield.getValue();

            if (value.matches("[0-9]{1,6}")) {
                value = "000000".substring(value.length()) + value;
                value = value.substring(0, 2) + ":" + value.substring(2, 4) + ":" + value.substring(4, 6);
            }

            if (value.matches("([0-5]?[0-9]?(:[0-5][0-9]){1,2})")) {
                value = "00:00:00".substring(0, 8 - value.length()) + value;
            }

            if (value.matches(EXPECTED_POLLING_TIME_FORMAT)) {
                textfield.setValue(value);
            }
        }
    }

    @Override
    public void save() {
        // TODO Auto-generated method stub
    }

    @Override
    public void undo() {

    }

    @Override
    public boolean isUserInputValid() {
        return fieldPollingTime.isValid() && fieldPollingOverdueTime.isValid();
    }

}
