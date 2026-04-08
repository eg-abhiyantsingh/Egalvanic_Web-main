package com.egalvanic.qa.utils.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Lightweight Claude API client using Java 11 HttpClient.
 * No external SDK dependency — just java.net.http + org.json.
 *
 * Configure via:
 *   -DCLAUDE_API_KEY=sk-ant-...    (system property)
 *   CLAUDE_API_KEY=sk-ant-...      (environment variable)
 */
public class ClaudeClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-20250514";
    private static final String API_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 4096;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static String apiKey;

    /**
     * Returns true if the Claude API key is configured.
     */
    public static boolean isConfigured() {
        return getApiKey() != null && !getApiKey().isEmpty();
    }

    /**
     * Send a prompt to Claude and return the text response.
     *
     * @param systemPrompt system-level instruction for Claude
     * @param userPrompt   the user message/question
     * @return Claude's text response, or null if the call fails
     */
    public static String ask(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            System.out.println("[ClaudeClient] API key not configured. Set CLAUDE_API_KEY env var or -DCLAUDE_API_KEY.");
            return null;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("max_tokens", MAX_TOKENS);
            body.put("system", systemPrompt);
            body.put("messages", new JSONArray()
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", userPrompt)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-api-key", getApiKey())
                    .header("anthropic-version", API_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray content = json.getJSONArray("content");
                if (content.length() > 0) {
                    return content.getJSONObject(0).getString("text");
                }
            } else {
                System.out.println("[ClaudeClient] API error " + response.statusCode() + ": "
                        + response.body().substring(0, Math.min(200, response.body().length())));
            }
        } catch (Exception e) {
            System.out.println("[ClaudeClient] Request failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Send a prompt with a screenshot (base64 PNG) for vision analysis.
     */
    public static String askWithImage(String systemPrompt, String userPrompt, String base64Image) {
        if (!isConfigured()) return null;
        try {
            JSONArray contentArray = new JSONArray();
            contentArray.put(new JSONObject()
                    .put("type", "image")
                    .put("source", new JSONObject()
                            .put("type", "base64")
                            .put("media_type", "image/png")
                            .put("data", base64Image)));
            contentArray.put(new JSONObject()
                    .put("type", "text")
                    .put("text", userPrompt));

            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("max_tokens", MAX_TOKENS);
            body.put("system", systemPrompt);
            body.put("messages", new JSONArray()
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", contentArray)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-api-key", getApiKey())
                    .header("anthropic-version", API_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray content = json.getJSONArray("content");
                if (content.length() > 0) {
                    return content.getJSONObject(0).getString("text");
                }
            } else {
                System.out.println("[ClaudeClient] Vision API error " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("[ClaudeClient] Vision request failed: " + e.getMessage());
        }
        return null;
    }

    private static String getApiKey() {
        if (apiKey == null) {
            apiKey = System.getProperty("CLAUDE_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = System.getenv("CLAUDE_API_KEY");
            }
        }
        return apiKey;
    }
}
