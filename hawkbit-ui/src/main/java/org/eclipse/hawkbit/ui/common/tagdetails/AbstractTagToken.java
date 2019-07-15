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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;
import org.eclipse.hawkbit.ui.common.tagdetails.TagPanel.TagAssignmentListener;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
//import org.vaadin.tokenfield.TokenField.InsertPosition;

import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract class for target/ds tag token layout.
 *
 * @param <T>
 *            the special entity
 */
public abstract class AbstractTagToken<T extends BaseEntity> implements Serializable, TagAssignmentListener {

    protected static final int MAX_TAG_QUERY = 500;

    private static final long serialVersionUID = 6599386705285184783L;

    protected TagPanel tagPanel;

    // protected IndexedContainer container;

    protected final transient Map<Long, TagData> tagDetailsById = new ConcurrentHashMap<>();
    protected final transient Map<String, TagData> tagDetailsByName = new ConcurrentHashMap<>();

    // protected final transient Map<Long, TagData> tokensAdded = new
    // HashMap<>();

    // protected CssLayout tokenLayout = new CssLayout();

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
        if (doSubscribeToEventBus()) {
            eventBus.subscribe(this);
        }
    }

    /**
     * Subscribes the view to the eventBus. Method has to be overriden (return
     * false) if the view does not contain any listener to avoid Vaadin blowing
     * up our logs with warnings.
     */
    protected boolean doSubscribeToEventBus() {
        return true;
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

        // final Container tokenContainer = createContainer();
        tagPanel = new TagPanel();
        tagPanel.addTagAssignmentListener(this);
        // tagPanel.setContainerDataSource(tokenContainer);
        // tagPanel.setNewTokensAllowed(false);
        // tagPanel.setFilteringMode(FilteringMode.CONTAINS);
        // tagPanel.setInputPrompt(getTokenInputPrompt());
        // tagPanel.setTokenInsertPosition(InsertPosition.AFTER);
        tagPanel.setImmediate(true);
        tagPanel.addStyleName(ValoTheme.COMBOBOX_TINY);
        tagPanel.setSizeFull();
        // tagPanel.setTokenCaptionPropertyId(NAME_PROPERTY);
    }

    protected void repopulateToken() {
        tagDetailsById.clear();
        tagDetailsByName.clear();

        final List<TagData> allAssignableTags = getAllAssignableTags();
        allAssignableTags.forEach(tagData -> {
            tagDetailsByName.put(tagData.getName(), tagData);
            tagDetailsById.put(tagData.getId(), tagData);
        });
        tagPanel.initializeTags(allAssignableTags, getAssignedTags());
        // populateContainer();
        // displayAlreadyAssignedTags();
    }

    // protected void addNewToken(final Long tagId, final String tagName, final
    // String tagColor) {
    // tagPanel.addToken(new TagData(tagId, tagName, tagColor));
    // removeTagAssignedFromCombo(tagId);
    // }
    //
    // private void removeTagAssignedFromCombo(final Long tagId) {
    // // tokensAdded.put(tagId, new TagData(tagId, getTagName(item),
    // // getColor(item)));
    // tagPanel.removeToken(tagId.toString());
    // }
    //
    // protected void setContainerPropertValues(final Long tagId, final String
    // tagName, final String tagColor) {
    // final TagData newTagData = new TagData(tagId, tagName, tagColor);
    // final TagData existingTagData = tagDetailsById.putIfAbsent(tagId,
    // newTagData);
    // if (existingTagData == null) {
    // tagPanel.addToken(newTagData);
    // }
    // }

    public void tagCreated(final TagData tagData) {
        tagDetailsByName.put(tagData.getName(), tagData);
        tagDetailsById.put(tagData.getId(), tagData);
    }

    public void tagDeleted(final Long id) {
        final TagData tagData = tagDetailsById.get(id);
        if (tagData != null) {
            tagDetailsByName.remove(tagData.getName());
            tagDetailsById.remove(id);
        }
    }

    public void tagUpdated(final TagData tagData) {
        tagPanel.tagUpdated(tagData);
    }

    protected void checkIfTagAssignedIsAllowed() {
        if (!isToggleTagAssignmentAllowed()) {
            tagPanel.addStyleName("hideTokenFieldcombo");
        }
    }

    @Override
    public void assignTag(final String tagName) {
        final TagData tagData = tagDetailsByName.get(tagName);
        if (tagData != null) {
            assignTag(tagData);
        }
    }

    @Override
    public void unassignTag(final String tagName) {
        final TagData tagData = tagDetailsByName.get(tagName);
        if (tagData != null) {
            unassignTag(tagData);
        }
    }

    protected abstract void assignTag(TagData tagData);

    protected abstract void unassignTag(TagData tagData);

    // class CustomTokenField extends TokenField {
    // private static final long serialVersionUID = 694216966472937436L;
    //
    // Container tokenContainer;
    //
    // CustomTokenField(final CssLayout cssLayout, final Container
    // tokenContainer) {
    // super(cssLayout);
    // this.tokenContainer = tokenContainer;
    // }
    //
    // @Override
    // protected void configureTokenButton(final Object tokenId, final Button
    // button) {
    // super.configureTokenButton(tokenId, button);
    // updateTokenStyle(tokenId, button);
    // button.addStyleName(SPUIDefinitions.TEXT_STYLE + " " +
    // SPUIStyleDefinitions.DETAILS_LAYOUT_STYLE);
    // }
    //
    // @Override
    // protected void onTokenInput(final Object tokenId) {
    // super.addToken(tokenId);
    // onTokenSearch(tokenId);
    // }
    //
    // @Override
    // protected void onTokenClick(final Object tokenId) {
    // if (isToggleTagAssignmentAllowed()) {
    // super.onTokenClick(tokenId);
    // tokenClick(tokenId);
    // }
    // }
    //
    // private void updateTokenStyle(final Object tokenId, final Button button)
    // {
    // final Item item = tagPanel.getContainerDataSource().getItem(tokenId);
    // if (item == null) {
    // return;
    // }
    //
    // final String color = getColor(item);
    // button.setCaption("<span style=\"color:" + color + " !important;\">" +
    // FontAwesome.CIRCLE.getHtml()
    // + "</span>" + " " +
    // getItemNameProperty(tokenId).getValue().toString().concat(" ×"));
    // button.setCaptionAsHtml(true);
    // }
    //
    // private void onTokenSearch(final Object tokenId) {
    // assignTag(getItemNameProperty(tokenId).getValue().toString());
    // removeTagAssignedFromCombo((Long) tokenId);
    // }
    //
    // private void tokenClick(final Object tokenId) {
    // final Item item = tagPanel.getContainerDataSource().addItem(tokenId);
    // item.getItemProperty(NAME_PROPERTY).setValue(tagDetailsById.get(tokenId).getName());
    // item.getItemProperty(COLOR_PROPERTY).setValue(tagDetailsById.get(tokenId).getColor());
    // unassignTag(tagDetailsById.get(tokenId).getName());
    // }
    // }

    // private static String getColor(final Item item) {
    // if (item.getItemProperty(COLOR_PROPERTY).getValue() != null) {
    // return (String) item.getItemProperty(COLOR_PROPERTY).getValue();
    // } else {
    // return SPUIDefinitions.DEFAULT_COLOR;
    // }
    // }

    // private static String getTagName(final Item item) {
    // return (String) item.getItemProperty(NAME_PROPERTY).getValue();
    // }

    // protected void removePreviouslyAddedTokens() {
    // tokensAdded.keySet().forEach(previouslyAddedToken ->
    // tagPanel.removeToken(previouslyAddedToken));
    // }

    protected Long getTagIdByTagName(final Long tagId) {
        return tagDetailsById.entrySet().stream().filter(entry -> entry.getValue().getId().equals(tagId)).findAny()
                .map(entry -> entry.getKey()).orElse(null);

    }

    protected boolean checkAssignmentResult(final List<? extends NamedEntity> assignedEntities,
            final Long expectedAssignedEntityId) {
        if (assignedEntities != null && !assignedEntities.isEmpty() && expectedAssignedEntityId != null) {
            final List<Long> assignedDsIds = assignedEntities.stream().map(NamedEntity::getId)
                    .collect(Collectors.toList());
            if (assignedDsIds.contains(expectedAssignedEntityId)) {
                return true;
            }
        }
        return false;
    }

    protected boolean checkUnassignmentResult(final NamedEntity unAssignedDistributionSet,
            final Long expectedUnAssignedEntityId) {
        return unAssignedDistributionSet != null && expectedUnAssignedEntityId != null
                && unAssignedDistributionSet.getId().equals(expectedUnAssignedEntityId);
    }

    // protected void removeTokenItem(final Long tokenId, final String name) {
    // tagPanel.removeToken(name);
    // tagDetailsById.remove(tokenId);
    // // setContainerPropertValues(tokenId, name,
    // // tokensAdded.get(tokenId).getColor());
    // }

    // protected void removeTagFromCombo(final Long deletedTagId) {
    // if (deletedTagId != null) {
    // container.removeItem(deletedTagId);
    // }
    // }

    protected abstract String getTagStyleName();

    protected abstract String getTokenInputPrompt();

    protected abstract Boolean isToggleTagAssignmentAllowed();

    // protected abstract void displayAlreadyAssignedTags();
    //
    // protected abstract void populateContainer();

    protected abstract List<TagData> getAllAssignableTags();

    protected abstract List<TagData> getAssignedTags();

    public TagPanel getTokenField() {
        return tagPanel;
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
