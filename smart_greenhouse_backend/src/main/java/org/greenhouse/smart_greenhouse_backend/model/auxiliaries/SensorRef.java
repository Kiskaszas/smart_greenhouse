package org.greenhouse.smart_greenhouse_backend.model.auxiliaries;

import com.mongodb.lang.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Type;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Unit;

import java.time.Instant;

public record SensorRef(
        @NotNull(message = "Id nem lehet üres")
        String id,

        @NotNull(message = "Code nem lehet üres")
        String code,

        @NotNull(message = "Type nem lehet üres")
        Type type,

        @NotNull(message = "Unit kötelező")
        Unit unit,

        @NotNull(message = "Érték kötelező")
        @Positive(message = "Értéknek pozitívnak kell lennie")
        Double lastValue,

        @Nullable
        Instant lastSeen
) {}