package com.escaperoom.ia.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MemoriaConfig {
    @Bean
    InMemoryChatMemoryRepository memoriaRepositorio() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    ChatMemory memoriaChat(InMemoryChatMemoryRepository repositorio) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(repositorio).maxMessages(20).build();
    }
}
