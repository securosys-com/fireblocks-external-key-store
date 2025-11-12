// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.filter;

import com.securosys.fireblocks.configuration.CustomServerProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyFilter.class);

    @SuppressWarnings("unused")
    private final CustomServerProperties properties;
    private final String configuredApiKey;

    public ApiKeyFilter(CustomServerProperties properties) throws RuntimeException {
        this.properties = properties;
        this.configuredApiKey = properties.getFireblocksAgentConfiguration().getApiAuthorization();

        // Crash the server at startup if no API key is configured.
        // Store configuredApiKey in a class field to make sure that afterwards it is always non-blank.
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            String msg = "No API key configured! Configuring an API key is required for security reasons.";
            LOGGER.error(msg);
            throw new RuntimeException("No API key configured");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri.contains("/swagger-ui") || uri.contains("/v3/api-docs") || "/".equals(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            // This should not happen, it thrown an exception at startup.
            LOGGER.warn("No API key configured");
            filterChain.doFilter(request, response);
            return;
        }

        String jsonResponse;
        String headerApiKey = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (headerApiKey == null || !headerApiKey.equals(configuredApiKey)) {
//            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API Key");
//            return;
            String msg = String.format("Unauthorized Access: A client attempted to access a restricted endpoint without proper authorization. Request-URL: '%s'", request.getRequestURL());
            jsonResponse = "{ \"errorCode\": " +HttpStatus.UNAUTHORIZED.value()+ ", \"message\": \"" + msg + "\" }";
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(jsonResponse);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("apiKeyUser", null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
