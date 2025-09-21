package com.gauravs.fpl_mcp_server.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gauravs.fpl_mcp_server.api.FantasyApi;

@Component
public class Gameweeks {

    @Autowired
    private FantasyApi fantasyApi;

    public List<Map<String,Object>> getGameweeksResource() throws Exception {
        JsonNode gameweeks = fantasyApi.getGameweeks();
        List<Map<String,Object>> formatted = new ArrayList<>();

        if (gameweeks != null && gameweeks.isArray()) {
            for (JsonNode gw : gameweeks) {
                Map<String,Object> gwData = new LinkedHashMap<>();
                gwData.put("id", gw.path("id").asInt());
                gwData.put("name", gw.path("name").asText());
                gwData.put("deadline_time", gw.path("deadline_time").asText());
                gwData.put("is_current", gw.path("is_current").asBoolean());
                gwData.put("is_next", gw.path("is_next").asBoolean());
                gwData.put("is_previous", gw.path("is_previous").asBoolean());
                gwData.put("finished", gw.path("finished").asBoolean());
                gwData.put("data_checked", gw.path("data_checked").asBoolean());

                // Optional fields: use asInt/asText with default for null
                gwData.put("highest_score", gw.hasNonNull("highest_score") ? gw.get("highest_score").asInt() : null);
                gwData.put("most_selected", gw.hasNonNull("most_selected") ? gw.get("most_selected").asInt() : null);
                gwData.put("most_transferred_in", gw.hasNonNull("most_transferred_in") ? gw.get("most_transferred_in").asInt() : null);
                gwData.put("most_captained", gw.hasNonNull("most_captained") ? gw.get("most_captained").asInt() : null);
                gwData.put("most_vice_captained", gw.hasNonNull("most_vice_captained") ? gw.get("most_vice_captained").asInt() : null);
                gwData.put("average_entry_score", gw.hasNonNull("average_entry_score") ? gw.get("average_entry_score").asInt() : null);

                formatted.add(gwData);
            }
        }

        return formatted;   }

    public Map<String,Object> getCurrentGameweekResource() {
        try {
            // --- Get data from your API layer ---
            JsonNode currentGw = fantasyApi.getCurrentGameweek();      // JsonNode
            JsonNode allData   = fantasyApi.getBootstrapStatic();      // JsonNode
            JsonNode fixtures  = fantasyApi.getFixtures();             // JsonNode array

            Map<String,Object> gwData = new LinkedHashMap<>();
            gwData.put("id",           currentGw.path("id").asInt());
            gwData.put("name",         currentGw.path("name").asText());
            gwData.put("deadline_time",currentGw.path("deadline_time").asText());
            gwData.put("is_current",   currentGw.path("is_current").asBoolean());
            gwData.put("is_next",      currentGw.path("is_next").asBoolean());
            gwData.put("finished",     currentGw.path("finished").asBoolean());
            gwData.put("data_checked", currentGw.path("data_checked").asBoolean());
            gwData.put("status", currentGw.path("is_current").asBoolean() ? "Current" : "Next");

            // --- Format deadline & calculate time until deadline ---
            String deadlineStr = currentGw.path("deadline_time").asText();
            try {
                Instant deadline = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(deadlineStr));
                ZonedDateTime zdt = deadline.atZone(ZoneOffset.UTC);
                String formatted = zdt.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy 'at' HH:mm 'UTC'"));
                gwData.put("deadline_formatted", formatted);

                Duration delta = Duration.between(Instant.now(), deadline);
                if (!delta.isNegative()) {
                    long days = delta.toDays();
                    long hours = delta.minusDays(days).toHours();
                    long minutes = delta.minusDays(days).minusHours(hours).toMinutes();

                    List<String> timeParts = new ArrayList<>();
                    if (days > 0)    timeParts.add(days + " day" + (days != 1 ? "s" : ""));
                    if (hours > 0)   timeParts.add(hours + " hour" + (hours != 1 ? "s" : ""));
                    if (minutes > 0) timeParts.add(minutes + " minute" + (minutes != 1 ? "s" : ""));
                    gwData.put("time_until_deadline",
                               timeParts.isEmpty() ? "Deadline passed" : String.join(", ", timeParts));
                } else {
                    gwData.put("time_until_deadline", "Deadline passed");
                }
            } catch (DateTimeParseException e) {
                gwData.put("deadline_formatted", deadlineStr);
            }

            // --- Stats if available ---
            if (!currentGw.path("highest_score").isMissingNode() && !currentGw.path("highest_score").isNull()) {
                Map<String,Object> stats = new HashMap<>();
                stats.put("highest_score", currentGw.path("highest_score").asInt());
                stats.put("average_score", currentGw.path("average_entry_score").isMissingNode()
                        ? "N/A" : currentGw.path("average_entry_score").asInt());
                stats.put("chip_plays",
                          new ObjectMapper().convertValue(currentGw.path("chip_plays"),
                                                          new TypeReference<List<Map<String,Object>>>() {}));
                gwData.put("stats", stats);
            }

            // --- Popular players ---
            Map<Integer,JsonNode> playerMap = new HashMap<>();
            for (JsonNode p : allData.withArray("elements")) {
                playerMap.put(p.path("id").asInt(), p);
            }
            Map<String,Object> popularPlayers = new LinkedHashMap<>();
            List<Map.Entry<String,String>> popularFields = List.of(
                    Map.entry("most_selected","Most Selected"),
                    Map.entry("most_transferred_in","Most Transferred In"),
                    Map.entry("most_captained","Most Captained"),
                    Map.entry("most_vice_captained","Most Vice Captained")
            );
            for (var field : popularFields) {
                JsonNode idNode = currentGw.path(field.getKey());
                if (idNode.isInt()) {
                    JsonNode player = playerMap.get(idNode.asInt());
                    if (player != null) {
                        Map<String,Object> pInfo = new HashMap<>();
                        pInfo.put("id", player.path("id").asInt());
                        pInfo.put("name", player.path("first_name").asText() + " " + player.path("second_name").asText());
                        pInfo.put("web_name", player.path("web_name").asText());
                        pInfo.put("team", player.path("team").asInt());
                        popularPlayers.put(field.getValue(), pInfo);
                    }
                }
            }
            if (!popularPlayers.isEmpty()) {
                gwData.put("popular_players", popularPlayers);
            }

            // --- Fixtures count ---
            if (fixtures != null && fixtures.isArray()) {
                int fixtureCount = 0;
                int currentId = currentGw.path("id").asInt();
                for (JsonNode f : fixtures) {
                    if (f.path("event").asInt() == currentId) fixtureCount++;
                }
                if (fixtureCount > 0) {
                    gwData.put("fixture_count", fixtureCount);
                }
            }

            return gwData;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build current gameweek resource", e);
        }
    }
}
