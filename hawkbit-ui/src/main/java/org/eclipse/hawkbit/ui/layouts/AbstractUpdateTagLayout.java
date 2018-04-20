package org.eclipse.hawkbit.ui.layouts;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public abstract class AbstractUpdateTagLayout<E extends NamedEntity> extends AbstractTagLayout<E> {

    private static final long serialVersionUID = 1L;

    protected Label comboLabel;

    protected VerticalLayout comboLayout;

    protected ComboBox tagNameComboBox;

    public AbstractUpdateTagLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
    }

    @Override
    public void init() {
        super.init();
        populateTagNameCombo();
    }

    protected abstract void populateTagNameCombo();

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        comboLabel = new LabelBuilder()
                .name(i18n.getMessage("label.choose.tag", i18n.getMessage("label.choose.tag.update"))).buildLabel();
        tagNameComboBox = SPUIComponentProvider.getComboBox(null, "", null, null, false, "",
                i18n.getMessage("label.combobox.tag"));
        tagNameComboBox.addStyleName(SPUIStyleDefinitions.FILTER_TYPE_COMBO_STYLE);
        tagNameComboBox.setImmediate(true);
        tagNameComboBox.setId(UIComponentIdProvider.DIST_TAG_COMBO);
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        comboLayout = new VerticalLayout();
        comboLayout.addComponent(comboLabel);
        comboLayout.addComponent(tagNameComboBox);
        getFormLayout().addComponent(comboLayout, 0);
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        tagNameComboBox.addValueChangeListener(this::tagNameChosen);
    }

    protected void tagNameChosen(final ValueChangeEvent event) {
        final String tagSelected = (String) event.getProperty().getValue();
        if (tagSelected != null) {
            setTagDetails(tagSelected);
        } else {
            resetTagNameField();
        }
        if (isUpdateAction()) {
            window.setOrginaleValues();
        }
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

    /**
     * reset the components.
     */
    @Override
    protected void reset() {
        super.reset();
        tagNameComboBox.clear();
    }

}
