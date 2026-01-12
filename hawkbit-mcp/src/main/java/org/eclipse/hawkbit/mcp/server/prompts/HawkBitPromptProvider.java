/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.prompts;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MCP prompts for hawkBit that provide initial context to LLMs.
 * <p>
 * These prompts help LLMs understand what hawkBit is and what documentation
 * resources are available at the start of a session.
 * </p>
 */
@Slf4j
public class HawkBitPromptProvider {

    private static final String PROMPTS_PATH = "prompts/";

    @McpPrompt(
            name = "hawkbit-context",
            description = "Provides initial context about hawkBit, available tools, and documentation resources. " +
                    "Use this prompt at the start of a session to understand what you can do with hawkBit MCP.")
    public GetPromptResult getHawkBitContext() {
        return new GetPromptResult(
                "hawkBit MCP Server Context",
                List.of(new PromptMessage(Role.ASSISTANT, new TextContent(loadPrompt("hawkbit-context.md"))))
        );
    }

    @McpPrompt(
            name = "rsql-help",
            description = "Explains RSQL query syntax for filtering hawkBit entities. " +
                    "Use this when you need help constructing filter queries for targets, rollouts, etc.")
    public GetPromptResult getRsqlHelp() {
        return new GetPromptResult(
                "RSQL Query Help",
                List.of(new PromptMessage(Role.ASSISTANT, new TextContent(loadPrompt("rsql-help.md"))))
        );
    }

    private String loadPrompt(final String filename) {
        try {
            ClassPathResource resource = new ClassPathResource(PROMPTS_PATH + filename);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt: {}", filename, e);
            return "Prompt content not available.";
        }
    }
}
