/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.persistence.annotations.CascadeOnDelete;

import com.google.common.base.Splitter;

/**
 * Entity to store the status for a specific action.
 */
@Table(name = "sp_action_status", indexes = { @Index(name = "sp_idx_action_status_01", columnList = "tenant,action"),
        @Index(name = "sp_idx_action_status_02", columnList = "tenant,action,status"),
        @Index(name = "sp_idx_action_status_prim", columnList = "tenant,id") })
@NamedEntityGraph(name = "ActionStatus.withMessages", attributeNodes = { @NamedAttributeNode("messages") })
@Entity
public class ActionStatus extends TenantAwareBaseEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "target_occurred_at")
    private Long occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_act_stat_action"))
    private Action action;

    @Column(name = "status")
    private Status status;

    @CascadeOnDelete
    @ElementCollection(fetch = FetchType.LAZY, targetClass = String.class)
    @CollectionTable(name = "sp_action_status_messages", joinColumns = @JoinColumn(name = "action_status_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_stat_msg_act_stat")), indexes = {
            @Index(name = "sp_idx_action_status_msgs_01", columnList = "action_status_id") })
    @Column(name = "detail_message", length = 512)
    private final List<String> messages = new ArrayList<>();

    /**
     * Creates a new {@link ActionStatus} object.
     *
     * @param action
     *            the action for this action status
     * @param status
     *            the status for this action status
     * @param occurredAt
     *            the occurred timestamp
     */
    public ActionStatus(final Action action, final Status status, final Long occurredAt) {
        this.action = action;
        this.status = status;
        this.occurredAt = occurredAt;
    }

    /**
     * Creates a new {@link ActionStatus} object.
     *
     * @param action
     *            the action for this action status
     * @param status
     *            the status for this action status
     * @param occurredAt
     *            the occurred timestamp
     * @param messages
     *            the messages which should be added to this action status
     */
    public ActionStatus(final Action action, final Status status, final Long occurredAt, final String... messages) {
        this.action = action;
        this.status = status;
        this.occurredAt = occurredAt;
        for (final String msg : messages) {
            addMessage(msg);
        }
    }

    /**
     * JPA default constructor.
     */
    public ActionStatus() {
        // JPA default constructor.
    }

    public Long getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(final Long occurredAt) {
        this.occurredAt = occurredAt;
    }

    /**
     * Adds message including splitting in case it exceeds 512 length.
     *
     * @param message
     *            to add
     */
    public final void addMessage(final String message) {
        Splitter.fixedLength(512).split(message).forEach(messages::add);
    }

    public List<String> getMessages() {
        return messages;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = action;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ActionStatus)) {
            return false;
        }

        return true;
    }

}
