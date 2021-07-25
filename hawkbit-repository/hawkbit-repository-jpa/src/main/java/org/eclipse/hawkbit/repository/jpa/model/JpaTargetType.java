package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "sp_target_type", indexes = {
        @Index(name = "sp_idx_target_type_01", columnList = "tenant,deleted"),
        @Index(name = "sp_idx_target_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_target_type_name")})
public class JpaTargetType extends AbstractJpaNamedEntity implements TargetType, EventAwareEntity{

    private static final long serialVersionUID = 1L;

    @CascadeOnDelete
    @OneToMany(mappedBy = "targetType", targetEntity = TargetTypeElement.class, cascade = {
            CascadeType.PERSIST }, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<TargetTypeElement> elements;

    @Column(name = "colour", nullable = true, length = TargetType.COLOUR_MAX_SIZE)
    @Size(max = TargetType.COLOUR_MAX_SIZE)
    private String colour;

    @Column(name = "deleted")
    private boolean deleted;

    public JpaTargetType() {
        // default public constructor for JPA
    }

    public JpaTargetType(String name, String description, String colour) {
        super(name,description);
        this.colour = colour;
    }

    public JpaTargetType addCompatibleDistributionSetType(final DistributionSetType dsSetType) {
        return setDistributionSetType(dsSetType);
    }

    private JpaTargetType setDistributionSetType(final DistributionSetType distributionSetType) {
        if (elements == null) {
            elements = new HashSet<>();
            elements.add(new TargetTypeElement(this, (JpaDistributionSetType) distributionSetType));
            return this;
        }

        // check if this was in the list before before
        final Optional<TargetTypeElement> existing = elements.stream()
                .filter(element -> element.getDsType().getKey().equals(distributionSetType.getKey())).findAny();


        elements.add(new TargetTypeElement(this, (JpaDistributionSetType) distributionSetType));


        return this;
    }

    public JpaTargetType removeDistributionSetType(final Long dsTypeId) {
        if (elements == null) {
            return this;
        }

        // we search by id (standard equals compares also revison)
        elements.stream().filter(element -> element.getDsType().getId().equals(dsTypeId)).findAny()
                .ifPresent(elements::remove);

        return this;
    }

    @Override
    public Set<DistributionSetType> getCompatibleDistributionSetTypes() {
        return null;
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
