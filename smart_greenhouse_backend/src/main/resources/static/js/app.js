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

/* ---------- HTTP helper-ek ---------- */

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
    try { return await response.json(); } catch (e) { return null; }
}

async function apiPut(path, body) {
    const response = await fetch(`${API_BASE}${path}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: body ? JSON.stringify(body) : null
    });
    if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new Error(text || `API hiba: ${response.status}`);
    }
    try { return await response.json(); } catch (e) { return null; }
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

/* ---------- Segédfüggvények ---------- */

function isGreenhouseActiveByCode(code) {
    if (!code) return false;
    const gh = previousStateByCode.get(code);
    if (!gh) return false;
    return !!gh.active;
}

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

    setTimeout(() => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(-6px)';
        setTimeout(() => el.remove(), 200);
    }, 3000);
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

/* ---------- Lista és részletek renderelése ---------- */

function renderGreenhouseList(greenhouses) {
    const listEl = document.getElementById('greenhouse-list');
    if (!listEl) return;

    if (!greenhouses || greenhouses.length === 0) {
        listEl.classList.add('empty-state');
        listEl.textContent = 'Még nincs üvegház az adatbázisban.';
        return;
    }

    listEl.classList.remove('empty-state');
    listEl.innerHTML = '';

    greenhouses.forEach(gh => {
        const item = document.createElement('div');
        item.className = 'greenhouse-item';
        if (gh.code === selectedGreenhouseCode) item.classList.add('active');

        const left = document.createElement('div');
        const name = document.createElement('div');
        name.className = 'greenhouse-name';
        name.textContent = gh.name ?? 'Névtelen üvegház';

        const code = document.createElement('div');
        code.className = 'greenhouse-code';
        code.textContent = gh.code ? `Kód: ${gh.code}` : '';

        left.appendChild(name);
        left.appendChild(code);

        const right = document.createElement('div');
        const status = document.createElement('span');
        status.className = 'badge';
        const isActive = gh.active ?? gh.irrigationActive ?? false;
        status.textContent = isActive ? 'Aktív' : 'Inaktív';
        status.style.background = isActive ? '#dcfce7' : '#fee2e2';
        status.style.color = isActive ? '#166534' : '#b91c1c';
        right.appendChild(status);

        item.appendChild(left);
        item.appendChild(right);

        item.addEventListener('click', async () => {
            selectedGreenhouseCode = gh.code;
            try {
                const detailed = await apiGet(`/greenhouses/${encodeURIComponent(gh.code)}`);
                previousStateByCode.set(detailed.code ?? detailed.id ?? detailed._id, detailed);
                renderGreenhouseDetails(detailed);
            } catch (err) {
                renderGreenhouseDetails(gh);
            }

            document.querySelectorAll('.greenhouse-item').forEach(el => el.classList.remove('active'));
            item.classList.add('active');
        });

        listEl.appendChild(item);
    });
}

async function renderGreenhouseDetails(greenHouse) {
    const titleEl = document.getElementById('details-title');
    const detailsEl = document.getElementById('greenhouse-details');
    const devicesEl = document.getElementById('greenhouse-details-devices');
    const statusPill = document.getElementById('details-status-pill');
    const statusPillSpan = document.getElementById('details-status-pill-span');
    const manualCard = document.getElementById('manual-control-card');
    const sensorSection = document.getElementById('sensor-section');

    if (!greenHouse) {
        if (titleEl) titleEl.textContent = 'Válassz egy üvegházat a listából…';
        if (detailsEl) detailsEl.innerHTML = '';
        if (devicesEl) devicesEl.innerHTML = '';
        if (statusPill && statusPillSpan) {
            statusPillSpan.textContent = 'Nincs kiválasztva';
            statusPill.classList.remove('status-pill--active', 'status-pill--inactive');
            statusPill.classList.add('status-pill--inactive');
        }
        if (manualCard) manualCard.classList.add('hidden');
        if (sensorSection) sensorSection.classList.add('hidden');
        return;
    }

    if (titleEl) titleEl.textContent = greenHouse.name ?? 'Üvegház részletek';

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

    let weather = null;
    try {
        weather = await apiGet(`/greenhouses/${encodeURIComponent(greenHouse.code)}/weather/current`);
    } catch (err) {
        console.error('Időjárás lekérés hiba', err);
    }

    const location = greenHouse.location || {};
    const devices = greenHouse.devices || {};
    const sensors = Array.isArray(greenHouse.sensors) ? greenHouse.sensors : [];

    function findSensorValue(code) {
        const sensor = sensors.find(s => s.code === code);
        if (!sensor || typeof sensor.lastValue !== 'number') return null;
        return sensor.lastValue;
    }

    const intTemp = findSensorValue('INT_TEMP');
    const intHum = findSensorValue('INT_HUMIDITY');
    const intSoil = findSensorValue('SOIL_MOIST');
    const intWind = findSensorValue("WIND_SPEED");

    function fmt(value, unit) {
        if (value == null) return '-';
        const v = Math.round(value * 10) / 10;
        return unit ? `${v} ${unit}` : `${v}`;
    }

    const formattedTimestamp = weather && weather.timestamp ? formatTimestamp(weather.timestamp) : '-';

    if (detailsEl) {
        detailsEl.innerHTML = `
            <p><strong>Kód:</strong> ${greenHouse.code ?? '-'}</p>
            <p><strong>Növény típus:</strong> ${plantLabel}</p>
            <div class="details-grid">
                <div class="stat-card">
                    <div class="stat-label">Helyszín</div>
                    <div class="stat-value">${location.city ?? '-'}</div>
                    <div class="stat-subvalue" style="font-size:0.8rem;color:#6b7280;">
                        ${(location.lat != null && location.lon != null) ? `Lat: ${location.lat.toFixed(4)}<br/>Lon: ${location.lon.toFixed(4)}` : ''}
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Talajnedvesség (belső)</div>
                    <div class="stat-value">${fmt(intSoil, '%')}</div>
                    <div class="stat-subvalue">Külső (kalkulált): ${weather?.soilMoistureExtPct ?? '-'} %</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Levegő páratartalom (belső)</div>
                    <div class="stat-value">${fmt(intHum, '%')}</div>
                    <div class="stat-subvalue">Külső: ${weather?.humidity ?? '-'} %</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Hőmérséklet (belső)</div>
                    <div class="stat-value">${fmt(intTemp, '°C')}</div>
                    <div class="stat-subvalue">Külső: ${weather?.temperature ?? '-'} °C</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Légmozgás (belső)</div>
                    <div class="stat-value">${fmt(intWind,'km/h')}</div>
                    <div class="stat-subvalue">Külső: ${weather?.windSpeed ?? '-'} km/h</div>
                </div>
            </div>
            <p style="margin-top:12px;font-size:0.85rem;color:#6b7280;"><strong>Utolsó frissítés:</strong> ${formattedTimestamp}</p>
        `;
    }

    if (devicesEl) {
        devicesEl.innerHTML = `
            <div class="details-grid">
                <div class="stat-card"><div class="stat-label">Öntözés</div><div class="stat-value">${devices.irrigationOn ? 'Aktív' : 'Inaktív'}</div></div>
                <div class="stat-card"><div class="stat-label">Szellőztetés</div><div class="stat-value">${devices.ventOpen ? 'Aktív' : 'Inaktív'}</div></div>
                <div class="stat-card"><div class="stat-label">Árnyékolás</div><div class="stat-value">${devices.shadeOn ? 'Aktív' : 'Inaktív'}</div></div>
                <div class="stat-card"><div class="stat-label">Világítás</div><div class="stat-value">${devices.lightOn ? 'Aktív' : 'Inaktív'}</div></div>
                <div class="stat-card"><div class="stat-label">Párásító</div><div class="stat-value">${devices.humidifierOn ? 'Aktív' : 'Inaktív'}</div></div>
            </div>
        `;
    }

    if (manualCard) {
        if (!isActive) manualCard.classList.add('hidden'); else manualCard.classList.remove('hidden');
    }

    const irrigationToggle = document.getElementById('toggle-irrigation');
    const ventilationToggle = document.getElementById('toggle-ventilation');
    const shadeToggle = document.getElementById('toggle-shade');
    const lightToggle = document.getElementById('toggle-light');
    const humidifierToggle = document.getElementById('toggle-humidifier');

    const toggles = [irrigationToggle, ventilationToggle, shadeToggle, lightToggle, humidifierToggle];
    toggles.forEach(t => {
        if (!t) return;
        if (t.id === 'toggle-irrigation') t.checked = !!devices.irrigationOn;
        if (t.id === 'toggle-ventilation') t.checked = !!devices.ventOpen;
        if (t.id === 'toggle-shade') t.checked = !!devices.shadeOn;
        if (t.id === 'toggle-light') t.checked = !!devices.lightOn;
        if (t.id === 'toggle-humidifier') t.checked = !!devices.humidifierOn;
        t.dataset.greenhouseCode = greenHouse.code;
        t.disabled = !isActive;
    });

    const deleteBtn = document.getElementById('btn-delete');
    if (deleteBtn) deleteBtn.dataset.greenhouseCode = greenHouse.code;
    const refreshBtn = document.getElementById('btn-refresh');
    if (refreshBtn) refreshBtn.dataset.greenhouseCode = greenHouse.code;

    if (sensorSection) {
        if (!isActive) sensorSection.classList.add('hidden'); else sensorSection.classList.remove('hidden');
    }

    // render szenzorok
    renderSensors(greenHouse);
}

/* ---------- Szenzorok megjelenítése, szerkesztés, törlés ---------- */

function renderSensors(greenHouse) {
    const section = document.getElementById('sensor-section');
    const listEl = document.getElementById('sensor-list');
    if (!section || !listEl) return;

    if (!greenHouse || !isGreenhouseActiveByCode(greenHouse.code)) {
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
                    <th>ID</th><th>Kód</th><th>Típus</th><th>Utolsó érték</th><th>Egység</th><th>Utolsó észlelés</th><th>Műveletek</th>
                </tr>
            </thead>
            <tbody>
                ${sensors.map(s => `
                    <tr data-sensor-id="${s.id}">
                        <td>${s.id ?? ''}</td>
                        <td>${s.code ?? ''}</td>
                        <td>${s.type ?? ''}</td>
                        <td>${s.lastValue ?? ''}</td>
                        <td>${s.unit ?? ''}</td>
                        <td>${s.lastSeen ? formatTimestamp(s.lastSeen) : '-'}</td>
                        <td>
                            <button class="btn-small btn-icon btn-edit-sensor" data-sensor-id="${s.id}" aria-label="Szerkesztés" title="Szerkeszt">
                              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
                                <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z" fill="currentColor"/>
                                <path d="M20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z" fill="currentColor"/>
                              </svg>
                            </button>

                            <button class="btn-small btn-icon btn-delete-sensor" data-sensor-id="${s.id}" aria-label="Törlés" title="Töröl">
                              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
                                <path d="M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12z" fill="currentColor"/>
                                <path d="M19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" fill="currentColor"/>
                              </svg>
                            </button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    listEl.classList.remove('empty-state');

    // Edit gombok
    const editButtons = listEl.querySelectorAll('.btn-edit-sensor');
    editButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const sensorId = btn.dataset.sensorId;
            const sensor = sensors.find(s => String(s.id) === String(sensorId));
            if (!sensor) return;
            fillSensorFormFromSensor(sensor);
            const form = document.getElementById('sensor-form');
            if (form) {
                form.dataset.editing = 'true';
                form.dataset.originalSensorId = sensor.id ?? '';
                form.classList.remove('hidden');
            }
        });
    });

    // Delete gombok - backend DELETE /api/greenhouses/{code}/{sensorId}
    const deleteButtons = listEl.querySelectorAll('.btn-delete-sensor');
    deleteButtons.forEach(btn => {
        btn.addEventListener('click', async () => {
            const sensorId = btn.dataset.sensorId;
            if (!sensorId) return;
            if (!confirm('Biztosan törlöd ezt a szenzort?')) return;

            try {
                await apiDelete(`/greenhouses/${encodeURIComponent(greenHouse.code)}/${encodeURIComponent(sensorId)}`);
                showNotification('Szenzor törölve');

                // Lekérjük a frissített üvegházat és rendereljük
                const updated = await apiGet(`/greenhouses/${encodeURIComponent(greenHouse.code)}`);
                if (updated) {
                    previousStateByCode.set(updated.code ?? updated.id ?? updated._id, updated);
                    renderGreenhouseDetails(updated);
                } else {
                    await loadGreenhouses(false);
                    const cached = previousStateByCode.get(greenHouse.code);
                    if (cached) renderGreenhouseDetails(cached);
                }
            } catch (err) {
                console.error('Szenzor törlése sikertelen', err);
                showNotification('Nem sikerült törölni a szenzort: ' + (err.message || ''));
            }
        });
    });
}

