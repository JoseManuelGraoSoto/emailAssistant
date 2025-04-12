package com.email.emailSP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${mistral.api.url}")
    private String apiUrl;

    @Value("${mistral.api.key}")
    private String apiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReplay(EmailRequest emailRequest) {

        String prompt = buildPrompt(emailRequest);
        Map<String, Object> craftPrompt = craftQuery(prompt, emailRequest);

        try {
            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(craftPrompt)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return extractResponseContent(response);
        } catch (Exception e) {
            return "Error during API call: " + e.getMessage();
        }
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            String result = root.path("choices").get(0).path("message").path("content").asText();
            return result;
        } catch (Exception e) {
            return "Error processing response: " + e.getMessage();
        }
    }

    private Map<String, Object> craftQuery(String prompt, EmailRequest emailRequest) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "mistral-medium");

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        requestBody.put("messages", Collections.singletonList(message));
        requestBody.put("temperature", emailRequest.getTemperature());
        return requestBody;
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content. Please don't generate a subject line. ");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Please use a ").append(emailRequest.getTone()).append(" tone. ");
        }
        prompt.append("\nOriginal email:\n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
