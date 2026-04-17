package com.example.superset.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.example.superset.dto.DashboardSummaryDto;
import com.example.superset.dto.SupersetGuestTokenApiRequest;
import com.example.superset.dto.SupersetGuestTokenApiResponse;
import com.example.superset.dto.SupersetLoginRequest;
import com.example.superset.dto.SupersetLoginResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class SupersetAuthService {

    private final WebClient webClient;
    private final Duration timeout;

    @Value("${superset.base-url}")
    private String supersetBaseUrl;

    @Value("${superset.username}")
    private String supersetUsername;

    @Value("${superset.password}")
    private String supersetPassword;

    @Value("${superset.provider}")
    private String supersetProvider;

    @Value("${superset.guest-user-username}")
    private String guestUsername;

    @Value("${superset.guest-user-first-name}")
    private String guestFirstName;

    @Value("${superset.guest-user-last-name}")
    private String guestLastName;

    public SupersetAuthService(WebClient webClient, Duration requestTimeout) {
        this.webClient = webClient;
        this.timeout = requestTimeout;
    }

    public String generateGuestToken(String dashboardId) {
        try {
            String accessToken = loginAndGetAccessToken();
            String csrfToken = fetchCsrfToken(accessToken);
            SupersetGuestTokenApiResponse response = null;
            WebClientResponseException lastError = null;

            for (String resourceId : resolveResourceCandidates(accessToken, dashboardId)) {
                try {
                    response = requestGuestToken(accessToken, csrfToken, resourceId);
                    break;
                } catch (WebClientResponseException ex) {
                    lastError = ex;
                }
            }

            if (response == null || response.token() == null || response.token().isBlank()) {
                if (lastError != null) {
                    throw new SupersetServiceException(
                            "Superset guest token request failed: "
                                    + lastError.getStatusCode()
                                    + " - "
                                    + lastError.getResponseBodyAsString(),
                            lastError
                    );
                }
                throw new SupersetServiceException("Superset returned an empty guest token");
            }

            return response.token();
        } catch (WebClientResponseException ex) {
            throw new SupersetServiceException(
                    "Superset guest token request failed: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (Exception ex) {
            if (ex instanceof SupersetServiceException serviceException) {
                throw serviceException;
            }
            throw new SupersetServiceException("Failed to generate Superset guest token", ex);
        }
    }

    public List<DashboardSummaryDto> fetchDashboards() {
        try {
            String accessToken = loginAndGetAccessToken();

            JsonNode response = webClient.get()
                    .uri(supersetBaseUrl + "/api/v1/dashboard/?q=(page:0,page_size:100)")
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(timeout)
                    .block();

            if (response == null || response.get("result") == null || !response.get("result").isArray()) {
                throw new SupersetServiceException("Unexpected response when loading dashboards from Superset");
            }

            List<DashboardSummaryDto> dashboards = new ArrayList<>();
            for (JsonNode dashboardNode : response.get("result")) {
                String id = dashboardNode.path("id").asText();
                String uuid = dashboardNode.path("uuid").asText("");
                String embeddedId = getEmbeddedId(accessToken, id);
                String title = dashboardNode.path("dashboard_title").asText(
                        dashboardNode.path("title").asText("Dashboard " + id)
                );
                String slug = dashboardNode.path("slug").asText("");

                dashboards.add(new DashboardSummaryDto(id, uuid, embeddedId, title, slug));
            }

            return dashboards;
        } catch (WebClientResponseException ex) {
            throw new SupersetServiceException(
                    "Superset dashboard fetch failed: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (Exception ex) {
            if (ex instanceof SupersetServiceException serviceException) {
                throw serviceException;
            }
            throw new SupersetServiceException("Failed to load Superset dashboards", ex);
        }
    }

    private String loginAndGetAccessToken() {
        SupersetLoginRequest request = new SupersetLoginRequest(
                supersetUsername,
                supersetPassword,
                supersetProvider,
                true
        );

        try {
            SupersetLoginResponse response = webClient.post()
                    .uri(supersetBaseUrl + "/api/v1/security/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SupersetLoginResponse.class)
                    .timeout(timeout)
                    .block();

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new SupersetServiceException("Superset login returned an empty access token");
            }
            return response.accessToken();
        } catch (WebClientResponseException ex) {
            throw new SupersetServiceException(
                    "Superset login failed: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    private String fetchCsrfToken(String accessToken) {
        try {
            JsonNode response = webClient.get()
                    .uri(supersetBaseUrl + "/api/v1/security/csrf_token/")
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(timeout)
                    .block();

            String csrfToken = response == null ? "" : response.path("result").asText("");
            if (csrfToken.isBlank()) {
                throw new SupersetServiceException("Superset returned an empty CSRF token");
            }
            return csrfToken;
        } catch (WebClientResponseException ex) {
            throw new SupersetServiceException(
                    "Superset CSRF token request failed: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    private SupersetGuestTokenApiResponse requestGuestToken(String accessToken, String csrfToken, String resourceId) {
        SupersetGuestTokenApiRequest.Resource resource =
                new SupersetGuestTokenApiRequest.Resource("dashboard", resourceId);
        SupersetGuestTokenApiRequest.User user =
                new SupersetGuestTokenApiRequest.User(guestUsername, guestFirstName, guestLastName);

        SupersetGuestTokenApiRequest payload = new SupersetGuestTokenApiRequest(
                List.of(resource),
                user,
                Collections.emptyList()
        );

        return webClient.post()
                .uri(supersetBaseUrl + "/api/v1/security/guest_token/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    headers.setBearerAuth(accessToken);
                    headers.add("X-CSRFToken", csrfToken);
                    headers.add("Referer", supersetBaseUrl);
                })
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(SupersetGuestTokenApiResponse.class)
                .timeout(timeout)
                .block();
    }

    private List<String> resolveResourceCandidates(String accessToken, String dashboardIdentifier) {
        Set<String> candidates = new LinkedHashSet<>();
        if (dashboardIdentifier != null && !dashboardIdentifier.isBlank()) {
            candidates.add(dashboardIdentifier);
        }

        String embeddedFromIdentifier = getEmbeddedId(accessToken, dashboardIdentifier);
        if (!embeddedFromIdentifier.isBlank()) {
            candidates.add(embeddedFromIdentifier);
        }

        String dashboardNumericId = findDashboardNumericIdByUuid(accessToken, dashboardIdentifier);
        if (!dashboardNumericId.isBlank()) {
            candidates.add(dashboardNumericId);

            String embeddedFromNumeric = getEmbeddedId(accessToken, dashboardNumericId);
            if (!embeddedFromNumeric.isBlank()) {
                candidates.add(embeddedFromNumeric);
            }
        }

        return new ArrayList<>(candidates);
    }

    private String findDashboardNumericIdByUuid(String accessToken, String dashboardUuid) {
        if (dashboardUuid == null || dashboardUuid.isBlank()) {
            return "";
        }

        try {
            String query = String.format(
                    "(filters:!((col:uuid,opr:eq,value:'%s')),page:0,page_size:1)",
                    dashboardUuid
            );

            JsonNode response = webClient.get()
                    .uri(supersetBaseUrl + "/api/v1/dashboard/?q=" + query)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(timeout)
                    .block();

            JsonNode first = response == null ? null : response.path("result").path(0);
            if (first == null || first.isMissingNode()) {
                return "";
            }

            return first.path("id").asText("");
        } catch (Exception ex) {
            return "";
        }
    }

    private String getEmbeddedId(String accessToken, String dashboardId) {
        try {
            JsonNode response = webClient.get()
                    .uri(supersetBaseUrl + "/api/v1/dashboard/" + dashboardId + "/embedded")
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(timeout)
                    .block();

            String embeddedId = response == null ? "" : response.path("result").path("uuid").asText("");
            if (!embeddedId.isBlank()) {
                return embeddedId;
            }
            return "";
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return "";
            }
            throw new SupersetServiceException(
                    "Superset embedded config fetch failed: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

}
