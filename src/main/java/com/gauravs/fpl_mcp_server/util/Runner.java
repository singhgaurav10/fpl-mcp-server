package com.gauravs.fpl_mcp_server.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.gauravs.fpl_mcp_server.api.FantasyApi;

public class Runner {
    public static void main(String[] args) throws Exception {
        FantasyApi api = new FantasyApi();
        JsonNode response = api.getTeams();
        String jsonResponse = response.toString();
        Path filePath = Path.of("Response.json");
        Files.writeString(filePath, jsonResponse, StandardCharsets.UTF_8);
    }
}
