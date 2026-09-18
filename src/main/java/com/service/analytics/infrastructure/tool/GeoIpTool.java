package com.service.analytics.infrastructure.tool;

import com.service.analytics.domain.agent.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tool that geolocates an IP address (MaxMind GeoIP). Stub returning a mock
 * result; wire it to a real GeoIP database/API in production.
 */
@Component
public class GeoIpTool implements Tool {

    @Override
    public String name() {
        return "enrich_geoip";
    }

    @Override
    public String description() {
        return "Geolocate an IP address to country and city.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "ip", Map.of("type", "string", "description", "The IP address to geolocate")
                ),
                "required", java.util.List.of("ip")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String ip = String.valueOf(arguments.getOrDefault("ip", ""));
        // Stub: in production, query MaxMind GeoIP here.
        return "IP " + ip + " geolocation: country=NL, city=Amsterdam";
    }
}