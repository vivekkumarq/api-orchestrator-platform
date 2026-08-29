package com.vivek.platform.apiorchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Central configuration for the execution engine, outbound-request safety policy and CORS.
 * Bound from the {@code app.*} prefix; see {@code application.yaml} and the README for defaults.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Executor executor = new Executor();
    private final Security security = new Security();
    private final Cors cors = new Cors();

    public Executor getExecutor() { return executor; }
    public Security getSecurity() { return security; }
    public Cors getCors() { return cors; }

    public static class Executor {
        /** Timeout applied when a request does not specify one. */
        private int defaultTimeoutMs = 10_000;
        /** Hard ceiling: a request may not ask for a longer timeout than this. */
        private int maxTimeoutMs = 60_000;
        /** Largest response body we will buffer in memory. Protects against OOM on huge payloads. */
        private int maxResponseBytes = 1_048_576;
        /** Response bodies longer than this are truncated before being written to history. */
        private int maxPersistedBodyChars = 20_000;
        /** Hard ceiling on the number of retries a request may ask for. */
        private int maxRetries = 5;

        public int getDefaultTimeoutMs() { return defaultTimeoutMs; }
        public void setDefaultTimeoutMs(int defaultTimeoutMs) { this.defaultTimeoutMs = defaultTimeoutMs; }

        public int getMaxTimeoutMs() { return maxTimeoutMs; }
        public void setMaxTimeoutMs(int maxTimeoutMs) { this.maxTimeoutMs = maxTimeoutMs; }

        public int getMaxResponseBytes() { return maxResponseBytes; }
        public void setMaxResponseBytes(int maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }

        public int getMaxPersistedBodyChars() { return maxPersistedBodyChars; }
        public void setMaxPersistedBodyChars(int maxPersistedBodyChars) { this.maxPersistedBodyChars = maxPersistedBodyChars; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }

    public static class Security {
        /**
         * When false, requests that resolve to loopback, link-local, site-local or any-local
         * addresses are rejected. This is the SSRF guard: leave it false anywhere the backend
         * can reach infrastructure the caller should not be able to probe.
         */
        private boolean allowPrivateNetworks = true;
        /** URI schemes the executor is willing to speak. */
        private List<String> allowedSchemes = new ArrayList<>(List.of("http", "https"));
        /** Host names that are always rejected, regardless of the private-network policy. */
        private List<String> blockedHosts =
                new ArrayList<>(List.of("169.254.169.254", "metadata.google.internal"));

        public boolean isAllowPrivateNetworks() { return allowPrivateNetworks; }
        public void setAllowPrivateNetworks(boolean allowPrivateNetworks) { this.allowPrivateNetworks = allowPrivateNetworks; }

        public List<String> getAllowedSchemes() { return allowedSchemes; }
        public void setAllowedSchemes(List<String> allowedSchemes) { this.allowedSchemes = allowedSchemes; }

        public List<String> getBlockedHosts() { return blockedHosts; }
        public void setBlockedHosts(List<String> blockedHosts) { this.blockedHosts = blockedHosts; }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }
}
