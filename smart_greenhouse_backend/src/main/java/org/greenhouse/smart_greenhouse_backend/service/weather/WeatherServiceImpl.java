package org.greenhouse.smart_greenhouse_backend.service.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WebClient weatherWebClient;

    @Value("${weather.api-key}")
    private String apiKey;

    @Value("${weather.units}")
    private String units;

    @Override
    public Mono<WeatherDto> fetchForLocation(String city, Double lat, Double lon) {
        if (lat != null && lon != null) {
            return weatherWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("appid", apiKey)
                            .queryParam("units", units)
                            .build())
                    .retrieve()
                    .bodyToMono(OpenWeatherResponse.class)
                    .map(this::mapToDto);
        } else {
            return weatherWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather")
                            .queryParam("q", city)
                            .queryParam("appid", apiKey)
                            .queryParam("units", units)
                            .build())
                    .retrieve()
                    .bodyToMono(OpenWeatherResponse.class)
                    .map(this::mapToDto);
        }
    }

    @Override
    public Flux<WeatherDto> fetchForecastForLocation(String city, Double lat, Double lon) {
        if (lat != null && lon != null) {
            return weatherWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("appid", apiKey)
                            .queryParam("units", units)
                            .build())
                    .retrieve()
                    .bodyToMono(OpenWeatherForecastResponse.class)
                    .flatMapMany(resp -> Flux.fromIterable(resp.getList())
                            .map(this::mapForecastItemToDto));
        } else {
            return weatherWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("q", city)
                            .queryParam("appid", apiKey)
                            .queryParam("units", units)
                            .build())
                    .retrieve()
                    .bodyToMono(OpenWeatherForecastResponse.class)
                    .flatMapMany(resp -> Flux.fromIterable(resp.getList())
                            .map(this::mapForecastItemToDto));
        }
    }

    private WeatherDto mapToDto(OpenWeatherResponse weatherResponse) {
        Double precipitation = 0.0;
        if (weatherResponse.getRain() != null && weatherResponse.getRain().getOneHour() != null) {
            precipitation = weatherResponse.getRain().getOneHour();
        }
        return WeatherDto.builder()
                .city(weatherResponse.getName())
                .temperature(weatherResponse.getMain().getTemp())
                .humidity(weatherResponse.getMain().getHumidity().doubleValue())
                .windSpeed(weatherResponse.getWind().getSpeed())
                .precipitationMm(precipitation)
                .build();
    }

    private WeatherDto mapForecastItemToDto(OpenWeatherForecastResponse.ForecastItem item) {
        Double precipitation = 0.0;
        if (item.getRain() != null && item.getRain().getThreeHour() != null) {
            precipitation = item.getRain().getThreeHour();
        }
        return WeatherDto.builder()
                .timestamp(Instant.ofEpochSecond(item.getDt())) // előrejelzés időpontja
                .temperature(item.getMain().getTemp())
                .humidity(item.getMain().getHumidity().doubleValue())
                .windSpeed(item.getWind().getSpeed())
                .precipitationMm(precipitation)
                .build();
    }

    @Data
    static class OpenWeatherResponse {
        private String name;
        private OpenWeatherResponse.Main main;
        private OpenWeatherResponse.Wind wind;
        private OpenWeatherResponse.Rain rain;
        @Data static class Main { private Double temp; private Integer humidity; }
        @Data static class Wind { private Double speed; }
        @Data static class Rain { @JsonProperty("1h") private Double oneHour; }
    }

    @Data
    static class OpenWeatherForecastResponse {
        private java.util.List<OpenWeatherForecastResponse.ForecastItem> list;

        @Data
        static class ForecastItem {
            private long dt;
            private OpenWeatherForecastResponse.ForecastItem.Main main;
            private OpenWeatherForecastResponse.ForecastItem.Wind wind;
            private OpenWeatherForecastResponse.ForecastItem.Rain rain;
            @JsonProperty("dt_txt")
            private String dtTxt; // pl. "2025-10-25 12:00:00"

            @Data static class Main { private Double temp; private Integer humidity; }
            @Data static class Wind { private Double speed; }
            @Data static class Rain { @JsonProperty("3h") private Double threeHour; }
        }
    }
}