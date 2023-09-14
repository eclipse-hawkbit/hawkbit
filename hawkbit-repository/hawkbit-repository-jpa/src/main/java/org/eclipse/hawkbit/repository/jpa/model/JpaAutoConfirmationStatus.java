/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;

@Entity
@Table(name = "sp_target_conf_status")
public class JpaAutoConfirmationStatus extends AbstractJpaTenantAwareBaseEntity implements AutoConfirmationStatus {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_auto_conf"))
    private JpaTarget target;

    @Column(name = "initiator", length = USERNAME_FIELD_LENGTH)
    @Size(max = USERNAME_FIELD_LENGTH)
    private String initiator;

    @Column(name = "remark", length = NamedEntity.DESCRIPTION_MAX_SIZE)
    @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
    private String remark;

    /**
     * Default constructor needed for JPA entities.
     */
    public JpaAutoConfirmationStatus() {
        // Default constructor needed for JPA entities.
    }

    public JpaAutoConfirmationStatus(final String initiator, final String remark, final Target target) {
        this.target = (JpaTarget) target;
        this.initiator = StringUtils.isEmpty(initiator) ? null : initiator;
        this.remark = StringUtils.isEmpty(remark) ? null : remark;
    }

    @Override
    public Target getTarget() {
        return target;
    }

    @Override
    public String getInitiator() {
        return initiator;
    }

    @Override
    public long getActivatedAt() {
        return getCreatedAt();
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public String constructActionMessage() {
        final String remarkMessage = StringUtils.hasText(remark) ? remark : "n/a";
        final String formattedInitiator = StringUtils.hasText(initiator) ? initiator : "n/a";
        final String createdByRolloutsUser = StringUtils.hasText(getCreatedBy()) ? getCreatedBy() : "n/a";
        // https://docs.oracle.com/en/java/javase/17/text-blocks/index.html#normalization-of-line-terminators
        // nevertheless of the end of line of the file (\r\n, \n or \r) the result will contains \n
        return """
            Assignment automatically confirmed by initiator '%s'.\040
            
            Auto confirmation activated by system user: '%s'\040
            
            Remark: %s""".formatted(formattedInitiator, createdByRolloutsUser, remarkMessage);
    }

    @Override
    public String toString() {
        return "AutoConfirmationStatus [id=" + getId() + ", target=" + target.getControllerId() + ", initiator="
                + initiator + ", bosch_user=" + getCreatedBy() + ", activatedAt=" + getCreatedAt() + ", remark="
                + remark + "]";
    }

}
