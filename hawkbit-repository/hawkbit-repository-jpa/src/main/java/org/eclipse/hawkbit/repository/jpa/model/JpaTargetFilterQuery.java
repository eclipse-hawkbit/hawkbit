/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.event.remote.TargetFilterQueryDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;
import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * Stored target filter.
 *
 */
@Entity
@Table(name = "sp_target_filter_query", uniqueConstraints = @UniqueConstraint(columnNames = { "name",
        "tenant" }, name = "uk_tenant_custom_filter_name"))
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaTargetFilterQuery extends AbstractJpaTenantAwareBaseEntity
        implements TargetFilterQuery, EventAwareEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = NamedEntity.NAME_MAX_SIZE, nullable = false)
    @Size(max = NamedEntity.NAME_MAX_SIZE)
    @NotEmpty
    private String name;

    @Column(name = "query", length = TargetFilterQuery.QUERY_MAX_SIZE, nullable = false)
    @Size(max = TargetFilterQuery.QUERY_MAX_SIZE)
    @NotEmpty
    private String query;

    @ManyToOne(optional = true, fetch = FetchType.LAZY, targetEntity = JpaDistributionSet.class)
    @JoinColumn(name = "auto_assign_distribution_set", nullable = true, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_filter_auto_assign_ds"))
    private JpaDistributionSet autoAssignDistributionSet;

    @Column(name = "auto_assign_action_type", nullable = true)
    @ObjectTypeConverter(name = "autoAssignActionType", objectType = Action.ActionType.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "FORCED", dataValue = "0"),
            @ConversionValue(objectValue = "SOFT", dataValue = "1"),
            // Conversion for 'TIMEFORCED' is disabled because it is not
            // permitted in autoAssignment
            @ConversionValue(objectValue = "DOWNLOAD_ONLY", dataValue = "3") })
    @Convert("autoAssignActionType")
    private ActionType autoAssignActionType;

    @Column(name = "auto_assign_weight", nullable = true)
    private Integer autoAssignWeight;

    @Column(name = "auto_assign_initiated_by", nullable = true, length = USERNAME_FIELD_LENGTH)
    private String autoAssignInitiatedBy;

    @Column(name = "confirmation_required")
    private boolean confirmationRequired;

    public JpaTargetFilterQuery() {
        // Default constructor for JPA.
    }

    /**
     * Construct a Target filter query with auto assign distribution set
     * 
     * @param name
     *            of the {@link TargetFilterQuery}.
     * @param query
     *            of the {@link TargetFilterQuery}.
     * @param autoAssignDistributionSet
     *            of the {@link TargetFilterQuery}.
     * @param autoAssignActionType
     *            of the {@link TargetFilterQuery}.
     * @param autoAssignWeight
     *            of the {@link TargetFilterQuery}.
     * @param confirmationRequired
     *            of the {@link TargetFilterQuery}.
     */
    public JpaTargetFilterQuery(final String name, final String query, final DistributionSet autoAssignDistributionSet,
            final ActionType autoAssignActionType, final Integer autoAssignWeight, final boolean confirmationRequired) {
        this.name = name;
        this.query = query;
        this.autoAssignDistributionSet = (JpaDistributionSet) autoAssignDistributionSet;
        this.autoAssignActionType = autoAssignActionType;
        this.autoAssignWeight = autoAssignWeight;
        this.confirmationRequired = confirmationRequired;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    @Override
    public DistributionSet getAutoAssignDistributionSet() {
        return autoAssignDistributionSet;
    }

    public void setAutoAssignDistributionSet(final JpaDistributionSet distributionSet) {
        this.autoAssignDistributionSet = distributionSet;
    }

    @Override
    public ActionType getAutoAssignActionType() {
        return autoAssignActionType;
    }

    public void setAutoAssignActionType(final ActionType actionType) {
        this.autoAssignActionType = actionType;
    }

    @Override
    public Optional<Integer> getAutoAssignWeight() {
        return Optional.ofNullable(autoAssignWeight);
    }

    public void setAutoAssignWeight(final Integer weight) {
        this.autoAssignWeight = weight;
    }

    public String getAutoAssignInitiatedBy() {
        return autoAssignInitiatedBy;
    }

    public void setAutoAssignInitiatedBy(final String autoAssignInitiatedBy) {
        this.autoAssignInitiatedBy = autoAssignInitiatedBy;
    }

    @Override
    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(final boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TargetFilterQueryCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TargetFilterQueryUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetFilterQueryDeletedEvent(
                getTenant(), getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }

}
