/*
 * Copyright (c) 1997-2023 by Bosch.IO GmbH
 * http://www.bosch.io
 * All rights reserved,
 * also regarding any disposal, exploitation, reproduction,
 * editing, distribution, as well as in the event of
 * applications for industrial property rights.
 *
 * This software is the confidential and proprietary information
 * of Bosch.IO GmbH. You shall not disclose
 * such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you
 * entered into with Bosch.IO GmbH.
 */
package org.eclipse.hawkbit.security.util;

import org.eclipse.hawkbit.security.HeaderAuthentication;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

public class DecodeUtil {

  public static Collection<Object> decodeHeaderAuthenticationCollection(final Collection<?> credentials) {
    final Collection<Object> result = new ArrayList<>();
    for (Object obj : credentials) {
      if (obj instanceof HeaderAuthentication) {
        result.add(decodeHeaderAuthentication(obj));
      } else {
        result.add(obj);
      }
    }
    return result;
  }

  public static HeaderAuthentication decodeHeaderAuthentication(Object obj) {
    HeaderAuthentication headerAuthentication = (HeaderAuthentication)obj;

    final String controllerId = headerAuthentication.getControllerId();
    final String headerAuth = headerAuthentication.getHeaderAuth();
    final String decodedControllerId = UriUtils.decode(controllerId, StandardCharsets.UTF_8);

    return new HeaderAuthentication(decodedControllerId, headerAuth);
  }

}
