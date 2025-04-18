package com.lps.tools.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

public class HttpUtil {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final RestTemplate restTemplate = new RestTemplate();

    public static JsonNode get(String url, String token, String githubApiVersion) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", githubApiVersion);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
        return mapper.readTree(response);
    }
}