package com.gauravs.fpl_mcp_server.api;

import static com.gauravs.fpl_mcp_server.util.FantasyConstants.FPL_BOOTSTRAP_STATIC_URL;
import static com.gauravs.fpl_mcp_server.util.FantasyConstants.FPL_FIXTURES_URL;
import static com.gauravs.fpl_mcp_server.util.FantasyConstants.FPL_PLAYERS_SUMMARY_URL;

import com.gauravs.fpl_mcp_server.util.HttpClientService;

public class FantasyApi {
    public String getBootstrapStatic() throws Exception {
        // Need to implement
        String response = new HttpClientService().getJsonAsString(FPL_BOOTSTRAP_STATIC_URL);
        return response;
    }

    public String getFixtures() throws Exception {
        // Need to implement
        String response = new HttpClientService().getJsonAsString(FPL_FIXTURES_URL);
        return response;
    }

    public String getGameweeks() throws Exception {
        // Need to implement
        String response = getBootstrapStatic();
        return response;
    }

    public String getCurrentGameweek() throws Exception {
        // Need to implement
        String response = getGameweeks();
        return response;
    }

    public String getPlayerSummary(int playerId) throws Exception {
        // Need to implement
        String response = new HttpClientService().getJsonAsString(FPL_PLAYERS_SUMMARY_URL + playerId);
        return response;
    }

    public String getTeams() throws Exception {
        // Need to implement
        String response = getBootstrapStatic();
        return response;
    }
}