/* ---------- Betöltés, pollolás, változás-detektálás ---------- */

function hasMeaningfulChange(oldgh, newgh) {
    if (!oldgh) return false;
    return FIELDS_TO_WATCH.some(k => oldgh[k] !== newgh[k]);
}

function greenhouseChangeSummary(oldgh, newgh) {
    const changes = [];
    FIELDS_TO_WATCH.forEach(k => {
        if (oldgh[k] !== newgh[k]) changes.push(`${k}: ${oldgh[k] ?? '-'} → ${newgh[k] ?? '-'}`);
    });
    return changes.join(', ');
}

async function loadGreenhouses(showNotifications = false) {
    try {
        const greenhouses = await apiGet('/greenhouses');

        const demoBtn = document.getElementById('create-demo-btn');
        if (demoBtn) demoBtn.style.display = 'inline-block';

        renderGreenhouseList(greenhouses);

        greenhouses.forEach(gh => {
            const code = gh.code ?? gh.id ?? gh._id;
            if (!code) return;
            const old = previousStateByCode.get(code);
            if (showNotifications && hasMeaningfulChange(old, gh)) {
                const summary = greenhouseChangeSummary(old, gh);
                const name = gh.name ?? `üvegház (${code})`;
                showNotification(`Frissült az üvegház állapota: ${name}`, summary);
            }
            previousStateByCode.set(code, gh);
            if (selectedGreenhouseCode && code === selectedGreenhouseCode) {
                renderGreenhouseDetails(gh);
            }
        });
    } catch (err) {
        console.error('Nem sikerült betölteni az üvegházakat', err);
    }
}

