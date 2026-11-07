
package com.example.pokemonlab.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring Securityのアクセス制御を設定するクラス
 *
 * ホーム画面・図鑑・クイズなどを一般公開する。
 * H2 Consoleは本番環境では利用できないようにする。
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Environment environment) throws Exception {

        // 本番環境ではH2 Consoleを許可しない
        boolean h2ConsoleEnabled = environment.getProperty(
                "spring.h2.console.enabled",
                Boolean.class,
                false
        );

        RequestMatcher h2Console = PathRequest.toH2Console();

        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/",
                            "/pokedex",
                            "/quiz/**",
                            "/ranking",
                            "/pokedex/**",
                            "/css/**",
                            "/js/**",
                            "/images/**",
                            "/error"
                    ).permitAll();

                    if (h2ConsoleEnabled) {
                        auth.requestMatchers(h2Console).permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .formLogin(Customizer.withDefaults());

        if (h2ConsoleEnabled) {
            http
                    .csrf(csrf -> csrf.ignoringRequestMatchers(h2Console))
                    .headers(headers -> headers
                            .frameOptions(frame -> frame.sameOrigin())
                    );
        }

        return http.build();
    }
}
