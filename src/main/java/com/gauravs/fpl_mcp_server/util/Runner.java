package com.gauravs.fpl_mcp_server.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.gauravs.fpl_mcp_server.service.Fixtures;

public class Runner {
    public static void main(String[] args) throws Exception {
        Fixtures service = new Fixtures();
        //Players service = new Players();
        //Teams service = new Teams();
        String ps = service.getTeamNameById(20);
        String jsonResponse = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(ps);
        Path filePath = Path.of("Response.json");
        Files.writeString(filePath, jsonResponse, StandardCharsets.UTF_8);
    }
}
