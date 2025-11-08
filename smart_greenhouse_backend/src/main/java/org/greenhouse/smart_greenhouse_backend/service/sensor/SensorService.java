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
     * Lekéri a legutolsó beérkezett szenzoradatot.
     *
     * @return {@link Optional} a legfrissebb {@link SensorData} példánnyal,
     *         vagy üres, ha még nem érkezett adat
     */
    Optional<SensorData> latest();
}