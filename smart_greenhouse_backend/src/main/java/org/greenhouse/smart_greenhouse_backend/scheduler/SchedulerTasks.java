package org.greenhouse.smart_greenhouse_backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenhouse.smart_greenhouse_backend.service.greenhouse.GreenhouseService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ütemezett feladatokat tartalmazó komponens.
 *
 * A Spring @Scheduled annotáció segítségével meghatározott időközönként
 * automatikusan lefutó folyamatokat definiálunk. Ezek a folyamatok
 * a szenzoradatok, az időjárás és az előrejelzések alapján
 * értékelik ki a szabályokat, és szükség esetén akciókat hajtanak végre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class SchedulerTasks {

    private final GreenhouseService service;

    /**
     * 5 percenként lefutó ütemezett feladat.
     * <p>
     * Feladata: az üvegházak belső szenzorainak legfrissebb értékeit
     * kiértékeli a hozzárendelt PlantProfile szabályai alapján.
     * Ha valamelyik érték átlépi a küszöböt, akkor a megfelelő
     * akció (pl. öntözés bekapcsolása, szellőző nyitása) végrehajtásra kerül,
     * és bekerül az akciónaplóba.
     */
    @Scheduled(fixedRate = 300000) // 5 perc
    public void checkSensors() {
        service.evaluateRulesFromSensors();
    }

    /**
     * 15 percenként lefutó ütemezett feladat.
     * <p>
     * Feladata: az aktuális külső időjárási adatokat lekéri az időjárás API-ból,
     * majd a PlantProfile szabályai alapján kiértékeli, hogy szükséges-e
     * beavatkozás az üvegházban. Például ha a külső hőmérséklet túl magas,
     * akkor a szellőző és az árnyékoló automatikusan aktiválódhat.
     */
    @Scheduled(fixedRate = 900000) // 15 perc
    public void fetchWeather() {
        service.evaluateRulesFromWeather();
    }

    /**
     * 12 óránként lefutó ütemezett feladat.
     * <p>
     * Feladata: a 3 napos időjárás-előrejelzés lekérése és elemzése.
     * Az előrejelzés alapján a rendszer "Plan" objektumokat generál,
     * amelyek tartalmazzák a várható eseményeket (pl. "Holnap 13:00 körül 32 °C,
     * javasolt SHADE_ON és VENT_OPEN"). Ezek a tervek segítenek a felhasználónak
     * előre látni a szükséges beavatkozásokat.
     */
    @Scheduled(cron = "0 0 */1 * * *") // 12 óránként
    public void generatePlans() {
        service.generatePlans();
    }
}