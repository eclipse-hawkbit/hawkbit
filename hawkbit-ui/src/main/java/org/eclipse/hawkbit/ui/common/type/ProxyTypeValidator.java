/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.type;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.AbstractValidator;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Validator used in *Type window controllers to validate {@link ProxyType}.
 */
public class ProxyTypeValidator extends AbstractValidator {

    private static final String KEY_MISSING_NAME_OR_KEY = "message.error.missing.typenameorkeyorsmtype";

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyTypeValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    /**
     * Checks if the entity is valid
     *
     * @param entity
     *            {@link ProxyTag}
     * @param keyHasChanged
     *            <code>true</code> if the entity key has changed
     * @param nameHasChanged
     *            <code>true</code> if the entity name has changed
     * @param keyExistsInRepository
     *            <code>true</code> if the entity key already exists in the
     *            repository
     * @param nameExistsInRepository
     *            <code>true</code> if the entity name already exists in the
     *            repository
     * @return <code>true</code> if the entity is valid
     */
    public boolean isDsTypeValid(final ProxyType entity, final boolean keyHasChanged, final boolean nameHasChanged,
            final BooleanSupplier keyExistsInRepository, final BooleanSupplier nameExistsInRepository) {
        return mandatoryDsAttributesPresent(entity) && nameValid(entity, nameHasChanged, nameExistsInRepository)
                && keyValid(entity, keyHasChanged, keyExistsInRepository, "message.type.key.ds.duplicate.check");
    }

    /**
     * Checks if the entity is valid
     *
     * @param entity
     *            {@link ProxyTag}
     * @param keyHasChanged
     *            <code>true</code> if the entity key has changed
     * @param nameHasChanged
     *            <code>true</code> if the entity name has changed
     * @param keyExistsInRepository
     *            <code>true</code> if the entity key already exists in the
     *            repository
     * @param nameExistsInRepository
     *            <code>true</code> if the entity name already exists in the
     *            repository
     * @return <code>true</code> if the entity is valid
     */
    public boolean isSmTypeValid(final ProxyType entity, final boolean keyHasChanged, final boolean nameHasChanged,
            final BooleanSupplier keyExistsInRepository, final BooleanSupplier nameExistsInRepository) {
        return mandatorySmAttributesPresent(entity) && nameValid(entity, nameHasChanged, nameExistsInRepository)
                && keyValid(entity, keyHasChanged, keyExistsInRepository, "message.type.key.swmodule.duplicate.check");
    }

    private boolean mandatorySmAttributesPresent(final ProxyType entity) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getKey())
                || entity.getSmTypeAssign() == null) {
            displayValidationError(KEY_MISSING_NAME_OR_KEY);
            return false;
        }
        return true;
    }

    private boolean mandatoryDsAttributesPresent(final ProxyType entity) {
        if (StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getKey())
                || CollectionUtils.isEmpty(entity.getSelectedSmTypes())) {
            displayValidationError(KEY_MISSING_NAME_OR_KEY);
            return false;
        }
        return true;
    }

    private boolean keyValid(final ProxyType entity, final boolean keyHasChanged,
            final BooleanSupplier keyExistsInRepository, final String duplicateKeyMessageKey) {
        if (keyHasChanged && keyExistsInRepository.getAsBoolean()) {
            final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
            displayValidationError(duplicateKeyMessageKey, trimmedKey);
            return false;
        }
        return true;
    }

    private boolean nameValid(final ProxyType entity, final boolean nameHasChanged,
            final BooleanSupplier nameExistsInRepository) {
        if (nameHasChanged && nameExistsInRepository.getAsBoolean()) {
            final String trimmedName = StringUtils.trimWhitespace(entity.getName());
            displayValidationError("message.type.duplicate.check", trimmedName);
            return false;
        }
        return true;
    }

}
