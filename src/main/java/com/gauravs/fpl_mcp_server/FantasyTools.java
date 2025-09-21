package com.gauravs.fpl_mcp_server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gauravs.fpl_mcp_server.service.Fixtures;
import com.gauravs.fpl_mcp_server.service.Gameweeks;
import com.gauravs.fpl_mcp_server.service.Players;
import com.gauravs.fpl_mcp_server.service.Teams;

@Service
public class FantasyTools {

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private Gameweeks gameweeks;

    @Autowired
    private Teams teams;

    @Autowired
    private Players players;

    @Tool(name = "get_all_players", description = "Get a formatted list of all players with comprehensive statistics")
    public List<Map<String,Object>> getAllPlayers() {
        return players.getPlayersResource(null, null);
    }

    @Tool(name = "get_player_by_name", description = "Get player information by searching for their name")
    public Map<String, Object> getPlayerByName(String name) {
        List<Map<String, Object>> response = players.findPlayersByName(name, 5);
        return response.isEmpty() ? Map.of("message", "No player found with name: " + name) : response.get(0);
    }

    @Tool(name = "get_all_teams", description = "Get a formatted list of all Premier League teams with strength ratings")
    public List<Map<String, Object>> getAllTeams() {
        try {
            return teams.getTeamsResource();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(Map.of("error", "Failed to fetch teams: " + e.getMessage()));
        }
    }

    @Tool(name = "get_team_by_name", description = "Get team information by searching for their name")
    public String getTeamByName(String name) {
        try {
            Map<String, Object> response = teams.getTeamByName(name);
            return response != null ? response.toString() : "No team found with name: " + name;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching team: " + e.getMessage();
        }
    }

    @Tool(name = "get_current_gameweek", description = "Get information about the current gameweek")
    public Map<String, Object> getCurrentGameweek() {
        return gameweeks.getCurrentGameweekResource();
    }

    @Tool(name = "get_all_fixtures", description = "Get all fixtures for the current Premier League season")
    public List<Map<String, Object>> getAllFixtures() {
        try {
            return fixtures.getFixturesResource(null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(Map.of("error", "Failed to fetch fixtures: " + e.getMessage()));
        }
    }

    @Tool(name = "get_gameweek_fixtures", description = "Get fixtures for a specific gameweek")
    public List<Map<String, Object>> getGameweekFixtures(int gameweek) {
        try {
            return fixtures.getFixturesResource(gameweek, null);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(Map.of("error", "Failed to fetch fixtures: " + e.getMessage()));
        }
    }

    @Tool(name = "get_team_fixtures", description = "Get fixtures for a specific team")
    public List<Map<String, Object>> getTeamFixtures(String teamName) {
        try {
            return fixtures.getFixturesResource(null, teamName);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(Map.of("error", "Failed to fetch fixtures: " + e.getMessage()));
        }
    }

    @Tool(name = "get_player_fixtures_by_name", description = "Get upcoming fixtures for a specific player")
    public Map<String, Object> getPlayerFixturesByName(String playerName) {
        try {
            List<Map<String, Object>> playerMatches = players.findPlayersByName(playerName, 5);
            if (playerMatches == null || playerMatches.isEmpty()) {
                return Map.of("error", "Failed to fetch player fixtures");
            }   
            Map<String, Object> player = playerMatches.get(0);
            List<Map<String, Object>> playerFixture = fixtures.getPlayerFixtures( (int) player.get("id"));
            
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("name", player.getOrDefault("name", ""));
            playerInfo.put("team", player.getOrDefault("team", ""));
            playerInfo.put("position", player.getOrDefault("position", ""));

            Map<String, Object> response = new HashMap<>();
            response.put("player", playerInfo);
            response.put("fixtures", playerFixture);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Failed to fetch player fixtures: " + e.getMessage());
        }
    }

    @Tool(name = "get_blank_gameweeks_resource", description = "Get information about upcoming blank gameweeks")
    public List<Map<String, Object>> getBlankGameweeksResource() {
        try {
            return fixtures.getBlankGameweeks();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(Map.of("error", "Failed to fetch blank gameweeks: " + e.getMessage()));
        }
    }

    @Tool(name = "get_double_gameweeks_resource", description = "Get information about upcoming double gameweeks")
    public List<Map<String, Object>> getDoubleGameweeksResource() {
        try {
            return fixtures.getDoubleGameweeks();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(Map.of("error", "Failed to fetch double gameweeks: " + e.getMessage()));
        }
    }

    @Tool(name = "check_fpl_authentication", description = "Check if the provided FPL authentication is valid. Returns: Authentication status and basic team information")
    public String checkFplAuthentication() {
        // Logic to check FPL authentication
        return "FPL authentication status";
    }

    @Tool(name = "get_gameweek_status", description = "et precise information about current, previous, and next gameweeks. Returns: Detailed information about gameweek timing, including exact status")
    public String getGameweekStatus() {
        // Logic to fetch and return gameweek status
        return "Gameweek status details";
    }

    @Tool(name = "analyze_player_fixtures", description = "Analyze upcoming fixtures for a player and provide a difficulty rating. Returns:Analysis of player's upcoming fixtures with difficulty ratings")
    public String analyzePlayerFixtures(String playerName, int numberOfFixtures) {
        // Logic to analyze and return player's fixture difficulty
        return "Fixture difficulty analysis for player: " + playerName;
    }

    @Tool(name = "get_blank_gameweeks", description = "Get information about upcoming blank gameweeks where teams don't have fixtures. Returns: Information about blank gameweeks and affected teams")
    public String getBlankGameweeks(int numberOfGameweeks) {
        // Logic to fetch and return blank gameweeks information
        return "Information about upcoming blank gameweeks";
    }

    @Tool(name = "get_double_gameweeks", description = "Get information about upcoming double gameweeks where teams play multiple times. Returns: Information about double gameweeks and affected teams")
    public String getDoubleGameweeks(int numberOfGameweeks) {
        // Logic to fetch and return double gameweeks information
        return "Information about upcoming double gameweeks";
    }

    @Tool(name = "analyze_players", description = "Filter and analyze FPL players based on multiple criteria. Returns: Filtered player data with summary statistics")
    public String analyzePlayers(String positon, String team, float minPrice, float maxPrice, int minPoints, float minOwnership, float maxOwnership, float formThreshold, boolean includeGameweeks, String sortBy, String sortOrder) {
        // Logic to analyze and return filtered players
        return "Filtered player data based on criteria";
    }

    @Tool(name = "analyze_fixtures", description = "Analyze upcoming fixtures for players, teams, or positions. Returns: Fixture analysis with difficulty ratings and summary")
    public String analyzeFixtures(String entityType, String entityName, int numberOfGameweeks, boolean includeBlank, boolean includeDoubles) {
        // Logic to analyze and return fixture analysis
        return "Fixture analysis for " + entityType + ": " + entityName;
    }

    @Tool(name = "compare_players", description = "Compare multiple players across various metrics. Returns: Detailed comparison of players across the specified metrics")
    public String comparePlayers(List<String> playerNames, List<String> metrics, boolean includeGameweeks, int numberOfGameweeks, boolean includeFixtureAnalysis) {
        // Logic to compare and return player comparisons
        return "Comparison of players: " + playerNames;
    }
}