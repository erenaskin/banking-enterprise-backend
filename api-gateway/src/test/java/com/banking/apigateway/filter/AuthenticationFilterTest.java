package com.banking.apigateway.filter;

import com.banking.apigateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        RouteValidator validator = new RouteValidator();
        authenticationFilter = new AuthenticationFilter(validator, jwtUtil, redisTemplate);
    }

    @Test
    void apply_WhenRouteIsNotSecured_ShouldPassThroughChain() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/register").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    void apply_WhenSecuredRouteAndValidToken_ShouldForwardRequest() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        doNothing().when(jwtUtil).validateToken(anyString());
        when(jwtUtil.extractUserId(anyString())).thenReturn("123");
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil).validateToken("valid-token");
        verify(jwtUtil).extractUserId("valid-token");
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void apply_WhenSecuredRouteAndMissingAuthHeader_ShouldReturnUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException e &&
                        e.getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        e.getReason() != null && e.getReason().contains("Missing or invalid authorization header"))
                .verify();
    }

    @Test
    void apply_WhenSecuredRouteAndEmptyAuthHeaderList_ShouldReturnUnauthorized() {
        // Arrange
        // Mocking request with empty Authorization header list
        MockServerHttpRequest request = mock(MockServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.AUTHORIZATION, Collections.emptyList());
        
        when(request.getHeaders()).thenReturn(headers);
        // lenient() kullanarak gereksiz stub hatasını önlüyoruz, çünkü RouteValidator kullanıyor olabilir
        lenient().when(request.getURI()).thenReturn(java.net.URI.create("/api/v1/accounts"));
        lenient().when(request.getPath()).thenReturn(org.springframework.http.server.RequestPath.parse("/api/v1/accounts", "/"));
        
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/accounts").build());
        // We need to spy on exchange to return our mocked request
        ServerWebExchange spyExchange = spy(exchange);
        when(spyExchange.getRequest()).thenReturn(request);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(spyExchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException e &&
                        e.getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        e.getReason() != null && e.getReason().contains("Missing or invalid authorization header"))
                .verify();
    }

    @Test
    void apply_WhenSecuredRouteAndInvalidAuthHeaderFormat_ShouldReturnUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "InvalidTokenFormat")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException e &&
                        e.getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        e.getReason() != null && e.getReason().contains("Missing or invalid authorization header"))
                .verify();
    }

    @Test
    void apply_WhenTokenIsBlacklisted_ShouldReturnUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer blacklisted-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(true));

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException e &&
                        e.getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        e.getReason() != null && e.getReason().contains("Token has been blacklisted"))
                .verify();
    }

    @Test
    void apply_WhenJwtValidationFails_ShouldReturnUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        doThrow(new RuntimeException("Invalid JWT")).when(jwtUtil).validateToken(anyString());

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException e &&
                        e.getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        e.getReason() != null && e.getReason().contains("Unauthorized access to application"))
                .verify();
    }

    @Test
    void apply_WhenUserIdExtractionFails_ShouldReturnUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-without-userid")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        doNothing().when(jwtUtil).validateToken(anyString());
        when(jwtUtil.extractUserId(anyString())).thenReturn(null);

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException e &&
                        e.getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        e.getReason() != null && e.getReason().contains("Invalid Token: userId not found"))
                .verify();
    }

    @Test
    void apply_WhenCorrelationIdIsMissing_ShouldGenerateNewOne() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        doNothing().when(jwtUtil).validateToken(anyString());
        when(jwtUtil.extractUserId(anyString())).thenReturn("123");
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange filteredExchange = invocation.getArgument(0);
            assertNotNull(filteredExchange.getRequest().getHeaders().getFirst("X-Correlation-ID"));
            return Mono.empty();
        });

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void apply_WhenCorrelationIdIsEmptyString_ShouldGenerateNewOne() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .header("X-Correlation-ID", "")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        doNothing().when(jwtUtil).validateToken(anyString());
        when(jwtUtil.extractUserId(anyString())).thenReturn("123");
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange filteredExchange = invocation.getArgument(0);
            String correlationId = filteredExchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
            assertNotNull(correlationId);
            assertNotEquals("", correlationId);
            return Mono.empty();
        });

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void apply_WhenCorrelationIdExists_ShouldUseExistingOne() {
        // Arrange
        String existingCorrelationId = "existing-corr-id";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .header("X-Correlation-ID", existingCorrelationId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        doNothing().when(jwtUtil).validateToken(anyString());
        when(jwtUtil.extractUserId(anyString())).thenReturn("123");
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange filteredExchange = invocation.getArgument(0);
            assertEquals(existingCorrelationId, filteredExchange.getRequest().getHeaders().getFirst("X-Correlation-ID"));
            return Mono.empty();
        });

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
    }
}