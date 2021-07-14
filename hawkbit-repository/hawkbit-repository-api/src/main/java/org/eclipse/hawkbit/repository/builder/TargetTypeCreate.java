package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.DistributionSetType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public interface TargetTypeCreate {
    /**
     * @param key
     *            for {@link TargetType#getKey()}
     * @return updated builder instance
     */
    TargetTypeCreate key(@Size(min = 1, max = TargetType.KEY_MAX_SIZE) @NotNull String key);

    /**
     * @param name
     *            for {@link TargetType#getName()}
     * @return updated builder instance
     */
    TargetTypeCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description
     *            for {@link TargetType#getDescription()}
     * @return updated builder instance
     */
    TargetTypeCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour
     *            for {@link TargetType#getColour()}
     * @return updated builder instance
     */
    TargetTypeCreate colour(@Size(max = TargetType.COLOUR_MAX_SIZE) String colour);

    /**
     * @param mandatory
     *            for {@link TargetType#getMandatoryModuleTypes()}
     * @return updated builder instance
     */
    TargetTypeCreate mandatory(Collection<Long> mandatory);

    /**
     * @param mandatory
     *            for {@link TargetType#getMandatoryModuleTypes()}
     * @return updated builder instance
     */
    default TargetTypeCreate mandatory(final Long mandatory) {
        return mandatory(Arrays.asList(mandatory));
    }

    /**
     * @param mandatory
     *            for {@link TargetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    default TargetTypeCreate mandatory(final DistributionSetType mandatory) {
        return mandatory(Optional.ofNullable(mandatory).map(DistributionSetType::getId).orElse(null));
    }

    /**
     * @param optional
     *            for {@link TargetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    TargetTypeCreate optional(Collection<Long> optional);

    /**
     * @param optional
     *            for {@link TargetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    default TargetTypeCreate optional(final Long optional) {
        return optional(Arrays.asList(optional));
    }

    /**
     * @param optional
     *            for {@link TargetType#getOptionalModuleTypes()}
     * @return updated builder instance
     */
    default TargetTypeCreate optional(final DistributionSetType optional) {
        return optional(Optional.ofNullable(optional).map(DistributionSetType::getId).orElse(null));
    }

    /**
     * @return peek on current state of {@link TargetType} in the
     *         builder
     */
    TargetType build();
}
