package com.lps.tools.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import java.time.Duration;

/**
 * @author Yale
 */
@Configuration
class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(5000) // 增加最大连接数
                .pendingAcquireMaxCount(20000) // 增加待处理获取队列大小
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .responseTimeout(Duration.ofMillis(300000)); // 设置响应超时时间为5分钟

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}