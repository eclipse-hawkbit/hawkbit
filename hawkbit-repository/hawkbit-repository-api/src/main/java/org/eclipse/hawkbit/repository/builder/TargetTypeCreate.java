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
     * @param compatible
     *            for {@link TargetType#getCompatibleDistributionSetTypes()}
     * @return updated builder instance
     */
    TargetTypeCreate compatible(Collection<Long> compatible);

    /**
     * @param compatible
     *            for {@link TargetType#getCompatibleDistributionSetTypes()}
     * @return updated builder instance
     */
    default TargetTypeCreate compatible(final Long compatible) {
        return compatible(Arrays.asList(compatible));
    }

    /**
     * @param compatible
     *            for {@link TargetType#getCompatibleDistributionSetTypes()}
     * @return updated builder instance
     */
    default TargetTypeCreate compatible(final DistributionSetType compatible) {
        return compatible(Optional.ofNullable(compatible).map(DistributionSetType::getId).orElse(null));
    }

    /**
     * @return peek on current state of {@link TargetType} in the
     *         builder
     */
    TargetType build();
}
