/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyKeyValueDetails;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * key value detail to component layout
 */
public class KeyValueDetailsComponent extends CustomField<List<ProxyKeyValueDetails>> {
    private static final long serialVersionUID = 1L;

    private final VerticalLayout keyValueDetailsLayout;

    /**
     * Constructor for KeyValueDetailsComponent
     */
    public KeyValueDetailsComponent() {
        keyValueDetailsLayout = new VerticalLayout();
        keyValueDetailsLayout.setSpacing(true);
        keyValueDetailsLayout.setMargin(false);
        keyValueDetailsLayout.setWidthFull();

        setReadOnly(true);
    }

    @Override
    public List<ProxyKeyValueDetails> getValue() {
        // not needed to return meaningful key-value pairs, because it is
        // intended to be read-only
        return Collections.emptyList();
    }

    @Override
    protected Component initContent() {
        return keyValueDetailsLayout;
    }

    @Override
    protected void doSetValue(final List<ProxyKeyValueDetails> keyValueDetails) {
        keyValueDetailsLayout.removeAllComponents();

        if (keyValueDetails != null) {
            keyValueDetails.forEach(this::addKeyValueDetail);
        }
    }

    private void addKeyValueDetail(final ProxyKeyValueDetails keyValueDetail) {
        final String id = keyValueDetail.getId();
        final String key = keyValueDetail.getKey();
        final String value = keyValueDetail.getValue();

        final Label keyDetail = buildKeyDetail(key);
        final Label valueDetail = buildValueDetail(id, value);

        final HorizontalLayout keyValueDetailLayout = new HorizontalLayout();
        keyValueDetailLayout.setSpacing(true);
        keyValueDetailLayout.setMargin(false);
        keyValueDetailLayout.setWidthUndefined();

        keyValueDetailLayout.addComponent(keyDetail);
        keyValueDetailLayout.setExpandRatio(keyDetail, 0.0F);

        keyValueDetailLayout.addComponent(valueDetail);
        keyValueDetailLayout.setExpandRatio(valueDetail, 1.0F);

        keyValueDetailLayout.setDescription(key.concat(": ") + value);

        keyValueDetailsLayout.addComponent(keyValueDetailLayout);
    }

    private static Label buildKeyDetail(final String key) {
        final Label keyLabel = new Label(key + ":");

        keyLabel.addStyleName(SPUIDefinitions.TEXT_STYLE);
        keyLabel.addStyleName("text-bold");

        return keyLabel;
    }

    private static Label buildValueDetail(final String id, final String value) {
        final Label valueLabel = new Label(value);

        valueLabel.setId(id);
        valueLabel.addStyleName(SPUIDefinitions.TEXT_STYLE);

        return valueLabel;
    }

    /**
     * Disable the spacing in keyValueDetailsLayout
     */
    public void disableSpacing() {
        keyValueDetailsLayout.setSpacing(false);
    }
}
