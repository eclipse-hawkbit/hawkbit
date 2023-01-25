/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.builder;

import com.vaadin.data.Binder;
import com.vaadin.data.Binder.Binding;
import com.vaadin.data.Binder.BindingBuilder;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.Setter;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Type;
import org.eclipse.hawkbit.ui.common.data.aware.ActionTypeAware;
import org.eclipse.hawkbit.ui.common.data.aware.DescriptionAware;
import org.eclipse.hawkbit.ui.common.data.aware.DsIdAware;
import org.eclipse.hawkbit.ui.common.data.aware.NameAware;
import org.eclipse.hawkbit.ui.common.data.aware.StartOptionAware;
import org.eclipse.hawkbit.ui.common.data.aware.TargetFilterQueryAware;
import org.eclipse.hawkbit.ui.common.data.aware.TypeInfoAware;
import org.eclipse.hawkbit.ui.common.data.aware.VersionAware;
import org.eclipse.hawkbit.ui.common.data.providers.AbstractProxyDataProvider;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetStatelessDataProvider;
import org.eclipse.hawkbit.ui.common.data.providers.TargetFilterQueryDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSetInfo;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQueryInfo;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupAssignmentLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

/**
 * Builder class for from components
 */
public final class FormComponentBuilder {
    public static final String TEXTFIELD_NAME = "textfield.name";
    public static final String TEXTFIELD_VERSION = "textfield.version";
    public static final String TEXTFIELD_DESCRIPTION = "textfield.description";

    public static final String PROMPT_DISTRIBUTION_SET = "prompt.distribution.set";
    public static final String PROMPT_TARGET_FILTER = "prompt.target.filter";

    public static final String CAPTION_TYPE = "caption.type";

    private FormComponentBuilder() {
    }

    /**
     * Create an input field for a name
     * 
     * @param <T>
     *            type of the binder
     * @param binder
     *            that is bound to the field
     * @param i18n
     *            message source
     * @param fieldId
     *            id of the field
     * @return the TextField with its Binding
     */
    public static <T extends NameAware> BoundComponent<TextField> createNameInput(final Binder<T> binder,
                                                                                  final VaadinMessageSource i18n, final String fieldId) {
        final TextField nameInput = new TextFieldBuilder(NamedEntity.NAME_MAX_SIZE).id(fieldId)
              .caption(i18n.getMessage(TEXTFIELD_NAME)).prompt(i18n.getMessage(TEXTFIELD_NAME)).buildTextComponent();
        nameInput.setSizeUndefined();

        final Binding<T, String> binding = binder.forField(nameInput)
              .asRequired(i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_NAMEREQUIRED))
              .bind(NameAware::getName, NameAware::setName);

