package com.concurrent.api.service;

import reactor.core.publisher.Mono;

public interface ApiService {
    public Mono<Object> fetchData();
}
