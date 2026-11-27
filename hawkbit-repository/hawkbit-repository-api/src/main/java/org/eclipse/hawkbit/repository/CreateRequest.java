/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

/**
 * Marker for the create requests of a {@link RepositoryManagement}.
 * <p/>
 * It bounds the create request type parameter so its declared (erased) type is not {@link Object}. Hibernate Validator treats a
 * {@code @Valid} parameter of a type that a container (e.g. {@link Iterable}, {@link java.util.Map}, {@link java.util.Optional}) could be
 * assigned to as a potential container (deprecated cascading, HV000272) and resolves the value extractors at runtime.
 */
public interface CreateRequest {}
