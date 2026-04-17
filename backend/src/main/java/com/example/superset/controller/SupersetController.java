package com.example.superset.controller;

import java.util.List;

import com.example.superset.dto.DashboardSummaryDto;
import com.example.superset.dto.GuestTokenRequest;
import com.example.superset.dto.GuestTokenResponse;
import com.example.superset.service.SupersetAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class SupersetController {

    private final SupersetAuthService supersetAuthService;

    public SupersetController(SupersetAuthService supersetAuthService) {
        this.supersetAuthService = supersetAuthService;
    }

    @PostMapping("/get-guest-token")
    public GuestTokenResponse getGuestToken(@Valid @RequestBody GuestTokenRequest request) {
        String guestToken = supersetAuthService.generateGuestToken(request.dashboardId());
        return new GuestTokenResponse(guestToken);
    }

    @GetMapping("/dashboards")
    public List<DashboardSummaryDto> dashboards() {
        return supersetAuthService.fetchDashboards();
    }
}
