package org.eclipse.hawkbit.repository.jpa;

import static org.junit.Assert.assertThat;

import io.qameta.allure.Description;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.TargetQueryExecutionManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.springframework.data.domain.Page;

import java.util.Collections;

@Feature("Component Tests - Repository")
@Story("Target Query")
public class TargetQueryExecutionManagementTest extends AbstractJpaIntegrationTest {

    @Autowired
    protected TargetQueryExecutionManagement sut;

    @Test
    @Description("Test that RSQL filter finds targets with metadata and/or controllerId.")
    public void findTargetsByRsqlWithMetadata() {
        final String controllerId1 = "target1";
        final String controllerId2 = "target2";
        createTargetWithMetadata(controllerId1, 2);
        createTargetWithMetadata(controllerId2, 2);

        final String rsqlAndControllerIdFilter = "id==target1 and metadata.key1==target1-value1";
        final String rsqlAndControllerIdWithWrongKeyFilter = "id==* and metadata.unknown==value1";
        final String rsqlAndControllerIdNotEqualFilter = "id==* and metadata.key2!=target1-value2";
        final String rsqlOrControllerIdFilter = "id==target1 or metadata.key1==*value1";
        final String rsqlOrControllerIdWithWrongKeyFilter = "id==target2 or metadata.unknown==value1";
        final String rsqlOrControllerIdNotEqualFilter = "id==target1 or metadata.key1!=target1-value1";

        Assertions.assertThat(targetManagement.count()).as("Total targets").isEqualTo(2);
        validateFoundTargetsByRsql(rsqlAndControllerIdFilter, controllerId1);
        validateFoundTargetsByRsql(rsqlAndControllerIdWithWrongKeyFilter);
        validateFoundTargetsByRsql(rsqlAndControllerIdNotEqualFilter, controllerId2);
        validateFoundTargetsByRsql(rsqlOrControllerIdFilter, controllerId1, controllerId2);
        validateFoundTargetsByRsql(rsqlOrControllerIdWithWrongKeyFilter, controllerId2);
        validateFoundTargetsByRsql(rsqlOrControllerIdNotEqualFilter, controllerId1, controllerId2);
    }

    private void validateFoundTargetsByRsql(final String rsqlFilter, final String... controllerIds) {
        final Page<? extends Target> foundTargetsByMetadataAndControllerId = targetQueryExecutionManagement
                .findByQuery(PAGE, rsqlFilter);

        Assertions.assertThat(foundTargetsByMetadataAndControllerId.getTotalElements())
                .as("Targets count in RSQL filter is wrong").isEqualTo(controllerIds.length);
        Assertions.assertThat(foundTargetsByMetadataAndControllerId.getContent().stream().map(Target::getControllerId))
                .as("Targets found by RSQL filter have wrong controller ids").containsExactlyInAnyOrder(controllerIds);
    }

    private Target createTargetWithMetadata(final String controllerId, final int count) {
        final Target target = testdataFactory.createTarget(controllerId);

        for (int index = 1; index <= count; index++) {
            insertTargetMetadata("key" + index, controllerId + "-value" + index, target);
        }
        return target;
    }

    private void insertTargetMetadata(final String knownKey, final String knownValue, final Target target) {
        final JpaTargetMetadata metadata = new JpaTargetMetadata(knownKey, knownValue, target);
        targetManagement.createMetaData(target.getControllerId(), Collections.singletonList(metadata));
    }

}
