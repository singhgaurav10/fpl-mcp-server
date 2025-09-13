package com.gauravs.fpl_mcp_server.api;

import com.gauravs.fpl_mcp_server.service.HttpClientService;
import static com.gauravs.fpl_mcp_server.util.FantasyConstants.FPL_BOOTSTRAP_STATIC_URL;
import static com.gauravs.fpl_mcp_server.util.FantasyConstants.FPL_FIXTURES_URL;

public class FantasyApi {
    //TODO
    public String getBootstrapStatic() throws Exception {
        String response = new HttpClientService().getJsonAsString(FPL_BOOTSTRAP_STATIC_URL);
        return response;
    }

    //TODO
    public String getFixtures() throws Exception {
        String response = new HttpClientService().getJsonAsString(FPL_FIXTURES_URL);
        return response;
    }
}
