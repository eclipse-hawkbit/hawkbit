package org.eclipse.hawkbit.ui.tenantconfiguration.polling;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.ConfigurationItem;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;

/**
 * The DurationConfigField consists of three vaadin fields. A {@link #Label}
 * {@link #DurationField} and a {@link #CheckBox}. The user can then enter a
 * duration in the DurationField or he can configure using the global duration
 * by changing the CheckBox.
 */
@SpringComponent
@ViewScope
@Scope("prototype")
public class DurationConfigField extends GridLayout implements ValueChangeListener, ConfigurationItem {

    private static final long serialVersionUID = 1L;

    private final List<ConfigurationItemChangeListener> configurationChangeListeners = new ArrayList<>();

    private CheckBox checkBox;
    private DurationField durationField;

    private Duration globalDuration;

    @Autowired
    private I18N i18n;

    /**
     * sets i18n
     * 
     * @param i18n
     */
    public void setI18n(I18N i18n) {
        this.i18n = i18n;
    }

    public DurationConfigField() {
        super(3, 2);
    }

    /**
     * Initialize Authentication Configuration layout.
     */
    @PostConstruct
    public void init() {

        this.addStyleName("duration-config-field");
        this.setSpacing(true);
        this.setImmediate(true);
        this.setColumnExpandRatio(1, 1.0F);
        // gridLayout.setSizeFull();

        checkBox = new CheckBox();

        this.addComponent(checkBox, 0, 0);
        this.setComponentAlignment(checkBox, Alignment.MIDDLE_LEFT);

        Label customValue = SPUIComponentProvider.getLabel(i18n.get("configuration.polling.custom.value"),
                SPUILabelDefinitions.SP_LABEL_SIMPLE);
        this.addComponent(customValue, 1, 0);
        this.setComponentAlignment(customValue, Alignment.MIDDLE_LEFT);

        durationField = new DurationField();

        this.addComponent(durationField, 2, 0);
        this.setComponentAlignment(durationField, Alignment.MIDDLE_LEFT);

        checkBox.addValueChangeListener(this);
    }

    @Override
    public void valueChange(ValueChangeEvent event) {
        if (event.getProperty() == checkBox) {
            if (checkBox.getValue()) {
                durationField.setEnabled(true);
            } else {
                durationField.setDuration(globalDuration);
                durationField.setEnabled(false);
            }
        }
        notifyConfigurationChanged();
    }

    /**
     * sets all mandatitory attributes for correct user interaction
     * 
     * @param caption
     *            the caption of the field
     * 
     * @param tenantDuration
     *            tenant specific duration value
     * @param globalDuration
     *            duration value which is stored in the global configuration
     */
    public void setInitValues(String caption, @NotNull Duration tenantDuration, @NotNull Duration globalDuration) {
        this.setCaption(caption);
        this.globalDuration = globalDuration;

        this.setValue(tenantDuration);
    }

    /**
     * sets the allowed range of the duration values
     * 
     * @param minimumDuration
     *            minimum allowed duration value
     * @param maximumDuration
     *            maximum allowed duration value
     */
    public void setAllowedRange(Duration minimumDuration, Duration maximumDuration) {
        durationField.setMinimumDuration(minimumDuration);
        durationField.setMaximumDuration(maximumDuration);

    }

    /**
     * Set the value of the duration field
     * 
     * @param tenantDuration
     *            duration which will be set in to the duration field, when
     *            {@code null} the global configuration will be used.
     */
    public void setValue(Duration tenantDuration) {
        if (tenantDuration == null) {
            // no tenant specific configuration
            checkBox.setValue(false);
            durationField.setDuration(globalDuration);
            durationField.setEnabled(false);
        } else {
            checkBox.setValue(true);
            durationField.setDuration(tenantDuration);
            durationField.setEnabled(true);
        }
    }

    /**
     * @return the duration of the duration field or null, when the user has
     *         configured to use the global value.
     */
    public Duration getValue() {
        if (checkBox.getValue()) {
            return durationField.getDuration();
        }
        return null;
    }

    @Override
    public boolean isUserInputValid() {
        return !checkBox.getValue() || (durationField.isValid() && durationField.getValue() != null);
    }

    private void notifyConfigurationChanged() {
        configurationChangeListeners.forEach(listener -> listener.configurationHasChanged());
    }

    @Override
    public void addChangeListener(final ConfigurationItemChangeListener listener) {
        configurationChangeListeners.add(listener);
    }
}