/* ---------- Új üvegház létrehozása (szenzorok nélkül) ---------- */

async function setupNewGreenHouseButton() {
    const modal = document.getElementById('modal-create-greenhouse');
    const btnCreate = document.getElementById('create-demo-btn');
    const btnSave = document.getElementById('btn-save-greenHouse');
    const btnCancel = document.getElementById('btn-cancel-greenHouse');
    const btnClose = document.getElementById('btn-close-create-greenhouse');


    if (!modal || !btnCreate || !btnSave || !btnCancel) return;

    btnCreate.addEventListener('click', () => modal.classList.remove('hidden'));
    btnCancel.addEventListener('click', () => modal.classList.add('hidden'));
    btnClose.addEventListener('click', () => modal.classList.add('hidden'));

    btnSave.addEventListener('click', async () => {
        const nameEl = document.getElementById('greenHouse-name');
        const codeEl = document.getElementById('greenHouse-code');
        const plantEl = document.getElementById('greenHouse-plant-type');
        const cityEl = document.getElementById('greenHouse-city');
        const latEl = document.getElementById('greenHouse-lat');
        const lonEl = document.getElementById('greenHouse-lon');

        const name = nameEl?.value?.trim() ?? '';
        const code = codeEl?.value?.trim() ?? '';
        const plantType = plantEl?.value ?? '';
        const city = cityEl?.value?.trim() ?? '';
        const latRaw = latEl?.value ?? '';
        const lonRaw = lonEl?.value ?? '';

        // Egyszerű validáció
        if (!name) {
            showNotification('Hiányzó mező', 'Kérlek add meg az üvegház nevét.');
            nameEl?.focus();
            return;
        }
        if (!code) {
            showNotification('Hiányzó mező', 'Kérlek add meg az üvegház kódját.');
            codeEl?.focus();
            return;
        }
        if (!plantType) {
            showNotification('Hiányzó mező', 'Válassz növénytípust.');
            plantEl?.focus();
            return;
        }
        if (!city) {
            showNotification('Hiányzó mező', 'Add meg a várost.');
            cityEl?.focus();
            return;
        }

        // Koordináták ellenőrzése (opcionális: lehet üres is, de ha megadták, legyen szám)
        let lat = null;
        let lon = null;
        if (latRaw !== '') {
            lat = parseFloat(latRaw);
            if (Number.isNaN(lat) || lat < -90 || lat > 90) {
                showNotification('Érvénytelen koordináta', 'A szélesség (lat) nem érvényes szám (−90 … 90).');
                latEl?.focus();
                return;
            }
        }
        if (lonRaw !== '') {
            lon = parseFloat(lonRaw);
            if (Number.isNaN(lon) || lon < -180 || lon > 180) {
                showNotification('Érvénytelen koordináta', 'A hosszúság (lon) nem érvényes szám (−180 … 180).');
                lonEl?.focus();
                return;
            }
        }

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
            sensors: [], // NINCS szenzor létrehozáskor
            devices: {
                irrigationOn: false,
                ventOpen: false,
                shadeOn: false,
                lightOn: false,
                humidifierOn: false
            }
        };

        try {
            const response = await fetch(`${API_BASE}/greenhouses`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(greenhouse)
            });
            if (response.ok) {
                modal.classList.add('hidden');
                resetCreateGreenhouseModal();
                await loadGreenhouses(false);
            } else {
                alert('Hiba történt az üvegház létrehozásakor.');
            }
        } catch (err) {
            console.error('Új üvegház létrehozása hiba', err);
            alert('Hiba történt az üvegház létrehozásakor.');
        }
    });
}

