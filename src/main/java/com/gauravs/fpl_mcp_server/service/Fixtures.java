package com.gauravs.fpl_mcp_server.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.gauravs.fpl_mcp_server.api.FantasyApi;

@Component
public class Fixtures {

    @Autowired
    private FantasyApi fantasyApi;

    @Autowired
    private Teams teamService;

    @Autowired
    private Players playerService;

    @Autowired
    private Gameweeks gameweekService;
    
    public List<Map<String, Object>> getFixturesResource(Integer gameweekId, String teamName) throws Exception {
        // Get raw fixtures
        JsonNode fixturesNode = fantasyApi.getFixtures();
        if (fixturesNode == null || !fixturesNode.isArray() || fixturesNode.size() == 0) {
            System.out.println("No fixtures data found");
            return Collections.emptyList();
        }

        // Get teams data and build ID -> team map
        List<Map<String, Object>> teamsData = teamService.getTeamsResource();
        Map<Integer, Map<String, Object>> teamMap = new HashMap<>();
        for (Map<String, Object> team : teamsData) {
            teamMap.put((Integer) team.get("id"), team);
        }

        // Format fixtures
        List<Map<String, Object>> formattedFixtures = new ArrayList<>();
        for (JsonNode fixture : fixturesNode) {
            int homeId = fixture.path("team_h").asInt(0);
            int awayId = fixture.path("team_a").asInt(0);

            Map<String, Object> homeTeam = teamMap.getOrDefault(homeId, Map.of(
                    "name", "Team " + homeId,
                    "short_name", "",
                    "strength_overall_home", 0
            ));
            Map<String, Object> awayTeam = teamMap.getOrDefault(awayId, Map.of(
                    "name", "Team " + awayId,
                    "short_name", "",
                    "strength_overall_away", 0
            ));

            Map<String, Object> formattedFixture = new LinkedHashMap<>();
            formattedFixture.put("id", fixture.path("id").asInt(0));
            formattedFixture.put("gameweek", fixture.path("event").asInt(0));

            formattedFixture.put("home_team", Map.of(
                    "id", homeId,
                    "name", homeTeam.getOrDefault("name", "Team " + homeId),
                    "short_name", homeTeam.getOrDefault("short_name", ""),
                    "strength", homeTeam.getOrDefault("strength_overall_home", 0)
            ));
            formattedFixture.put("away_team", Map.of(
                    "id", awayId,
                    "name", awayTeam.getOrDefault("name", "Team " + awayId),
                    "short_name", awayTeam.getOrDefault("short_name", ""),
                    "strength", awayTeam.getOrDefault("strength_overall_away", 0)
            ));

            formattedFixture.put("kickoff_time", fixture.path("kickoff_time").asText(""));
            formattedFixture.put("difficulty", Map.of(
                    "home", fixture.path("team_h_difficulty").asInt(0),
                    "away", fixture.path("team_a_difficulty").asInt(0)
            ));

            formattedFixture.put("stats", fixture.has("stats") ? fixture.get("stats") : Collections.emptyList());

            formattedFixtures.add(formattedFixture);
        }

        // Filter by gameweek if provided
        if (gameweekId != null) {
            formattedFixtures.removeIf(f -> !gameweekId.equals(f.get("gameweek")));
        }

        // Filter by team name if provided
        if (teamName != null && !teamName.isEmpty()) {
            String teamNameLower = teamName.toLowerCase();
            formattedFixtures.removeIf(f -> {
                Map<String, Object> home = (Map<String, Object>) f.get("home_team");
                Map<String, Object> away = (Map<String, Object>) f.get("away_team");

                String homeName = ((String) home.getOrDefault("name", "")).toLowerCase();
                String homeShort = ((String) home.getOrDefault("short_name", "")).toLowerCase();
                String awayName = ((String) away.getOrDefault("name", "")).toLowerCase();
                String awayShort = ((String) away.getOrDefault("short_name", "")).toLowerCase();

                return !(homeName.contains(teamNameLower) || homeShort.contains(teamNameLower) ||
                        awayName.contains(teamNameLower) || awayShort.contains(teamNameLower));
            });
        }

        // Sort by gameweek then kickoff_time
        formattedFixtures.sort(Comparator
                .comparing((Map<String, Object> f) -> (Integer) f.getOrDefault("gameweek", 0))
                .thenComparing(f -> (String) f.getOrDefault("kickoff_time", ""))
        );

        return formattedFixtures;
    }

