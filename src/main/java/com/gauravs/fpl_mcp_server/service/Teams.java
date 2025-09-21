package com.gauravs.fpl_mcp_server.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.gauravs.fpl_mcp_server.api.FantasyApi;

@Component
public class Teams {

    @Autowired
    private FantasyApi fantasyApi;
    
    public List<Map<String, Object>> getTeamsResource() throws Exception {
        // Get raw data from API synchronously
        JsonNode data = fantasyApi.getBootstrapStatic();  // JsonNode

        List<Map<String, Object>> teams = new ArrayList<>();

        for (JsonNode team : data.withArray("teams")) {
            Map<String, Object> teamData = new LinkedHashMap<>();
            teamData.put("id", team.path("id").asInt());
            teamData.put("name", team.path("name").asText());
            teamData.put("short_name", team.path("short_name").asText());
            teamData.put("code", team.path("code").asText());

            // Strength ratings
            teamData.put("strength", team.path("strength").asInt());
            teamData.put("strength_overall_home", team.path("strength_overall_home").asInt());
            teamData.put("strength_overall_away", team.path("strength_overall_away").asInt());
            teamData.put("strength_attack_home", team.path("strength_attack_home").asInt());
            teamData.put("strength_attack_away", team.path("strength_attack_away").asInt());
            teamData.put("strength_defence_home", team.path("strength_defence_home").asInt());
            teamData.put("strength_defence_away", team.path("strength_defence_away").asInt());

            // Performance stats
            teamData.put("position", team.path("position").asInt());

            teams.add(teamData);
        }

        // Sort by position (league standing)
        teams.sort(Comparator.comparingInt(t -> (int) t.get("position")));

        return teams;
    }


    public Map<String, Object> getTeamById(int teamId) throws Exception {
        // Get all teams synchronously
        List<Map<String, Object>> teams = getTeamsResource();

        for (Map<String, Object> team : teams) {
            if ((int) team.get("id") == teamId) {
                return team;
            }
        }

        // Return null if not found
        return null;
    }


    public Map<String, Object> getTeamByName(String name) throws Exception {
        if (name == null || name.isEmpty()) return null;

        List<Map<String, Object>> teams = getTeamsResource();
        String nameLower = name.toLowerCase();

        // Try exact match first
        for (Map<String, Object> team : teams) {
            String teamName = ((String) team.get("name")).toLowerCase();
            String shortName = ((String) team.get("short_name")).toLowerCase();

            if (teamName.equals(nameLower) || shortName.equals(nameLower)) {
                return team;
            }
        }

        // Try partial match
        for (Map<String, Object> team : teams) {
            String teamName = ((String) team.get("name")).toLowerCase();
            String shortName = ((String) team.get("short_name")).toLowerCase();

            if (teamName.contains(nameLower) || shortName.contains(nameLower)) {
                return team;
            }
        }

        return null; // Not found
    }

}
