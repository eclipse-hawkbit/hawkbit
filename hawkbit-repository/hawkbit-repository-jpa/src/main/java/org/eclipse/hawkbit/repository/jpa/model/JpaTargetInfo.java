/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.hawkbit.repository.jpa.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;

/**
 * A table which contains all the information inserted, updated by the
 * controller itself. So this entity does not provide audit information because
 * changes on this entity are mostly only done by controller requests. That's
 * the reason that we store these information in a separated table so we don't
 * modifying the {@link Target} itself when a controller reports it's
 * {@link #lastTargetQuery} for example.
 *
 */
@Table(name = "sp_target_info", indexes = {
        @Index(name = "sp_idx_target_info_02", columnList = "target_id,update_status") })
@Entity
public class JpaTargetInfo implements Persistable<Long>, TargetInfo {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(TargetInfo.class);

    @Id
    private Long targetId;

    @Transient
    private boolean entityNew;

    @CascadeOnDelete
    @OneToOne(cascade = { CascadeType.MERGE,
            CascadeType.REMOVE }, fetch = FetchType.LAZY, targetEntity = JpaTarget.class)
    @JoinColumn(name = "target_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_stat_targ"))
    @MapsId
    private JpaTarget target;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "last_target_query")
    private Long lastTargetQuery;

    @Column(name = "install_date")
    private Long installationDate;

    @Column(name = "update_status", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private TargetUpdateStatus updateStatus = TargetUpdateStatus.UNKNOWN;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "installed_distribution_set", nullable = true, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_inst_ds"))
    private JpaDistributionSet installedDistributionSet;

    /**
     * Read only on management API. Are commited by controller.
     */
    @ElementCollection
    @Column(name = "attribute_value", length = 128)
    @MapKeyColumn(name = "attribute_key", nullable = false, length = 32)
    @CollectionTable(name = "sp_target_attributes", joinColumns = {
            @JoinColumn(name = "target_id") }, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_attrib_target"))

    private final Map<String, String> controllerAttributes = Collections.synchronizedMap(new HashMap<String, String>());

    // set default request controller attributes to true, because we want to
    // request them the first
    // time
    @Column(name = "request_controller_attributes", nullable = false)
    private boolean requestControllerAttributes = true;

    /**
     * Constructor for {@link TargetStatus}.
     *
     * @param target
     *            related to this status.
     */
    public JpaTargetInfo(final JpaTarget target) {
        this.target = target;
        targetId = target.getId();
    }

    JpaTargetInfo() {
        target = null;
        targetId = null;
    }

    @Override
    public Long getId() {
        return targetId;
    }

    @Override
    @Transient
    public boolean isNew() {
        return entityNew;
    }

    /**
     * @param isNew
     *            the isNew to set
     */
    public void setNew(final boolean entityNew) {
        this.entityNew = entityNew;
    }

    /**
     * @return the ipAddress
     */
    @Override
    public URI getAddress() {
        if (address == null) {
            return null;
        }
        try {
            return URI.create(address);
        } catch (final IllegalArgumentException e) {
            LOG.warn("Invalid address provided. Cloud not be configured to URI", e);
            return null;
        }
    }

    /**
     * @param address
     *            the target address to set
     *
     * @throws IllegalArgumentException
     *             If the given string violates RFC&nbsp;2396
     */
    public void setAddress(final String address) {
        // check if this is a real URI
        if (address != null) {
            URI.create(address);
        }

        this.address = address;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(final Long targetId) {
        this.targetId = targetId;
    }

    @Override
    public Target getTarget() {
        return target;
    }

    public void setTarget(final JpaTarget target) {
        this.target = target;
    }

    @Override
    public Long getLastTargetQuery() {
        return lastTargetQuery;
    }

    public void setLastTargetQuery(final Long lastTargetQuery) {
        this.lastTargetQuery = lastTargetQuery;
    }

    public void setRequestControllerAttributes(final boolean requestControllerAttributes) {
        this.requestControllerAttributes = requestControllerAttributes;
    }

    @Override
    public Map<String, String> getControllerAttributes() {
        return controllerAttributes;
    }

    @Override
    public boolean isRequestControllerAttributes() {
        return requestControllerAttributes;
    }

    @Override
    public Long getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(final Long installationDate) {
        this.installationDate = installationDate;
    }

    @Override
    public TargetUpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(final TargetUpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    @Override
    public DistributionSet getInstalledDistributionSet() {
        return installedDistributionSet;
    }

    public void setInstalledDistributionSet(final JpaDistributionSet installedDistributionSet) {
        this.installedDistributionSet = installedDistributionSet;
    }

    /**
     * @return the poll time which holds the last poll time of the target, the
     *         next poll time and the overdue time. In case the
     *         {@link #lastTargetQuery} is not set e.g. the target never polled
     *         before this method returns {@code null}
     */
    @Override
    public PollStatus getPollStatus() {
        if (lastTargetQuery == null) {
            return null;
        }
        return SystemSecurityContextHolder.getInstance().getSystemSecurityContext().runAsSystem(() -> {
            final Duration pollTime = DurationHelper.formattedStringToDuration(TenantConfigurationManagementHolder
                    .getInstance().getTenantConfigurationManagement()
                    .getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class).getValue());
            final Duration overdueTime = DurationHelper.formattedStringToDuration(
                    TenantConfigurationManagementHolder.getInstance().getTenantConfigurationManagement()
                            .getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class)
                            .getValue());
            final LocalDateTime currentDate = LocalDateTime.now();
            final LocalDateTime lastPollDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTargetQuery),
                    ZoneId.systemDefault());
            final LocalDateTime nextPollDate = lastPollDate.plus(pollTime);
            final LocalDateTime overdueDate = nextPollDate.plus(overdueTime);
            return new PollStatus(lastPollDate, nextPollDate, overdueDate, currentDate);
        });
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TargetInfo)) {
            return false;
        }
        final JpaTargetInfo other = (JpaTargetInfo) obj;
        if (target == null) {
            if (other.target != null) {
                return false;
            }
        } else if (!target.equals(other.target)) {
            return false;
        }
        if (targetId == null) {
            if (other.targetId != null) {
                return false;
            }
        } else if (!targetId.equals(other.targetId)) {
            return false;
        }
        return true;
    }
}
