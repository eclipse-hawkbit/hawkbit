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
import java.util.Optional;

import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
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
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract class for target/ds tag token layout.
 * 
 *
 *
 *
 */
public abstract class AbstractTagToken implements Serializable {

    private static final String COLOR_PROPERTY = "color";

    private static final long serialVersionUID = 6599386705285184783L;

    protected TokenField tokenField;

    protected IndexedContainer container;

    protected final Map<Long, TagData> tagDetails = new HashMap<Long, TagData>();

    protected final Map<Long, TagData> tokensAdded = new HashMap<Long, TagData>();

    protected CssLayout tokenLayout = new CssLayout();

    protected void init() {
        createTokenField();
        checkIfTagAssignedIsAllowed();
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
        tokenField.setTokenCaptionPropertyId("name");
    }

    protected void repopulateToken() {
        populateContainer();
        displayAlreadyAssignedTags();
    }

    private Container createContainer() {
        container = new IndexedContainer();
        container.addContainerProperty("name", String.class, "");
        container.addContainerProperty("id", Long.class, "");
        container.addContainerProperty(COLOR_PROPERTY, String.class, "");
        return container;
    }

    protected void addNewToken(final Long tagId) {
        tokenField.addToken(tagId);
        removeTagAssignedFromCombo(tagId);
    }

    private void removeTagAssignedFromCombo(final Long tagId) {
        tokensAdded.put(tagId, new TagData(tagId, getTagName(tagId), getColor(tagId)));
        container.removeItem(tagId);
    }

    protected void setContainerPropertValues(final Long tagId, final String tagName, final String tagColor) {
        tagDetails.put(tagId, new TagData(tagId, tagName, tagColor));
        final Item item = container.addItem(tagId);
        item.getItemProperty("name").setValue(tagName);
        item.getItemProperty("id").setValue(tagId);
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

        final Container tokenContainer;

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

    }

    private void updateTokenStyle(final Object tokenId, final Button button) {
        final String color = getColor(tokenId);
        button.setCaption("<span style=\"color:" + color + " !important;\">" + FontAwesome.CIRCLE.getHtml() + "</span>"
                + " " + getItemNameProperty(tokenId).getValue().toString().concat(" ×"));
        button.setCaptionAsHtml(true);
    }

    private void onTokenSearch(final Object tokenId) {
        assignTag(getItemNameProperty(tokenId).getValue().toString());
        removeTagAssignedFromCombo((Long) tokenId);
    }

    private Property getItemNameProperty(final Object tokenId) {
        final Item item = tokenField.getContainerDataSource().getItem(tokenId);
        return item.getItemProperty("name");
    }

    private String getColor(final Object tokenId) {
        final Item item = tokenField.getContainerDataSource().getItem(tokenId);
        if (item.getItemProperty(COLOR_PROPERTY).getValue() != null) {
            return (String) item.getItemProperty(COLOR_PROPERTY).getValue();
        } else {
            return SPUIDefinitions.DEFAULT_COLOR;
        }
    }

    private String getTagName(final Object tokenId) {
        final Item item = tokenField.getContainerDataSource().getItem(tokenId);
        return (String) item.getItemProperty("name").getValue();
    }

    private void tokenClick(final Object tokenId) {
        final Item item = tokenField.getContainerDataSource().addItem(tokenId);
        item.getItemProperty("name").setValue(tagDetails.get(tokenId).getName());
        item.getItemProperty(COLOR_PROPERTY).setValue(tagDetails.get(tokenId).getColor());
        unassignTag(tagDetails.get(tokenId).getName());
    }

    protected void removePreviouslyAddedTokens() {
        tokensAdded.keySet().forEach(previouslyAddedToken -> tokenField.removeToken(previouslyAddedToken));
    }

    protected Long getTagIdByTagName(final String tagName) {
        final Optional<Map.Entry<Long, TagData>> mapEntry = tagDetails.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equals(tagName)).findFirst();
        if (mapEntry.isPresent()) {
            return mapEntry.get().getKey();
        }
        return null;
    }

    protected void removeTokenItem(final Long tokenId, final String name) {
        tokenField.removeToken(tokenId);
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

    static class TagData {

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
