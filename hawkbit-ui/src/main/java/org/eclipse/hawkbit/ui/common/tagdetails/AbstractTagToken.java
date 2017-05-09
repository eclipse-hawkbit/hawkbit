/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.tokenfield.TokenField;
import org.vaadin.tokenfield.TokenField.InsertPosition;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract class for target/ds tag token layout.
 *
 * @param <T>
 *            the special entity
 */
public abstract class AbstractTagToken<T extends BaseEntity> implements Serializable {

    private static final String ID_PROPERTY = "id";
    private static final String NAME_PROPERTY = "name";
    private static final String COLOR_PROPERTY = "color";

    protected static final int MAX_TAG_QUERY = 500;

    private static final long serialVersionUID = 6599386705285184783L;

    protected TokenField tokenField;

    protected IndexedContainer container;

    protected final transient Map<Long, TagData> tagDetails = new ConcurrentHashMap<>();

    protected final transient Map<Long, TagData> tokensAdded = new HashMap<>();

    protected CssLayout tokenLayout = new CssLayout();

    protected SpPermissionChecker checker;

    protected VaadinMessageSource i18n;

    protected UINotification uinotification;

    protected transient EventBus.UIEventBus eventBus;

    protected ManagementUIState managementUIState;

    // BaseEntity implements Serializable so this entity is serializable. Maybe
    // a sonar bug
    @SuppressWarnings("squid:S1948")
    protected T selectedEntity;

    protected AbstractTagToken(final SpPermissionChecker checker, final VaadinMessageSource i18n,
            final UINotification uinotification, final UIEventBus eventBus, final ManagementUIState managementUIState) {
        this.checker = checker;
        this.i18n = i18n;
        this.uinotification = uinotification;
        this.eventBus = eventBus;
        this.managementUIState = managementUIState;
        createTokenField();
        checkIfTagAssignedIsAllowed();
        eventBus.subscribe(this);
    }

    protected void onBaseEntityEvent(final BaseUIEntityEvent<T> baseEntityEvent) {
        if (BaseEntityEventType.SELECTED_ENTITY != baseEntityEvent.getEventType()) {
            return;
        }
        UI.getCurrent().access(() -> {
            final T entity = baseEntityEvent.getEntity();
            if (entity != null) {
                selectedEntity = entity;
                repopulateToken();
            }
        });
    }

    private void createTokenField() {
        final Container tokenContainer = createContainer();
        tokenField = createTokenField(tokenContainer);
        tokenField.setContainerDataSource(tokenContainer);
        tokenField.setNewTokensAllowed(false);
        tokenField.setFilteringMode(FilteringMode.CONTAINS);
        tokenField.setInputPrompt(getTokenInputPrompt());
        tokenField.setTokenInsertPosition(InsertPosition.AFTER);
        tokenField.setImmediate(true);
        tokenField.addStyleName(ValoTheme.COMBOBOX_TINY);
        tokenField.setSizeFull();
        tokenField.setTokenCaptionPropertyId(NAME_PROPERTY);
    }

    protected void repopulateToken() {
        populateContainer();
        displayAlreadyAssignedTags();
    }

    private Container createContainer() {
        container = new IndexedContainer();
        container.addContainerProperty(NAME_PROPERTY, String.class, "");
        container.addContainerProperty(ID_PROPERTY, Long.class, "");
        container.addContainerProperty(COLOR_PROPERTY, String.class, "");
        return container;
    }

    protected void addNewToken(final Long tagId) {
        tokenField.addToken(tagId);
        removeTagAssignedFromCombo(tagId);
    }

    private void removeTagAssignedFromCombo(final Long tagId) {
        // might not yet exist or not anymore due to unprocessed events
        final Item item = tokenField.getContainerDataSource().getItem(tagId);
        if (item == null) {
            return;
        }

        tokensAdded.put(tagId, new TagData(tagId, getTagName(item), getColor(item)));
        container.removeItem(tagId);
    }

    protected void setContainerPropertValues(final Long tagId, final String tagName, final String tagColor) {
        final TagData tagData = tagDetails.putIfAbsent(tagId, new TagData(tagId, tagName, tagColor));
        if (tagData == null) {
            final Item item = container.addItem(tagId);
            if (item == null) {
                return;
            }

            item.getItemProperty(ID_PROPERTY).setValue(tagId);
            updateItem(tagName, tagColor, item);
        }
    }

    protected void updateItem(final String tagName, final String tagColor, final Item item) {
        item.getItemProperty(NAME_PROPERTY).setValue(tagName);
        item.getItemProperty(COLOR_PROPERTY).setValue(tagColor);
    }

    protected void checkIfTagAssignedIsAllowed() {
        if (!isToggleTagAssignmentAllowed()) {
            tokenField.addStyleName("hideTokenFieldcombo");
        }
    }

