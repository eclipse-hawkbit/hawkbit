package org.eclipse.hawkbit.repository.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.hawkbit.repository.event.remote.ActionStatusUpdateEvent;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Junit tests for RolloutStatusHandlerService.
 */
@ActiveProfiles({ "test" })
@Feature("Component Tests - Repository")
@Story("Rollout Status Handler")
@SpringBootTest(classes = { RepositoryApplicationConfiguration.class })
public class ActionStatusUpdateHandlerServiceTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that the status update(finished state) for a distribution id marks")
    public void test() {

        String controllerId = "com.bosch.iot.dm.ac:cac";
        String tenant = "test";

        // generate data in database
        JpaSoftwareModule swModule = this.generateSoftwareModule(tenant);
        JpaDistributionSet ds = generateDistributionSet(swModule, tenant);
        JpaTarget target = generateSampleTarget(1L, controllerId, tenant);
        JpaAction action = generateAction(ds, target, tenant);

        ActionStatusUpdateHandlerService rolloutStatusHandlerService = new ActionStatusUpdateHandlerService(
                this.controllerManagement, this.entityFactory);

        // initiate the test
        ActionStatusUpdateEvent targetStatus = new ActionStatusUpdateEvent("default",
                ds.getId(), controllerId, Status.FINISHED, new ArrayList<>());
        rolloutStatusHandlerService.handle(targetStatus);

        // verify if intended dataSetId is really installed
        Long installedId = this.targetRepository.findById(target.getId()).get().getInstalledDistributionSet().getId();
        assertEquals("Status update for a given distirbution set has failed", installedId, ds.getId());
        
        // verify that action is database is marked inactive.
        Optional<Action> activeAction = this.actionRepository.findByActiveAndTargetAndDistributionSet(target.getControllerId(), ds.getId(), true);
        assertFalse(activeAction.isPresent());
        
        // clean up data - start
        this.actionRepository.deleteById(action.getId());
        this.softwareModuleRepository.deleteById(swModule.getId());
        this.targetRepository.deleteById(target.getId());
        this.distributionSetRepository.deleteById(ds.getId());
    }

    private JpaDistributionSet generateDistributionSet(JpaSoftwareModule swModule, String tenant) {
        // TODO Auto-generated method stub
        JpaDistributionSet ds = new JpaDistributionSet();
        // ds.setId(distributionSetId);
        ds.setName("test ds");
        ds.setVersion("v1");
        JpaDistributionSetType type = this.distributionSetTypeRepository.findById(4L).get();
        ds.setType(type);
        ds.setTenant(tenant);
        ds.addModule(swModule);
        ds.setRequiredMigrationStep(false);
        return this.distributionSetRepository.save(ds);
    }

    private JpaSoftwareModule generateSoftwareModule(String tenant) {
        JpaSoftwareModuleType type = this.softwareModuleTypeRepository.findById(3L).get();
        JpaSoftwareModule swMod = new JpaSoftwareModule(type, "test swm", "0.0.1", "", "");
        swMod.setId(1L);
        swMod.setTenant(tenant);
        return this.softwareModuleRepository.save(swMod);
    }

    private JpaAction generateAction(DistributionSet distributionSet, Target target, String tenant) {
        final JpaAction action = new JpaAction();
        action.setActive(true);
        action.setDistributionSet(distributionSet);
        action.setActionType(ActionType.FORCED);
        action.setTenant(tenant);
        action.setStatus(Status.SCHEDULED);
        action.setTarget(target);
        return actionRepository.save(action);
    }

    private JpaTarget generateSampleTarget(Long id, String controllerId, String tenant) {
        JpaTarget jpaTarget = new JpaTarget(controllerId);
        jpaTarget.setId(id);
        jpaTarget.setTenant(tenant);
        jpaTarget.setName("device " + controllerId);
        return this.targetRepository.save(jpaTarget);
    }

}
