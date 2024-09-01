package com.concurrent.api.controller;

import com.concurrent.api.service.ApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ApiService apiService;

    public ApiController(final ApiService service) {
        this.apiService = service;
    }

    @GetMapping("/fetch-data")
    public Mono<ResponseEntity<Object>> fetchData(){
        return apiService.fetchData()
                .flatMap(data -> {
                    if(data instanceof Map && ((Map<?,?>) data).containsKey("error")){
                        return Mono.just(ResponseEntity.status(400).body(data));
                    }
                    return Mono.just(ResponseEntity.ok(data));
                })
                .onErrorResume(throwable -> {
                    return Mono.just(ResponseEntity.status(500).body("An error occurred while fetching data"));
                });
    }
}
