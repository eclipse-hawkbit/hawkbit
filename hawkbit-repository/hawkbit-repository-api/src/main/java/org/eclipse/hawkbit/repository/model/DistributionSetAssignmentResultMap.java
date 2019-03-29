package org.eclipse.hawkbit.repository.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DistributionSetAssignmentResultMap {

    private final Map<Long, DistributionSetAssignmentResult> results = new HashMap<>();

    public Set<Long> distributionSetIds() {
        return results.keySet();
    }

    public void putResult(final long dsId, final DistributionSetAssignmentResult result) {
        results.put(dsId, result);
    }

    public DistributionSetAssignmentResult getResult(final long dsId) {
        return results.get(dsId);
    }

    public Set<Entry<Long, DistributionSetAssignmentResult>> resultSet() {
        return results.entrySet();
    }

}
