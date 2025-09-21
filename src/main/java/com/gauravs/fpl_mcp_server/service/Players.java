package com.gauravs.fpl_mcp_server.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gauravs.fpl_mcp_server.api.FantasyApi;

@Component
public class Players {

    @Autowired
    private FantasyApi fantasyApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String,Object>> getPlayersResource(String nameFilter, String teamFilter) {
        try {
            // ----- Get raw data -----
            JsonNode data = fantasyApi.getBootstrapStatic();   // JsonNode from HTTP call

            // ----- Create team and position lookup maps -----
            Map<Integer, JsonNode> teamMap = new HashMap<>();
            for (JsonNode t : data.withArray("teams")) {
                teamMap.put(t.path("id").asInt(), t);
            }

            Map<Integer, JsonNode> positionMap = new HashMap<>();
            for (JsonNode p : data.withArray("element_types")) {
                positionMap.put(p.path("id").asInt(), p);
            }

            System.out.println("Team map size: " + teamMap.size());
            System.out.println("Position map size: " + positionMap.size());

            // ----- Format player data -----
            List<Map<String,Object>> players = new ArrayList<>();
            for (JsonNode player : data.withArray("elements")) {

                JsonNode teamNode     = teamMap.get(player.path("team").asInt());
                JsonNode positionNode = positionMap.get(player.path("element_type").asInt());

                String playerName = player.path("first_name").asText() + " "
                                  + player.path("second_name").asText();
                String teamName   = teamNode != null
                        ? teamNode.path("name").asText("Unknown")
                        : "Unknown";

                // Apply filters if present
                if (nameFilter != null &&
                    !playerName.toLowerCase().contains(nameFilter.toLowerCase())) {
                    continue;
                }
                if (teamFilter != null &&
                    !teamName.toLowerCase().contains(teamFilter.toLowerCase())) {
                    continue;
                }

                Map<String,Object> playerData = new LinkedHashMap<>();
                playerData.put("id", player.path("id").asInt());
                playerData.put("name", playerName);
                playerData.put("web_name", player.path("web_name").asText());
                playerData.put("team", teamName);
                playerData.put("team_short", teamNode != null
                        ? teamNode.path("short_name").asText("UNK") : "UNK");
                playerData.put("position", positionNode != null
                        ? positionNode.path("singular_name_short").asText("UNK") : "UNK");
                playerData.put("price", player.path("now_cost").asDouble() / 10.0);
                playerData.put("form", player.path("form").asText());
                playerData.put("points", player.path("total_points").asInt());
                playerData.put("points_per_game", player.path("points_per_game").asText());

                // Playing time
                playerData.put("minutes", player.path("minutes").asInt());
                playerData.put("starts", player.path("starts").asInt());

                // Key stats
                playerData.put("goals", player.path("goals_scored").asInt());
                playerData.put("assists", player.path("assists").asInt());
                playerData.put("clean_sheets", player.path("clean_sheets").asInt());
                playerData.put("goals_conceded", player.path("goals_conceded").asInt());
                playerData.put("own_goals", player.path("own_goals").asInt());
                playerData.put("penalties_saved", player.path("penalties_saved").asInt());
                playerData.put("penalties_missed", player.path("penalties_missed").asInt());
                playerData.put("yellow_cards", player.path("yellow_cards").asInt());
                playerData.put("red_cards", player.path("red_cards").asInt());
                playerData.put("saves", player.path("saves").asInt());
                playerData.put("bonus", player.path("bonus").asInt());
                playerData.put("bps", player.path("bps").asInt());

                // Advanced metrics
                playerData.put("influence", player.path("influence").asText());
                playerData.put("creativity", player.path("creativity").asText());
                playerData.put("threat", player.path("threat").asText());
                playerData.put("ict_index", player.path("ict_index").asText());

                // Expected stats
                playerData.put("expected_goals",
                        player.has("expected_goals") ? player.path("expected_goals").asText() : "N/A");
                playerData.put("expected_assists",
                        player.has("expected_assists") ? player.path("expected_assists").asText() : "N/A");
                playerData.put("expected_goal_involvements",
                        player.has("expected_goal_involvements") ? player.path("expected_goal_involvements").asText() : "N/A");
                playerData.put("expected_goals_conceded",
                        player.has("expected_goals_conceded") ? player.path("expected_goals_conceded").asText() : "N/A");

                // Ownership & transfers
                playerData.put("selected_by_percent", player.path("selected_by_percent").asText());
                playerData.put("transfers_in_event", player.path("transfers_in_event").asInt());
                playerData.put("transfers_out_event", player.path("transfers_out_event").asInt());

                // Price changes
                playerData.put("cost_change_event", player.path("cost_change_event").asDouble() / 10.0);
                playerData.put("cost_change_start", player.path("cost_change_start").asDouble() / 10.0);

                // Status info
                playerData.put("status", player.path("status").asText());
                playerData.put("news", player.path("news").asText());
                playerData.put("chance_of_playing_next_round",
                        player.path("chance_of_playing_next_round").isMissingNode()
                                ? null
                                : player.path("chance_of_playing_next_round").asText());

                players.add(playerData);
            }

            System.out.println("Formatted " + players.size() + " players");
            return players;
        } catch (Exception e) {
            throw new RuntimeException("Failed to format player data", e);
        }
    }

