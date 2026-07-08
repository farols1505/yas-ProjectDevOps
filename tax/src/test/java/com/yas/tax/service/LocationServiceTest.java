package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.tax.config.ServiceUrlConfig;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class LocationServiceTest {

    private static final String LOCATION_URL = "http://api.yas.local/location";

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private LocationService locationService;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        locationService = new LocationService(restClient, serviceUrlConfig);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);

        when(jwt.getTokenValue()).thenReturn("test-jwt-token");
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
        when(serviceUrlConfig.location()).thenReturn(LOCATION_URL);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStateOrProvinceAndCountryNames_shouldReturnLocationNames() {
        List<Long> stateOrProvinceIds = List.of(100L, 101L);
        URI url = UriComponentsBuilder.fromUriString(LOCATION_URL)
            .path("/backoffice/state-or-provinces/state-country-names")
            .queryParam("stateOrProvinceIds", stateOrProvinceIds)
            .build()
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        List<StateOrProvinceAndCountryGetNameVm> expected = List.of(
            new StateOrProvinceAndCountryGetNameVm(100L, "California", "United States"),
            new StateOrProvinceAndCountryGetNameVm(101L, "Texas", "United States")
        );
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(expected);

        List<StateOrProvinceAndCountryGetNameVm> result =
            locationService.getStateOrProvinceAndCountryNames(stateOrProvinceIds);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void getStateOrProvinceAndCountryNames_shouldThrowWhenRemoteFails() {
        List<Long> stateOrProvinceIds = List.of(100L);
        URI url = UriComponentsBuilder.fromUriString(LOCATION_URL)
            .path("/backoffice/state-or-provinces/state-country-names")
            .queryParam("stateOrProvinceIds", stateOrProvinceIds)
            .build()
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        RuntimeException remoteError = new RuntimeException("location service unavailable");
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenThrow(remoteError);

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> locationService.getStateOrProvinceAndCountryNames(stateOrProvinceIds));

        assertSame(remoteError, thrown);
    }

    @Test
    void handleLocationNameListFallback_shouldRethrowOriginalThrowable() {
        RuntimeException expected = new RuntimeException("fallback error");

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> locationService.handleLocationNameListFallback(expected));

        assertSame(expected, thrown);
    }
}
