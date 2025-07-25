package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Grouped event for {@link TargetAssignDistributionSetEvent}. Event that needs single processing
 */
public class GroupedTargetAssignDistributionSetEvent extends AbstractGroupedRemoteEvent<TargetAssignDistributionSetEvent> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param remoteEvent the remote event to group
     */
    @JsonCreator
    public GroupedTargetAssignDistributionSetEvent(@JsonProperty("payload") final TargetAssignDistributionSetEvent remoteEvent) {
        super(remoteEvent);
    }
}
