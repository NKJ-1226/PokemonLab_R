
package com.example.pokemonlab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Securityのアクセス制御を設定するクラス
 *
 * ホーム画面・図鑑・クイズなどを一般公開し、
 * それ以外のURLには認証を要求する
 */
@Configuration
public class SecurityConfig {

    /**
     * URLごとのアクセス権限を設定する
     *
     * @param http HTTPセキュリティ設定
     * @return セキュリティフィルターチェーン
     * @throws Exception セキュリティ設定に失敗した場合
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/pokedex",
                                "/quiz/**",
                                "/ranking",
                                "/pokedex/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error" 
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