function resetCreateGreenhouseModal() {
    const els = ['greenHouse-name','greenHouse-code','greenHouse-plant-type','greenHouse-city','greenHouse-lat','greenHouse-lon'];
    els.forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
}

/* ---------- Manuális vezérlők ---------- */

function setupManualControls() {
    const irrigationToggle = document.getElementById('toggle-irrigation');
    const ventilationToggle = document.getElementById('toggle-ventilation');
    const shadeToggle = document.getElementById('toggle-shade');
    const lightToggle = document.getElementById('toggle-light');
    const humidifierToggle = document.getElementById('toggle-humidifier');
    const deleteBtn = document.getElementById('btn-delete');
    const refreshBtn = document.getElementById('btn-refresh');

    console.log('vent toggle exists', !!document.getElementById('toggle-ventilation'));
    document.getElementById('toggle-ventilation')?.addEventListener('change', e => console.log('vent change', e.target.checked));

    function requireSelection(e) {
        if (!selectedGreenhouseCode) {
            alert('Előbb válassz ki egy üvegházat a listából!');
            if (e && e.target && 'checked' in e.target) e.target.checked = !e.target.checked;
            return false;
        }
        if (!isGreenhouseActiveByCode(selectedGreenhouseCode)) {
            alert('A kiválasztott üvegház inaktív, a manuális vezérlés nem engedélyezett.');
            if (e && e.target && 'checked' in e.target) e.target.checked = !e.target.checked;
            return false;
        }
        return true;
    }

    if (irrigationToggle) {
        irrigationToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;
            const action = e.target.checked ? 'IRRIGATION_ON' : 'IRRIGATION_OFF';
            try {
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/devices/${action}`);
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/simulate`);
                showNotification('Öntözés állapot módosítva', e.target.checked ? 'Öntözés bekapcsolva' : 'Öntözés kikapcsolva');
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Öntözés hiba', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (ventilationToggle) {
        ventilationToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;
            const action = e.target.checked ? 'VENT_OPEN' : 'VENT_CLOSE';
            try {
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/devices/${action}`);
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/simulate`);
                showNotification('Szellőztetés módosítva', e.target.checked ? 'Szellőztetés megnyitva' : 'Szellőztetés lezárva');
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Szellőztetés hiba', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (shadeToggle) {
        shadeToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;
            const action = e.target.checked ? 'SHADE_ON' : 'SHADE_OFF';
            try {
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/devices/${action}`);
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/simulate`);
                showNotification('Árnyékolás módosítva', e.target.checked ? 'Árnyékolás bekapcsolva' : 'Árnyékolás kikapcsolva');
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Árnyékolás hiba', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (lightToggle) {
        lightToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;
            const action = e.target.checked ? 'LIGHT_ON' : 'LIGHT_OFF';
            try {
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/devices/${action}`);
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/simulate`);
                showNotification('Világítás módosítva', e.target.checked ? 'Világítás bekapcsolva' : 'Világítás kikapcsolva');
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Világítás hiba', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (humidifierToggle) {
        humidifierToggle.addEventListener('change', async (e) => {
            if (!requireSelection(e)) return;
            const action = e.target.checked ? 'HUMIDIFIER_ON' : 'HUMIDIFIER_OFF';
            try {
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/devices/${action}`);
                await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/simulate`);
                showNotification('Párásító módosítva', e.target.checked ? 'Párásító bekapcsolva' : 'Párásító kikapcsolva');
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Párásító hiba', err);
                e.target.checked = !e.target.checked;
            }
        });
    }

    if (deleteBtn) {
        deleteBtn.addEventListener('click', async () => {
            const code = deleteBtn.dataset.greenhouseCode;
            if (!code) return;
            if (!confirm('Biztosan törlöd ezt az üvegházat?')) return;
            try {
                await apiDelete(`/greenhouses/${encodeURIComponent(code)}`);
                showNotification('Üvegház törölve');
                selectedGreenhouseCode = null;
                renderGreenhouseDetails(null);
                await loadGreenhouses(false);
            } catch (err) {
                console.error('Üvegház törlése hiba', err);
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

/* ---------- Aktív toggle ---------- */

function setupActiveToggle() {
    const activeToggle = document.getElementById('toggle-active');
    if (!activeToggle) return;

    activeToggle.addEventListener('change', async (e) => {
        if (!selectedGreenhouseCode) {
            alert('Előbb válassz ki egy üvegházat a listából!');
            e.target.checked = !e.target.checked;
            return;
        }
        const newActive = e.target.checked;
        try {
            await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/active?active=${newActive}`, null);
            showNotification('Üvegház állapot módosítva', newActive ? 'Az üvegház mostantól aktív' : 'Az üvegház inaktív lett');
            await loadGreenhouses(false);
        } catch (err) {
            console.error('Aktív toggle hiba', err);
            alert('Nem sikerült módosítani az üvegház állapotát.');
            e.target.checked = !e.target.checked;
        }
    });
}

/* ---------- Szenzor űrlap és vezérlők (CRUD) ---------- */

function fillSensorFormFromSensor(sensor) {
    const form = document.getElementById('sensor-form');
    if (!form || !sensor) return;
    form.classList.remove('hidden');

    const idInput = document.getElementById('sensor-id');
    const codeSelect = document.getElementById('sensor-code'); // select
    const typeSelect = document.getElementById('sensor-type');
    const unitSelect = document.getElementById('sensor-unit');
    const valueInput = document.getElementById('sensor-value');

    if (idInput) idInput.value = sensor.id ?? '';
    if (codeSelect) {
        const exists = Array.from(codeSelect.options).some(opt => opt.value === sensor.code);
        if (!exists && sensor.code) {
            const opt = document.createElement('option');
            opt.value = sensor.code;
            opt.textContent = sensor.code;
            codeSelect.appendChild(opt);
        }
        codeSelect.value = sensor.code ?? '';
    }
    if (typeSelect) typeSelect.value = sensor.type ?? '';
    if (unitSelect) unitSelect.value = sensor.unit ?? '';
    if (valueInput) valueInput.value = (sensor.lastValue != null && !Number.isNaN(sensor.lastValue)) ? sensor.lastValue : '';

    let lastSeenInput = document.getElementById('sensor-lastseen');
    if (!lastSeenInput) {
        lastSeenInput = document.createElement('input');
        lastSeenInput.type = 'hidden';
        lastSeenInput.id = 'sensor-lastseen';
        form.appendChild(lastSeenInput);
    }
    lastSeenInput.value = sensor.lastSeen ? sensor.lastSeen : '';
}

function clearSensorForm() {
    const form = document.getElementById('sensor-form');
    if (!form) return;
    form.reset();
    form.classList.add('hidden');
    form.dataset.editing = '';
    form.dataset.originalSensorId = '';

    const lastSeenInput = document.getElementById('sensor-lastseen');
    if (lastSeenInput) lastSeenInput.value = '';
}

function setupSensorControls() {
    const toggleBtn = document.getElementById('btn-sensor-form-toggle');
    const form = document.getElementById('sensor-form');
    const cancelBtn = document.getElementById('btn-sensor-form-cancel');
    if (!toggleBtn || !form) return;

    form.reset();
    form.classList.add('hidden');

    toggleBtn.addEventListener('click', () => {
        if (!selectedGreenhouseCode) {
            alert('Előbb válassz ki egy üvegházat a listából!');
            return;
        }
        if (!isGreenhouseActiveByCode(selectedGreenhouseCode)) {
            alert('A kiválasztott üvegház inaktív, szenzorok kezelése nem engedélyezett.');
            return;
        }
        clearSensorForm();
        form.classList.remove('hidden');
    });

    if (cancelBtn) {
        cancelBtn.addEventListener('click', (e) => {
            e.preventDefault();
            clearSensorForm();
        });
    }

    // A submit handler itt, a form DOM referencia biztosított
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!selectedGreenhouseCode) {
            alert('Előbb válassz ki egy üvegházat a listából!');
            return;
        }
        if (!isGreenhouseActiveByCode(selectedGreenhouseCode)) {
            alert('A kiválasztott üvegház inaktív, szenzorok kezelése nem engedélyezett.');
            return;
        }

        const idInput = document.getElementById('sensor-id');
        const codeSelect = document.getElementById('sensor-code');
        const typeSelect = document.getElementById('sensor-type');
        const unitSelect = document.getElementById('sensor-unit');
        const valueInput = document.getElementById('sensor-value');
        const lastSeenInput = document.getElementById('sensor-lastseen');

        const sensor = {
            id: idInput?.value || null,
            code: codeSelect?.value || null,
            type: typeSelect?.value || null,
            unit: unitSelect?.value || null,
            lastValue: valueInput?.value ? parseFloat(valueInput.value) : null,
            lastSeen: lastSeenInput?.value || new Date().toISOString()
        };

        try {
            const updatedGreenhouse = await apiPost(`/greenhouses/${encodeURIComponent(selectedGreenhouseCode)}/sensors`, sensor);
            if (updatedGreenhouse) {
                previousStateByCode.set(updatedGreenhouse.code ?? updatedGreenhouse.id ?? updatedGreenhouse._id, updatedGreenhouse);
                renderGreenhouseDetails(updatedGreenhouse);
            } else {
                await loadGreenhouses(false);
            }
            showNotification('Szenzor mentve');
            clearSensorForm();
        } catch (err) {
            console.error('Szenzor mentése sikertelen', err);
            showNotification('Nem sikerült menteni a szenzort: ' + (err.message || ''));
        }
    });
}

// Állapot a modalhoz
let actionLogPager = {
    page: 0,
    size: 20,
    totalPages: null, // ha backend adja, különben null
    greenhouseCode: null
};

// formázó (ha nincs már): használhatod a korábbi formatTimestamp függvényt
function formatTimestampShort(ts) {
    if (!ts) return '-';
    const d = new Date(ts);
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}

async function loadActionLogPage(greenhouseCode, page = 0, size = 20) {
    const modal = document.getElementById('modal-action-log');
    const listEl = document.getElementById('action-log-list');
    const pageInfo = document.getElementById('action-log-page-info');
    const prevBtn = document.getElementById('action-log-prev');
    const nextBtn = document.getElementById('action-log-next');

    if (!modal || !listEl || !pageInfo || !prevBtn || !nextBtn) return;

    listEl.innerHTML = '<p>Betöltés…</p>';
    modal.classList.remove('hidden');

    actionLogPager.page = page;
    actionLogPager.size = size;
    actionLogPager.greenhouseCode = greenhouseCode;

    try {
        // Ha a backend Page-t ad vissza (Page<ActionLog>), akkor a response JSON tartalmazhat content, totalPages, totalElements
        const resp = await fetch(`${API_BASE}/greenhouses/${encodeURIComponent(greenhouseCode)}/actions?page=${page}&size=${size}`);
        if (!resp.ok) throw new Error('Hálózati hiba: ' + resp.status);
        const data = await resp.json();

        // Támogatjuk két formátumot:
        // 1) Page objektum: { content: [...], totalPages: N, totalElements: M }
        // 2) Egyszerű tömb: [...]
        let logs = [];
        let totalPages = null;
        let totalElements = null;

        if (Array.isArray(data)) {
            logs = data;
            // ha nincs total, a kliens a visszakapott elemszám alapján dönt
            totalPages = null;
            totalElements = null;
        } else if (data && data.content) {
            logs = data.content;
            totalPages = Number.isFinite(data.totalPages) ? data.totalPages : null;
        } else {
            // ha a backend limit paramot használ (pl. /actions?limit=20&offset=40) és tömböt ad vissza
            logs = Array.isArray(data) ? data : [];
        }

        // render
        if (!logs || logs.length === 0) {
            listEl.innerHTML = '<p>Nincs naplóbejegyzés ehhez az üvegházhoz.</p>';
        } else {
            listEl.innerHTML = logs.map(l => `
                <div class="action-log-row">
                    <div class="action-log-time">${formatTimestampShort(l.timestamp)}</div>
                    <div class="action-log-action"><strong>${l.action}</strong></div>
                    <div class="action-log-reason">${l.reason ?? ''}</div>
                </div>
            `).join('');
        }

        // pager info és gombok állapota
        if (totalPages != null) {
            actionLogPager.totalPages = totalPages;
            pageInfo.textContent = `${actionLogPager.page + 1} / ${actionLogPager.totalPages}`;
            prevBtn.disabled = actionLogPager.page <= 0;
            nextBtn.disabled = actionLogPager.page >= (actionLogPager.totalPages - 1);
        } else {
            // nincs totalPages: a következő gomb akkor engedélyezett, ha a visszakapott elemszám == size
            pageInfo.textContent = `${actionLogPager.page + 1} / ?`;
            prevBtn.disabled = actionLogPager.page <= 0;
            nextBtn.disabled = !(logs.length === actionLogPager.size);
        }

    } catch (err) {
        console.error('Action log betöltési hiba', err);
        listEl.innerHTML = '<p>Hiba történt a napló betöltésekor.</p>';
    }
}

// Hívjuk egyszer, amikor a DOM készen van
// debug: modal init
function setupActionLogModalControls() {
    const modal = document.getElementById('modal-action-log');
    const closeBtn = document.getElementById('btn-close-action-log');
    const prevBtn = document.getElementById('action-log-prev');
    const nextBtn = document.getElementById('action-log-next');
    const sizeSelect = document.getElementById('action-log-page-size');

    if (!modal) return;
    modal.classList.add('hidden');

    if (closeBtn) {
        closeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            modal.classList.add('hidden');
            document.getElementById('action-log-list').innerHTML = '';
        });
    }

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.add('hidden');
            document.getElementById('action-log-list').innerHTML = '';
        }
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
            modal.classList.add('hidden');
            document.getElementById('action-log-list').innerHTML = '';
        }
    });

    if (prevBtn) {
        prevBtn.addEventListener('click', () => {
            if (actionLogPager.page > 0) {
                loadActionLogPage(actionLogPager.greenhouseCode, actionLogPager.page - 1, actionLogPager.size);
            }
        });
    }
    if (nextBtn) {
        nextBtn.addEventListener('click', () => {
            loadActionLogPage(actionLogPager.greenhouseCode, actionLogPager.page + 1, actionLogPager.size);
        });
    }
    if (sizeSelect) {
        sizeSelect.addEventListener('change', (e) => {
            const newSize = parseInt(e.target.value, 10) || 20;
            loadActionLogPage(actionLogPager.greenhouseCode, 0, newSize);
        });
    }
}

// Megnyitó függvény: csak akkor nyit, ha gombra kattintanak
async function openActionLogModal(greenhouseCode) {
    const modal = document.getElementById('modal-action-log');
    const listEl = document.getElementById('action-log-list');
    if (!modal || !listEl) return;

    // betöltés jelzés
    listEl.innerHTML = '<p>Betöltés…</p>';
    modal.classList.remove('hidden');
    modal.focus();

    try {
        const logs = await apiGet(`/greenhouses/${encodeURIComponent(greenhouseCode)}/actions?limit=200`);
        if (!logs || logs.length === 0) {
            listEl.innerHTML = '<p>Nincs naplóbejegyzés ehhez az üvegházhoz.</p>';
            return;
        }
        listEl.innerHTML = logs.map(l => `
            <div class="action-log-row">
                <div class="action-log-time">${formatTimestamp(l.timestamp)}</div>
                <div class="action-log-action"><strong>${l.action}</strong></div>
                <div class="action-log-reason">${l.reason ?? ''}</div>
            </div>
        `).join('');
    } catch (err) {
        console.error('Action log betöltési hiba', err);
        listEl.innerHTML = '<p>Hiba történt a napló betöltésekor.</p>';
    }
}

function closeActionLogModal() {
    const modal = document.getElementById('modal-action-log');
    if (!modal) return;
    modal.classList.add('hidden');
    document.body.classList.remove('modal-open');
    const listEl = document.getElementById('action-log-list');
    if (listEl) listEl.innerHTML = '';
}

// Gomb csatolása (hívjuk render után vagy DOMContentLoaded-ben)
function attachActionLogButton() {
    const btn = document.getElementById('btn-show-actions');
    if (!btn) return;
    btn.addEventListener('click', () => {
        if (!selectedGreenhouseCode) {
            alert('Előbb válassz ki egy üvegházat a listából!');
            return;
        }
        // alapértelmezett: oldal 0, size a select értéke
        const sizeSelect = document.getElementById('action-log-page-size');
        const size = sizeSelect ? parseInt(sizeSelect.value, 10) || 20 : 20;
        loadActionLogPage(selectedGreenhouseCode, 0, size);
    });
}

/* ---------- Inicializálás ---------- */

document.addEventListener('DOMContentLoaded', () => {
    loadGreenhouses(false);
    setupNewGreenHouseButton();
    setupManualControls();
    setupActiveToggle();
    setupSensorControls();
    setupActionLogModalControls();
    attachActionLogButton();
    setInterval(() => loadGreenhouses(true), 30000);
});