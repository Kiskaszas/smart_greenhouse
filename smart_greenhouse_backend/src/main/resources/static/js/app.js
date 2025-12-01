
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

/* ---------- Üvegház lista ---------- */

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
        if (gh.code === selectedGreenhouseCode) {
            item.classList.add('active');
        }

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
        right.className = 'badge';
        const plantType = gh.plantType ?? gh.plantName ?? 'Ismeretlen';
        right.textContent = plantType;

        item.appendChild(left);
        item.appendChild(right);

        item.addEventListener('click', () => {
            selectedGreenhouseCode = gh.code;
            renderGreenhouseDetails(gh);
            // aktív jelölés frissítése
            document
                .querySelectorAll('.greenhouse-item')
                .forEach(el => el.classList.remove('active'));
            item.classList.add('active');
        });

        listEl.appendChild(item);

        // kis fade-in animáció
        item.classList.add('fade-in');
        setTimeout(() => item.classList.remove('fade-in'), 300);

        // highlight animáció frissüléskor
        detailsEl.classList.remove('highlight');
        // force reflow, hogy újrainduljon az animáció
        void detailsEl.offsetWidth;
        detailsEl.classList.add('highlight');
    });
}

/* ---------- Üvegház részletek ---------- */

function renderGreenhouseDetails(gh) {
    const titleEl = document.getElementById('details-title');
    const detailsEl = document.getElementById('greenhouse-details');

    if (!gh) {
        titleEl.textContent = 'Válassz egy üvegházat a listából…';
        detailsEl.innerHTML = '';
        return;
    }

    titleEl.textContent = gh.name ?? 'Üvegház részletek';

    // Itt azt írod ki, ami tényleg van a DTO-ban / entitásban
    detailsEl.innerHTML = `
        <p><strong>Kód:</strong> ${gh.code ?? '-'}</p>
        <p><strong>Növény:</strong> ${gh.plantType ?? gh.plantName ?? '-'}</p>
        <p><strong>Talajnedvesség:</strong> ${gh.soilMoisture ?? '-'} %</p>
        <p><strong>Levegő páratartalom:</strong> ${gh.airHumidity ?? '-'} %</p>
        <p><strong>Hőmérséklet:</strong> ${gh.airTemperature ?? '-'} °C</p>
        <p><strong>Öntözés:</strong> ${gh.irrigationActive ? 'Aktív' : 'Inaktív'}</p>
        <p><strong>Szellőztetés:</strong> ${gh.ventilationActive ? 'Aktív' : 'Inaktív'}</p>
        <p><strong>Utolsó frissítés:</strong> ${gh.lastUpdated ?? '-'}</p>
    `;
}

/* ---------- Változás-detektálás a notificationhöz ---------- */

function hasMeaningfulChange(oldGh, newGh) {
    if (!oldGh) return false;
    return FIELDS_TO_WATCH.some(key => oldGh[key] !== newGh[key]);
}

function greenhouseChangeSummary(oldGh, newGh) {
    const changes = [];

    FIELDS_TO_WATCH.forEach(key => {
        if (oldGh[key] !== newGh[key]) {
            changes.push(`${key}: ${oldGh[key] ?? '-'} → ${newGh[key] ?? '-'}`);
        }
    });

    return changes.join(', ');
}

/* ---------- Üvegházak betöltése és pollolás ---------- */

async function loadGreenhouses(showNotifications = false) {
    try {
        const greenhouses = await apiGet('/greenhouses');

        const demoBtn = document.getElementById('create-demo-btn');
        if (demoBtn) {
            if (greenhouses.length > 0) {
                // már van legalább 1 üvegház → demo gomb tűnjön el
                demoBtn.style.display = 'none';
            } else {
                // nincs üvegház → demo gomb látszódjon
                demoBtn.style.display = 'inline-block';
            }
        }

        renderGreenhouseList(greenhouses);

        greenhouses.forEach(gh => {
            const code = gh.code ?? gh.id ?? gh._id;
            if (!code) {
                return;
            }

            const old = previousStateByCode.get(code);

            if (showNotifications && hasMeaningfulChange(old, gh)) {
                const summary = greenhouseChangeSummary(old, gh);
                const name = gh.name ?? `Üvegház (${code})`;
                showNotification(`Frissült az üvegház állapota: ${name}`, summary);
            }

            previousStateByCode.set(code, gh);

            // Ha a kiválasztott üvegház frissült, jobb oldali panelt is update-eljük
            if (selectedGreenhouseCode && code === selectedGreenhouseCode) {
                renderGreenhouseDetails(gh);
            }
        });
    } catch (err) {
        console.error('Nem sikerült betölteni az üvegházakat', err);
    }
}

/* ---------- Demo üvegház gomb ---------- */

async function setupDemoButton() {
    const btn = document.getElementById('create-demo-btn');
    if (!btn) return;

    btn.addEventListener('click', async () => {
        try {
            // Ezt a payloadot igazítsd a saját Greenhouse DTO-dhoz
            const demoGreenhouse = {
                code: 'DEMO-1',
                name: 'Demo üvegház',
                plantType: 'Paradicsom',
                irrigationMode: 'AUTO',
                irrigationActive: false,
                ventilationActive: false,
                soilMoisture: 40,
                airHumidity: 60,
                airTemperature: 22
            };

            await apiPost('/greenhouses/demo', demoGreenhouse);

            showNotification('Demo üvegház létrehozva', 'A lista frissítésre került.');
            await loadGreenhouses(false);
        } catch (err) {
            console.error('Nem sikerült létrehozni a demo üvegházat', err);
            showNotification('Nem sikerült létrehozni a demo üvegházat.');
        }
    });
}

/* ---------- Init ---------- */

document.addEventListener('DOMContentLoaded', async () => {
    await setupDemoButton();
    await setupControlButtons();
    await setupCreateGreenhouseForm();

    // első betöltés – még nem dobunk értesítést
    await loadGreenhouses(false);

    // pollolás 10 másodpercenként – itt jönnek a notificationök
    setInterval(() => {
        loadGreenhouses(true);
    }, 10000);
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
            showNotification("Öntözés bekapcsolva");
            await loadGreenhouses(false);
        });
    }

    if (btnOff) {
        btnOff.addEventListener('click', async () => {
            if (!requireSelection()) return;
            await apiPost(`/greenhouses/${selectedGreenhouseCode}/irrigation/off`);
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
                showNotification("Üvegház törölve");
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
            name: name || `Üvegház ${code}`,
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