/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Query;
import javax.validation.ConstraintViolationException;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Iterables;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Target Management")
public class TargetManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Ensures that retrieving the target security is only permitted with the necessary permissions.")
    public void getTargetSecurityTokenOnlyWithCorrectPermission() throws Exception {
        final Target createdTarget = targetManagement.createTarget(new JpaTarget("targetWithSecurityToken", "token"));

        // retrieve security token only with READ_TARGET_SEC_TOKEN permission
        final String securityTokenWithReadPermission = securityRule.runAs(WithSpringAuthorityRule
                .withUser("OnlyTargetReadPermission", false, SpPermission.READ_TARGET_SEC_TOKEN.toString()), () -> {
                    return createdTarget.getSecurityToken();
                });

        // retrieve security token as system code execution
        final String securityTokenAsSystemCode = systemSecurityContext.runAsSystem(() -> {
            return createdTarget.getSecurityToken();
        });

        // retrieve security token without any permissions
        final String securityTokenWithoutPermission = securityRule
                .runAs(WithSpringAuthorityRule.withUser("NoPermission", false), () -> {
                    return createdTarget.getSecurityToken();
                });

        assertThat(createdTarget.getSecurityToken()).isEqualTo("token");
        assertThat(securityTokenWithReadPermission).isNotNull();
        assertThat(securityTokenAsSystemCode).isNotNull();

        assertThat(securityTokenWithoutPermission).isNull();

    }

    @Test
    @Description("Ensures that targets cannot be created e.g. in plug'n play scenarios when tenant does not exists.")
    @WithUser(tenantId = "tenantWhichDoesNotExists", allSpPermissions = true, autoCreateTenant = false)
    public void createTargetForTenantWhichDoesNotExistThrowsTenantNotExistException() {
        try {
            targetManagement.createTarget(new JpaTarget("targetId123"));
            fail("should not be possible as the tenant does not exist");
        } catch (final TenantNotExistException e) {
            // ok
        }
    }

    @Test
    @Description("Verify that a target with empty controller id cannot be created")
    public void createTargetWithNoControllerId() {
        try {
            targetManagement.createTarget(new JpaTarget(""));
            fail("target with empty controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }

        try {
            targetManagement.createTarget(new JpaTarget(null));
            fail("target with empty controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }
    }

    @Test
    @Description("Ensures that targets can assigned and unassigned to a target tag. Not exists target will be ignored for the assignment.")
    public void assignAndUnassignTargetsToTag() {
        final List<String> assignTarget = new ArrayList<String>();
        assignTarget.add(targetManagement.createTarget(new JpaTarget("targetId123")).getControllerId());
        assignTarget.add(targetManagement.createTarget(new JpaTarget("targetId1234")).getControllerId());
        assignTarget.add(targetManagement.createTarget(new JpaTarget("targetId1235")).getControllerId());
        assignTarget.add(targetManagement.createTarget(new JpaTarget("targetId1236")).getControllerId());
        assignTarget.add("NotExist");

        final TargetTag targetTag = tagManagement.createTargetTag(new JpaTargetTag("Tag1"));

        final List<Target> assignedTargets = targetManagement.assignTag(assignTarget, targetTag);
        assertThat(assignedTargets.size()).as("Assigned targets are wrong").isEqualTo(4);
        assignedTargets.forEach(target -> assertThat(target.getTags().size()).isEqualTo(1));

        TargetTag findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(assignedTargets.size()).as("Assigned targets are wrong")
                .isEqualTo(findTargetTag.getAssignedToTargets().size());

        assertThat(targetManagement.unAssignTag("NotExist", findTargetTag)).as("Unassign target does not work")
                .isNull();

        final Target unAssignTarget = targetManagement.unAssignTag("targetId123", findTargetTag);
        assertThat(unAssignTarget.getControllerId()).as("Controller id is wrong").isEqualTo("targetId123");
        assertThat(unAssignTarget.getTags()).as("Tag size is wrong").isEmpty();
        findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(findTargetTag.getAssignedToTargets()).as("Assigned targets are wrong").hasSize(3);

        final List<Target> unAssignTargets = targetManagement.unAssignAllTargetsByTag(findTargetTag);
        findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(findTargetTag.getAssignedToTargets()).as("Unassigned targets are wrong").isEmpty();
        assertThat(unAssignTargets).as("Unassigned targets are wrong").hasSize(3);
        unAssignTargets.forEach(target -> assertThat(target.getTags().size()).isEqualTo(0));
    }

    @Test
    @Description("Ensures that targets can deleted e.g. test all cascades")
    public void deleteAndCreateTargets() {
        Target target = targetManagement.createTarget(new JpaTarget("targetId123"));
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(1);
        targetManagement.deleteTargets(target.getId());
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(0);

        target = createTargetWithAttributes("4711");
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(1);
        targetManagement.deleteTargets(target.getId());
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(0);

        final List<Long> targets = new ArrayList<Long>();
        for (int i = 0; i < 5; i++) {
            target = targetManagement.createTarget(new JpaTarget("" + i));
            targets.add(target.getId());
            targets.add(createTargetWithAttributes("" + (i * i + 1000)).getId());
        }
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(10);
        targetManagement.deleteTargets(targets.toArray(new Long[targets.size()]));
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(0);
    }

    private Target createTargetWithAttributes(final String controllerId) {
        Target target = new JpaTarget(controllerId);
        final Map<String, String> testData = new HashMap<>();
        testData.put("test1", "testdata1");

        target = targetManagement.createTarget(target);
        target = controllerManagament.updateControllerAttributes(controllerId, testData);

        target = targetManagement.findTargetByControllerIDWithDetails(controllerId);
        assertThat(target.getTargetInfo().getControllerAttributes()).as("Controller Attributes are wrong")
                .isEqualTo(testData);
        return target;
    }

    @Test
    @Description("Finds a target by given ID and checks if all data is in the reponse (including the data defined as lazy).")
    public void findTargetByControllerIDWithDetails() {
        final DistributionSet set = testdataFactory.createDistributionSet("test");
        final DistributionSet set2 = testdataFactory.createDistributionSet("test2");

        assertThat(targetManagement.countTargetByAssignedDistributionSet(set.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(targetManagement.countTargetByAssignedDistributionSet(set2.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set2.getId())).as("Target count is wrong")
                .isEqualTo(0);

        Target target = createTargetWithAttributes("4711");

        final long current = System.currentTimeMillis();
        controllerManagament.updateLastTargetQuery("4711", null);

        final DistributionSetAssignmentResult result = deploymentManagement.assignDistributionSet(set.getId(), "4711");

        final JpaAction action = (JpaAction) deploymentManagement.findActionWithDetails(result.getActions().get(0));
        action.setStatus(Status.FINISHED);
        controllerManagament.addUpdateActionStatus(
                new JpaActionStatus(action, Status.FINISHED, System.currentTimeMillis(), "message"));
        deploymentManagement.assignDistributionSet(set2.getId(), "4711");

        target = targetManagement.findTargetByControllerIDWithDetails("4711");
        // read data

        assertThat(targetManagement.countTargetByAssignedDistributionSet(set.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set.getId())).as("Target count is wrong")
                .isEqualTo(1);
        assertThat(targetManagement.countTargetByAssignedDistributionSet(set2.getId())).as("Target count is wrong")
                .isEqualTo(1);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set2.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(target.getTargetInfo().getLastTargetQuery()).as("Target query is not work")
                .isGreaterThanOrEqualTo(current);
        assertThat(target.getAssignedDistributionSet()).as("Assigned ds size is wrong").isEqualTo(set2);
        assertThat(target.getTargetInfo().getInstalledDistributionSet().getId()).as("Installed ds is wrong")
                .isEqualTo(set.getId());

    }

    @Test
    @Description("Ensures that repositoy returns null if given controller ID does not exist without exception.")
    public void findTargetByControllerIDWithDetailsReturnsNullForNonexisting() {
        assertThat(targetManagement.findTargetByControllerIDWithDetails("dsfsdfsdfsd")).as("Expected as").isNull();
    }

    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if the targets with the same controller ID are created twice.")
    public void createMultipleTargetsDuplicate() {
        final List<Target> targets = testdataFactory.generateTargets(5, "mySimpleTargs", "my simple targets");
        targetManagement.createTargets(targets);
        try {
            targetManagement.createTargets(targets);
            fail("Targets already exists");
        } catch (final EntityAlreadyExistsException e) {
        }

    }

    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if a single target with the same controller ID are created twice.")
    public void createTargetDuplicate() {
        targetManagement.createTarget(new JpaTarget("4711"));
        try {
            targetManagement.createTarget(new JpaTarget("4711"));
            fail("Target already exists");
        } catch (final EntityAlreadyExistsException e) {
        }
    }

    /**
     * verifies, that all {@link TargetTag} of parameter. NOTE: it's accepted
     * that the target have additional tags assigned to them which are not
     * contained within parameter tags.
     *
     * @param strict
     *            if true, the given targets MUST contain EXACTLY ALL given
     *            tags, AND NO OTHERS. If false, the given targets MUST contain
     *            ALL given tags, BUT MAY CONTAIN FURTHER ONE
     * @param targets
     *            targets to be verified
     * @param tags
     *            are contained within tags of all targets.
     * @param tags
     *            to be found in the tags of the targets
     */
    private void checkTargetHasTags(final boolean strict, final Iterable<Target> targets, final TargetTag... tags) {
        _target: for (final Target tl : targets) {
            final Target t = targetManagement.findTargetByControllerID(tl.getControllerId());

            for (final Tag tt : t.getTags()) {
                for (final Tag tag : tags) {
                    if (tag.getName().equals(tt.getName())) {
                        continue _target;
                    }
                }
                if (strict) {
                    fail("Target does not contain all tags");
                }
            }
            fail("Target does not contain any tags or the expected tag was not found");
        }
    }

    private void checkTargetHasNotTags(final Iterable<Target> targets, final TargetTag... tags) {
        for (final Target tl : targets) {
            final Target t = targetManagement.findTargetByControllerID(tl.getControllerId());

            for (final Tag tag : tags) {
                for (final Tag tt : t.getTags()) {
                    if (tag.getName().equals(tt.getName())) {
                        fail("Target should have no tags");
                    }
                }
            }
        }
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Creates and updates a target and verifies the changes in the repository.")
    public void singleTargetIsInsertedIntoRepo() throws Exception {

        final String myCtrlID = "myCtrlID";
        final Target target = testdataFactory.generateTarget(myCtrlID, "the description!");

        Target savedTarget = targetManagement.createTarget(target);
        assertNotNull("The target should not be null", savedTarget);
        final Long createdAt = savedTarget.getCreatedAt();
        Long modifiedAt = savedTarget.getLastModifiedAt();

        assertThat(createdAt).as("CreatedAt compared with modifiedAt").isEqualTo(modifiedAt);
        assertNotNull("The createdAt attribut of the target should no be null", savedTarget.getCreatedAt());
        assertNotNull("The lastModifiedAt attribut of the target should no be null", savedTarget.getLastModifiedAt());
        assertThat(target).as("Target compared with saved target").isEqualTo(savedTarget);

        savedTarget.setDescription("changed description");
        Thread.sleep(1);
        savedTarget = targetManagement.updateTarget(savedTarget);
        assertNotNull("The lastModifiedAt attribute of the target should not be null", savedTarget.getLastModifiedAt());
        assertThat(createdAt).as("CreatedAt compared with saved modifiedAt")
                .isNotEqualTo(savedTarget.getLastModifiedAt());
        assertThat(modifiedAt).as("ModifiedAt compared with saved modifiedAt")
                .isNotEqualTo(savedTarget.getLastModifiedAt());
        modifiedAt = savedTarget.getLastModifiedAt();

        final Target foundTarget = targetManagement.findTargetByControllerID(savedTarget.getControllerId());
        assertNotNull("The target should not be null", foundTarget);
        assertThat(myCtrlID).as("ControllerId compared with saved controllerId")
                .isEqualTo(foundTarget.getControllerId());
        assertThat(savedTarget).as("Target compared with saved target").isEqualTo(foundTarget);
        assertThat(createdAt).as("CreatedAt compared with saved createdAt").isEqualTo(foundTarget.getCreatedAt());
        assertThat(modifiedAt).as("LastModifiedAt compared with saved lastModifiedAt")
                .isEqualTo(foundTarget.getLastModifiedAt());
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Create multiple tragets as bulk operation and delete them in bulk.")
    public void bulkTargetCreationAndDelete() throws Exception {
        final String myCtrlID = "myCtrlID";
        final List<Target> firstList = testdataFactory.generateTargets(100, myCtrlID, "first description");

        final Target extra = testdataFactory.generateTarget("myCtrlID-00081XX", "first description");

        List<Target> firstSaved = targetManagement.createTargets(firstList);

        final Target savedExtra = targetManagement.createTarget(extra);

        final Iterable<JpaTarget> allFound = targetRepository.findAll();

        assertThat(Long.valueOf(firstList.size())).as("List size of targets")
                .isEqualTo(firstSaved.spliterator().getExactSizeIfKnown());
        assertThat(Long.valueOf(firstList.size() + 1)).as("LastModifiedAt compared with saved lastModifiedAt")
                .isEqualTo(allFound.spliterator().getExactSizeIfKnown());

        // change the objects and save to again to trigger a change on
        // lastModifiedAt
        firstSaved.forEach(t -> t.setName(t.getName().concat("\tchanged")));
        firstSaved = targetManagement.updateTargets(firstSaved);

        // verify that all entries are found
        _founds: for (final Target foundTarget : allFound) {
            for (final Target changedTarget : firstSaved) {
                if (changedTarget.getControllerId().equals(foundTarget.getControllerId())) {
                    assertThat(changedTarget.getDescription())
                            .as("Description of changed target compared with description saved target")
                            .isEqualTo(foundTarget.getDescription());
                    assertThat(changedTarget.getName()).as("Name of changed target starts with name of saved target")
                            .startsWith(foundTarget.getName());
                    assertThat(changedTarget.getName()).as("Name of changed target ends with 'changed'")
                            .endsWith("changed");
                    assertThat(changedTarget.getCreatedAt()).as("CreatedAt compared with saved createdAt")
                            .isEqualTo(foundTarget.getCreatedAt());
                    assertThat(changedTarget.getLastModifiedAt()).as("LastModifiedAt compared with saved createdAt")
                            .isNotEqualTo(changedTarget.getCreatedAt());
                    continue _founds;
                }
            }

            if (!foundTarget.getControllerId().equals(savedExtra.getControllerId())) {
                fail("The controllerId of the found target is not equal to the controllerId of the saved target");
            }
        }

        targetManagement.deleteTargets(savedExtra.getId());

        final int nr2Del = 50;
        int i = nr2Del;
        final Long[] deletedTargetIDs = new Long[nr2Del];
        final Target[] deletedTargets = new Target[nr2Del];

        final Iterator<Target> it = firstSaved.iterator();
        while (nr2Del > 0 && it.hasNext() && i > 0) {
            final Target pt = it.next();
            deletedTargetIDs[i - 1] = pt.getId();
            deletedTargets[i - 1] = pt;
            i--;
        }

        targetManagement.deleteTargets(deletedTargetIDs);

        final List<Target> found = targetManagement.findTargetsAll(new PageRequest(0, 200)).getContent();
        assertThat(firstSaved.spliterator().getExactSizeIfKnown() - nr2Del).as("Size of splited list")
                .isEqualTo(found.spliterator().getExactSizeIfKnown());

        assertThat(found).as("Not all undeleted found").doesNotContain(deletedTargets);
    }

    @Test
    @Description("Stores target attributes and verfies existence in the repository.")
    public void savingTargetControllerAttributes() {
        Iterable<Target> ts = targetManagement
                .createTargets(testdataFactory.generateTargets(100, "myCtrlID", "first description"));

        final Map<String, String> attribs = new HashMap<>();
        attribs.put("a.b.c", "abc");
        attribs.put("x.y.z", "");
        attribs.put("1.2.3", "123");
        attribs.put("1.2.3.4", "1234");
        attribs.put("1.2.3.4.5", "12345");
        final Set<String> attribs2Del = new HashSet<>();
        attribs2Del.add("x.y.z");
        attribs2Del.add("1.2.3");

        for (final Target t : ts) {
            JpaTargetInfo targetInfo = (JpaTargetInfo) t.getTargetInfo();
            targetInfo.setNew(false);
            for (final Entry<String, String> attrib : attribs.entrySet()) {
                final String key = attrib.getKey();
                final String value = String.format("%s-%s", attrib.getValue(), t.getControllerId());
                targetInfo.getControllerAttributes().put(key, value);
            }
            targetInfo = targetInfoRepository.save(targetInfo);
        }
        final Query qry = entityManager.createNativeQuery("select * from sp_target_attributes ta");
        final List<?> result = qry.getResultList();

        assertThat(attribs.size() * ts.spliterator().getExactSizeIfKnown()).as("Amount of all target attributes")
                .isEqualTo(result.size());

        for (final Target myT : ts) {
            final Target t = targetManagement.findTargetByControllerIDWithDetails(myT.getControllerId());
            assertThat(attribs.size()).as("Amount of target attributes per target")
                    .isEqualTo(t.getTargetInfo().getControllerAttributes().size());

            for (final Entry<String, String> ca : t.getTargetInfo().getControllerAttributes().entrySet()) {
                assertTrue("Attributes list does not contain target attribute key", attribs.containsKey(ca.getKey()));
                // has the same value: see string concatenation above
                // assertThat(String.format("%s-%s",
                // attribs.get(ca.getKey()))).as("Value of string
                // concatenation")
                // .isEqualTo(ca.getValue());

                assertEquals("The value of the string concatenation is not equal to the value of the target attributes",
                        String.format("%s-%s", attribs.get(ca.getKey()), t.getControllerId()), ca.getValue());

            }
        }

        ts = targetManagement.findTargetsAll(new PageRequest(0, 100)).getContent();
        final Iterator<Target> tsIt = ts.iterator();
        // all attributs of the target are deleted
        final Target[] ts2DelAllAttribs = new Target[] { tsIt.next(), tsIt.next(), tsIt.next() };
        // a few attributs are deleted
        final Target[] ts2DelAttribs = new Target[] { tsIt.next(), tsIt.next() };

        // perform the deletion operations accordingly
        for (final Target ta : ts2DelAllAttribs) {
            final Target t = targetManagement.findTargetByControllerIDWithDetails(ta.getControllerId());

            final JpaTargetInfo targetStatus = (JpaTargetInfo) t.getTargetInfo();
            targetStatus.getControllerAttributes().clear();
            targetInfoRepository.save(targetStatus);
        }

        for (final Target ta : ts2DelAttribs) {
            final Target t = targetManagement.findTargetByControllerIDWithDetails(ta.getControllerId());

            final JpaTargetInfo targetStatus = (JpaTargetInfo) t.getTargetInfo();
            for (final String attribKey : attribs2Del) {
                targetStatus.getControllerAttributes().remove(attribKey);
            }
            targetInfoRepository.save(targetStatus);
        }

        // only the number of the remaining targets and controller attributes
        // are checked
        final Iterable<JpaTarget> restTS = targetRepository.findAll();

        restTarget_: for (final Target targetl : restTS) {
            final Target target = targetManagement.findTargetByControllerIDWithDetails(targetl.getControllerId());

            // verify that all members of the list ts2DelAllAttribs don't have
            // any attributes
            for (final Target tNoAttribl : ts2DelAllAttribs) {
                final Target tNoAttrib = targetManagement.findTargetByControllerID(tNoAttribl.getControllerId());

                if (tNoAttrib.getControllerId().equals(target.getControllerId())) {
                    assertThat(target.getTargetInfo().getControllerAttributes())
                            .as("Controller attributes should be empty").isEmpty();
                    continue restTarget_;
                }
            }
            // verify that that the attribute list of all members of the list
            // ts2DelAttribs don't have
            // attributes which have been deleted
            for (final Target tNoAttribl : ts2DelAttribs) {
                final Target tNoAttrib = targetManagement.findTargetByControllerID(tNoAttribl.getControllerId());

                if (tNoAttrib.getControllerId().equals(target.getControllerId())) {
                    assertThat(target.getTargetInfo().getControllerAttributes().keySet().toArray())
                            .as("Controller attributes are wrong").doesNotContain(attribs2Del.toArray());
                    continue restTarget_;
                }
            }
        }
    }

    @Test
    @Description("Tests the assigment of tags to the a single target.")
    public void targetTagAssignment() {
        Target t1 = testdataFactory.generateTarget("id-1", "blablub");
        final int noT2Tags = 4;
        final int noT1Tags = 3;
        final List<TargetTag> t1Tags = tagManagement
                .createTargetTags(testdataFactory.generateTargetTags(noT1Tags, "tag1"));
        t1.getTags().addAll(t1Tags);
        t1 = targetManagement.createTarget(t1);

        Target t2 = testdataFactory.generateTarget("id-2", "blablub");
        final List<TargetTag> t2Tags = tagManagement
                .createTargetTags(testdataFactory.generateTargetTags(noT2Tags, "tag2"));
        t2.getTags().addAll(t2Tags);
        t2 = targetManagement.createTarget(t2);

        t1 = targetManagement.findTargetByControllerID(t1.getControllerId());
        assertThat(t1.getTags()).as("Tag size is wrong").hasSize(noT1Tags).containsAll(t1Tags);
        assertThat(t1.getTags()).as("Tag size is wrong").hasSize(noT1Tags)
                .doesNotContain(Iterables.toArray(t2Tags, TargetTag.class));

        t2 = targetManagement.findTargetByControllerID(t2.getControllerId());
        assertThat(t2.getTags()).as("Tag size is wrong").hasSize(noT2Tags).containsAll(t2Tags);
        assertThat(t2.getTags()).as("Tag size is wrong").hasSize(noT2Tags)
                .doesNotContain(Iterables.toArray(t1Tags, TargetTag.class));
    }

    @Test
    @Description("Tests the assigment of tags to multiple targets.")
    public void targetTagBulkAssignments() {
        final List<Target> tagATargets = targetManagement
                .createTargets(testdataFactory.generateTargets(10, "tagATargets", "first description"));
        final List<Target> tagBTargets = targetManagement
                .createTargets(testdataFactory.generateTargets(10, "tagBTargets", "first description"));
        final List<Target> tagCTargets = targetManagement
                .createTargets(testdataFactory.generateTargets(10, "tagCTargets", "first description"));

        final List<Target> tagABTargets = targetManagement
                .createTargets(testdataFactory.generateTargets(10, "tagABTargets", "first description"));

        final List<Target> tagABCTargets = targetManagement
                .createTargets(testdataFactory.generateTargets(10, "tagABCTargets", "first description"));

        final TargetTag tagA = tagManagement.createTargetTag(new JpaTargetTag("A"));
        final TargetTag tagB = tagManagement.createTargetTag(new JpaTargetTag("B"));
        final TargetTag tagC = tagManagement.createTargetTag(new JpaTargetTag("C"));
        tagManagement.createTargetTag(new JpaTargetTag("X"));

        // doing different assignments
        targetManagement.toggleTagAssignment(tagATargets, tagA);
        targetManagement.toggleTagAssignment(tagBTargets, tagB);
        targetManagement.toggleTagAssignment(tagCTargets, tagC);

        targetManagement.toggleTagAssignment(tagABTargets, tagA);
        targetManagement.toggleTagAssignment(tagABTargets, tagB);

        targetManagement.toggleTagAssignment(tagABCTargets, tagA);
        targetManagement.toggleTagAssignment(tagABCTargets, tagB);
        targetManagement.toggleTagAssignment(tagABCTargets, tagC);

        assertThat(targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, "X"))
                .as("Target count is wrong").isEqualTo(0);

        // search for targets with tag tagA
        final List<Target> targetWithTagA = new ArrayList<Target>();
        final List<Target> targetWithTagB = new ArrayList<Target>();
        final List<Target> targetWithTagC = new ArrayList<Target>();

        // storing target lists to enable easy evaluation
        Iterables.addAll(targetWithTagA, tagATargets);
        Iterables.addAll(targetWithTagA, tagABTargets);
        Iterables.addAll(targetWithTagA, tagABCTargets);

        Iterables.addAll(targetWithTagB, tagBTargets);
        Iterables.addAll(targetWithTagB, tagABTargets);
        Iterables.addAll(targetWithTagB, tagABCTargets);

        Iterables.addAll(targetWithTagC, tagCTargets);
        Iterables.addAll(targetWithTagC, tagABCTargets);

        // check the target lists as returned by assignTag
        checkTargetHasTags(false, targetWithTagA, tagA);
        checkTargetHasTags(false, targetWithTagB, tagB);
        checkTargetHasTags(false, targetWithTagC, tagC);

        checkTargetHasNotTags(tagATargets, tagB, tagC);
        checkTargetHasNotTags(tagBTargets, tagA, tagC);
        checkTargetHasNotTags(tagCTargets, tagA, tagB);

        // check again target lists refreshed from DB
        assertThat(targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, "A"))
                .as("Target count is wrong").isEqualTo(targetWithTagA.size());
        assertThat(targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, "B"))
                .as("Target count is wrong").isEqualTo(targetWithTagB.size());
        assertThat(targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, "C"))
                .as("Target count is wrong").isEqualTo(targetWithTagC.size());
    }

    @Test
    @Description("Tests the unassigment of tags to multiple targets.")
    public void targetTagBulkUnassignments() {
        final TargetTag targTagA = tagManagement.createTargetTag(new JpaTargetTag("Targ-A-Tag"));
        final TargetTag targTagB = tagManagement.createTargetTag(new JpaTargetTag("Targ-B-Tag"));
        final TargetTag targTagC = tagManagement.createTargetTag(new JpaTargetTag("Targ-C-Tag"));

        final List<Target> targAs = targetManagement
                .createTargets(testdataFactory.generateTargets(25, "target-id-A", "first description"));
        final List<Target> targBs = targetManagement
                .createTargets(testdataFactory.generateTargets(20, "target-id-B", "first description"));
        final List<Target> targCs = targetManagement
                .createTargets(testdataFactory.generateTargets(15, "target-id-C", "first description"));

        final List<Target> targABs = targetManagement
                .createTargets(testdataFactory.generateTargets(12, "target-id-AB", "first description"));
        final List<Target> targACs = targetManagement
                .createTargets(testdataFactory.generateTargets(13, "target-id-AC", "first description"));
        final List<Target> targBCs = targetManagement
                .createTargets(testdataFactory.generateTargets(7, "target-id-BC", "first description"));
        final List<Target> targABCs = targetManagement
                .createTargets(testdataFactory.generateTargets(17, "target-id-ABC", "first description"));

        targetManagement.toggleTagAssignment(targAs, targTagA);
        targetManagement.toggleTagAssignment(targABs, targTagA);
        targetManagement.toggleTagAssignment(targACs, targTagA);
        targetManagement.toggleTagAssignment(targABCs, targTagA);

        targetManagement.toggleTagAssignment(targBs, targTagB);
        targetManagement.toggleTagAssignment(targABs, targTagB);
        targetManagement.toggleTagAssignment(targBCs, targTagB);
        targetManagement.toggleTagAssignment(targABCs, targTagB);

        targetManagement.toggleTagAssignment(targCs, targTagC);
        targetManagement.toggleTagAssignment(targACs, targTagC);
        targetManagement.toggleTagAssignment(targBCs, targTagC);
        targetManagement.toggleTagAssignment(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA);
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targACs, targTagA, targTagC);
        checkTargetHasTags(true, targBCs, targTagB, targTagC);
        checkTargetHasTags(true, targABCs, targTagA, targTagB, targTagC);

        targetManagement.toggleTagAssignment(targCs, targTagC);
        targetManagement.toggleTagAssignment(targACs, targTagC);
        targetManagement.toggleTagAssignment(targBCs, targTagC);
        targetManagement.toggleTagAssignment(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA);
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targBCs, targTagB);
        checkTargetHasTags(true, targACs, targTagA);

        checkTargetHasNotTags(targCs, targTagC);
        checkTargetHasNotTags(targACs, targTagC);
        checkTargetHasNotTags(targBCs, targTagC);
        checkTargetHasNotTags(targABCs, targTagC);

    }

    @Test
    @Description("Retrieves targets by ID with lazy loading of the tags. Checks the successfull load.")
    public void findTargetsByControllerIDsWithTags() {
        final TargetTag targTagA = tagManagement.createTargetTag(new JpaTargetTag("Targ-A-Tag"));

        final List<Target> targAs = targetManagement
                .createTargets(testdataFactory.generateTargets(25, "target-id-A", "first description"));

        targetManagement.toggleTagAssignment(targAs, targTagA);

        assertThat(targetManagement.findTargetsByControllerIDsWithTags(
                targAs.stream().map(target -> target.getControllerId()).collect(Collectors.toList())))
                        .as("Target count is wrong").hasSize(25);

        // no lazy loading exception and tag correctly assigned
        assertThat(targetManagement
                .findTargetsByControllerIDsWithTags(
                        targAs.stream().map(target -> target.getControllerId()).collect(Collectors.toList()))
                .stream().map(target -> target.getTags().contains(targTagA)).collect(Collectors.toList()))
                        .as("Tags not correctly assigned").containsOnly(true);
    }

    @Test
    @Description("Test the optimized quere for retrieving all ID/name pairs of targets.")
    public void findAllTargetIdNamePaiss() {
        final List<Target> targAs = targetManagement
                .createTargets(testdataFactory.generateTargets(25, "target-id-A", "first description"));
        final String[] createdTargetIds = targAs.stream().map(t -> t.getControllerId())
                .toArray(size -> new String[size]);

        final List<TargetIdName> findAllTargetIdNames = targetManagement.findAllTargetIds();
        final List<String> findAllTargetIds = findAllTargetIdNames.stream().map(TargetIdName::getControllerId)
                .collect(Collectors.toList());

        assertThat(findAllTargetIds).as("Target list has wrong content").containsOnly(createdTargetIds);
    }

    @Test
    @Description("Test that NO TAG functionality which gives all targets with no tag assigned.")
    public void findTargetsWithNoTag() {

        final TargetTag targTagA = tagManagement.createTargetTag(new JpaTargetTag("Targ-A-Tag"));
        final List<Target> targAs = targetManagement
                .createTargets(testdataFactory.generateTargets(25, "target-id-A", "first description"));
        targetManagement.toggleTagAssignment(targAs, targTagA);

        targetManagement.createTargets(testdataFactory.generateTargets(25, "target-id-B", "first description"));

        final String[] tagNames = null;
        final List<Target> targetsListWithNoTag = targetManagement
                .findTargetByFilters(new PageRequest(0, 500), null, null, null, Boolean.TRUE, tagNames).getContent();

        assertThat(50).as("Total targets").isEqualTo(targetManagement.findAllTargetIds().size());
        assertThat(25).as("Targets with no tag").isEqualTo(targetsListWithNoTag.size());

    }

    @Test
    @Description("Tests the a target can be read with only the read target permission")
    public void targetCanBeReadWithOnlyReadTargetPermission() throws Exception {
        final String knownTargetControllerId = "readTarget";
        controllerManagament.findOrRegisterTargetIfItDoesNotexist(knownTargetControllerId, new URI("http://127.0.0.1"));

        securityRule.runAs(WithSpringAuthorityRule.withUser("bumlux", "READ_TARGET"), () -> {
            final Target findTargetByControllerID = targetManagement.findTargetByControllerID(knownTargetControllerId);
            assertThat(findTargetByControllerID).isNotNull();
            assertThat(findTargetByControllerID.getTargetInfo()).isNotNull();
            assertThat(findTargetByControllerID.getTargetInfo().getPollStatus()).isNotNull();
            return null;
        });

    }
}
