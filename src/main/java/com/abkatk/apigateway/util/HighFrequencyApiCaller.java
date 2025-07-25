package com.abkatk.apigateway.util;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HighFrequencyApiCaller {

    private static final String API_URL = "http://localhost:8085/unison/user/find/id/1";
    private static final String BEARER_TOKEN = "test";

    public static void main(String[] args) {
        // Significantly increase requestCount to guarantee hitting the limit
        int requestCount = 500; // <--- Increased to 500
        // Increase threadPoolSize to ensure maximum concurrency
        int threadPoolSize = 50; // <--- Increased to 50

        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        RestTemplate restTemplate = new RestTemplate();

        System.out.println("Starting " + requestCount + " API calls...");

        for (int i = 0; i < requestCount; i++) {
            final int requestNum = i;
            executorService.submit(() -> callApi(restTemplate, requestNum));
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) { // Increased timeout
                System.err.println("Some requests did not complete within the timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Executor service termination interrupted: " + e.getMessage());
        }
        System.out.println("All API calls submitted and processing completed (or timed out).");
    }

    private static void callApi(RestTemplate restTemplate, int requestNum) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(BEARER_TOKEN);

            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            System.out.println("Request " + requestNum + ": Response: " + response.getStatusCode() + " - " + response.getBody());
        } catch (HttpClientErrorException.TooManyRequests e) {
            System.err.println("Request " + requestNum + ": API call failed with 429 Too Many Requests: " + e.getResponseBodyAsString());
        } catch (HttpClientErrorException e) {
            System.err.println("Request " + requestNum + ": API call failed with HTTP status " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Request " + requestNum + ": API call failed: " + e.getMessage());
        }
    }
}
