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

import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Validator used in *Type window controllers to validate {@link ProxyType}.
 */
public class ProxyTypeValidator extends EntityValidator {

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
     * @param keyDuplicateCheck
     *            <code>true</code> if the entity key already exists in the
     *            repository
     * @param nameDuplicateCheck
     *            <code>true</code> if the entity name already exists in the
     *            repository
     * @return <code>true</code> if the entity is valid
     */
    public boolean isDsTypeValid(final ProxyType entity, final BooleanSupplier keyDuplicateCheck,
            final BooleanSupplier nameDuplicateCheck) {
        return mandatoryDsAttributesPresent(entity) && nameDoesNotExistInRepo(entity, nameDuplicateCheck)
                && keyDoesNotExistInRepo(entity, keyDuplicateCheck, "message.type.key.ds.duplicate.check");
    }

    /**
     * Checks if the entity is valid
     *
     * @param entity
     *            {@link ProxyTag}
     * @param keyDuplicateCheck
     *            <code>true</code> if the entity key already exists in the
     *            repository
     * @param nameDuplicateCheck
     *            <code>true</code> if the entity name already exists in the
     *            repository
     * @return <code>true</code> if the entity is valid
     */
    public boolean isSmTypeValid(final ProxyType entity, final BooleanSupplier keyDuplicateCheck,
            final BooleanSupplier nameDuplicateCheck) {
        return mandatorySmAttributesPresent(entity) && nameDoesNotExistInRepo(entity, nameDuplicateCheck)
                && keyDoesNotExistInRepo(entity, keyDuplicateCheck, "message.type.key.swmodule.duplicate.check");
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
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getKey())
                || CollectionUtils.isEmpty(entity.getSelectedSmTypes())) {
            displayValidationError(KEY_MISSING_NAME_OR_KEY);
            return false;
        }
        return true;
    }

    private boolean keyDoesNotExistInRepo(final ProxyType entity, final BooleanSupplier keyExistsInRepository,
            final String duplicateKeyMessageKey) {
        if (keyExistsInRepository.getAsBoolean()) {
            final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
            displayValidationError(duplicateKeyMessageKey, trimmedKey);
            return false;
        }
        return true;
    }

    private boolean nameDoesNotExistInRepo(final ProxyType entity, final BooleanSupplier nameExistsInRepository) {
        if (nameExistsInRepository.getAsBoolean()) {
            final String trimmedName = StringUtils.trimWhitespace(entity.getName());
            displayValidationError("message.type.duplicate.check", trimmedName);
            return false;
        }
        return true;
    }

}