    public List<Map<String, Object>> getPlayerFixtures(int playerId) throws Exception {
        return getPlayerFixtures(playerId, 5);
    }


    public List<Map<String, Object>> getPlayerFixtures(int playerId, int numFixtures) throws Exception {
        // Get all players
        List<Map<String, Object>> players = playerService.getPlayersResource(null, null);
        Map<String, Object> player = null;
        for (Map<String, Object> p : players) {
            if (playerId == (Integer) p.get("id")) {
                player = p;
                break;
            }
        }

        if (player == null) {
            System.out.println("Player with ID " + playerId + " not found");
            return Collections.emptyList();
        }

        String teamName = (String) player.get("team");
        if (teamName == null) {
            System.out.println("Team ID not found for player " + playerId);
            return Collections.emptyList();
        }

        // Get all fixtures
        List<Map<String, Object>> allFixtures = getFixturesResource(null, null);
        if (allFixtures.isEmpty()) {
            System.out.println("No fixtures data found");
            return Collections.emptyList();
        }

        // Get current gameweek
        List<Map<String, Object>> gameweeks = gameweekService.getGameweeksResource();
        Integer currentGameweek = null;
        for (Map<String, Object> gw : gameweeks) {
            if (Boolean.TRUE.equals(gw.get("is_current"))) {
                currentGameweek = (Integer) gw.get("id");
                break;
            }
        }
        if (currentGameweek == null) {
            for (Map<String, Object> gw : gameweeks) {
                if (Boolean.TRUE.equals(gw.get("is_next"))) {
                    currentGameweek = ((Integer) gw.get("id")) - 1;
                    break;
                }
            }
        }
        if (currentGameweek == null) {
            System.out.println("Could not determine current gameweek");
            return Collections.emptyList();
        }

        // Filter upcoming fixtures for player's team
        List<Map<String, Object>> upcomingFixtures = new ArrayList<>();
        for (Map<String, Object> fixture : allFixtures) {
            Integer gw = (Integer) fixture.getOrDefault("gameweek", 0);
            if (gw >= currentGameweek) {
                Map<String, Object> homeTeam = (Map<String, Object>) fixture.get("home_team");
                Map<String, Object> awayTeam = (Map<String, Object>) fixture.get("away_team");

                String homeName = (String) homeTeam.getOrDefault("name", "");
                String awayName = (String) awayTeam.getOrDefault("name", "");

                if (homeName.equalsIgnoreCase(teamName) || awayName.equalsIgnoreCase(teamName)) {
                    upcomingFixtures.add(fixture);
                }
            }
        }

        // Sort by gameweek
        upcomingFixtures.sort(Comparator.comparing(f -> (Integer) f.getOrDefault("gameweek", 0)));

        // Limit to requested number of fixtures
        if (upcomingFixtures.size() > numFixtures) {
            upcomingFixtures = upcomingFixtures.subList(0, numFixtures);
        }

        // Get teams map for opponent info
        List<Map<String, Object>> teamsData = teamService.getTeamsResource();
        Map<Integer, Map<String, Object>> teamMap = new HashMap<>();
        for (Map<String, Object> t : teamsData) {
            teamMap.put((Integer) t.get("id"), t);
        }

        // Format fixtures
        List<Map<String, Object>> formattedFixtures = new ArrayList<>();
        for (Map<String, Object> fixture : upcomingFixtures) {
            Map<String, Object> homeTeam = (Map<String, Object>) fixture.get("home_team");
            Map<String, Object> awayTeam = (Map<String, Object>) fixture.get("away_team");
            String homeName = (String) homeTeam.getOrDefault("name", "");

            boolean isHome = homeName.equalsIgnoreCase(teamName);
            Map<String, Object> opponentTeam = isHome ? awayTeam : homeTeam;
            int opponentId = (Integer) opponentTeam.getOrDefault("id", 0);

            int difficulty = (Integer) fixture.getOrDefault(isHome ? "difficulty_home" : "difficulty_away", 3);

            Map<String, Object> formatted = new LinkedHashMap<>();
            formatted.put("gameweek", fixture.get("gameweek"));
            formatted.put("kickoff_time", fixture.getOrDefault("kickoff_time", ""));
            formatted.put("location", isHome ? "home" : "away");
            formatted.put("opponent", opponentTeam.getOrDefault("name", "Team " + opponentId));
            formatted.put("opponent_short", opponentTeam.getOrDefault("short_name", ""));
            formatted.put("difficulty", difficulty);

            formattedFixtures.add(formatted);
        }

        return formattedFixtures;
    }


