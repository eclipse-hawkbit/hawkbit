/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Security")
@Stories("SecurityToken Generator Test")
public class SecurityTokenGeneratorTest {

    @Test
    public void test() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final SecurityTokenGenerator securityTokenGenerator = new SecurityTokenGenerator();
        for (int index = 0; index < 1; index++) {
            System.out.println(securityTokenGenerator.generateToken());
        }
    }

}