    public Map<String, Object> getPlayerById(int playerId) {
        // Get all players synchronously
        List<Map<String, Object>> allPlayers = getPlayersResource(null, null);

        for (Map<String, Object> player : allPlayers) {
            if (((Number) player.get("id")).intValue() == playerId) {
                try {
                    // Get detailed summary from API
                    JsonNode summary = fantasyApi.getPlayerSummary(playerId); // synchronous

                    // Add fixture history
                    if (summary.has("history")) {
                        List<Map<String, Object>> history = objectMapper.convertValue(
                                summary.get("history"),
                                new TypeReference<List<Map<String, Object>>>() {}
                        );
                        player.put("history", history);
                    } else {
                        player.put("history", Collections.emptyList());
                    }

                    // Add upcoming fixtures
                    if (summary.has("fixtures")) {
                        List<Map<String, Object>> fixtures = objectMapper.convertValue(
                                summary.get("fixtures"),
                                new TypeReference<List<Map<String, Object>>>() {}
                        );
                        player.put("fixtures", fixtures);
                    } else {
                        player.put("fixtures", Collections.emptyList());
                    }

                    return player;
                } catch (Exception e) {
                    // Return basic player data if detailed data not available
                    return player;
                }
            }
        }

        // Player not found
        return null;
    }

