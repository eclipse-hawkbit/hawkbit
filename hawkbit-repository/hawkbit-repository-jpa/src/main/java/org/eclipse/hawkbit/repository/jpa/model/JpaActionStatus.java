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
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.jpa.cache.CacheField;
import org.eclipse.hawkbit.repository.jpa.cache.CacheKeys;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
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
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaActionStatus extends AbstractJpaTenantAwareBaseEntity implements ActionStatus {
    private static final long serialVersionUID = 1L;

    @Column(name = "target_occurred_at")
    private Long occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_act_stat_action"))
    @NotNull
    private JpaAction action;

    @Column(name = "status")
    @NotNull
    private Status status;

    @CascadeOnDelete
    @ElementCollection(fetch = FetchType.LAZY, targetClass = String.class)
    @CollectionTable(name = "sp_action_status_messages", joinColumns = @JoinColumn(name = "action_status_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_stat_msg_act_stat")), indexes = {
            @Index(name = "sp_idx_action_status_msgs_01", columnList = "action_status_id") })
    @Column(name = "detail_message", length = 512)
    private final List<String> messages = new ArrayList<>();

    /**
     * Note: filled only in {@link Status#DOWNLOAD}.
     */
    @Transient
    @CacheField(key = CacheKeys.DOWNLOAD_PROGRESS_PERCENT)
    private int downloadProgressPercent;

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
    public JpaActionStatus(final Action action, final Status status, final Long occurredAt) {
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
    public JpaActionStatus(final JpaAction action, final Status status, final Long occurredAt, final String message) {
        this.action = action;
        this.status = status;
        this.occurredAt = occurredAt;
        addMessage(message);
    }

    /**
     * JPA default constructor.
     */
    public JpaActionStatus() {
        // JPA default constructor.
    }

    @Override
    public int getDownloadProgressPercent() {
        return downloadProgressPercent;
    }

    @Override
    public Long getOccurredAt() {
        return occurredAt;
    }

    @Override
    public void setOccurredAt(final Long occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public final void addMessage(final String message) {
        if (message != null) {
            Splitter.fixedLength(512).split(message).forEach(messages::add);
        }
    }

    @Override
    public List<String> getMessages() {
        return messages;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public void setAction(final Action action) {
        this.action = (JpaAction) action;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(final Status status) {
        this.status = status;
    }

}
