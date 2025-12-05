const API_BASE = '/api';

const FIELDS_TO_WATCH = [
    'irrigationMode',
    'irrigationActive',
    'ventilationActive',
    'weatherCondition',
    'soilMoisture',
    'airHumidity',
    'airTemperature'
];

let previousStateByCode = new Map();
let selectedGreenhouseCode = null;

async function apiGet(path) {
    const response = await fetch(`${API_BASE}${path}`);
    if (!response.ok) {
        throw new Error(`API hiba: ${response.status}`);
    }
    return response.json();
}

async function apiPost(path, body) {
    const response = await fetch(`${API_BASE}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: body ? JSON.stringify(body) : null
    });

    if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new Error(text || `API hiba: ${response.status}`);
    }

    return response.json().catch(() => null);
}

async function apiDelete(path) {
    const response = await fetch(`${API_BASE}${path}`, {
        method: 'DELETE'
    });

    if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new Error(text || `API hiba: ${response.status}`);
    }

    return response.text().catch(() => null);
}

/* ---------- Notification rendszer ---------- */

function showNotification(title, body) {
    const container = document.getElementById('notification-container');
    if (!container) return;

    const el = document.createElement('div');
    el.className = 'notification';

    const titleEl = document.createElement('div');
    titleEl.className = 'notification-title';
    titleEl.textContent = title;

    el.appendChild(titleEl);

    if (body) {
        const bodyEl = document.createElement('div');
        bodyEl.className = 'notification-body';
        bodyEl.textContent = body;
        el.appendChild(bodyEl);
    }

    container.appendChild(el);

    //eltűnés 3 sec
    setTimeout(() => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(-6px)';
        setTimeout(() => {
            el.remove();
        }, 200);
    }, 3000);
}

/* ---------- üvegház lista ---------- */

function renderGreenhouseList(greenhouses) {
    const listOfGreenHouses = document.getElementById('greenhouse-list');
    if (!listOfGreenHouses) return;

    if (!greenhouses || greenhouses.length === 0) {
        listOfGreenHouses.classList.add('empty-state');
        listOfGreenHouses.textContent = 'Még nincs üvegház az adatbázisban.';
        return;
    }

    listOfGreenHouses.classList.remove('empty-state');
    listOfGreenHouses.innerHTML = '';

    greenhouses.forEach(greenHouse => {
        const item = document.createElement('div');
        item.className = 'greenhouse-item';
        if (greenHouse.code === selectedGreenhouseCode) {
            item.classList.add('active');
        }

        const left = document.createElement('div');
        const name = document.createElement('div');
        name.className = 'greenhouse-name';
        name.textContent = greenHouse.name ?? 'Névtelen üvegház';

        const code = document.createElement('div');
        code.className = 'greenhouse-code';
        code.textContent = greenHouse.code ? `Kód: ${greenHouse.code}` : '';

        left.appendChild(name);
        left.appendChild(code);

        const rigreenHouset = document.createElement('div');
        const status = document.createElement('span');
        status.className = 'badge';

        // ha van külön greenHouse.active mező, azt használjuk; ha nincs, akkor az öntözés állapota alapján döntünk
        const isActive = greenHouse.active ?? greenHouse.irrigationActive ?? false;
        status.textContent = isActive ? 'Aktív' : 'Inaktív';
        status.style.background = isActive ? '#dcfce7' : '#fee2e2';
        status.style.color = isActive ? '#166534' : '#b91c1c';

        rigreenHouset.appendChild(status);

        item.appendChild(left);
        item.appendChild(rigreenHouset);

        item.addEventListener('click', () => {
            selectedGreenhouseCode = greenHouse.code;
            renderGreenhouseDetails(greenHouse);

            document
                .querySelectorAll('.greenhouse-item')
                .forEach(el => el.classList.remove('active'));
            item.classList.add('active');
        });

        listOfGreenHouses.appendChild(item);
    });
}

/* ---------- üvegház részletek ---------- */

async function renderGreenhouseDetails(greenHouse) {
    const titleEl = document.getElementById('details-title');
    const detailsEl = document.getElementById('greenhouse-details');
    const devicesEl = document.getElementById('greenhouse-details-devices');
    const statusPill = document.getElementById('details-status-pill');
    const manualCard = document.getElementById('manual-control-card');
    const sensorSection = document.getElementById('sensor-section');

    // Ha nincs kiválasztott üvegház
    if (!greenHouse) {
        titleEl.textContent = 'Válassz egy üvegházat a listából…';
        detailsEl.innerHTML = '';
        if (devicesEl) devicesEl.innerHTML = '';

        if (statusPill) {
            statusPill.textContent = 'Nincs kiválasztva';
            statusPill.classList.remove('status-pill--active', 'status-pill--inactive');
            statusPill.classList.add('status-pill--inactive');
        }
        if (manualCard) {
            manualCard.classList.add('hidden');
        }
        if (sensorSection) {
            sensorSection.classList.add('hidden');
        }
        return;
    }

    // Cím
    titleEl.textContent = greenHouse.name ?? 'Üvegház részletek';

    // Aktív / inaktív státusz pill
    const isActive = greenHouse.active ?? false;
    if (statusPill) {
        statusPill.textContent = isActive ? 'Aktív üvegház' : 'Inaktív üvegház';
        statusPill.classList.remove('status-pill--active', 'status-pill--inactive');
        statusPill.classList.add(isActive ? 'status-pill--active' : 'status-pill--inactive');
    }

    const activeToggle = document.getElementById('toggle-active');
    if (activeToggle) {
        activeToggle.checked = isActive;
        activeToggle.dataset.greenhouseCode = greenHouse.code;
    }

    const plantLabel = greenHouse.plantType ?? greenHouse.plantName ?? '-';

    // Időjárás lekérése
    let weather = null;
    try {
        weather = await apiGet('/greenhouses/' + greenHouse.code + '/weather/current');
    } catch (err) {
        console.error('Nem sikerült betölteni az időjárás adatokat', err);
    }

    const location = greenHouse.location || {};
    const devices = greenHouse.devices || {};
    const sensors = Array.isArray(greenHouse.sensors) ? greenHouse.sensors : [];

    // Helper: szenzor érték kinyerése code alapján
    function findSensorValue(code) {
        const s = sensors.find(s => s.code === code);
        if (!s || typeof s.lastValue !== 'number') {
            return null;
        }
        return s.lastValue;
    }

    // Belső szenzorok
    const intTemp = findSensorValue('INT_TEMP');          // belső hőmérséklet
    const intHum = findSensorValue('INT_HUMIDITY');       // belső páratartalom
    const intSoil = findSensorValue('SOIL_MOIST');        // belső talajnedvesség

    // Helper: formázás
    function fmt(value, unit) {
        if (value == null) return '-';
        const v = Math.round(value * 10) / 10;
        return unit ? `${v} ${unit}` : `${v}`;
    }

    const formattedTimestamp = weather && weather.timestamp
        ? formatTimestamp(weather.timestamp)
        : '-';

    // ---------- FŐ RÉSZLETEK (helyszín + belső/külső statok) ----------
    detailsEl.innerHTML = `
        <p><strong>Kód:</strong> ${greenHouse.code ?? '-'}</p>
        <p><strong>Növény típus:</strong> ${plantLabel}</p>

        <div class="details-grid">
            <div class="stat-card">
                <div class="stat-label">Helyszín</div>
                <div class="stat-value">
                    ${location.city ?? '-'}
                </div>
                <div class="stat-subvalue" style="font-size: 0.8rem; color:#6b7280;">
                    ${
        (location.lat != null && location.lon != null)
            ? `Lat: ${location.lat.toFixed(4)}<br/>Lon: ${location.lon.toFixed(4)}`
            : ''
    }
                </div>
            </div>

            <div class="stat-card">
                <div class="stat-label">Talajnedvesség (belső)</div>
                <div class="stat-value">
                    ${fmt(intSoil, '%')}
                </div>
                <div class="stat-subvalue">
                    Külső (kalkulált): ${weather?.soilMoistureExtPct ?? '-'} %
                </div>
            </div>

            <div class="stat-card">
                <div class="stat-label">Levegő páratartalom (belső)</div>
                <div class="stat-value">
                    ${fmt(intHum, '%')}
                </div>
                <div class="stat-subvalue">
                    Külső: ${weather?.humidity ?? '-'} %
                </div>
            </div>

            <div class="stat-card">
                <div class="stat-label">Hőmérséklet (belső)</div>
                <div class="stat-value">
                    ${fmt(intTemp, '°C')}
                </div>
                <div class="stat-subvalue">
                    Külső: ${weather?.temperature ?? '-'} °C
                </div>
            </div>
        </div>

        <p style="margin-top: 12px; font-size: 0.85rem; color: #6b7280;">
            <strong>Utolsó frissítés:</strong> ${formattedTimestamp}
        </p>
    `;

    // ---------- ESZKÖZÖK (öntözés, szellőztetés, stb.) ----------
    if (devicesEl) {
        devicesEl.innerHTML = `
            <div class="details-grid">
                <div class="stat-card">
                    <div class="stat-label">Öntözés</div>
                    <div class="stat-value">
                        ${devices.irrigationOn ? 'Aktív' : 'Inaktív'}
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-label">Szellőztetés</div>
                    <div class="stat-value">
                        ${devices.ventOpen ? 'Aktív' : 'Inaktív'}
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-label">Árnyékolás</div>
                    <div class="stat-value">
                        ${devices.shadeOn ? 'Aktív' : 'Inaktív'}
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-label">Világítás</div>
                    <div class="stat-value">
                        ${devices.lightOn ? 'Aktív' : 'Inaktív'}
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-label">Párásító</div>
                    <div class="stat-value">
                        ${devices.humidifierOn ? 'Aktív' : 'Inaktív'}
                    </div>
                </div>
            </div>
        `;
    }

    // ---------- MANUÁLIS VEZÉRLÉS (toggle-ok szinkronizálása) ----------
    if (manualCard) {
        manualCard.classList.remove('hidden');
    }

    const irrigationToggle   = document.getElementById('toggle-irrigation');
    const ventilationToggle  = document.getElementById('toggle-ventilation');
    const shadeToggle        = document.getElementById('toggle-shade');
    const lightToggle        = document.getElementById('toggle-light');
    const humidifierToggle   = document.getElementById('toggle-humidifier');

    if (irrigationToggle) {
        irrigationToggle.checked = !!devices.irrigationOn;
        irrigationToggle.dataset.greenhouseCode = greenHouse.code;
    }
    if (ventilationToggle) {
        ventilationToggle.checked = !!devices.ventOpen;
        ventilationToggle.dataset.greenhouseCode = greenHouse.code;
    }
    if (shadeToggle) {
        shadeToggle.checked = !!devices.shadeOn;
        shadeToggle.dataset.greenhouseCode = greenHouse.code;
    }
    if (lightToggle) {
        lightToggle.checked = !!devices.lightOn;
        lightToggle.dataset.greenhouseCode = greenHouse.code;
    }
    if (humidifierToggle) {
        humidifierToggle.checked = !!devices.humidifierOn;
        humidifierToggle.dataset.greenhouseCode = greenHouse.code;
    }

    const deleteBtn = document.getElementById('btn-delete');
    if (deleteBtn) {
        deleteBtn.dataset.greenhouseCode = greenHouse.code;
    }

    const refreshBtn = document.getElementById('btn-refresh');
    if (refreshBtn) {
        refreshBtn.dataset.greenhouseCode = greenHouse.code;
    }

    // ---------- SZENZOROK BLOKK ----------
    if (typeof renderSensors === 'function') {
        if (sensorSection) {
            sensorSection.classList.remove('hidden');
        }
        renderSensors(greenHouse);
    } else if (sensorSection) {
        sensorSection.classList.add('hidden');
    }
}

function renderSensors(greenHouse) {
    const section = document.getElementById('sensor-section');
    const listEl = document.getElementById('sensor-list');

    if (!section || !listEl) return;

    if (!greenHouse) {
        section.classList.add('hidden');
        listEl.innerHTML = '';
        return;
    }

    section.classList.remove('hidden');

    const sensors = greenHouse.sensors || [];

    if (sensors.length === 0) {
        listEl.classList.add('empty-state');
        listEl.textContent = 'Ehhez az üvegházhoz még nincs szenzor regisztrálva.';
        return;
    }

    listEl.innerHTML = `
        <table class="sensor-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Kód</th>
                    <th>Típus</th>
                    <th>Utolsó érték</th>
                    <th>Egység</th>
                    <th>Utolsó észlelés</th>
                </tr>
            </thead>
            <tbody>
                ${sensors.map(s => `
                    <tr>
                        <td>${s.id}</td>
                        <td>${s.code}</td>
                        <td>${s.type}</td>
                        <td>${s.lastValue}</td>
                        <td>${s.unit}</td>
                        <td>${s.lastSeen ? formatTimestamp(s.lastSeen) : '-'}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    listEl.classList.remove('empty-state');
}

function formatTimestamp(ts) {
    if (!ts) return '-';
    const d = new Date(ts);

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hour = String(d.getHours()).padStart(2, '0');
    const minute = String(d.getMinutes()).padStart(2, '0');

    return `${year}. ${month}. ${day}. ${hour}:${minute}`;
}

/* ---------- Változás-detektálás a notificationhöz ---------- */

function hasMeaningfulChange(oldgreenHouse, newgreenHouse) {
    if (!oldgreenHouse) return false;
    return FIELDS_TO_WATCH.some(key => oldgreenHouse[key] !== newgreenHouse[key]);
}

function greenhouseChangeSummary(oldgreenHouse, newgreenHouse) {
    const changes = [];

    FIELDS_TO_WATCH.forEach(key => {
        if (oldgreenHouse[key] !== newgreenHouse[key]) {
            changes.push(`${key}: ${oldgreenHouse[key] ?? '-'} → ${newgreenHouse[key] ?? '-'}`);
        }
    });

    return changes.join(', ');
}

/* ---------- üvegházak betöltése és pollolás ---------- */

async function loadGreenhouses(showNotifications = false) {
    try {
        const greenhouses = await apiGet('/greenhouses');

        const demoBtn = document.getElementById('create-demo-btn');
        if (demoBtn) {
            demoBtn.style.display = 'inline-block';
        }

        renderGreenhouseList(greenhouses);

        greenhouses.forEach(greenHouse => {
            const code = greenHouse.code ?? greenHouse.id ?? greenHouse._id;
            if (!code) {
                return;
            }

            const old = previousStateByCode.get(code);

            if (showNotifications && hasMeaningfulChange(old, greenHouse)) {
                const summary = greenhouseChangeSummary(old, greenHouse);
                const name = greenHouse.name ?? `üvegház (${code})`;
                showNotification(`Frissült az üvegház állapota: ${name}`, summary);
            }

            previousStateByCode.set(code, greenHouse);

            // Ha a kiválasztott üvegház frissült, jobb oldali panelt is update-eljük
            if (selectedGreenhouseCode && code === selectedGreenhouseCode) {
                renderGreenhouseDetails(greenHouse);
            }
        });
    } catch (err) {
        console.error('Nem sikerült betölteni az üvegházakat', err);
    }
}

/* ---------- Új üvegház gomb ---------- */

async function setupNewGreenHouseButton() {
    const modal = document.getElementById('modal-create-greenhouse');
    const btnCreate = document.getElementById('create-demo-btn');
    const btnSave = document.getElementById('btn-save-greenHouse');
    const btnCancel = document.getElementById('btn-cancel-greenHouse');

    if (!modal || !btnCreate || !btnSave || !btnCancel) {
        return;
    }

    btnCreate.addEventListener('click', () => {
        modal.classList.remove('hidden');
    });

    btnCancel.addEventListener('click', () => {
        modal.classList.add('hidden');
    });

// ----------- SAVE NEW GREENHOUSE -----------
    btnSave.addEventListener('click', async () => {

        const greenhouse = {
            name: document.getElementById('greenHouse-name').value,
            code: document.getElementById('greenHouse-code').value,
            plantType: document.getElementById('greenHouse-plant-type').value,
            active: true,
            location: {
                city: document.getElementById('greenHouse-city').value,
                lat: parseFloat(document.getElementById('greenHouse-lat').value),
                lon: parseFloat(document.getElementById('greenHouse-lon').value)
            },
            sensors: [],
            devices: {
                irrigationOn: false,
                ventOpen: false,
                shadeOn: false,
                lightOn: false,
                humidifierOn: false
            }
        };

        // ---------- SEND POST ----------
        const response = await fetch(`${API_BASE}/greenhouses`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(greenhouse)
        });

        if (response.ok) {
            modal.classList.add('hidden');
            resetCreateGreenhouseModal();
            await loadGreenhouseList(false); // újra betölti bal oldalon

        } else {
            alert("Hiba történt az üvegház létrehozásakor.");
        }
    });
}

function resetCreateGreenhouseModal() {
    document.getElementById('greenHouse-name').value = '';
    document.getElementById('greenHouse-code').value = '';
    document.getElementById('greenHouse-plant-type').value = '';
    document.getElementById('greenHouse-city').value = '';
    document.getElementById('greenHouse-lat').value = '';
    document.getElementById('greenHouse-lon').value = '';
}

async function loadGreenhouseList() {
    const response = await fetch(`${API_BASE}/greenhouses`);
    const data = await response.json();

    const container = document.getElementById('greenhouse-list');
    container.innerHTML = ""; // clear

    if (data.length === 0) {
        container.classList.add("empty-state");
        container.innerText = "Még nincs üvegház az adatbázisban.";
        return;
    }

    container.classList.remove("empty-state");

    data.forEach(greenHouse => {
        const div = document.createElement("div");
        div.className = "greenhouse-item";
        div.innerHTML = `
           <strong>${greenHouse.name}</strong><br>
           Kód: ${greenHouse.code}<br>
           Típus: ${greenHouse.plantType}<br>
           Város: ${greenHouse.location?.city ?? "-"}<br>
           Aktív: ${greenHouse.active ? "Igen" : "Nem"}
        `;
        container.appendChild(div);
    });
}

function setupManualControls() {
    const irrigationToggle = document.getElementById('toggle-irrigation');
    const ventilationToggle = document.getElementById('toggle-ventilation');
    const shadeToggle = document.getElementById('toggle-shade');
    const lightToggle = document.getElementById('toggle-light');
    const humidifierToggle = document.getElementById('toggle-humidifier');
    const deleteBtn = document.getElementById('btn-delete');
    const refreshBtn = document.getElementById('btn-refresh');

    function requireSelection(e) {
        if (!selectedGreenhouseCode) {
            alert("Előbb válassz ki egy üvegházat a listából!");
            if (e && e.target && 'checked' in e.target) {
                e.target.checked = !e.target.checked;
            }
            return false;
        }
        return true;
    }

        if (irrigationToggle) {
        irrigationToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;

            const action = e.target.checked ? 'IRRIGATION_ON' : 'IRRIGATION_OFF';

            try {
                await apiPost(`/greenhouses/${selectedGreenhouseCode}/devices/${action}`);

                await apiPost(`/greenhouses/${selectedGreenhouseCode}/simulate`);

                showNotification(
                    'Öntözés állapot módosítva',
                    e.target.checked ? 'Öntözés bekapcsolva' : 'Öntözés kikapcsolva'
                );

                await loadGreenhouses(false);
            } catch (err) {
                console.error('Hiba az öntözés állapot módosításakor', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (ventilationToggle) {
        ventilationToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;

            const action = e.target.checked ? 'VENT_OPEN' : 'VENT_CLOSE';

            try {
                await apiPost(`/greenhouses/${selectedGreenhouseCode}/devices/${action}`);

                await apiPost(`/greenhouses/${selectedGreenhouseCode}/simulate`);

                showNotification(
                    'Szellőztetés állapot módosítva',
                    e.target.checked ? 'Szellőztetés megnyitva' : 'Szellőztetés lezárva'
                );
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Hiba a szellőztetés módosításakor', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (shadeToggle) {
        shadeToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;

            const action = e.target.checked ? 'SHADE_ON' : 'SHADE_OFF';

            try {
                await apiPost(`/greenhouses/${selectedGreenhouseCode}/devices/${action}`);

                await apiPost(`/greenhouses/${selectedGreenhouseCode}/simulate`);

                showNotification(
                    'Árnyékolás módosítva',
                    e.target.checked ? 'Árnyékolás bekapcsolva' : 'Árnyékolás kikapcsolva'
                );
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Hiba az árnyékolás módosításakor', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (lightToggle) {
        lightToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;

            const action = e.target.checked ? 'LIGHT_ON' : 'LIGHT_OFF';

            try {
                await apiPost(`/greenhouses/${selectedGreenhouseCode}/devices/${action}`);

                await apiPost(`/greenhouses/${selectedGreenhouseCode}/simulate`);

                showNotification(
                    'Világítás módosítva',
                    e.target.checked ? 'Világítás bekapcsolva' : 'Világítás kikapcsolva'
                );
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Hiba a világítás módosításakor', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (humidifierToggle) {
        humidifierToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;

            const action = e.target.checked ? 'HUMIDIFIER_ON' : 'HUMIDIFIER_OFF';

            try {
                await apiPost(`/greenhouses/${selectedGreenhouseCode}/devices/${action}`);

                await apiPost(`/greenhouses/${selectedGreenhouseCode}/simulate`);

                showNotification(
                    'Párásító módosítva',
                    e.target.checked ? 'Párásító bekapcsolva' : 'Párásító kikapcsolva'
                );
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Hiba a párásító módosításakor', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (deleteBtn) {
        deleteBtn.addEventListener('click', async () => {
            const code = deleteBtn.dataset.greenhouseCode;
            if (!code) return;

            if (!confirm('Biztosan törlöd ezt az üvegházat?')) {
                return;
            }

            try {
                await fetch(`${API_BASE}/greenhouses/${code}`, { method: 'DELETE' });
                showNotification('üvegház törölve');
                selectedGreenhouseCode = null;
                renderGreenhouseDetails(null);
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Nem sikerült törölni az üvegházat', err);
                showNotification('Nem sikerült törölni az üvegházat.');
            }
        });
    }

    if (refreshBtn) {
        refreshBtn.addEventListener('click', async () => {
            await loadGreenhouses(false);
            showNotification('Adatok frissítve');
        });
    }
}

function setupActiveToggle(){
    const activeToggle = document.getElementById('toggle-active');

    if (activeToggle) {
        activeToggle.addEventListener('change', async (e) => {
            if (!selectedGreenhouseCode) {
                alert("Előbb válassz ki egy üvegházat a listából!");
                e.target.checked = !e.target.checked;
                return;
            }

            const newActive = e.target.checked;

            try {
                await apiPost(
                    `/greenhouses/${selectedGreenhouseCode}/active?active=${newActive}`,
                    null
                );
                showNotification(
                    'Üvegház állapot módosítva',
                    newActive ? 'Az üvegház mostantól aktív' : 'Az üvegház inaktív lett'
                );
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Hiba az üvegház aktív/inaktív állapot módosításakor', err);
                alert('Nem sikerült módosítani az üvegház állapotát.');
                e.target.checked = !e.target.checked;
            }
        });
    }
}

function setupSensorControls() {
    const toggleBtn = document.getElementById('btn-sensor-form-toggle');
    const form = document.getElementById('sensor-form');
    const cancelBtn = document.getElementById('btn-sensor-form-cancel');

    if (!toggleBtn || !form) return;

    toggleBtn.addEventListener('click', () => {
        form.classList.toggle('hidden');
    });

    if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
            form.reset();
            form.classList.add('hidden');
        });
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!selectedGreenhouseCode) {
            alert('Előbb válassz ki egy üvegházat!');
            return;
        }

        const id = document.getElementById('sensor-id').value.trim();
        const code = document.getElementById('sensor-code').value.trim();
        const type = document.getElementById('sensor-type').value;
        const unit = document.getElementById('sensor-unit').value;
        const valueRaw = document.getElementById('sensor-value').value.trim();
        const lastValue = valueRaw === '' ? NaN : parseFloat(valueRaw);

        // FRONTENDes validáció – csak azt nézzük, ami tényleg kötelező a backenden
        if (!id || !code || !type || !unit || Number.isNaN(lastValue) || lastValue <= 0) {
            alert('Minden mező kitöltése kötelező, és az értéknek pozitívnak kell lennie.');
            return;
        }

        const sensorPayload = {
            id: id,
            code: code,
            type: type,
            unit: unit,
            lastValue: lastValue
        };

        try {
            console.log('Küldött sensor payload:', sensorPayload);
            await apiPost(`/greenhouses/${selectedGreenhouseCode}/sensors`, sensorPayload);

            showNotification(
                'Szenzor adat mentve',
                `${code} – ${lastValue} (${type})`
            );

            form.reset();
            form.classList.add('hidden');
            await loadGreenhouses(false);

        } catch (err) {
            console.error('Nem sikerült a szenzor adat mentése', err);
            showNotification('Nem sikerült a szenzor adat mentése');
        }
    });
}

/* ---------- Init ---------- */

document.addEventListener('DOMContentLoaded', async () => {
    await setupNewGreenHouseButton();
    await setupManualControls();
    await setupActiveToggle();
    await setupControlButtons();
    await setupCreateGreenhouseForm();
    await setupSensorControls();

    // első betöltés
    await loadGreenhouses(false);

    // Folyamatos frissítés 3 másodpercenként
    setInterval(() => {
        loadGreenhouses(true);
    }, 3000);
});

async function setupControlButtons() {
    const btnOn = document.getElementById('btn-irrigation-on');
    const btnOff = document.getElementById('btn-irrigation-off');
    const btnVent = document.getElementById('btn-ventilation-toggle');
    const btnRefresh = document.getElementById('btn-refresh');
    const btnDelete = document.getElementById('btn-delete');

    function requireSelection() {
        if (!selectedGreenhouseCode) {
            alert("Előbb válassz ki egy üvegházat!");
            return false;
        }
        return true;
    }

    if (btnOn) {
        btnOn.addEventListener('click', async () => {
            if (!requireSelection()) return;
            await apiPost(`/greenhouses/${selectedGreenhouseCode}/irrigation/on`);
            await apiPost(`/greenhouses/${selectedGreenhouseCode}/simulate`);
            showNotification("Öntözés bekapcsolva");
            await loadGreenhouses(false);
        });
    }

    if (btnOff) {
        btnOff.addEventListener('click', async () => {
            if (!requireSelection()) return;
            await apiPost(`/greenhouses/${selectedGreenhouseCode}/irrigation/off`);
            await apiPost(`/greenhouses/${selectedGreenhouseCode}/simulate`);
            showNotification("Öntözés kikapcsolva");
            await loadGreenhouses(false);
        });
    }

    if (btnVent) {
        btnVent.addEventListener('click', async () => {
            if (!requireSelection()) return;
            await apiPost(`/greenhouses/${selectedGreenhouseCode}/ventilation/toggle`);
            showNotification("Szellőztetés kapcsolva");
            await loadGreenhouses(false);
        });
    }

    if (btnRefresh) {
        btnRefresh.addEventListener('click', async () => {
            if (!requireSelection()) return;
            showNotification("Adatok frissítése…");
            await loadGreenhouses(false);
        });
    }

    if (btnDelete) {
        btnDelete.addEventListener('click', async () => {
            if (!requireSelection()) return;
            const confirmed = confirm("Biztosan törlöd ezt az üvegházat?");
            if (!confirmed) return;

            try {
                await apiDelete(`/greenhouses/${selectedGreenhouseCode}`);
                showNotification("üvegház törölve");
                selectedGreenhouseCode = null;
                renderGreenhouseDetails(null);
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Nem sikerült törölni az üvegházat', err);
                showNotification("Nem sikerült törölni az üvegházat");
            }
        });
    }
}

async function setupCreateGreenhouseForm() {
    const toggleBtn = document.getElementById('create-greenhouse-toggle');
    const form = document.getElementById('create-greenhouse-form');

    if (!form) return;

    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            form.classList.toggle('hidden');
        });
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const code = form.elements['code'].value.trim();
        const name = form.elements['name'].value.trim();
        const plantType = form.elements['plantType'].value;
        const city = form.elements['city'].value.trim();
        const latRaw = form.elements['lat'].value.trim();
        const lonRaw = form.elements['lon'].value.trim();

        if (!code) {
            alert('A kód megadása kötelező.');
            return;
        }
        if (!plantType) {
            alert('Válaszd ki a növény típusát.');
            return;
        }

        const lat = latRaw ? parseFloat(latRaw) : null;
        const lon = lonRaw ? parseFloat(lonRaw) : null;

        const payload = {
            name: name || `üvegház ${code}`,
            code: code,
            plantType: plantType,

            // alapértelmezett státusz
            active: false,

            // location – város + lat/lon ha megadtad, különben null vagy default
            location: {
                city: city || 'Budapest',
                lat: lat,
                lon: lon
            },

            // induláskor nincsenek szenzor értékek (backend / IoT tölti majd)
            sensors: [
                // ide is tehetünk default szenzor slotokat, de most üresen hagyom
            ],

            // alapértelmezett eszközállapotok
            devices: {
                ventOpen: false,
                shadeOn: false,
                lightOn: false,
                humidifierOn: false,
                irrigationOn: false
            }
        };

        try {
            await apiPost('/greenhouses', payload);
            showNotification('Új üvegház létrehozva', `${payload.name} (${payload.code})`);

            form.reset();
            form.classList.add('hidden');

            await loadGreenhouses(false);
        } catch (err) {
            console.error('Nem sikerült létrehozni az üvegházat', err);
            showNotification('Nem sikerült létrehozni az üvegházat');
        }
    });
}