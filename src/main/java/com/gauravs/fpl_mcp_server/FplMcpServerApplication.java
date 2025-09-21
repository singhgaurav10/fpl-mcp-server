package com.gauravs.fpl_mcp_server;

import java.util.List;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.gauravs.fpl_mcp_server",
    "com.gauravs.fpl_mcp_server.api",
    "com.gauravs.fpl_mcp_server.controller",
    "com.gauravs.fpl_mcp_server.service"
})
public class FplMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FplMcpServerApplication.class, args);
	}

	@Bean
	public List<ToolCallback> tools(FantasyTools fantasyTools) {
		return List.of(ToolCallbacks.from(fantasyTools));
	}

}
