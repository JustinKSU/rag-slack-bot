package com.keyholesoftware.rag.service;

import static com.keyholesoftware.rag.service.SlackConstants.*;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsInfoRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlackService {

    private final ChatRagService chatRagService;
    private final Slack slackApi;

    @Value("${slack.bot-token}")
    private String slackBotToken;

    public SlackService(ChatRagService chatRagService, Slack slackApi) {
        this.chatRagService = chatRagService;
        this.slackApi = slackApi;
    }

    @Async
    public void handleSlackEvent(Map<String, Object> slackEventPayload) throws IOException, SlackApiException {
        @SuppressWarnings("unchecked")
        var event = (Map<String, Object>) slackEventPayload.get(EVENT);
        var eventType = (String) event.get(TYPE);

        if (MESSAGE.equals(eventType)) {
            var channelType = (String) event.get(CHANNEL_TYPE);

            switch (channelType) {
                case IM:
                    if (!event.containsKey(BOT_ID) // Respond to DMs (im)
                            && event.containsKey(TEXT)) {
                        respondToPrompt(event);
                    }
                    break;

                case CHANNEL:
                    if (!event.containsKey(BOT_ID) // Respond to channel messages
                            && event.containsKey(TEXT)) {
                        respondToPrompt(event);
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private void respondToPrompt(Map<String, Object> event) throws IOException, SlackApiException {
        var channelId = (String) event.get(CHANNEL);
        var userId = (String) event.get(USER);
        var threadTs = (String) event.get(THREAD_TS);
        var userPrompt = (String) event.get(TEXT);
        var channelName = getChannelName(channelId);

        var response = this.chatRagService.getChatResponse(channelName, userId, userPrompt);

        if (response != null) {
            sendMessage(channelId, threadTs, response.getResult().getOutput().getText());
        }
    }

    private String getChannelName(String channelId) throws IOException, SlackApiException {

        var response = slackApi.methods(slackBotToken)
                .conversationsInfo(ConversationsInfoRequest.builder()
                        .channel(channelId)
                        .build());

        if (response.isOk()) {
            return response.getChannel().getName();
        } else {
            throw new RuntimeException("Slack API error: " + response.getError());
        }
    }

    private void sendMessage(String channel, String threadTs, String message) {
        try {
            var response = slackApi.methods(slackBotToken)
                    .chatPostMessage(ChatPostMessageRequest.builder()
                            .channel(channel)
                            .text(message)
                            .threadTs(threadTs) // can be null
                            .build());

            if (!response.isOk()) {
                throw new RuntimeException("Slack API error: " + response.getError());
            }
        } catch (IOException | SlackApiException e) {
            throw new RuntimeException("Failed to send Slack message", e);
        }
    }
}