    public List<Map<String, Object>> findPlayersByName(String name, int limit) {
        if (name == null || name.isBlank()) {
            return Collections.emptyList();
        }

        // Get all players (synchronously)
        List<Map<String, Object>> allPlayers = getPlayersResource(null, null);

        // Normalize search term
        String searchTerm = name.toLowerCase().trim();

        // Nicknames mapping (modifiable if needed)
        Map<String, String> nicknames = new HashMap<>();
        nicknames.put("kdb", "kevin de bruyne");
        nicknames.put("vvd", "virgil van dijk");
        nicknames.put("taa", "trent alexander-arnold");
        nicknames.put("cr7", "cristiano ronaldo");
        nicknames.put("bobby", "roberto firmino");
        nicknames.put("mo salah", "mohamed salah");
        nicknames.put("mane", "sadio mane");
        nicknames.put("auba", "aubameyang");
        nicknames.put("lewa", "lewandowski");
        nicknames.put("kane", "harry kane");
        nicknames.put("rashford", "marcus rashford");
        nicknames.put("son", "heung-min son");

        // Use nickname if exists
        final String normalizedSearchTerm = nicknames.getOrDefault(searchTerm, searchTerm);
        final String[] searchParts = normalizedSearchTerm.split("\\s+");

        List<ScoredPlayer> scoredPlayers = new ArrayList<>();

        for (Map<String, Object> player : allPlayers) {
            String fullName = ((String) player.get("name")).toLowerCase();
            String webName = player.getOrDefault("web_name", "").toString().toLowerCase();

            String[] nameParts = fullName.split("\\s+");
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : "";

            double points = player.get("points") != null ? Double.parseDouble(player.get("points").toString()) : 0;

            int score = 0;

            // 1. Exact full name match
            if (normalizedSearchTerm.equals(fullName)) score += 100;

            // 2. Exact web name match
            else if (normalizedSearchTerm.equals(webName)) score += 90;

            // 3. Last name match
            else if (searchParts.length == 1 && normalizedSearchTerm.equals(lastName)) score += 80;

            // 4. First name match
            else if (searchParts.length == 1 && normalizedSearchTerm.equals(firstName)) score += 70;

            // 5. Initials match
            if (normalizedSearchTerm.length() <= 5 && normalizedSearchTerm.chars().allMatch(Character::isLetter)) {
                String initials = Arrays.stream(nameParts).filter(p -> !p.isEmpty())
                                        .map(p -> p.substring(0, 1))
                                        .collect(Collectors.joining());
                if (normalizedSearchTerm.equalsIgnoreCase(initials)) score += 85;
            }

            // 6. Multi-part name match
            if (searchParts.length > 1) {
                if (firstName.contains(searchParts[0]) && lastName.contains(searchParts[searchParts.length - 1])) score += 75;

                String searchCombined = String.join("", searchParts);
                String fullCombined = String.join("", nameParts);
                if (fullCombined.contains(searchCombined)) score += 50;
            }

            // 7. Substring in full name
            if (fullName.contains(normalizedSearchTerm)) score += 40;

            // 8. Partial word matches in full name
            for (String part : searchParts) {
                if (fullName.contains(part)) score += 30;
            }

            // 9. Partial word matches in web name
            for (String part : searchParts) {
                if (webName.contains(part)) score += 25;
            }

            // 10. Points bonus
            double pointsScore = Math.min(20, points / 50);
            double totalScore = score > 0 ? score + pointsScore : 0;

            if (score > 0) {
                scoredPlayers.add(new ScoredPlayer(totalScore, player));
            }
        }

        // Sort by score descending
        scoredPlayers.sort((a, b) -> Double.compare(b.score, a.score));

        // Fallback if no good matches
        if (scoredPlayers.isEmpty() || scoredPlayers.get(0).score < 30) {
            List<Map<String, Object>> fallback = allPlayers.stream()
                    .filter(p -> ((String)p.get("name")).toLowerCase().contains(normalizedSearchTerm)
                            || ((String)p.getOrDefault("web_name","")).toLowerCase().contains(normalizedSearchTerm))
                    .sorted((a,b) -> Double.compare(
                            Double.parseDouble(b.get("points").toString()),
                            Double.parseDouble(a.get("points").toString())
                    ))
                    .collect(Collectors.toList());

            Set<Object> seenIds = scoredPlayers.stream()
                    .map(sp -> sp.player.get("id"))
                    .collect(Collectors.toSet());

            for (Map<String,Object> p : fallback) {
                if (!seenIds.contains(p.get("id"))) {
                    scoredPlayers.add(new ScoredPlayer(0, p));
                }
            }
        }

        // Return limited results
        return scoredPlayers.stream()
                .limit(limit)
                .map(sp -> sp.player)
                .collect(Collectors.toList());
    }

    // Helper class to keep player and score
    private static class ScoredPlayer {
        double score;
        Map<String,Object> player;

        public ScoredPlayer(double score, Map<String,Object> player) {
            this.score = score;
            this.player = player;
        }
    }

}
