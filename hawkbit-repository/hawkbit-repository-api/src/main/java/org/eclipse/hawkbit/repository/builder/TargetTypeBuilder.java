package org.eclipse.hawkbit.repository.builder;

public interface TargetTypeBuilder {
    /**
     * @param id
     *            of the updatable entity
     * @return builder instance
     */
    TargetTypeUpdate update(long id);

    /**
     * @return builder instance
     */
    TargetTypeCreate create();
}
