/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

@SpringApplicationConfiguration(classes = {
        org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration.class })
@TestPropertySource(locations = "classpath:/jpa-test.properties")
public abstract class AbstractJpaIntegrationTest extends AbstractIntegrationTest {

    protected static final String INVALID_TEXT_HTML = "</noscript><br><script>";
    protected static final String NOT_EXIST_ID = "1234";
    protected static final long NOT_EXIST_IDL = Long.parseLong(NOT_EXIST_ID);

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected TargetRepository targetRepository;

    @Autowired
    protected ActionRepository actionRepository;

    @Autowired
    protected DistributionSetRepository distributionSetRepository;

    @Autowired
    protected SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    protected TenantMetaDataRepository tenantMetaDataRepository;

    @Autowired
    protected DistributionSetTypeRepository distributionSetTypeRepository;

    @Autowired
    protected SoftwareModuleTypeRepository softwareModuleTypeRepository;

    @Autowired
    protected TargetTagRepository targetTagRepository;

    @Autowired
    protected DistributionSetTagRepository distributionSetTagRepository;

    @Autowired
    protected SoftwareModuleMetadataRepository softwareModuleMetadataRepository;

    @Autowired
    protected ActionStatusRepository actionStatusRepository;

    @Autowired
    protected LocalArtifactRepository artifactRepository;

    @Autowired
    protected RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    protected RolloutTargetGroupRepository rolloutTargetGroupRepository;

    @Autowired
    protected RolloutRepository rolloutRepository;

    @Autowired
    protected TenantConfigurationProperties tenantConfigurationProperties;

    @Transactional(readOnly = true)
    protected List<Action> findActionsByRolloutAndStatus(final Rollout rollout, final Action.Status actionStatus) {
        return Lists.newArrayList(actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), actionStatus));
    }

    protected static void verifyThrownExceptionBy(final ThrowingCallable tc, final String objectType) {
        Assertions.assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(tc)
                .withMessageContaining(NOT_EXIST_ID).withMessageContaining(objectType);
    }

    protected TargetTagAssignmentResult toggleTagAssignment(final Collection<Target> targets, final TargetTag tag) {
        return targetManagement.toggleTagAssignment(
                targets.stream().map(Target::getControllerId).collect(Collectors.toList()), tag.getName());
    }

    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<DistributionSet> sets,
            final DistributionSetTag tag) {
        return distributionSetManagement.toggleTagAssignment(
                sets.stream().map(DistributionSet::getId).collect(Collectors.toList()), tag.getName());
    }
}
