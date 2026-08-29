package com.vivek.platform.apiorchestrator.service;

import com.vivek.platform.apiorchestrator.config.AppProperties;
import com.vivek.platform.apiorchestrator.exception.UnsafeUrlException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlSafetyValidatorTest {

    private AppProperties properties(boolean allowPrivate) {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setAllowPrivateNetworks(allowPrivate);
        return properties;
    }

    @Test
    @DisplayName("accepts an ordinary https URL")
    void acceptsPublicHttps() {
        UrlSafetyValidator validator = new UrlSafetyValidator(properties(false));

        assertThat(validator.validate("https://example.com/api/v1?x=1").getHost())
                .isEqualTo("example.com");
    }

    @Test
    @DisplayName("refuses schemes outside the allowlist")
    void refusesOtherSchemes() {
        UrlSafetyValidator validator = new UrlSafetyValidator(properties(true));

        assertThatThrownBy(() -> validator.validate("file:///etc/passwd"))
                .isInstanceOf(UnsafeUrlException.class)
                .hasMessageContaining("Unsupported URL scheme");
        assertThatThrownBy(() -> validator.validate("ftp://example.com/x"))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    @DisplayName("refuses relative and malformed URLs")
    void refusesMalformed() {
        UrlSafetyValidator validator = new UrlSafetyValidator(properties(true));

        assertThatThrownBy(() -> validator.validate("/just/a/path"))
                .isInstanceOf(UnsafeUrlException.class)
                .hasMessageContaining("absolute");
        assertThatThrownBy(() -> validator.validate("http://exa mple.com"))
                .isInstanceOf(UnsafeUrlException.class)
                .hasMessageContaining("Malformed");
    }

    @Test
    @DisplayName("blocks the cloud metadata endpoint even when private networks are allowed")
    void blocksMetadataHost() {
        UrlSafetyValidator validator = new UrlSafetyValidator(properties(true));

        assertThatThrownBy(() -> validator.validate("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(UnsafeUrlException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    @DisplayName("refuses loopback and private ranges when the policy is off")
    void refusesPrivateWhenDisallowed() {
        UrlSafetyValidator validator = new UrlSafetyValidator(properties(false));

        assertThatThrownBy(() -> validator.validate("http://127.0.0.1:8080/x"))
                .isInstanceOf(UnsafeUrlException.class)
                .hasMessageContaining("private address");
        assertThatThrownBy(() -> validator.validate("http://10.0.0.5/x"))
                .isInstanceOf(UnsafeUrlException.class);
        assertThatThrownBy(() -> validator.validate("http://192.168.1.1/x"))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    @DisplayName("allows loopback when the policy explicitly permits it")
    void allowsPrivateWhenPermitted() {
        UrlSafetyValidator validator = new UrlSafetyValidator(properties(true));

        assertThat(validator.validate("http://127.0.0.1:8080/x").getPort()).isEqualTo(8080);
    }

    @Test
    @DisplayName("honours a custom blocked-hosts list")
    void honoursCustomBlockList() {
        AppProperties properties = properties(true);
        properties.getSecurity().setBlockedHosts(List.of("internal.corp"));
        UrlSafetyValidator validator = new UrlSafetyValidator(properties);

        assertThatThrownBy(() -> validator.validate("https://internal.corp/secrets"))
                .isInstanceOf(UnsafeUrlException.class);
        assertThat(validator.validate("https://example.com/ok")).isNotNull();
    }
}
