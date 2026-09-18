package com.service.analytics.infrastructure.tool;

import com.service.analytics.domain.agent.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tool that looks up an IP address's reputation against a threat-intel
 * service (abuseIPDB / VirusTotal). This is a stub returning a mock result;
 * wire it to a real API in production.
 */
@Component
public class IpReputationTool implements Tool {

    @Override
    public String name() {
        return "lookup_ip_reputation";
    }

    @Override
    public String description() {
        return "Look up the reputation of an IP address from a threat-intel service.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "ip", Map.of("type", "string", "description", "The IP address to look up")
                ),
                "required", java.util.List.of("ip")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String ip = String.valueOf(arguments.getOrDefault("ip", ""));
        // Stub: in production, call abuseIPDB/VirusTotal API here.
        return "IP " + ip + " reputation: score 97, reported for SSH brute force";
    }
}