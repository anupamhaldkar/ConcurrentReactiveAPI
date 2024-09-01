package com.concurrent.api.serviceImpl;

import com.concurrent.api.service.ApiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class ApiServiceImpl implements ApiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ApiServiceImpl(WebClient.Builder webClient, ObjectMapper objectMapper) {
        this.webClient = webClient.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Object> fetchData() {
        String api1Url = "https://jsonblob.om/api/jsonBlob/1278302869768757248";
        String api2Url = "https://jsonblob.com/api/jsonBlob/1278303163344871424";

        Mono<String> api1Response = fetchFromApi(api1Url);
        Mono<String> api2Response = fetchFromApi(api2Url);
        return api1Response.zipWith(api2Response, (res1, res2) -> {
            if (res1 != null && !res1.trim().isEmpty() &&
                    res2 != null && !res2.trim().isEmpty()) {
                try {
                    return mergeResponses(res1, res2);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            if (res1 != null && !res1.trim().isEmpty()) {
                try {
                    return Map.of("api1Response", parseJson(res1));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            if (res2 != null && !res2.trim().isEmpty()) {
                try {
                    return Map.of("api2Response", parseJson(res2));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            return Map.of("Error", "Both API Calls failed due to network connection");
        });
    }

    private Object parseJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    private Mono<?> handlePartialResponse(Mono<String> api1Response, Mono<String> api2Response) {
        return Mono.firstWithSignal(api1Response, api2Response)
                .map(response -> Map.of("SuccessfulResponse", response))
                .switchIfEmpty(Mono.error(new RuntimeException("Both API calls failed or timed out")));
    }

    private Object mergeResponses(String response1, String response2) throws JsonProcessingException {
        return Map.<String, Object>of("api1Response", parseJson(response1), "api2Response", parseJson(response2));
    }

    private Mono<String> fetchFromApi(String url) {

        long startTime = System.currentTimeMillis();
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .doOnSuccess(response -> log.info("API call to {} succeeded in {} ms", url, System.currentTimeMillis() - startTime))
                .doOnError(error -> log.error("API call to {} failed or timed out in {} ms", url, System.currentTimeMillis() - startTime))
                .onErrorResume(e -> {
                    log.error("Handling error for URL {}: {}", url, e.getMessage());
                    // Return a Mono with an error message to be processed later
                    return Mono.just("");
                });
    }
}