    public Map<String, Object> analyzePlayerFixtures(int playerId) throws Exception {
        return analyzePlayerFixtures(playerId, 5);
    }

    public Map<String, Object> analyzePlayerFixtures(int playerId, int numFixtures) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();

        // Get all players
        List<Map<String, Object>> players = playerService.getPlayersResource(null, null);
        Map<String, Object> player = null;
        for (Map<String, Object> p : players) {
            if ((Integer)p.get("id") == playerId) {
                player = p;
                break;
            }
        }

        if (player == null) {
            result.put("error", "Player with ID " + playerId + " not found");
            return result;
        }

        // Teams and positions mapping
        List<Map<String, Object>> teamsList = teamService.getTeamsResource();
        Map<Integer, Map<String, Object>> teamMap = new HashMap<>();
        for (Map<String, Object> t : teamsList) {
            teamMap.put((Integer)t.get("id"), t);
        }

        // Map team
        String teamName = (String)player.getOrDefault("team", "Unknown team");

        // Map position
        String positionId = (String)player.get("position");

        // Normalize position to GKP/DEF/MID/FWD
        Map<String, String> positionMapping = Map.of(
                "GKP", "GK",
                "DEF", "DEF",
                "MID", "MID",
                "FWD", "FWD"
        );
        String position = positionMapping.getOrDefault(positionId, "Unknown position");

        // Get player fixtures
        List<Map<String, Object>> fixtures = getPlayerFixtures(playerId, numFixtures);

        if (fixtures.isEmpty()) {
            Map<String, Object> playerData = new LinkedHashMap<>();
            playerData.put("id", playerId);
            playerData.put("name", player.getOrDefault("web_name", "Unknown player"));
            playerData.put("team", teamName);
            playerData.put("position", position);

            Map<String, Object> analysis = new LinkedHashMap<>();
            analysis.put("fixtures_analyzed", Collections.emptyList());
            analysis.put("difficulty_score", 0);
            analysis.put("analysis", "No upcoming fixtures found");

            result.put("player", playerData);
            result.put("fixture_analysis", analysis);
            return result;
        }

        // Calculate difficulty
        double totalDifficulty = 0.0;
        int homeCount = 0;
        for (Map<String, Object> f : fixtures) {
            int diff = (Integer) f.getOrDefault("difficulty", 3);
            totalDifficulty += diff;
            if ("home".equals(f.get("location"))) homeCount++;
        }
        double avgDifficulty = totalDifficulty / fixtures.size();
        double homePercentage = ((double) homeCount / fixtures.size()) * 100;

        // Scale to 1-10
        double fixtureScore = (6 - avgDifficulty) * 2;
        double homeAdjustment = (homePercentage - 50) / 100;
        double finalScore = Math.max(1, Math.min(10, fixtureScore + homeAdjustment));

        // Generate analysis text
        String analysisText;
        if (finalScore >= 8.5) analysisText = "Excellent fixtures - highly favorable schedule";
        else if (finalScore >= 7) analysisText = "Good fixtures - favorable schedule";
        else if (finalScore >= 5.5) analysisText = "Average fixtures - balanced schedule";
        else if (finalScore >= 4) analysisText = "Difficult fixtures - challenging schedule";
        else analysisText = "Very difficult fixtures - extremely challenging schedule";

        // Build result
        Map<String, Object> playerData = new LinkedHashMap<>();
        playerData.put("id", playerId);
        playerData.put("name", player.getOrDefault("web_name", "Unknown player"));
        playerData.put("team", teamName);
        playerData.put("position", position);

        Map<String, Object> fixtureAnalysis = new LinkedHashMap<>();
        fixtureAnalysis.put("fixtures_analyzed", fixtures);
        fixtureAnalysis.put("difficulty_score", Math.round(finalScore * 10.0) / 10.0);
        fixtureAnalysis.put("analysis", analysisText);
        fixtureAnalysis.put("home_fixtures_percentage", Math.round(homePercentage * 10.0) / 10.0);

        result.put("player", playerData);
        result.put("fixture_analysis", fixtureAnalysis);

