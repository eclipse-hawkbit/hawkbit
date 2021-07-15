package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;

import javax.validation.constraints.Size;
import java.util.Collection;

public interface TargetTypeUpdate {
    /**
     * @param description
     *            for {@link TargetType#getDescription()}
     * @return updated builder instance
     */
    TargetTypeUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour
     *            for {@link TargetType#getColour()}
     * @return updated builder instance
     */
    TargetTypeUpdate colour(@Size(max = TargetType.COLOUR_MAX_SIZE) String colour);

    /**
     * @param optional
     *            for {@link TargetType#getOptionalSetTypes()}
     * @return updated builder instance
     */
    TargetTypeUpdate optional(Collection<Long> optional);
}
