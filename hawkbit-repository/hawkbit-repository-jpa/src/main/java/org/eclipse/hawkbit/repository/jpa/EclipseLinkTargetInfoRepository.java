/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

/**
 * Custom repository implementation as standard spring repository fails as of
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=415027 .
 *
 */
// @Service
// @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
// public class EclipseLinkTargetInfoRepository implements TargetRepository {
//
// @Autowired
// private EntityManager entityManager;
//
// @Override
// @Modifying
// @Transactional(isolation = Isolation.READ_UNCOMMITTED)
// public void setTargetUpdateStatus(final TargetUpdateStatus status, final
// List<Long> targets) {
// final Query query = entityManager.createQuery(
// "update JpaTargetInfo ti set ti.updateStatus = :status where ti.targetId in
// :targets and ti.updateStatus != :status");
// query.setParameter("targets", targets);
// query.setParameter("status", status);
//
// }
//
// }
