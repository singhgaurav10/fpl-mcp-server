package com.gauravs.fpl_mcp_server.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class HttpClientService {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public HttpClientService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    // GET request returning JSON as String
    public String getJsonAsString(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
            
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("HTTP request failed with status: " + response.statusCode());
        }
    }
    
    // GET request returning parsed JSON object
    public <T> T getJsonObject(String url, Class<T> responseType) throws Exception {
        String jsonResponse = getJsonAsString(url);
        return objectMapper.readValue(jsonResponse, responseType);
    }
    
    // GET request returning List of objects
    public <T> List<T> getJsonList(String url, Class<T> elementType) throws Exception {
        String jsonResponse = getJsonAsString(url);
        return objectMapper.readValue(
            jsonResponse, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
        );
    }   
}
