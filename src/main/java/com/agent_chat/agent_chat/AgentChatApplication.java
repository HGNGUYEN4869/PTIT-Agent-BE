package com.agent_chat.agent_chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentChatApplication.class, args);
	}

}
//mvn clean package -DskipTests
//docker-compose down
//docker-compose up --build