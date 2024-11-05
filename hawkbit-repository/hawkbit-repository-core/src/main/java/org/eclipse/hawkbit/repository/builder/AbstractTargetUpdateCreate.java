/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import java.net.URI;
import java.util.Optional;

import lombok.ToString;
import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAddressException;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T> update or create builder interface
 */
public class AbstractTargetUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {

    @ValidString
    protected String controllerId;

    protected String address;

    @ToString.Exclude
    @ValidString
    protected String securityToken;

    protected Long lastTargetQuery;
    protected TargetUpdateStatus status;

    protected Boolean requestAttributes;

    protected Long targetTypeId;

    protected AbstractTargetUpdateCreate(final String controllerId) {
        this.controllerId = StringUtils.trimWhitespace(controllerId);
    }

    public T status(final TargetUpdateStatus status) {
        this.status = status;
        return (T) this;
    }

    public T address(final String address) {
        // check if this is a real URI
        if (address != null) {
            try {
                URI.create(StringUtils.trimWhitespace(address));
            } catch (final IllegalArgumentException e) {
                throw new InvalidTargetAddressException(
                        "The given address " + address + " violates the RFC-2396 specification", e);
            }
        }
        this.address = address;
        return (T) this;
    }

    public T securityToken(final String securityToken) {
        this.securityToken = StringUtils.trimWhitespace(securityToken);
        return (T) this;
    }

    public T requestAttributes(final Boolean requestAttributes) {
        this.requestAttributes = requestAttributes;
        return (T) this;
    }

    public T lastTargetQuery(final Long lastTargetQuery) {
        this.lastTargetQuery = lastTargetQuery;
        return (T) this;
    }

    public TargetCreate controllerId(final String controllerId) {
        this.controllerId = StringUtils.trimWhitespace(controllerId);
        return (TargetCreate) this;
    }

    public T targetType(final Long targetTypeId) {
        this.targetTypeId = targetTypeId;
        return (T) this;
    }

    public String getControllerId() {
        return controllerId;
    }

    public Optional<String> getAddress() {
        return Optional.ofNullable(address);
    }

    public Optional<String> getSecurityToken() {
        return Optional.ofNullable(securityToken);
    }

    public Optional<Long> getLastTargetQuery() {
        return Optional.ofNullable(lastTargetQuery);
    }

    public Optional<TargetUpdateStatus> getStatus() {
        return Optional.ofNullable(status);
    }

    public Long getTargetTypeId() {
        return targetTypeId;
    }

}
