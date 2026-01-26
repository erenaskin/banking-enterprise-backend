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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    private RouteValidator validator; // Real object
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        validator = new RouteValidator(); // RouteValidator'ı gerçek nesne olarak kullanıyoruz
        authenticationFilter = new AuthenticationFilter(validator, jwtUtil, redisTemplate);
    }

    @Test
    void apply_WhenRouteIsNotSecured_ShouldPassThroughChain() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/register").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // RouteValidator'ın isSecured metodu false dönecek
        // (çünkü /auth/register openApiEndpoints içinde)
        // when(validator.isSecured.test(request)).thenReturn(false); // Predicate mocklanamaz

        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain).filter(exchange); // Zincirin çağrıldığını doğrula
    }

    @Test
    void apply_WhenSecuredRouteAndValidToken_ShouldForwardRequest() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // RouteValidator'ın isSecured metodu true dönecek
        // (çünkü /api/v1/accounts openApiEndpoints içinde değil)
        // when(validator.isSecured.test(request)).thenReturn(true); // Predicate mocklanamaz

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false)); // Token kara listede değil
        when(jwtUtil.extractUserId(anyString())).thenReturn("123"); // userId başarıyla çıkarıldı
        when(chain.filter(any())).thenReturn(Mono.empty()); // Zincir devam etti

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        verify(jwtUtil).validateToken("valid-token");
        verify(jwtUtil).extractUserId("valid-token");
        verify(chain).filter(any()); // Zincirin değiştirilmiş request ile çağrıldığını doğrula
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
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        ((ResponseStatusException) throwable).getReason().contains("Missing authorization header"))
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
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        ((ResponseStatusException) throwable).getReason().contains("Invalid authorization header"))
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

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(true)); // Token kara listede

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        ((ResponseStatusException) throwable).getReason().contains("Token has been blacklisted"))
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
        when(jwtUtil.validateToken(anyString())).thenThrow(new RuntimeException("Invalid JWT")); // JWT doğrulama hatası

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        ((ResponseStatusException) throwable).getReason().contains("Unauthorized access to application"))
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
        when(jwtUtil.extractUserId(anyString())).thenReturn(null); // userId null döndü

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                        ((ResponseStatusException) throwable).getReason().contains("Invalid Token: userId not found"))
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
        when(jwtUtil.extractUserId(anyString())).thenReturn("123");
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange filteredExchange = invocation.getArgument(0);
            // Yeni Correlation-ID'nin eklendiğini doğrula
            assert(filteredExchange.getRequest().getHeaders().getFirst("X-Correlation-ID") != null);
            return Mono.empty();
        });

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
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
        when(jwtUtil.extractUserId(anyString())).thenReturn("123");
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange filteredExchange = invocation.getArgument(0);
            // Mevcut Correlation-ID'nin korunduğunu doğrula
            assert(filteredExchange.getRequest().getHeaders().getFirst("X-Correlation-ID").equals(existingCorrelationId));
            return Mono.empty();
        });

        // Act
        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }
}