        return new BoundComponent<>(nameInput, binding);
    }

    /**
     * Create a required input field for a version
     * 
     * @param <T>
     *            type of the binder
     * @param binder
     *            that is bound to the field
     * @param i18n
     *            message source
     * @param fieldId
     *            id of the field
     * @return the TextField
     */
    public static <T extends VersionAware> BoundComponent<TextField> createVersionInput(final Binder<T> binder,
            final VaadinMessageSource i18n, final String fieldId) {
        final TextField versionInput = new TextFieldBuilder(NamedVersionedEntity.VERSION_MAX_SIZE).id(fieldId)
                .caption(i18n.getMessage(TEXTFIELD_VERSION)).prompt(i18n.getMessage(TEXTFIELD_VERSION))
                .buildTextComponent();
        versionInput.setSizeUndefined();

        final Binding<T, String> binding = binder.forField(versionInput)
                .asRequired(i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_VERSIONREQUIRED))
                .bind(VersionAware::getVersion, VersionAware::setVersion);

        return new BoundComponent<>(versionInput, binding);
    }

    /**
     * Create an optional input field for a description
     * 
     * @param <T>
     *            type of the binder
     * @param binder
     *            that is bound to the field
     * @param i18n
     *            message source
     * @param fieldId
     *            id of the field
     * @return the TextArea
     */
    public static <T extends DescriptionAware> BoundComponent<TextArea> createDescriptionInput(final Binder<T> binder,
            final VaadinMessageSource i18n, final String fieldId) {
        return createBigTextInput(binder, i18n, fieldId, TEXTFIELD_DESCRIPTION, TEXTFIELD_DESCRIPTION,
                DescriptionAware::getDescription, DescriptionAware::setDescription);
    }

    public static <T> BoundComponent<TextArea> createBigTextInput(final Binder<T> binder,
            final VaadinMessageSource i18n, final String fieldId, final String caption, final String prompt,
            ValueProvider<T, String> getter, Setter<T, String> setter) {
        final TextArea descriptionInput = new TextAreaBuilder(NamedEntity.DESCRIPTION_MAX_SIZE).id(fieldId)
                .caption(i18n.getMessage(caption)).prompt(i18n.getMessage(prompt)).style("text-area-style")
                .buildTextComponent();
        descriptionInput.setSizeUndefined();

        final Binding<T, String> binding = binder.forField(descriptionInput).bind(getter, setter);

        return new BoundComponent<>(descriptionInput, binding);
    }

    /**
     * Create a bound {@link ActionTypeOptionGroupAssignmentLayout}
     * 
     * @param <T>
     *            type of the binder
     * @param binder
     *            that is bound to the layout
     * @param i18n
     *            message source
     * @param componentId
     *            id of the input layout
     * @return a bound layout
     */
    public static <T extends ActionTypeAware> BoundComponent<ActionTypeOptionGroupAssignmentLayout> createActionTypeOptionGroupLayout(
            final Binder<T> binder, final VaadinMessageSource i18n, final String componentId) {
        final ActionTypeOptionGroupAssignmentLayout actionTypeOptionGroupLayout = new ActionTypeOptionGroupAssignmentLayout(
                i18n, componentId);

        binder.forField(actionTypeOptionGroupLayout.getActionTypeOptionGroup()).bind(ActionTypeAware::getActionType,
                ActionTypeAware::setActionType);

        final Binding<T, Long> binding = binder.forField(actionTypeOptionGroupLayout.getForcedTimeDateField())
                .asRequired(i18n.getMessage("message.forcedTime.cannotBeEmpty")).withConverter(localDateTime -> {
                    if (localDateTime == null) {
                        return null;
                    }

                    return SPDateTimeUtil.localDateTimeToEpochMilli(localDateTime);
                }, forcedTime -> {
                    if (forcedTime == null) {
                        return null;
                    }

                    return SPDateTimeUtil.epochMilliToLocalDateTime(forcedTime);
                }).bind(ActionTypeAware::getForcedTime, ActionTypeAware::setForcedTime);

        return new BoundComponent<>(actionTypeOptionGroupLayout, binding);
    }

    /**
     * Create a bound {@link AutoStartOptionGroupLayout}
     * 
     * @param <T>
     *            type of the binder
     * @param binder
     *            that is bound to the layout
     * @param i18n
     *            message source
     * @param componentId
     *            id of the input layout
     * @return a bound layout
     */
    public static <T extends StartOptionAware> BoundComponent<AutoStartOptionGroupLayout> createAutoStartOptionGroupLayout(
            final Binder<T> binder, final VaadinMessageSource i18n, final String componentId) {
        final AutoStartOptionGroupLayout autoStartOptionGroup = new AutoStartOptionGroupLayout(i18n, componentId);

        binder.forField(autoStartOptionGroup.getAutoStartOptionGroup()).bind(StartOptionAware::getStartOption,
                StartOptionAware::setStartOption);

        final Binding<T, Long> binding = binder.forField(autoStartOptionGroup.getStartAtDateField())
                .asRequired(i18n.getMessage("message.scheduledTime.cannotBeEmpty")).withConverter(localDateTime -> {
                    if (localDateTime == null) {
                        return null;
                    }

                    return SPDateTimeUtil.localDateTimeToEpochMilli(localDateTime);
                }, startAtTime -> {
                    if (startAtTime == null) {
                        return null;
                    }

                    return SPDateTimeUtil.epochMilliToLocalDateTime(startAtTime);
                }).bind(StartOptionAware::getStartAt, StartOptionAware::setStartAt);

        return new BoundComponent<>(autoStartOptionGroup, binding);
    }

    /**
     * create a bound input for distribution sets
     * 
     * @param <T>
     *            type of the binder
     * @param binder
     *            that is bound to the input
     * @param dataProvider
     *            provides distribution sets
     * @param i18n
     *            i18n
     * @param componentId
     *            id of the input layout
     * @return bound ComboBox of distribution sets
     */
    public static <T extends DsIdAware> BoundComponent<ComboBox<ProxyDistributionSet>> createDistributionSetComboBox(
            final Binder<T> binder, final DistributionSetStatelessDataProvider dataProvider,
            final VaadinMessageSource i18n, final String componentId) {
        final ComboBox<ProxyDistributionSet> dsComboBox = SPUIComponentProvider.getComboBox(componentId,
                i18n.getMessage(UIMessageIdProvider.HEADER_DISTRIBUTION_SET), i18n.getMessage(PROMPT_DISTRIBUTION_SET),
                i18n.getMessage(PROMPT_DISTRIBUTION_SET), false, ProxyDistributionSet::getNameVersion, dataProvider);

        final Binding<T, ProxyDistributionSetInfo> binding = binder.forField(dsComboBox)
                .asRequired(i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_DISTRIBUTIONSET_REQUIRED))
                .withConverter(ds -> {
                    if (ds == null) {
                        return null;
                    }

                    return ds.getInfo();
                }, dsInfo -> {
                    if (dsInfo == null) {
                        return null;
                    }

                    return ProxyDistributionSet.of(dsInfo);
                }).bind(DsIdAware::getDistributionSetInfo, DsIdAware::setDistributionSetInfo);

        return new BoundComponent<>(dsComboBox, binding);
    }

    /**
     * create a bound input for target filter queries.
     * 
     * @param <T>
     *            type of the binder
     * @param binder
     *            that is bound to the input
     * @param validator
     *            Target filter query validator
     * @param dataProvider
     *            provides target filter queries
     * @param i18n
     *            i18n
     * @param componentId
     *            id of the input layout
     * @return bound ComboBox of target filter queries
     */
    public static <T extends TargetFilterQueryAware> BoundComponent<ComboBox<ProxyTargetFilterQuery>> createTargetFilterQueryCombo(
            final Binder<T> binder, final Validator<ProxyTargetFilterQuery> validator,
            final TargetFilterQueryDataProvider dataProvider, final VaadinMessageSource i18n,
            final String componentId) {
        final ComboBox<ProxyTargetFilterQuery> tfqCombo = SPUIComponentProvider.getComboBox(componentId, null,
                i18n.getMessage(PROMPT_TARGET_FILTER), i18n.getMessage(PROMPT_TARGET_FILTER), true,
                ProxyTargetFilterQuery::getName, dataProvider);

        final BindingBuilder<T, ProxyTargetFilterQuery> bindingBuilder = binder.forField(tfqCombo)
                .asRequired(i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_TFQ_REQUIRED));
        if (validator != null) {
            bindingBuilder.withValidator(validator);
        }

        final Binding<T, ProxyTargetFilterQueryInfo> binding = bindingBuilder.withConverter(tfq -> {
            if (tfq == null) {
                return null;
            }

            return tfq.getInfo();
        }, tfqInfo -> {
            if (tfqInfo == null) {
                return null;
            }

            return ProxyTargetFilterQuery.of(tfqInfo);
        }).bind(TargetFilterQueryAware::getTargetFilterQueryInfo, TargetFilterQueryAware::setTargetFilterQueryInfo);

        return new BoundComponent<>(tfqCombo, binding);
    }

    /**
     * create a bound input for sm/ds types.
     * 
     * @param <T>
     *            type of the binder
     * @param binder
     *            that is bound to the input
     * @param dataProvider
     *            provides types
     * @param i18n
     *            i18n
     * @param componentId
     *            id of the input layout
     * @return bound ComboBox of sm/ds types
     */
    public static <T extends TypeInfoAware> BoundComponent<ComboBox<ProxyTypeInfo>> createTypeCombo(
            final Binder<T> binder, final AbstractProxyDataProvider<ProxyTypeInfo, ?, String> dataProvider,
            final VaadinMessageSource i18n, final String componentId, final boolean isRequired) {
        final ComboBox<ProxyTypeInfo> typeCombo = SPUIComponentProvider.getComboBox(componentId,
                i18n.getMessage(CAPTION_TYPE), i18n.getMessage(CAPTION_TYPE), i18n.getMessage(CAPTION_TYPE), !isRequired,
                ProxyTypeInfo::getName, dataProvider.withConvertedFilter(filterString -> filterString.trim() + "%"));

        final BindingBuilder<T, ProxyTypeInfo> bindingBuilder = binder.forField(typeCombo);

        if (isRequired){
            bindingBuilder.asRequired(i18n.getMessage("message.error.typeRequired"));
        }

        final Binding<T, ProxyTypeInfo> binding = bindingBuilder.bind(TypeInfoAware::getTypeInfo, TypeInfoAware::setTypeInfo);
        return new BoundComponent<>(typeCombo, binding);
    }

    /**
     * Create type key text field
     *
     * @param binder
     *            that is bound to the input
     * @param i18n
     *            i18n
     *
     * @return text field
     */
    public static TextField createTypeKeyField(final Binder<ProxyType> binder, final VaadinMessageSource i18n) {
        final TextField typeKey = new TextFieldBuilder(Type.KEY_MAX_SIZE).id(UIComponentIdProvider.TYPE_POPUP_KEY)
                .caption(i18n.getMessage("textfield.key")).prompt(i18n.getMessage("textfield.key"))
                .buildTextComponent();
        typeKey.setSizeUndefined();

        binder.forField(typeKey).asRequired(i18n.getMessage("message.type.key.empty")).bind(ProxyType::getKey,
                ProxyType::setKey);

        return typeKey;
    }

    /**
     * Generate a check box
     * 
     * @param <T>
     *            entity type the check box can be bound to
     * @param id
     *            id of the check box element
     * @param binder
     *            the box is bound to
     * @param getter
     *            getter for the binder
     * @param setter
     *            setter for the binder
     * @return the bound box
     */
    public static <T> CheckBox createCheckBox(final String id, final Binder<T> binder,
            final ValueProvider<T, Boolean> getter, final Setter<T, Boolean> setter) {
        return createCheckBox(null, id, binder, getter, setter);
    }

    /**
     * Generate a check box
     * 
     * @param <T>
     *            entity type the check box can be bound to
     * @param caption
     *            check box caption
     * @param id
     *            id of the check box element
     * @param binder
     *            the box is bound to
     * @param getter
     *            getter for the binder
     * @param setter
     *            setter for the binder
     * @return the bound box
     */
    public static <T> CheckBox createCheckBox(final String caption, final String id, final Binder<T> binder,
            final ValueProvider<T, Boolean> getter, final Setter<T, Boolean> setter) {
        final CheckBox checkBox;
        if (StringUtils.isEmpty(caption)) {
            checkBox = new CheckBox();
        } else {
            checkBox = new CheckBox(caption);
        }
        checkBox.setId(id);
        binder.forField(checkBox).bind(getter, setter);
        return checkBox;
    }
}
