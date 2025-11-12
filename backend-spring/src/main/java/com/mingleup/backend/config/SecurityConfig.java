package com.mingleup.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // (추후) JWT 필터를 주입받는 곳
    // private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF, Form Login, HTTP Basic 비활성화
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 2. 세션 정책: STATELESS (JWT 사용)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // --- 인증 없이 접근 허용 ---
                        .requestMatchers(
                                "/",
                                "/error",
                                "/api/v1/auth/**" // 카카오 로그인 관련 경로는 모두 허용
                        ).permitAll()

                        // --- (예시) 호스트 권한이 필요한 경우 ---
                        // .requestMatchers("/api/v1/host/**").hasRole("HOST")

                        // --- 나머지 경로는 모두 인증 필요 ---
                        .anyRequest().authenticated()
                );

        // 4. (추후) JWT 필터 추가
        // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 5. CORS 설정 (개발 편의를 위해 임시로 모두 허용)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    /**
     * (임시) 개발용 CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // 모든 오리진 허용
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * (참고) MingleUp은 카카오 로그인을 사용하므로 당장 BCrypt는 필요 없으나,
     * Spring Security는 PasswordEncoder 빈을 요구하므로 생성해 둡니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}