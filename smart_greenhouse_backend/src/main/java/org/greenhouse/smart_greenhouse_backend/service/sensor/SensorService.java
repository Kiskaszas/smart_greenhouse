package org.greenhouse.smart_greenhouse_backend.service.sensor;

import org.greenhouse.smart_greenhouse_backend.model.documents.SensorData;

import java.util.Optional;

public interface SensorService {

    /**
     * Feldolgozza és eltárolja egy szenzorból érkező mérési adatot.
     *
     * @param data a szenzor által küldött nyers mérési adat
     * @return a mentett {@link SensorData} entitás (pl. adatbázis azonosítóval kiegészítve)
     */
    SensorData ingest(SensorData data);

    /**
     * Lekéri a legutolsó beérkezett szenzoradatot a szenzorra.
     *
     * @return {@link Optional} a legfrissebb {@link SensorData} példánnyal,
     *         vagy üres, ha még nem érkezett adat
     */
    Optional<SensorData> latest(final String sensorCode);

    /**
     * A szenzoron hajt végre módosítást az Id alapján.
     *
     * @param sensorCode A szenzor egyedi azonosító kódja.
     * @param sensor A szenzor objektum.
     * @return void
     */
    void updateByCode(String sensorCode, SensorData sensor);

    /**
     * A szenzor törlése a szenzor code és a greenhouse code alapján.
     * @param sensorId
     * @param greenhouseCode
     */
    void deleteByIdAndGreenhouseCode(String sensorId, String greenhouseCode);
}