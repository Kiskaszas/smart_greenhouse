package org.greenhouse.smart_greenhouse_backend.service.weather;

import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WeatherService {

    /**
     * Aktuális időjárás lekérése város vagy koordináták alapján.
     */
    Mono<WeatherDto> fetchForLocation(final String city, final Double lat, final Double lon);

    /**
     * 3 napos előrejelzés lekérése város vagy koordináták alapján.
     * Az OpenWeatherMap /forecast végpontját használja.
     */
    Flux<WeatherDto> fetchForecastForLocation(final String city, final Double lat, final Double lon);
}
