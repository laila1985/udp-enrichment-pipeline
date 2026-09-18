package com.service.analytics.infrastructure.config;

import com.service.analytics.application.agent.AnalysisAgent;
import com.service.analytics.application.agent.IngestionAgent;
import com.service.analytics.application.agent.OrchestratorAgent;
import com.service.analytics.application.agent.ReportingAgent;
import com.service.analytics.application.service.PermissionService;
import com.service.analytics.application.service.SessionService;
import com.service.analytics.domain.port.LlmClient;
import com.service.analytics.domain.port.MessagePublisher;
import com.service.analytics.domain.port.PermissionStore;
import com.service.analytics.domain.port.SessionStore;
import com.service.analytics.domain.port.ToolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the application layer (use cases and agents) to their ports.
 * The application layer depends only on interfaces; concrete adapters are
 * injected here by Spring.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public SessionService sessionService(SessionStore sessionStore) {
        return new SessionService(sessionStore);
    }

    @Bean
    public PermissionService permissionService(PermissionStore permissionStore) {
        return new PermissionService(permissionStore);
    }

    @Bean
    public IngestionAgent ingestionAgent(MessagePublisher messagePublisher) {
        return new IngestionAgent(messagePublisher);
    }

    @Bean
    public AnalysisAgent analysisAgent(LlmClient llmClient, ToolExecutor toolExecutor) {
        return new AnalysisAgent(llmClient, toolExecutor);
    }

    @Bean
    public ReportingAgent reportingAgent(LlmClient llmClient) {
        return new ReportingAgent(llmClient);
    }

    @Bean
    public OrchestratorAgent orchestratorAgent(
            IngestionAgent ingestionAgent,
            AnalysisAgent analysisAgent,
            ReportingAgent reportingAgent) {
        return new OrchestratorAgent(ingestionAgent, analysisAgent, reportingAgent);
    }
}