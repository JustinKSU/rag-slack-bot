package com.keyholesoftware.rag.controller;

import static com.keyholesoftware.rag.service.SlackConstants.*;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keyholesoftware.rag.service.SlackService;
import com.slack.api.methods.SlackApiException;

@RestController
@RequestMapping("/slack/events")
public class SlackAPI {

    private final SlackService slackService;

    public SlackAPI(SlackService slackService) {
        this.slackService = slackService;
    }

    @PostMapping
    public ResponseEntity<?> handleSlackEvent(@RequestBody Map<String, Object> slackEventPayload)
            throws IOException, SlackApiException {
        String type = (String) slackEventPayload.get(TYPE);

        switch (type) {
            case URL_VERIFICATION: // Respond to URL verification
                return ResponseEntity.ok(Map.of(CHALLENGE, slackEventPayload.get(CHALLENGE)));
            case EVENT_CALLBACK: // Handle actual slack events
                slackService.handleSlackEvent(slackEventPayload);
                break;
            default:
                break;
        }

        return ResponseEntity.ok().build();
    }

}
