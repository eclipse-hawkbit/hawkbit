/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.resources;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * MCP resources providing hawkBit documentation for LLMs.
 */
@Slf4j
public class HawkBitDocumentationResource {

    private static final String DOCS_PATH = "hawkbit-docs/";

    @McpResource(
            uri = "hawkbit://docs/overview",
            name = "hawkBit Overview",
            description = "High-level introduction to hawkBit: interfaces (DDI, DMF, Management API), " +
                    "rollout management, and package model for IoT software updates")
    public String getOverview() {
        return loadDoc("README.md");
    }

    @McpResource(
            uri = "hawkbit://docs/what-is-hawkbit",
            name = "What is hawkBit",
            description = "Explains what hawkBit is, why IoT software updates matter, " +
                    "and scalability features for cloud deployments")
    public String getWhatIsHawkbit() {
        return loadDoc("what-is-hawkbit.md");
    }

    @McpResource(
            uri = "hawkbit://docs/quick-start",
            name = "Quick Start Guide",
            description = "Docker-based setup guides for monolith and microservices deployments, " +
                    "building from sources, and credential configuration")
    public String getQuickStart() {
        return loadDoc("quick-start.md");
    }

    @McpResource(
            uri = "hawkbit://docs/features",
            name = "Feature Overview",
            description = "Comprehensive feature list: device repository, software management, " +
                    "artifact delivery, rollout management, and API interfaces")
    public String getFeatures() {
        return loadDoc("features.md");
    }

    @McpResource(
            uri = "hawkbit://docs/architecture",
            name = "System Architecture",
            description = "Architecture overview with module diagram and third-party technology stack")
    public String getArchitecture() {
        return loadDoc("architecture.md");
    }

    @McpResource(
            uri = "hawkbit://docs/base-setup",
            name = "Production Setup",
            description = "Configuring production infrastructure with MariaDB/MySQL database " +
                    "and RabbitMQ for DMF (Device Management Federation) communication")
    public String getBaseSetup() {
        return loadDoc("base-setup.md");
    }

    @McpResource(
            uri = "hawkbit://docs/sdk",
            name = "SDK Guide",
            description = "hawkBit SDK for device and gateway integration: configuration properties, " +
                    "Maven dependencies, and usage examples with DdiTenant and MgmtAPI clients")
    public String getSdkGuide() {
        return loadDoc("hawkbit-sdk.md");
    }

    @McpResource(
            uri = "hawkbit://docs/feign-client",
            name = "Feign Client Guide",
            description = "Creating Feign-based REST clients for Management API and DDI API " +
                    "with Spring Boot integration examples")
    public String getFeignClientGuide() {
        return loadDoc("feign-client.md");
    }

    @McpResource(
            uri = "hawkbit://docs/clustering",
            name = "Clustering Guide",
            description = "Running hawkBit in clustered environments: Spring Cloud Stream event distribution, " +
                    "caching with TTL, scheduler behavior, and DoS filter constraints")
    public String getClusteringGuide() {
        return loadDoc("clustering.md");
    }

    @McpResource(
            uri = "hawkbit://docs/authentication",
            name = "Authentication",
            description = "Security token authentication (target and gateway tokens), certificate-based auth " +
                    "via reverse proxy, TLS/mTLS setup, and Nginx configuration examples")
    public String getAuthentication() {
        return loadDoc("authentication.md");
    }

    @McpResource(
            uri = "hawkbit://docs/authorization",
            name = "Authorization",
            description = "Fine-grained permission system for Management API/UI, DDI API authorization, " +
                    "permission groups, OpenID Connect support, and role-based access control")
    public String getAuthorization() {
        return loadDoc("authorization.md");
    }

    @McpResource(
            uri = "hawkbit://docs/datamodel",
            name = "Data Model",
            description = "Entity definitions: provisioning targets, distribution sets, software modules, " +
                    "artifacts, entity relationships, and soft/hard delete strategies")
    public String getDataModel() {
        return loadDoc("datamodel.md");
    }

    @McpResource(
            uri = "hawkbit://docs/rollout-management",
            name = "Rollout Management",
            description = "Rollout campaigns: cascading deployment groups, success/error thresholds, " +
                    "approval workflow, multi-assignments (beta), action weight prioritization, and state machines")
    public String getRolloutManagement() {
        return loadDoc("rollout-management.md");
    }

    @McpResource(
            uri = "hawkbit://docs/target-state",
            name = "Target State",
            description = "Target state definitions (UNKNOWN, IN_SYNC, PENDING, ERROR, REGISTERED) " +
                    "and state transition diagrams")
    public String getTargetState() {
        return loadDoc("targetstate.md");
    }

    @McpResource(
            uri = "hawkbit://docs/management-api",
            name = "Management API",
            description = "RESTful API for CRUD operations on targets and software: API versioning, " +
                    "HTTP methods, headers, error handling, and embedded Swagger UI reference")
    public String getManagementApi() {
        return loadDoc("management-api.md");
    }

    @McpResource(
            uri = "hawkbit://docs/ddi-api",
            name = "DDI API (Direct Device Integration)",
            description = "HTTP polling-based device integration API: state machine mapping, " +
                    "status feedback mechanisms, update retrieval, and embedded Swagger UI reference")
    public String getDdiApi() {
        return loadDoc("direct-device-integration-api.md");
    }

    @McpResource(
            uri = "hawkbit://docs/dmf-api",
            name = "DMF API (Device Management Federation)",
            description = "AMQP-based indirect device integration: message formats (THING_CREATED, etc.), " +
                    "exchanges, queues, bindings, and high-throughput service-to-service communication")
    public String getDmfApi() {
        return loadDoc("device-management-federation-api.md");
    }

    @McpResource(
            uri = "hawkbit://docs/entity-definitions",
            name = "hawkBit Entity Definitions",
            description = "RSQL filtering syntax for querying targets, rollouts, distribution sets, " +
                    "actions, software modules, and target filter queries with examples")
    public String getEntityDefinitions() {
        return loadResource("hawkbit-entity-definitions.md");
    }

    private String loadDoc(final String filename) {
        return loadResource(DOCS_PATH + filename);
    }

    private String loadResource(final String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load documentation: {}", path, e);
            return "Documentation not available. Please refer to the hawkBit documentation at https://eclipse.dev/hawkbit/";
        }
    }
}
