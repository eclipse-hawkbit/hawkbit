/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;

/**
 * Entity to store the status for a specific action.
 */
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA default constructor
@Table(name = "sp_action_status", indexes = {
        @Index(name = "sp_idx_action_status_02", columnList = "tenant,action,status"),
        @Index(name = "sp_idx_action_status_prim", columnList = "tenant,id")
})
@NamedEntityGraph(name = "ActionStatus.withMessages", attributeNodes = { @NamedAttributeNode("messages") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaActionStatus extends AbstractJpaTenantAwareBaseEntity implements ActionStatus {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MESSAGE_ENTRY_LENGTH = 512;

    @Setter @Getter
    @Column(name = "target_occurred_at", nullable = false, updatable = false)
    private long occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "action", nullable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_act_stat_action"))
    @NotNull
    private JpaAction action;

    @Setter @Getter
    @Column(name = "status", nullable = false, updatable = false)
    @Convert(converter = JpaAction.StatusConverter.class)
    @NotNull
    private Status status;

    // TODO - messages is not used yet. Check and verify if to remove or expose via REST API
    // no cascade option on an ElementCollection, the target objects are always persisted, merged, removed with their parent.
    @ElementCollection(fetch = FetchType.LAZY, targetClass = String.class)
    @CollectionTable(
            name = "sp_action_status_messages",
            joinColumns = @JoinColumn(
                    name = "action_status_id", insertable = false, updatable = false, nullable = false,
                    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_stat_msg_act_stat")),
            indexes = {
                    @Index(name = "sp_idx_action_status_msgs_01", columnList = "action_status_id") })
    @Column(name = "detail_message", length = MESSAGE_ENTRY_LENGTH, nullable = false, insertable = false, updatable = false)
    private List<String> messages;

    @Setter
    @Column(name = "code", nullable = true, updatable = false)
    private Integer code;

    /**
     * Creates a new {@link ActionStatus} object.
     *
     * @param action the action for this action status
     * @param status the status for this action status
     * @param occurredAt the occurred timestamp
     */
    public JpaActionStatus(final Action action, final Status status, final long occurredAt) {
        this.action = (JpaAction) action;
        this.status = status;
        this.occurredAt = occurredAt;
    }

    /**
     * Creates a new {@link ActionStatus} object.
     *
     * @param action the action for this action status
     * @param status the status for this action status
     * @param occurredAt the occurred timestamp
     * @param message the message which should be added to this action status
     */
    public JpaActionStatus(final JpaAction action, final Status status, final long occurredAt, final String message) {
        this.action = action;
        this.status = status;
        this.occurredAt = occurredAt;
        addMessage(message);
    }

    /**
     * Creates a new {@link ActionStatus} object.
     *
     * @param status the status for this action status
     * @param occurredAt the occurred timestamp
     */
    public JpaActionStatus(final Status status, final long occurredAt) {
        this.status = status;
        this.occurredAt = occurredAt;
    }

    @Override
    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = (JpaAction) action;
    }

    public Optional<Integer> getCode() {
        return Optional.ofNullable(code);
    }

    public final void addMessage(final String message) {
        if (message != null) {
            if (messages == null) {
                messages = new ArrayList<>((message.length() / MESSAGE_ENTRY_LENGTH) + 1);
            }
            if (message.length() > MESSAGE_ENTRY_LENGTH) {
                // split
                for (int off = 0; off < message.length(); ) {
                    final int end = off + MESSAGE_ENTRY_LENGTH;
                    if (end < message.length()) {
                        messages.add(message.substring(off, end));
                    } else {
                        messages.add(message.substring(off));
                    }
                    off = end;
                }
            } else {
                messages.add(message);
            }
        }
    }

    public List<String> getMessages() {
        if (messages == null) {
            messages = Collections.emptyList();
        }

        return Collections.unmodifiableList(messages);
    }
}