package org.sid;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableHystrix
@EnableCircuitBreaker
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @Bean
    RouteLocator staticRoutes(RouteLocatorBuilder builder){
        return builder.routes()
                .route(r->r.path("/customers/**").uri("lb://CUSTOMER-SERVICE"))
                .route(r->r.path("/products/**").uri("lb://INVENTORY-SERVICE"))
                .route(r->r
                        .path("/publicCountries/**")
                        .filters(f->f
                                .addRequestHeader("x-rapidapi-host", "restcountries-v1.p.rapidapi.com")
                                .addRequestHeader("x-rapidapi-key", "7c5cd2bc01mshfde02a7a6027b61p1669d1jsnadc6aa04ff2e")
                                .rewritePath("/publicCountries/(?<segment>.*)", "/${segment}")
//                                .hystrix(h->h.setName("countries").setFallbackUri(":/defaultCountries"))
                            )
                        .uri("https://restcountries-v1.p.rapidapi.com/"))
                .route(r->r
                        .path("/muslim/**")
                        .filters(f->f
                                .addRequestHeader("x-rapidapi-host", "muslimsalat.p.rapidapi.com")
                                .addRequestHeader("x-rapidapi-key", "7c5cd2bc01mshfde02a7a6027b61p1669d1jsnadc6aa04ff2e")
                                .rewritePath("/muslim/(?<segment>.*)", "/${segment}")
                        )
                        .uri("https://muslimsalat.p.rapidapi.com/"))
                .build();
      }

    @Bean
    DiscoveryClientRouteDefinitionLocator dynamicRoutes(ReactiveDiscoveryClient rdc,
                                                        DiscoveryLocatorProperties dlp){

        return new DiscoveryClientRouteDefinitionLocator(rdc, dlp);
      }
}

//Pour Hystrix mais problem
@RestController
class CircuitBreakerRestController{
    @GetMapping("/defaultCountries")
    public Map<String, String> countries(){
        Map<String, String> data = new HashMap<>();
        data.put("message", "default Countries");
        data.put("countries", "Senegal, Guinee, Mali, ....");
        return data;
    }
}
