package com.keyholesoftware.rag.service;

import static com.keyholesoftware.rag.config.ConfigConstants.ADVISORS_MAP;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.keyholesoftware.rag.config.PromptRefinementsProvider;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ChatRagService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final Map<String, QuestionAnswerAdvisor> qaAdvisors;
    private final PromptRefinementsProvider promptRefinementsProvider;

    public ChatRagService(ChatClient.Builder chatClientBuilder,
            ChatMemory chatMemory,
            @Qualifier(ADVISORS_MAP) Map<String, QuestionAnswerAdvisor> qaAdvisors,
            PromptRefinementsProvider promptRefinementsProvider) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemory = chatMemory;
        this.qaAdvisors = qaAdvisors;
        this.promptRefinementsProvider = promptRefinementsProvider;
    }

    public ChatResponse getChatResponse(String channel, String userId, String userPrompt) {

        var qaAdvisor = qaAdvisors.get(channel);
        var promptWithRefinements = String.format("%s\n%s", userPrompt,
                promptRefinementsProvider.getPromptRefinement(channel));
        var chatMemoryKey = String.format("%s:%s", channel, userId);

        // Get the response using the QuestionAnswerAdvisor for the channel
        ChatResponse response = chatClient
                .prompt()
                .advisors(qaAdvisor)
                .user(promptWithRefinements)
                .messages(chatMemory.get(chatMemoryKey))
                .call()
                .chatResponse();

        saveChatUserPromptAndResponse(chatMemoryKey, userPrompt, response);

        return response;
    }

    private void saveChatUserPromptAndResponse(String chatMemoryKey, String userPrompt, ChatResponse response) {
        chatMemory.add(chatMemoryKey, new UserMessage(userPrompt));
        if (response != null) {
            chatMemory.add(chatMemoryKey, response.getResult().getOutput());
        }
    }
}
