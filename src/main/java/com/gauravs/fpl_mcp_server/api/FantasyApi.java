package com.gauravs.fpl_mcp_server.api;

import static com.gauravs.fpl_mcp_server.util.FantasyConstants.FPL_BOOTSTRAP_STATIC_URL;
import static com.gauravs.fpl_mcp_server.util.FantasyConstants.FPL_FIXTURES_URL;
import static com.gauravs.fpl_mcp_server.util.FantasyConstants.FPL_PLAYERS_SUMMARY_URL;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gauravs.fpl_mcp_server.util.HttpClientService;

@Service
public class FantasyApi {
    
    public JsonNode getBootstrapStatic() throws Exception {
        String jsonResponse = new HttpClientService().getJsonAsString(FPL_BOOTSTRAP_STATIC_URL);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);

        if (root.has("phases") && root.get("phases").isArray()) {
            ArrayNode phases = (ArrayNode) root.get("phases");
            for (JsonNode phaseNode : phases) {
                if (phaseNode.has("highest_score") && phaseNode.get("highest_score").isNull()) {
                    ((ObjectNode) phaseNode).put("highest_score", 0);
                }
            }
        }
        return root;
    }

    public JsonNode getFixtures() throws Exception {
        String jsonResponse = new HttpClientService().getJsonAsString(FPL_FIXTURES_URL);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        return root;
    }

    public JsonNode getGameweeks() throws Exception {
        JsonNode staticData = getBootstrapStatic();
        JsonNode gameweeks = staticData.get("events");
        return gameweeks;
    }

    public JsonNode getCurrentGameweek() throws Exception {
        JsonNode gameweeks = getGameweeks();
        for (JsonNode gw : gameweeks) {
            if (gw.get("is_current").asBoolean()) {
                return gw;
            }
        }
        return gameweeks.get(0); // Fallback to first gameweek if none is current
    }

    public JsonNode getPlayerSummary(int playerId) throws Exception {
        try {
            String jsonResponse = new HttpClientService().getJsonAsString(FPL_PLAYERS_SUMMARY_URL + playerId);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode playerSummary = mapper.readTree(jsonResponse);
            return playerSummary;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error fetching player summary for ID: " + playerId, e);
        }        
    }

    public JsonNode getPlayers() throws Exception {
        JsonNode jsonrResponse = getBootstrapStatic();
        JsonNode players = jsonrResponse.get("elements");
        return players;
    }

    public JsonNode getTeams() throws Exception {
        JsonNode jsonrResponse = getBootstrapStatic();
        JsonNode teams = jsonrResponse.get("teams");
        return teams;
    }
}
