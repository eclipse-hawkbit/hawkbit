package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sp_target_type", indexes = {
        @Index(name = "sp_idx_target_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_target_type_name")})
public class JpaTargetType extends AbstractJpaNamedEntity implements TargetType, EventAwareEntity{

    private static final long serialVersionUID = 1L;

    @Column(name = "colour", nullable = true, length = TargetType.COLOUR_MAX_SIZE)
    @Size(max = TargetType.COLOUR_MAX_SIZE)
    private String colour;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaDistributionSetType.class)
    @JoinTable(name = "sp_target_type_ds_type_relation", joinColumns = {
            @JoinColumn(name = "target_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_type_relation_target_type"))}, inverseJoinColumns = {
            @JoinColumn(name = "distribution_set_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_type_relation_ds_type"))})
    private Set<DistributionSetType> distributionSetTypes;

    public JpaTargetType() {
        // default public constructor for JPA
    }

    public JpaTargetType(String name, String description, String colour) {
        super(name,description);
        this.colour = colour;
    }

    public JpaTargetType addCompatibleDistributionSetType(final DistributionSetType dsSetType) {
        if (distributionSetTypes == null) {
            distributionSetTypes = new HashSet<>();
        }

        distributionSetTypes.add(dsSetType);
        return this;
    }

    public JpaTargetType removeDistributionSetType(final Long dsTypeId) {
        if (distributionSetTypes == null) {
            return this;
        }
        distributionSetTypes.remove(dsTypeId);
        return this;
    }

    @Override
    public Set<DistributionSetType> getCompatibleDistributionSetTypes() {

        if (distributionSetTypes == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(distributionSetTypes);
    }

    @Override
    public boolean checkComplete(Target target) {
        return false;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public String getColour() {
        return null;
    }

    @Override
    public void fireCreateEvent(DescriptorEvent descriptorEvent) {

    }

    @Override
    public void fireUpdateEvent(DescriptorEvent descriptorEvent) {

    }

    @Override
    public void fireDeleteEvent(DescriptorEvent descriptorEvent) {

    }
}
