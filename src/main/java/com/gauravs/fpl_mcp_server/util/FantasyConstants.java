package com.gauravs.fpl_mcp_server.util;

public class FantasyConstants {
    public static final String FPL_API_BASE_URL = "https://fantasy.premierleague.com/api/";
    public static final String FPL_BOOTSTRAP_STATIC_URL = FPL_API_BASE_URL + "bootstrap-static/";
    public static final String FPL_FIXTURES_URL = FPL_API_BASE_URL + "fixtures/";
    public static final String FPL_PLAYERS_URL = FPL_API_BASE_URL + "elements/";
    public static final String FPL_TEAMS_URL = FPL_API_BASE_URL + "teams/";
    public static final String FPL_GAMEWEEKS_URL = FPL_API_BASE_URL + "events/";

    public static final String FPL_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
    public static final String FPL_LOGIN_URL = "https://users.premierleague.com/accounts/login/";
    public static final int RATE_LIMIT_MAX_REQUESTS = 20;
    public static final int RATE_LIMIT_PERIOD_SECONDS = 60;
    public static final int CACHE_EXPIRY_SECONDS = 3600;
    public static final int LEAGUE_RESULTS_LIMIT = 25;
}
