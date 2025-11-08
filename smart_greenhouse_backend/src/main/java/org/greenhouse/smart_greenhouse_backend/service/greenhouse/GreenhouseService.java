package org.greenhouse.smart_greenhouse_backend.service.greenhouse;

import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.DeviceState;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.SensorRef;
import org.greenhouse.smart_greenhouse_backend.model.documents.ActionLog;
import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;

import java.util.List;

/**
 * A GreenhouseService felelős az üvegházak üzleti logikájáért.
 *
 * Tartalmazza a CRUD műveleteket, a szenzoradatok kezelését,
 * az eszközök manuális és automatikus vezérlését, az időjárás
 * lekérdezését, valamint a szabályok kiértékelését és a tervek generálását.
 */
public interface GreenhouseService {

    /**
     * Új üvegház létrehozása.
     * Ha a plantType mező ki van töltve, automatikusan hozzárendeli
     * a megfelelő PlantProfile ID-t.
     *
     * @param greenhouse az új üvegház adatai
     * @return a mentett üvegház
     */
    Greenhouse create(final Greenhouse greenhouse);

    /**
     * Az összes üvegház listázása.
     *
     * @return az összes üvegház listája
     */
    List<Greenhouse> listAll();

    /**
     * Egy üvegház lekérése azonosító alapján.
     *
     * @param code az üvegház azonosítója
     * @return a megtalált üvegház
     * @throws RuntimeException ha nem található
     */
    Greenhouse getByCode(final String code);

    /**
     * Egy üvegház adatainak frissítése.
     *
     * @param id az üvegház azonosítója
     * @param updated az új adatok
     * @return a frissített üvegház
     */
    Greenhouse updateById(final String id, final Greenhouse updated);

    /**
     * Egy üvegház törlése.
     *
     * @param id az üvegház azonosítója
     * @throws RuntimeException ha nem található
     */
    void deleteByCode(final String id);

    /**
     * Szenzoradat hozzáadása vagy frissítése egy üvegházhoz.
     *
     * @param code az üvegház azonosítója
     * @param sensor a szenzor adatai
     * @return a frissített üvegház
     */
    Greenhouse addSensorData(final String code, final SensorRef sensor);

    /**
     * Egy üvegház eszközeinek aktuális állapotát adja vissza.
     *
     * @param code az üvegház azonosítója
     * @return az eszközök állapota
     */
    DeviceState getState(final String code);

    /**
     * Manuális beavatkozás végrehajtása egy üvegházban.
     * Az akció naplózásra kerül az ActionLog táblában.
     *
     * @param id az üvegház azonosítója
     * @param action a végrehajtandó akció (pl. VENT_OPEN)
     * @return az eszközök frissített állapota
     */
    DeviceState manualAction(final String id, final String action);

    /**
     * Aktuális időjárás lekérése egy üvegházhoz.
     * Az OpenWeather API-t hívja meg, de a city mezőt mindig
     * az üvegházban tárolt városnévre állítja.
     *
     * @param code az üvegház azonosítója
     * @return az aktuális időjárás DTO
     */
    WeatherDto fetchWeatherForGreenhouse(final String code);

    /**
     * 3 napos előrejelzés lekérése egy üvegházhoz.
     *
     * @param id az üvegház azonosítója
     * @return előrejelzés WeatherDto listaként
     */
    List<WeatherDto> fetchForecastForGreenhouse(final String id);

    /**
     * Egy üvegház akciónaplójának lekérése.
     *
     * @param code az üvegház azonosítója
     * @return az akciók listája időrendben
     */
    List<ActionLog> getLogs(final String code);

    /**
     * Szabályok kiértékelése a belső szenzoradatok alapján.
     * Ha valamelyik érték átlépi a küszöböt, a megfelelő akció
     * végrehajtásra kerül és naplózódik.
     */
    void evaluateRulesFromSensors();

    /**
     * Szabályok kiértékelése az aktuális külső időjárás alapján.
     * Ha valamelyik érték átlépi a küszöböt, a megfelelő akció
     * végrehajtásra kerül és naplózódik.
     */
    void evaluateRulesFromWeather();

    /**
     * Előrejelzésből automatikusan generált terv létrehozása.
     * 12 óránként fut, és a következő 3 napra javasolt eseményeket
     * tartalmazza (pl. "Holnap 13:00 körül 32 °C, javasolt SHADE_ON").
     */
    void generatePlans();

    /**
     * Egy üvegházhoz tartozó aktuális terv lekérése.
     *
     * @param code az üvegház azonosítója
     * @return a legutóbbi generált terv
     */
    Plan getLastPlanForGreenhouse(final String code);

    /**
     * Lekéri az összes üvegházhoz az aktuális időjárási adatokat,
     * pillanatfelvételt készít belőlük, és elmenti az adatbázisba.
     *
     * Tipikusan időzítve fut (pl. @Scheduled), hogy folyamatosan
     * naprakész adatok álljanak rendelkezésre az üvegházakhoz.
     */
    void pollAllGreenhouses();

}
