package org.eclipse.hawkbit.ui.distributions.event;

public class DistributionSetTableEvent {

    private final DistributionTableComponentEvent distributionSetTableEvent;

    /**
     * The component event.
     * 
     * @param distributionSetTableEvent
     *            the distributionSet component event.
     */
    public DistributionSetTableEvent(final DistributionTableComponentEvent distributionSetTableEvent) {
        this.distributionSetTableEvent = distributionSetTableEvent;
    }

    /**
     * DistributionSet table components events.
     *
     */
    public enum DistributionTableComponentEvent {
        SELECT_ALL
    }

}
