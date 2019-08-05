/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.hono;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HonoTargetSync implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(HonoTargetSync.class);

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Value("${hawkbit.server.repository.hono-sync.devices-uri}")
    private String honoDevicesEndpoint;

    @Override
    public void run() {
        try {
            Map<Long, Target> honoTargets = getAllTargets();
            List<Long> targetIdsToDelete = new ArrayList<>();

            long count = targetManagement.count();
            for (int i = 0; count > 0; count -= 100, ++i) {
                Slice<Target> slice = targetManagement.findAll(PageRequest.of(i, 100));
                for (Target target : slice) {
                    Long id = target.getId();

                    if (honoTargets.containsKey(id)) {
                        // TODO: Update Target if necessary! What properties will be synchronized?
                        // Remove target from map since it won't be needed anymore
                        honoTargets.remove(id);
                    } else {
                        targetIdsToDelete.add(id);
                    }
                }
            }

            if (!targetIdsToDelete.isEmpty()) {
                targetManagement.delete(targetIdsToDelete);
            }

            // At this point honoTargets only contains objects which were not found in hawkBit's target repository
            for (Map.Entry<Long, Target> entry : honoTargets.entrySet()) {
                targetManagement.create(entityFactory.target().create()
                        .controllerId(entry.getKey().toString())
                );
            }
        } catch (IOException | NoSuchFieldException | ParseException e) {
            LOG.error("Could not parse hono api response", e);
        }
    }

    private Map<Long, Target> getAllTargets() throws IOException, ParseException, NoSuchFieldException {

        Map<Long, Target> honoTargets = new HashMap<>();

        // Initialize the total variable with an arbitrary number > 0 so it starts fetching targets. It will be updated
        // with the correct number during the interpretation of the first response.
        long total = Long.MAX_VALUE;

        JSONParser parser = new JSONParser();
        while (honoTargets.size() < total) {
            URL url = new URL(honoDevicesEndpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes("offset=" + honoTargets.size());
            outputStream.flush();
            outputStream.close();

            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                String totalHeader = connection.getHeaderField("Total");
                if (totalHeader == null) {
                    throw new NoSuchFieldException("Response does not contain expected HTTP header \"Total\".");
                }
                total = Long.parseLong(totalHeader);

                Object response = parser.parse(connection.getInputStream());
                if (response instanceof JSONArray) {
                    JSONArray targetArray = (JSONArray) response;
                    for (Object object : targetArray) {
                        JSONObject target = (JSONObject) object;
                        Long controllerId = (Long) target.get("controllerId");

                        honoTargets.put(controllerId, entityFactory.target().create()
                                .controllerId(String.valueOf(controllerId))
                                .build());
                    }
                }
            }
            else {
                throw new IOException("Received HTTP status code " + statusCode + " connecting to " + url.toString());
            }
        }

        return honoTargets;
    }
}
