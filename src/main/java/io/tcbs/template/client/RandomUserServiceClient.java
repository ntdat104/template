package io.tcbs.template.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient (
    name = "randomuser-service-client",
    url = "${client.randomuser-service.url}"
)
public interface RandomUserServiceClient {

    @GetMapping("/api")
    Map<String, Object> generate();
}
