/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.error.extractors;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.ui.error.UiErrorDetails;

public abstract class AbstractSingleUiErrorDetailsExtractor implements UiErrorDetailsExtractor {

    @Override
    public List<UiErrorDetails> extractErrorDetailsFrom(final Throwable error) {
        return findDetails(error).map(Collections::singletonList).orElseGet(Collections::emptyList);
    }

    protected abstract Optional<UiErrorDetails> findDetails(Throwable error);
}
