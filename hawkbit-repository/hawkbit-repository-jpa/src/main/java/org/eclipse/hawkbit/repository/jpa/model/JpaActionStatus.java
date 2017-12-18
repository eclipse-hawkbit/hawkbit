/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.ArrayList;
import java.util.Collections;
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
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

import com.google.common.base.Splitter;

/**
 * Entity to store the status for a specific action.
 */
@Table(name = "sp_action_status", indexes = {
        @Index(name = "sp_idx_action_status_02", columnList = "tenant,action,status"),
        @Index(name = "sp_idx_action_status_prim", columnList = "tenant,id") })
@NamedEntityGraph(name = "ActionStatus.withMessages", attributeNodes = { @NamedAttributeNode("messages") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaActionStatus extends AbstractJpaTenantAwareBaseEntity implements ActionStatus {
    private static final int MESSAGE_ENTRY_LENGTH = 512;

    private static final long serialVersionUID = 1L;

    @Column(name = "target_occurred_at", nullable = false, updatable = false)
    private long occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_act_stat_action"))
    @NotNull
    private JpaAction action;

    @Column(name = "status", nullable = false, updatable = false)
    @ObjectTypeConverter(name = "status", objectType = Action.Status.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "FINISHED", dataValue = "0"),
            @ConversionValue(objectValue = "ERROR", dataValue = "1"),
            @ConversionValue(objectValue = "WARNING", dataValue = "2"),
            @ConversionValue(objectValue = "RUNNING", dataValue = "3"),
            @ConversionValue(objectValue = "CANCELED", dataValue = "4"),
            @ConversionValue(objectValue = "CANCELING", dataValue = "5"),
            @ConversionValue(objectValue = "RETRIEVED", dataValue = "6"),
            @ConversionValue(objectValue = "DOWNLOAD", dataValue = "7"),
            @ConversionValue(objectValue = "SCHEDULED", dataValue = "8"),
            @ConversionValue(objectValue = "CANCEL_REJECTED", dataValue = "9") })
    @Convert("status")
    @NotNull
    private Status status;

    @CascadeOnDelete
    @ElementCollection(fetch = FetchType.LAZY, targetClass = String.class)
    @CollectionTable(name = "sp_action_status_messages", joinColumns = @JoinColumn(name = "action_status_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_stat_msg_act_stat")), indexes = {
            @Index(name = "sp_idx_action_status_msgs_01", columnList = "action_status_id") })
    @Column(name = "detail_message", length = MESSAGE_ENTRY_LENGTH, nullable = false, updatable = false)
    private List<String> messages;

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
    public JpaActionStatus(final Action action, final Status status, final long occurredAt) {
        this.action = (JpaAction) action;
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
     * @param message
     *            the message which should be added to this action status
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
     * @param status
     *            the status for this action status
     * @param occurredAt
     *            the occurred timestamp
     */
    public JpaActionStatus(final Status status, final long occurredAt) {
        this.status = status;
        this.occurredAt = occurredAt;
    }

    /**
     * JPA default constructor.
     */
    public JpaActionStatus() {
        // JPA default constructor.
    }

    @Override
    public long getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(final long occurredAt) {
        this.occurredAt = occurredAt;
    }

    public final void addMessage(final String message) {
        if (message != null) {
            if (messages == null) {
                messages = new ArrayList<>((message.length() / MESSAGE_ENTRY_LENGTH) + 1);
            }
            Splitter.fixedLength(MESSAGE_ENTRY_LENGTH).split(message).forEach(messages::add);
        }
    }

    public List<String> getMessages() {
        if (messages == null) {
            messages = Collections.emptyList();
        }

        return Collections.unmodifiableList(messages);
    }

    @Override
    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = (JpaAction) action;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

}