    private TokenField createTokenField(final Container tokenContainer) {
        return new CustomTokenField(tokenLayout, tokenContainer);
    }

    class CustomTokenField extends TokenField {
        private static final long serialVersionUID = 694216966472937436L;

        Container tokenContainer;

        CustomTokenField(final CssLayout cssLayout, final Container tokenContainer) {
            super(cssLayout);
            this.tokenContainer = tokenContainer;
        }

        @Override
        protected void configureTokenButton(final Object tokenId, final Button button) {
            super.configureTokenButton(tokenId, button);
            updateTokenStyle(tokenId, button);
            button.addStyleName(SPUIDefinitions.TEXT_STYLE + " " + SPUIStyleDefinitions.DETAILS_LAYOUT_STYLE);
        }

        @Override
        protected void onTokenInput(final Object tokenId) {
            super.addToken(tokenId);
            onTokenSearch(tokenId);
        }

        @Override
        protected void onTokenClick(final Object tokenId) {
            if (isToggleTagAssignmentAllowed()) {
                super.onTokenClick(tokenId);
                tokenClick(tokenId);
            }
        }

        private void updateTokenStyle(final Object tokenId, final Button button) {
            final Item item = tokenField.getContainerDataSource().getItem(tokenId);
            if (item == null) {
                return;
            }

            final String color = getColor(item);
            button.setCaption("<span style=\"color:" + color + " !important;\">" + FontAwesome.CIRCLE.getHtml()
                    + "</span>" + " " + getItemNameProperty(tokenId).getValue().toString().concat(" ×"));
            button.setCaptionAsHtml(true);
        }

        private void onTokenSearch(final Object tokenId) {
            assignTag(getItemNameProperty(tokenId).getValue().toString());
            removeTagAssignedFromCombo((Long) tokenId);
        }

        private void tokenClick(final Object tokenId) {
            final Item item = tokenField.getContainerDataSource().addItem(tokenId);
            item.getItemProperty(NAME_PROPERTY).setValue(tagDetails.get(tokenId).getName());
            item.getItemProperty(COLOR_PROPERTY).setValue(tagDetails.get(tokenId).getColor());
            unassignTag(tagDetails.get(tokenId).getName());
        }

        private Property getItemNameProperty(final Object tokenId) {
            final Item item = tokenField.getContainerDataSource().getItem(tokenId);
            return item.getItemProperty(NAME_PROPERTY);
        }

    }

    private static String getColor(final Item item) {
        if (item.getItemProperty(COLOR_PROPERTY).getValue() != null) {
            return (String) item.getItemProperty(COLOR_PROPERTY).getValue();
        } else {
            return SPUIDefinitions.DEFAULT_COLOR;
        }
    }

    private static String getTagName(final Item item) {
        return (String) item.getItemProperty(NAME_PROPERTY).getValue();
    }

    protected void removePreviouslyAddedTokens() {
        tokensAdded.keySet().forEach(previouslyAddedToken -> tokenField.removeToken(previouslyAddedToken));
    }

    protected Long getTagIdByTagName(final Long tagId) {
        return tagDetails.entrySet().stream().filter(entry -> entry.getValue().getId().equals(tagId)).findAny()
                .map(entry -> entry.getKey()).orElse(null);

    }

    protected void removeTokenItem(final Long tokenId, final String name) {
        tokenField.removeToken(tokenId);
        tagDetails.remove(tokenId);
        setContainerPropertValues(tokenId, name, tokensAdded.get(tokenId).getColor());
    }

    protected void removeTagFromCombo(final Long deletedTagId) {
        if (deletedTagId != null) {
            container.removeItem(deletedTagId);
        }
    }

    protected abstract String getTagStyleName();

    protected abstract String getTokenInputPrompt();

    protected abstract void assignTag(final String tagNameSelected);

    protected abstract void unassignTag(final String tagName);

    protected abstract Boolean isToggleTagAssignmentAllowed();

    protected abstract void displayAlreadyAssignedTags();

    protected abstract void populateContainer();

    public TokenField getTokenField() {
        return tokenField;
    }

    /**
     * Tag details.
     *
     */
    public static class TagData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;

        private Long id;

        private String color;

        /**
         * Tag data constructor.
         *
         * @param id
         * @param name
         * @param color
         */
        public TagData(final Long id, final String name, final String color) {
            this.color = color;
            this.id = id;
            this.name = name;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name
         *            the name to set
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * @return the id
         */
        public Long getId() {
            return id;
        }

        /**
         * @param id
         *            the id to set
         */
        public void setId(final Long id) {
            this.id = id;
        }

        /**
         * @return the color
         */
        public String getColor() {
            return color;
        }

        /**
         * @param color
         *            the color to set
         */
        public void setColor(final String color) {
            this.color = color;
        }

    }
}