        return result;
    }

    public List<Map<String, Object>> getBlankGameweeks() throws Exception {
        return getBlankGameweeks(5); // default numGameweeks = 5
    }

    public List<Map<String, Object>> getBlankGameweeks(int numGameweeks) throws Exception {
        List<Map<String, Object>> blankGameweeks = new ArrayList<>();

        // Get gameweeks, fixtures, and teams
        List<Map<String, Object>> allGameweeks = gameweekService.getGameweeksResource();
        List<Map<String, Object>> allFixtures = getFixturesResource(null, null);
        List<Map<String, Object>> teamData = teamService.getTeamsResource();

        // Map team IDs to team info
        Map<Integer, Map<String, Object>> teamMap = new HashMap<>();
        for (Map<String, Object> team : teamData) {
            teamMap.put((Integer) team.get("id"), team);
        }

        // Determine current gameweek
        Map<String, Object> currentGW = null;
        for (Map<String, Object> gw : allGameweeks) {
            Boolean isCurrent = (Boolean) gw.getOrDefault("is_current", false);
            Boolean isNext = (Boolean) gw.getOrDefault("is_next", false);
            if (isCurrent || isNext) {
                currentGW = gw;
                break;
            }
        }

        if (currentGW == null) {
            return blankGameweeks; // no current or next gameweek found
        }

        int currentGwId = (Integer) currentGW.get("id");

        // Filter upcoming gameweeks
        List<Map<String, Object>> upcomingGameweeks = new ArrayList<>();
        for (Map<String, Object> gw : allGameweeks) {
            int gwId = (Integer) gw.get("id");
            if (gwId >= currentGwId && gwId < currentGwId + numGameweeks) {
                upcomingGameweeks.add(gw);
            }
        }

        // Analyze each upcoming gameweek
        for (Map<String, Object> gw : upcomingGameweeks) {
            int gwId = (Integer) gw.get("id");

            // Fixtures for this gameweek
            Set<Integer> teamsWithFixtures = new HashSet<>();
            for (Map<String, Object> f : allFixtures) {
                int fixtureGwId = (Integer) f.getOrDefault("gameweek", 0);
                if (fixtureGwId == gwId) {
                    teamsWithFixtures.add((Integer) ((Map<String,Object>)f.get("home_team")).get("id"));
                    teamsWithFixtures.add((Integer) ((Map<String,Object>)f.get("away_team")).get("id"));
                }
            }

            // Identify teams without fixtures
            List<Map<String, Object>> teamsWithoutFixtures = new ArrayList<>();
            for (Map.Entry<Integer, Map<String, Object>> entry : teamMap.entrySet()) {
                int teamId = entry.getKey();
                if (!teamsWithFixtures.contains(teamId)) {
                    Map<String, Object> teamInfo = new LinkedHashMap<>();
                    teamInfo.put("id", teamId);
                    teamInfo.put("name", entry.getValue().getOrDefault("name", "Team " + teamId));
                    teamInfo.put("short_name", entry.getValue().getOrDefault("short_name", ""));
                    teamsWithoutFixtures.add(teamInfo);
                }
            }

            if (!teamsWithoutFixtures.isEmpty()) {
                Map<String, Object> gwInfo = new LinkedHashMap<>();
                gwInfo.put("gameweek", gwId);
                gwInfo.put("name", gw.getOrDefault("name", "Gameweek " + gwId));
                gwInfo.put("teams_without_fixtures", teamsWithoutFixtures);
                gwInfo.put("count", teamsWithoutFixtures.size());
                blankGameweeks.add(gwInfo);
            }
        }

        return blankGameweeks;
    }

    public List<Map<String, Object>> getDoubleGameweeks() throws Exception {
        return getDoubleGameweeks(5); // default numGameweeks = 5
    }

    public List<Map<String, Object>> getDoubleGameweeks(int numGameweeks) throws Exception {
        List<Map<String, Object>> doubleGameweeks = new ArrayList<>();

        // Get gameweeks, fixtures, and teams
        List<Map<String, Object>> allGameweeks = gameweekService.getGameweeksResource();
        List<Map<String, Object>> allFixtures = getFixturesResource(null, null);
        List<Map<String, Object>> teamData = teamService.getTeamsResource();

        // Map team IDs to team info
        Map<Integer, Map<String, Object>> teamMap = new HashMap<>();
        for (Map<String, Object> team : teamData) {
            teamMap.put((Integer) team.get("id"), team);
        }

        // Determine current gameweek
        Map<String, Object> currentGW = null;
        for (Map<String, Object> gw : allGameweeks) {
            Boolean isCurrent = (Boolean) gw.getOrDefault("is_current", false);
            Boolean isNext = (Boolean) gw.getOrDefault("is_next", false);
            if (isCurrent || isNext) {
                currentGW = gw;
                break;
            }
        }

        if (currentGW == null) {
            return doubleGameweeks; // no current or next gameweek found
        }

        int currentGwId = (Integer) currentGW.get("id");

        // Filter upcoming gameweeks
        List<Map<String, Object>> upcomingGameweeks = new ArrayList<>();
        for (Map<String, Object> gw : allGameweeks) {
            int gwId = (Integer) gw.get("id");
            if (gwId >= currentGwId && gwId < currentGwId + numGameweeks) {
                upcomingGameweeks.add(gw);
            }
        }

        // Analyze each upcoming gameweek
        for (Map<String, Object> gw : upcomingGameweeks) {
            int gwId = (Integer) gw.get("id");

            // Fixtures for this gameweek
            List<Map<String, Object>> gwFixtures = new ArrayList<>();
            for (Map<String, Object> f : allFixtures) {
                int fixtureGwId = (Integer) f.getOrDefault("gameweek", 0);
                if (fixtureGwId == gwId) {
                    gwFixtures.add(f);
                }
            }

            // Count fixtures per team
            Map<Integer, Integer> teamFixtureCount = new HashMap<>();
            for (Map<String, Object> fixture : gwFixtures) {
                int homeTeam = (Integer) ((Map<String,Object>)fixture.get("home_team")).get("id");
                int awayTeam = (Integer) ((Map<String,Object>)fixture.get("away_team")).get("id");

                teamFixtureCount.put(homeTeam, teamFixtureCount.getOrDefault(homeTeam, 0) + 1);
                teamFixtureCount.put(awayTeam, teamFixtureCount.getOrDefault(awayTeam, 0) + 1);
            }

            // Identify teams with multiple fixtures (double gameweek)
            List<Map<String, Object>> teamsWithDoubles = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : teamFixtureCount.entrySet()) {
                int count = entry.getValue();
                if (count > 1) {
                    int teamId = entry.getKey();
                    Map<String, Object> team = teamMap.getOrDefault(teamId, new HashMap<>());
                    Map<String, Object> teamInfo = new LinkedHashMap<>();
                    teamInfo.put("id", teamId);
                    teamInfo.put("name", team.getOrDefault("name", "Team " + teamId));
                    teamInfo.put("short_name", team.getOrDefault("short_name", ""));
                    teamInfo.put("fixture_count", count);
                    teamsWithDoubles.add(teamInfo);
                }
            }

            if (!teamsWithDoubles.isEmpty()) {
                Map<String, Object> gwInfo = new LinkedHashMap<>();
                gwInfo.put("gameweek", gwId);
                gwInfo.put("name", gw.getOrDefault("name", "Gameweek " + gwId));
                gwInfo.put("teams_with_doubles", teamsWithDoubles);
                gwInfo.put("count", teamsWithDoubles.size());
                doubleGameweeks.add(gwInfo);
            }
        }

        return doubleGameweeks;
    }

    public Map<String, Object> getPlayerGameweekHistory(List<Integer> playerIds) throws Exception {
        return getPlayerGameweekHistory(playerIds, 5); // default numGameweeks = 5
    }

    public Map<String, Object> getPlayerGameweekHistory(List<Integer> playerIds, int numGameweeks) throws Exception {
        // Get all gameweeks
        List<Map<String, Object>> gameweeks = gameweekService.getGameweeksResource();

        // Determine current gameweek
        Integer currentGameweek = null;
        for (Map<String, Object> gw : gameweeks) {
            Boolean isCurrent = (Boolean) gw.getOrDefault("is_current", false);
            if (isCurrent) {
                currentGameweek = (Integer) gw.get("id");
                break;
            }
        }

        if (currentGameweek == null) {
            for (Map<String, Object> gw : gameweeks) {
                Boolean isNext = (Boolean) gw.getOrDefault("is_next", false);
                if (isNext) {
                    currentGameweek = (Integer) gw.get("id") - 1;
                    break;
                }
            }
        }

        if (currentGameweek == null) {
            Map<String, Object> errorMap = new LinkedHashMap<>();
            errorMap.put("error", "Could not determine current gameweek");
            return errorMap;
        }

        // Determine gameweek range
        int startGameweek = Math.max(1, currentGameweek - numGameweeks + 1);
        List<Integer> gameweekRange = new ArrayList<>();
        for (int gw = startGameweek; gw <= currentGameweek; gw++) {
            gameweekRange.add(gw);
        }

        Map<Integer, Map<String, Object>> teamCache = new HashMap<>();

        Map<Integer, List<Map<String, Object>>> playerHistories = new LinkedHashMap<>();

        for (Integer playerId : playerIds) {
            try {
                Map<String, Object> playerSummary = playerService.getPlayerById(playerId); // synchronous

                if (playerSummary == null || !playerSummary.containsKey("history")) {
                    continue;
                }

                List<Map<String, Object>> history = (List<Map<String, Object>>) playerSummary.get("history");
                List<Map<String, Object>> filteredHistory = new ArrayList<>();

                for (Map<String, Object> entry : history) {
                    Integer roundNum = (Integer) entry.get("round");
                    if (gameweekRange.contains(roundNum)) {
                        boolean wasHome = (Boolean) entry.getOrDefault("was_home", false);
                        Integer opponentTeamId = (Integer) entry.get("opponent_team");

                        // Cache team names to avoid repeated lookups
                        Map<String, Object> opponentTeam = teamCache.computeIfAbsent(opponentTeamId,
                                id -> {
                                    try {
                                        return teamService.getTeamById(id);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                });

                        Map<String, Object> historyEntry = new LinkedHashMap<>();
                        historyEntry.put("gameweek", roundNum);
                        historyEntry.put("minutes", entry.getOrDefault("minutes", 0));
                        historyEntry.put("points", entry.getOrDefault("total_points", 0));
                        historyEntry.put("goals", entry.getOrDefault("goals_scored", 0));
                        historyEntry.put("assists", entry.getOrDefault("assists", 0));
                        historyEntry.put("clean_sheets", entry.getOrDefault("clean_sheets", 0));
                        historyEntry.put("bonus", entry.getOrDefault("bonus", 0));
                        historyEntry.put("opponent", opponentTeam != null ? opponentTeam.get("name") : "Unknown");
                        historyEntry.put("was_home", wasHome);
                        historyEntry.put("expected_goals", entry.getOrDefault("expected_goals", 0));
                        historyEntry.put("expected_assists", entry.getOrDefault("expected_assists", 0));
                        historyEntry.put("expected_goal_involvements", entry.getOrDefault("expected_goal_involvements", 0));
                        historyEntry.put("expected_goals_conceded", entry.getOrDefault("expected_goals_conceded", 0));
                        historyEntry.put("transfers_in", entry.getOrDefault("transfers_in", 0));
                        historyEntry.put("transfers_out", entry.getOrDefault("transfers_out", 0));
                        historyEntry.put("selected", entry.getOrDefault("selected", 0));
                        historyEntry.put("value", entry.containsKey("value") ? ((Number) entry.get("value")).doubleValue() / 10.0 : 0);
                        historyEntry.put("team_score", entry.getOrDefault(wasHome ? "team_h_score" : "team_a_score", 0));
                        historyEntry.put("opponent_score", entry.getOrDefault(wasHome ? "team_a_score" : "team_h_score", 0));

                        filteredHistory.add(historyEntry);
                    }
                }

                filteredHistory.sort(Comparator.comparingInt(h -> (Integer) h.get("gameweek")));
                playerHistories.put(playerId, filteredHistory);

            } catch (Exception e) {
                System.err.println("Error fetching history for player " + playerId + ": " + e.getMessage());
            }
        }

        Map<String, Object> finalResult = new LinkedHashMap<>();
        finalResult.put("players", playerHistories);
        finalResult.put("gameweeks", gameweekRange);

        return finalResult;
    }

    public String getTeamNameById(Integer teamId) throws Exception {
        if (teamId == null) {
            return "Unknown team";
        }

        List<Map<String, Object>> teams = teamService.getTeamsResource(); // assumes synchronous method

        for (Map<String, Object> team : teams) {
            Integer id = (Integer) team.get("id");
            if (id != null && id.equals(teamId)) {
                return (String) team.getOrDefault("name", "Unknown team");
            }
        }

        return "Unknown team";
    }

}
