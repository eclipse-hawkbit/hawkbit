package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.ui.common.tagdetails.AbstractTagToken.TagData;

import com.google.common.collect.Lists;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;

public class TokenField extends CssLayout {

    private static final String NAME_PROPERTY = "name";
    private static final String COLOR_PROPERTY = "color";

    private Container container;

    private final transient Map<String, Button> tokens = new ConcurrentHashMap<>();
    private final transient List<TagAssignmentListener> listeners = Lists.newCopyOnWriteArrayList();

    public TokenField() {
        createContainer();
    }

    private Container createContainer() {
        container = new IndexedContainer();
        container.addContainerProperty(NAME_PROPERTY, String.class, "");
        container.addContainerProperty(COLOR_PROPERTY, String.class, "");
        return container;
    }

    public void addToken(final TagData tagData) {
        final Item item = container.addItem(tagData.getName());
        if (item == null) {
            return;
        }

        item.getItemProperty(NAME_PROPERTY).setValue(tagData.getName());
        item.getItemProperty(COLOR_PROPERTY).setValue(tagData.getColor());

        final Button token = new Button(tagData.getName());
        tokens.put(tagData.getName(), token);
        token.addClickListener(e -> removeTagAssignment(e.getButton().getCaption()));
        addComponent(token);

    }

    public void updateToken(final String tagName, final String tagColor) {
        final Item item = container.getItem(tagName);
        if (item != null) {
            item.getItemProperty(COLOR_PROPERTY).setValue(tagColor);
        }
    }

    public String getTokenColor(final String tokenName) {
        final Item item = container.getItem(tokenName);
        return (String) item.getItemProperty(COLOR_PROPERTY).getValue();
    }

    private void removeTagAssignment(final String tagName) {
        removeToken(tagName);
        notifyListenersTagAssignmentRemoved(tagName);
    }

    public void removeToken(final String tokenName) {
        final Button token = tokens.get(tokenName);
        if (token != null) {
            tokens.remove(tokenName);
            removeComponent(token);
        }
        container.removeItem(tokenName);
    }

    public void removeAllTokens() {
        container.removeAllItems();
        removeAllComponents();
        tokens.clear();
    }

    public interface TagAssignmentListener {
        public void tagAssigned(String tagName);

        public void tagAssignmentRemoved(String tagName);
    }

    public void addTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.add(listener);
    }

    public void removeTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.remove(listener);
    }

    private void notifyListenersTagAssigned(final String tagName) {
        listeners.forEach(listener -> listener.tagAssigned(tagName));
    }

    private void notifyListenersTagAssignmentRemoved(final String tagName) {
        listeners.forEach(listener -> listener.tagAssignmentRemoved(tagName));
    }
}
