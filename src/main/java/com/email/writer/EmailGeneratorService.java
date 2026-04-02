package com.email.writer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailGeneratorService {
    private final WebClient webClient;
    private final String apiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder, @Value("${gemini.api.url}") String baseUrl, @Value("${gemini.api.key}") String geminiApiKey) {
        this.apiKey = geminiApiKey;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        //BUILDING PROMPT
        String prompt = buildPrompt(emailRequest);

        // PREPARE RAW JSON BODY
        String requestBody = String.format("""
                                {
                                "contents": [
                        {
                            "parts": [
                            {
                                "text": "%s"
                            }
                        ]
                        }
                    ]
                  }
                """, prompt);


        //SEND REQUEST

        String response = webClient.post().
                uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-3-flash-preview:generateContent")
                        .build())
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();


        //EXTRACT RESONSE
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            // Return the extracted text
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error extracting response";
        }
    }




    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt=new StringBuilder();
        prompt.append("Generate a professional email reply for the folloing email:");
        prompt.append("Generate ONLY one professional email reply.\n");
        prompt.append("Do NOT generate multiple options.\n");
        //prompt.append("Do NOT include placeholders like [Name].\n");
        prompt.append("Do it approx 70 words.\n");
        prompt.append("Return a clean email without subject body only.\n\n");
        if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
           prompt.append("Use a ").append(emailRequest.getTone()).append("tone.");
           //Use a professional tone.
            //Use a professional tone.if tone is mentioned
        }
        prompt.append("original Email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
