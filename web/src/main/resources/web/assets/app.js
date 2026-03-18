const state = {
  token: "",
  refreshMs: 5000,
  activePage: "overview",
  locale: normalizeLocale(localStorage.getItem("serverscopeLocale") || navigator.language || "en")
};

const i18n = {
  en: {
    brandEyebrow: "ServerScope MVP",
    brandTitle: "Diagnostics",
    brandSubtitle: "Lightweight observability for Paper and Folia servers",
    navOverview: "Overview",
    navMetrics: "Metrics",
    navWorlds: "Worlds and Chunks",
    navProfiling: "Profiling",
    navFindings: "Findings and Alerts",
    pageOverviewEyebrow: "Overview",
    pageOverviewTitle: "Server overview",
    pageMetricsEyebrow: "Metrics",
    pageMetricsTitle: "Realtime metrics",
    pageWorldsEyebrow: "Worlds and Chunks",
    pageWorldsTitle: "World and chunk diagnostics",
    pageProfilingEyebrow: "Profiling",
    pageProfilingTitle: "Plugin and event profiling",
    pageFindingsEyebrow: "Findings and Alerts",
    pageFindingsTitle: "Diagnostics and notifications",
    statusWaiting: "Waiting",
    statusRefreshing: "Refreshing",
    statusConnected: "Connected",
    statusError: "Error",
    never: "Never",
    polling: "{seconds}s polling",
    globalIdle: "Using embedded ServerScope API",
    globalConnected: "Serving data from the embedded ServerScope API",
    tokenLabel: "API token",
    tokenPlaceholder: "Enter token",
    save: "Save",
    clear: "Clear",
    localeLabel: "Language",
    statusLabel: "Status",
    refreshLabel: "Refresh",
    lastUpdateLabel: "Last update",
    overviewHealthTitle: "Server Health",
    overviewHealthSubtitle: "Current point-in-time snapshot",
    overviewIssuesTitle: "Active Issues",
    overviewIssuesSubtitle: "Most important current signals",
    overviewSlowTitle: "Top Slow Events",
    overviewSlowSubtitle: "Dispatch cost hotspots",
    overviewBurstTitle: "Suspicious Bursts",
    overviewBurstSubtitle: "Fast-rising event pressure",
    metricsPanelTitle: "Server Metrics",
    metricsPanelSubtitle: "Latest top-level metrics",
    collectorFeedTitle: "Collector Feed",
    collectorFeedSubtitle: "Raw latest metric items",
    worldsPanelTitle: "World Loaded Chunks",
    worldsPanelSubtitle: "World-level chunk footprint",
    chunksPanelTitle: "Chunk Hotspots",
    chunksPanelSubtitle: "Highest-cost sampled chunks",
    profilingPluginsTitle: "Plugin Impact",
    profilingPluginsSubtitle: "Approximate attributed event cost",
    profilingFrequentTitle: "Frequent Events",
    profilingFrequentSubtitle: "Most active profiled events",
    profilingSlowTitle: "Slow Events",
    profilingSlowSubtitle: "Highest average dispatch cost",
    profilingBurstTitle: "Event Bursts",
    profilingBurstSubtitle: "Peak short-window pressure",
    findingsPanelTitle: "Diagnostics",
    findingsPanelSubtitle: "Rule-based findings for administrators",
    alertsPanelTitle: "Alerts",
    alertsPanelSubtitle: "Channel-aware notifications and history",
    noData: "No data",
    noIssues: "No active issues",
    metric: "Metric",
    value: "Value",
    updated: "Updated",
    collector: "Collector",
    labels: "Labels",
    world: "World",
    loadedChunks: "Loaded Chunks",
    chunk: "Chunk",
    entities: "Entities",
    players: "Players",
    heap: "Heap",
    blockEntities: "Block Entities",
    event: "Event",
    class: "Class",
    avgMs: "Avg ms",
    maxMs: "Max ms",
    count: "Count",
    plugins: "Plugins",
    plugin: "Plugin",
    events: "Events",
    totalMs: "Total ms",
    peakWindow: "Peak Window",
    burstScore: "Burst Score",
    severity: "Severity",
    finding: "Finding",
    probableCause: "Probable Cause",
    suggestedAction: "Suggested Action",
    code: "Code",
    status: "Status",
    message: "Message",
    sevCritical: "CRITICAL",
    sevWarn: "WARN",
    sevInfo: "INFO",
    stateActive: "ACTIVE",
    stateResolved: "RESOLVED"
  },
  ru: {
    brandEyebrow: "ServerScope MVP",
    brandTitle: "Диагностика",
    brandSubtitle: "Лёгкая observability-панель для серверов Paper и Folia",
    navOverview: "Обзор",
    navMetrics: "Метрики",
    navWorlds: "Миры и чанки",
    navProfiling: "Профилирование",
    navFindings: "Диагностика и алерты",
    pageOverviewEyebrow: "Обзор",
    pageOverviewTitle: "Состояние сервера",
    pageMetricsEyebrow: "Метрики",
    pageMetricsTitle: "Метрики в реальном времени",
    pageWorldsEyebrow: "Миры и чанки",
    pageWorldsTitle: "Диагностика миров и чанков",
    pageProfilingEyebrow: "Профилирование",
    pageProfilingTitle: "Плагины и события",
    pageFindingsEyebrow: "Диагностика и алерты",
    pageFindingsTitle: "Диагностика и уведомления",
    statusWaiting: "Ожидание",
    statusRefreshing: "Обновление",
    statusConnected: "Подключено",
    statusError: "Ошибка",
    never: "Никогда",
    polling: "опрос каждые {seconds}с",
    globalIdle: "Используется встроенный API ServerScope",
    globalConnected: "Данные поступают из встроенного API ServerScope",
    tokenLabel: "API-токен",
    tokenPlaceholder: "Введите токен",
    save: "Сохранить",
    clear: "Очистить",
    localeLabel: "Язык",
    statusLabel: "Статус",
    refreshLabel: "Обновление",
    lastUpdateLabel: "Последнее обновление",
    overviewHealthTitle: "Состояние сервера",
    overviewHealthSubtitle: "Текущий снимок состояния",
    overviewIssuesTitle: "Активные проблемы",
    overviewIssuesSubtitle: "Самые важные текущие сигналы",
    overviewSlowTitle: "Самые медленные события",
    overviewSlowSubtitle: "Горячие точки по стоимости обработки",
    overviewBurstTitle: "Подозрительные всплески",
    overviewBurstSubtitle: "Быстро растущее давление событий",
    metricsPanelTitle: "Метрики сервера",
    metricsPanelSubtitle: "Последние ключевые показатели",
    collectorFeedTitle: "Лента коллекторов",
    collectorFeedSubtitle: "Последние сырые элементы метрик",
    worldsPanelTitle: "Загруженные чанки по мирам",
    worldsPanelSubtitle: "Нагрузка чанков по каждому миру",
    chunksPanelTitle: "Проблемные чанки",
    chunksPanelSubtitle: "Самые тяжёлые зафиксированные чанки",
    profilingPluginsTitle: "Влияние плагинов",
    profilingPluginsSubtitle: "Примерная стоимость событий по плагинам",
    profilingFrequentTitle: "Частые события",
    profilingFrequentSubtitle: "Самые активные профилируемые события",
    profilingSlowTitle: "Медленные события",
    profilingSlowSubtitle: "Наибольшая средняя стоимость обработки",
    profilingBurstTitle: "Всплески событий",
    profilingBurstSubtitle: "Пиковая нагрузка в коротком окне",
    findingsPanelTitle: "Диагностика",
    findingsPanelSubtitle: "Rule-based выводы для администраторов",
    alertsPanelTitle: "Алерты",
    alertsPanelSubtitle: "История и уведомления по каналам",
    noData: "Нет данных",
    noIssues: "Активных проблем нет",
    metric: "Метрика",
    value: "Значение",
    updated: "Обновлено",
    collector: "Коллектор",
    labels: "Метки",
    world: "Мир",
    loadedChunks: "Загруженные чанки",
    chunk: "Чанк",
    entities: "Сущности",
    players: "Игроки",
    heap: "Память JVM",
    blockEntities: "Блочные сущности",
    event: "Событие",
    class: "Класс",
    avgMs: "Средн. мс",
    maxMs: "Макс. мс",
    count: "Количество",
    plugins: "Плагины",
    plugin: "Плагин",
    events: "События",
    totalMs: "Всего мс",
    peakWindow: "Пиковое окно",
    burstScore: "Burst score",
    severity: "Серьёзность",
    finding: "Находка",
    probableCause: "Вероятная причина",
    suggestedAction: "Рекомендуемое действие",
    code: "Код",
    status: "Статус",
    message: "Сообщение",
    sevCritical: "КРИТИЧНО",
    sevWarn: "ПРЕДУПРЕЖДЕНИЕ",
    sevInfo: "ИНФО",
    stateActive: "АКТИВНО",
    stateResolved: "УСТРАНЕНО"
  },
  uk: {
    brandEyebrow: "ServerScope MVP",
    brandTitle: "Діагностика",
    brandSubtitle: "Легка observability-панель для серверів Paper і Folia",
    navOverview: "Огляд",
    navMetrics: "Метрики",
    navWorlds: "Світи й чанки",
    navProfiling: "Профілювання",
    navFindings: "Діагностика й алерти",
    pageOverviewEyebrow: "Огляд",
    pageOverviewTitle: "Стан сервера",
    pageMetricsEyebrow: "Метрики",
    pageMetricsTitle: "Метрики в реальному часі",
    pageWorldsEyebrow: "Світи й чанки",
    pageWorldsTitle: "Діагностика світів і чанків",
    pageProfilingEyebrow: "Профілювання",
    pageProfilingTitle: "Плагіни та події",
    pageFindingsEyebrow: "Діагностика й алерти",
    pageFindingsTitle: "Діагностика та сповіщення",
    statusWaiting: "Очікування",
    statusRefreshing: "Оновлення",
    statusConnected: "Підключено",
    statusError: "Помилка",
    never: "Ніколи",
    polling: "опитування кожні {seconds}с",
    globalIdle: "Використовується вбудований API ServerScope",
    globalConnected: "Дані надходять із вбудованого API ServerScope",
    tokenLabel: "API-токен",
    tokenPlaceholder: "Введіть токен",
    save: "Зберегти",
    clear: "Очистити",
    localeLabel: "Мова",
    statusLabel: "Статус",
    refreshLabel: "Оновлення",
    lastUpdateLabel: "Останнє оновлення",
    overviewHealthTitle: "Стан сервера",
    overviewHealthSubtitle: "Поточний зріз стану",
    overviewIssuesTitle: "Активні проблеми",
    overviewIssuesSubtitle: "Найважливіші поточні сигнали",
    overviewSlowTitle: "Найповільніші події",
    overviewSlowSubtitle: "Гарячі точки за вартістю обробки",
    overviewBurstTitle: "Підозрілі сплески",
    overviewBurstSubtitle: "Швидко зростаючий тиск подій",
    metricsPanelTitle: "Метрики сервера",
    metricsPanelSubtitle: "Останні ключові показники",
    collectorFeedTitle: "Стрічка колекторів",
    collectorFeedSubtitle: "Останні сирі елементи метрик",
    worldsPanelTitle: "Завантажені чанки по світах",
    worldsPanelSubtitle: "Навантаження чанків у кожному світі",
    chunksPanelTitle: "Проблемні чанки",
    chunksPanelSubtitle: "Найдорожчі зафіксовані чанки",
    profilingPluginsTitle: "Вплив плагінів",
    profilingPluginsSubtitle: "Приблизна вартість подій за плагінами",
    profilingFrequentTitle: "Часті події",
    profilingFrequentSubtitle: "Найактивніші профільовані події",
    profilingSlowTitle: "Повільні події",
    profilingSlowSubtitle: "Найвища середня вартість обробки",
    profilingBurstTitle: "Сплески подій",
    profilingBurstSubtitle: "Пікове навантаження в короткому вікні",
    findingsPanelTitle: "Діагностика",
    findingsPanelSubtitle: "Rule-based висновки для адміністраторів",
    alertsPanelTitle: "Алерти",
    alertsPanelSubtitle: "Історія та сповіщення по каналах",
    noData: "Немає даних",
    noIssues: "Активних проблем немає",
    metric: "Метрика",
    value: "Значення",
    updated: "Оновлено",
    collector: "Колектор",
    labels: "Мітки",
    world: "Світ",
    loadedChunks: "Завантажені чанки",
    chunk: "Чанк",
    entities: "Сутності",
    players: "Гравці",
    heap: "Пам'ять JVM",
    blockEntities: "Блокові сутності",
    event: "Подія",
    class: "Клас",
    avgMs: "Сер. мс",
    maxMs: "Макс. мс",
    count: "Кількість",
    plugins: "Плагіни",
    plugin: "Плагін",
    events: "Події",
    totalMs: "Усього мс",
    peakWindow: "Пікове вікно",
    burstScore: "Burst score",
    severity: "Серйозність",
    finding: "Знахідка",
    probableCause: "Ймовірна причина",
    suggestedAction: "Рекомендована дія",
    code: "Код",
    status: "Статус",
    message: "Повідомлення",
    sevCritical: "КРИТИЧНО",
    sevWarn: "ПОПЕРЕДЖЕННЯ",
    sevInfo: "ІНФО",
    stateActive: "АКТИВНО",
    stateResolved: "ВИРІШЕНО"
  }
};

const pageKeys = {
  overview: ["pageOverviewEyebrow", "pageOverviewTitle"],
  metrics: ["pageMetricsEyebrow", "pageMetricsTitle"],
  worlds: ["pageWorldsEyebrow", "pageWorldsTitle"],
  profiling: ["pageProfilingEyebrow", "pageProfilingTitle"],
  findings: ["pageFindingsEyebrow", "pageFindingsTitle"]
};

const tokenInput = document.getElementById("tokenInput");
const saveTokenButton = document.getElementById("saveTokenButton");
const clearTokenButton = document.getElementById("clearTokenButton");
const localeSelect = document.getElementById("localeSelect");
const connectionState = document.getElementById("connectionState");
const refreshState = document.getElementById("refreshState");
const lastUpdated = document.getElementById("lastUpdated");
const pageEyebrow = document.getElementById("pageEyebrow");
const pageTitle = document.getElementById("pageTitle");
const globalMessage = document.getElementById("globalMessage");

tokenInput.value = state.token;
localeSelect.value = state.locale;

function normalizeLocale(value) {
  const normalized = String(value || "en").toLowerCase();
  if (normalized.startsWith("ru")) return "ru";
  if (normalized.startsWith("uk") || normalized.startsWith("ua")) return "uk";
  return "en";
}

function t(key, args = {}) {
  const bundle = i18n[state.locale] || i18n.en;
  const template = bundle[key] || i18n.en[key] || key;
  return template.replace(/\{([a-zA-Z0-9_.-]+)\}/g, (_, token) => args[token] ?? `{${token}}`);
}

function translateSeverity(severity) {
  const normalized = String(severity || "INFO").toUpperCase();
  if (normalized === "CRITICAL") return t("sevCritical");
  if (normalized === "WARN") return t("sevWarn");
  return t("sevInfo");
}

function translateStatus(status) {
  return String(status || "ACTIVE").toUpperCase() === "RESOLVED" ? t("stateResolved") : t("stateActive");
}

function applyStaticText() {
  document.documentElement.lang = state.locale;
  document.getElementById("brandEyebrow").textContent = t("brandEyebrow");
  document.getElementById("brandTitle").textContent = t("brandTitle");
  document.getElementById("brandSubtitle").textContent = t("brandSubtitle");
  document.getElementById("tokenLabel").textContent = t("tokenLabel");
  tokenInput.placeholder = t("tokenPlaceholder");
  saveTokenButton.textContent = t("save");
  clearTokenButton.textContent = t("clear");
  document.getElementById("localeLabel").textContent = t("localeLabel");
  document.getElementById("statusLabel").textContent = t("statusLabel");
  document.getElementById("refreshLabel").textContent = t("refreshLabel");
  document.getElementById("lastUpdateLabel").textContent = t("lastUpdateLabel");
  document.getElementById("overviewHealthTitle").textContent = t("overviewHealthTitle");
  document.getElementById("overviewHealthSubtitle").textContent = t("overviewHealthSubtitle");
  document.getElementById("overviewIssuesTitle").textContent = t("overviewIssuesTitle");
  document.getElementById("overviewIssuesSubtitle").textContent = t("overviewIssuesSubtitle");
  document.getElementById("overviewSlowTitle").textContent = t("overviewSlowTitle");
  document.getElementById("overviewSlowSubtitle").textContent = t("overviewSlowSubtitle");
  document.getElementById("overviewBurstTitle").textContent = t("overviewBurstTitle");
  document.getElementById("overviewBurstSubtitle").textContent = t("overviewBurstSubtitle");
  document.getElementById("metricsTitle").textContent = t("metricsPanelTitle");
  document.getElementById("metricsSubtitle").textContent = t("metricsPanelSubtitle");
  document.getElementById("collectorFeedTitle").textContent = t("collectorFeedTitle");
  document.getElementById("collectorFeedSubtitle").textContent = t("collectorFeedSubtitle");
  document.getElementById("worldsTitle").textContent = t("worldsPanelTitle");
  document.getElementById("worldsSubtitle").textContent = t("worldsPanelSubtitle");
  document.getElementById("chunksTitle").textContent = t("chunksPanelTitle");
  document.getElementById("chunksSubtitle").textContent = t("chunksPanelSubtitle");
  document.getElementById("profilingPluginsTitle").textContent = t("profilingPluginsTitle");
  document.getElementById("profilingPluginsSubtitle").textContent = t("profilingPluginsSubtitle");
  document.getElementById("profilingFrequentTitle").textContent = t("profilingFrequentTitle");
  document.getElementById("profilingFrequentSubtitle").textContent = t("profilingFrequentSubtitle");
  document.getElementById("profilingSlowTitle").textContent = t("profilingSlowTitle");
  document.getElementById("profilingSlowSubtitle").textContent = t("profilingSlowSubtitle");
  document.getElementById("profilingBurstTitle").textContent = t("profilingBurstTitle");
  document.getElementById("profilingBurstSubtitle").textContent = t("profilingBurstSubtitle");
  document.getElementById("findingsTitle").textContent = t("findingsPanelTitle");
  document.getElementById("findingsSubtitle").textContent = t("findingsPanelSubtitle");
  document.getElementById("alertsTitle").textContent = t("alertsPanelTitle");
  document.getElementById("alertsSubtitle").textContent = t("alertsPanelSubtitle");
  document.querySelector('.nav-link[data-page="overview"]').textContent = t("navOverview");
  document.querySelector('.nav-link[data-page="metrics"]').textContent = t("navMetrics");
  document.querySelector('.nav-link[data-page="worlds"]').textContent = t("navWorlds");
  document.querySelector('.nav-link[data-page="profiling"]').textContent = t("navProfiling");
  document.querySelector('.nav-link[data-page="findings"]').textContent = t("navFindings");
  refreshState.textContent = t("polling", { seconds: Math.round(state.refreshMs / 1000) });
  connectionState.textContent = t("statusWaiting");
  lastUpdated.textContent = t("never");
  globalMessage.textContent = t("globalIdle");
}

function renderPageState() {
  document.querySelectorAll(".nav-link").forEach((button) => button.classList.toggle("active", button.dataset.page === state.activePage));
  document.querySelectorAll(".page").forEach((page) => page.classList.toggle("active", page.dataset.page === state.activePage));
  const [eyebrowKey, titleKey] = pageKeys[state.activePage] || pageKeys.overview;
  pageEyebrow.textContent = t(eyebrowKey);
  pageTitle.textContent = t(titleKey);
}

async function api(path) {
  const headers = { "Accept-Language": state.locale };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, { headers });
  const body = await response.json();
  if (!response.ok || !body.ok) throw new Error(body.error?.message || `Request failed: ${response.status}`);
  return body.data;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function renderCellContent(content, allowHtml = false) {
  if (content == null) return "";
  return allowHtml ? String(content) : escapeHtml(content);
}

function renderTable(containerId, columns, rows) {
  const container = document.getElementById(containerId);
  if (!rows.length) {
    container.innerHTML = `<div class="empty">${t("noData")}</div>`;
    return;
  }
  const header = columns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join("");
  const body = rows.map((row) => `<tr>${columns.map((column) => {
    const content = column.render(row);
    return `<td>${renderCellContent(content, column.allowHtml === true)}</td>`;
  }).join("")}</tr>`).join("");
  container.innerHTML = `<table><thead><tr>${header}</tr></thead><tbody>${body}</tbody></table>`;
}

function metricValue(metric) {
  if (!metric) return "n/a";
  const numeric = Number(metric.value);
  if (Number.isNaN(numeric)) return "n/a";
  return numeric % 1 === 0 ? numeric.toString() : numeric.toFixed(2);
}

function bytes(value) {
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return "n/a";
  const units = ["B", "KB", "MB", "GB"];
  let current = numeric;
  let unitIndex = 0;
  while (current >= 1024 && unitIndex < units.length - 1) {
    current /= 1024;
    unitIndex += 1;
  }
  return `${current.toFixed(1)} ${units[unitIndex]}`;
}

function severityPill(severity) {
  const level = String(severity || "INFO").toLowerCase();
  return `<span class="pill ${level}">${translateSeverity(severity)}</span>`;
}

function shortClass(name) {
  if (!name) return "-";
  const parts = String(name).split(".");
  return parts[parts.length - 1] || name;
}

function formatGauge(value, max) {
  const numeric = Number(value);
  const safeMax = Math.max(Number(max) || 1, 1);
  if (Number.isNaN(numeric)) return 0;
  return Math.max(4, Math.min(100, Math.round((numeric / safeMax) * 100)));
}

function invertMetric(value, max) {
  const numeric = Number(value);
  const safeMax = Math.max(Number(max) || 1, 1);
  if (Number.isNaN(numeric)) return 0;
  return Math.max(0, safeMax - numeric);
}

function formatLocalTime(value) {
  return value ? new Date(value).toLocaleTimeString(state.locale) : "-";
}

function renderCards(metrics) {
  const cards = [
    ["TPS", metricValue(metrics.SERVER_TPS), formatGauge(metrics.SERVER_TPS?.value, 20)],
    ["MSPT", metricValue(metrics.SERVER_MSPT), formatGauge(invertMetric(metrics.SERVER_MSPT?.value, 100), 100)],
    [t("entities"), metricValue(metrics.ENTITY_COUNT), formatGauge(metrics.ENTITY_COUNT?.value, 4000)],
    [t("loadedChunks"), metricValue(metrics.LOADED_CHUNKS), formatGauge(metrics.LOADED_CHUNKS?.value, 1000)],
    [t("players"), metricValue(metrics.PLAYERS_ONLINE), formatGauge(metrics.PLAYERS_ONLINE?.value, 100)],
    [t("heap"), bytes(metricValue(metrics.JVM_HEAP_USED_BYTES)), formatGauge(metrics.JVM_HEAP_USED_BYTES?.value, metrics.JVM_HEAP_MAX_BYTES?.value || 1)]
  ];
  document.getElementById("overviewCards").innerHTML = cards.map(([label, value, fill]) => `
    <article class="card">
      <div class="card-label">${escapeHtml(label)}</div>
      <div class="card-value">${escapeHtml(value)}</div>
      <div class="chart-track" aria-hidden="true"><div class="chart-fill" style="width:${fill}%"></div></div>
    </article>`).join("");
}

function renderMetricBars(containerId, items, formatter) {
  const container = document.getElementById(containerId);
  if (!items.length) {
    container.innerHTML = `<div class="empty">${t("noData")}</div>`;
    return;
  }
  const max = Math.max(...items.map((item) => Number(item.value)), 1);
  container.innerHTML = items.map((item) => {
    const fill = Math.max(4, Math.round((Number(item.value) / max) * 100));
    return `<div class="chart-card"><div class="chart-label"><span>${escapeHtml(item.label)}</span><span>${escapeHtml(formatter(item.value))}</span></div><div class="chart-track"><div class="chart-fill" style="width:${fill}%"></div></div></div>`;
  }).join("");
}

function renderIssueList(containerId, findings, alerts) {
  const container = document.getElementById(containerId);
  const merged = [
    ...findings.slice(0, 3).map((item) => ({ severity: item.severity, title: item.title, text: item.description })),
    ...alerts.slice(0, 2).map((item) => ({ severity: item.severity, title: `${item.code} (${translateStatus(item.status)})`, text: item.message }))
  ];
  if (!merged.length) {
    container.innerHTML = `<div class="empty">${t("noIssues")}</div>`;
    return;
  }
  container.innerHTML = `<div class="issue-list">${merged.map((item) => `<article class="issue-item"><div class="issue-head"><strong>${escapeHtml(item.title)}</strong>${severityPill(item.severity)}</div><p>${escapeHtml(item.text)}</p></article>`).join("")}</div>`;
}

async function refreshOverview() {
  const overview = await api("/api/overview?limit=8");
  renderCards(overview.serverMetrics || {});
  renderMetricBars("overviewHealthBars", [
    { label: "TPS", value: overview.serverMetrics?.SERVER_TPS?.value || 0 },
    { label: "MSPT", value: overview.serverMetrics?.SERVER_MSPT?.value || 0 },
    { label: t("entities"), value: overview.serverMetrics?.ENTITY_COUNT?.value || 0 },
    { label: t("loadedChunks"), value: overview.serverMetrics?.LOADED_CHUNKS?.value || 0 }
  ], (value) => Number(value).toFixed(2));
  renderIssueList("overviewIssues", overview.activeFindings || [], overview.activeAlerts || []);
  renderTable("overviewSlowEvents", [
    { label: t("event"), render: (row) => row.eventId },
    { label: t("class"), render: (row) => shortClass(row.eventClassName) },
    { label: t("avgMs"), render: (row) => (row.averageTimeNanos / 1_000_000).toFixed(2) },
    { label: t("count"), render: (row) => row.count }
  ], overview.topSlowEvents || []);
  renderTable("overviewBurstEvents", [
    { label: t("event"), render: (row) => row.eventId },
    { label: t("peakWindow"), render: (row) => `${row.maxWindowCount}/${Math.max(1, Math.round(row.burstWindowMillis / 1000))}s` },
    { label: t("burstScore"), render: (row) => Number(row.burstScore).toFixed(2) },
    { label: t("plugins"), render: (row) => row.participatingPlugins.join(", ") || "-" }
  ], overview.topSuspiciousBursts || []);
}

async function refreshMetrics() {
  const [overview, metrics] = await Promise.all([api("/api/overview"), api("/api/metrics?limit=80")]);
  const serverMetrics = overview.serverMetrics || {};
  renderMetricBars("metricsBars", [
    { label: "TPS", value: serverMetrics.SERVER_TPS?.value || 0 },
    { label: "MSPT", value: serverMetrics.SERVER_MSPT?.value || 0 },
    { label: t("entities"), value: serverMetrics.ENTITY_COUNT?.value || 0 },
    { label: t("loadedChunks"), value: serverMetrics.LOADED_CHUNKS?.value || 0 }
  ], (value) => Number(value).toFixed(2));
  const metricRows = Object.entries(serverMetrics).map(([metricType, item]) => ({ metricType, value: item.value, timestamp: item.timestamp }));
  renderTable("metricsTable", [
    { label: t("metric"), render: (row) => row.metricType },
    { label: t("value"), render: (row) => Number(row.value).toFixed(2) },
    { label: t("updated"), render: (row) => formatLocalTime(row.timestamp) }
  ], metricRows);
  renderTable("metricFeedTable", [
    { label: t("collector"), render: (row) => row.collectorId },
    { label: t("metric"), render: (row) => row.metricType },
    { label: t("value"), render: (row) => Number(row.value).toFixed(2) },
    { label: t("labels"), render: (row) => formatLabels(row.labels) }
  ], metrics);
}

async function refreshWorlds() {
  const [worlds, chunks] = await Promise.all([api("/api/worlds?limit=20"), api("/api/chunks?limit=20")]);
  renderMetricBars("worldsBars", worlds.map((row) => ({ label: row.world, value: row.loadedChunks })), (value) => value);
  renderTable("worldsTable", [
    { label: t("world"), render: (row) => row.world },
    { label: t("loadedChunks"), render: (row) => row.loadedChunks },
    { label: t("updated"), render: (row) => formatLocalTime(row.timestamp) }
  ], worlds);
  renderMetricBars("chunksHeat", chunks.map((row) => ({
    label: `${row.world} ${row.chunkX},${row.chunkZ}`,
    value: Number(row.entityCount || 0) * 10 + Number(row.blockEntityCount || 0) * 25
  })), (value) => Math.round(value));
  renderTable("chunksTable", [
    { label: t("world"), render: (row) => row.world },
    { label: t("chunk"), render: (row) => `<span class="mono">${escapeHtml(`${row.chunkX}, ${row.chunkZ}`)}</span>`, allowHtml: true },
    { label: t("entities"), render: (row) => row.entityCount ?? "-" },
    { label: t("blockEntities"), render: (row) => row.blockEntityCount ?? "-" },
    { label: t("updated"), render: (row) => formatLocalTime(row.timestamp) }
  ], chunks);
}

async function refreshProfiling() {
  const profile = await api("/api/profiling?limit=12");
  renderMetricBars("profilingPluginsBars", (profile.topPlugins || []).map((row) => ({ label: row.pluginName, value: row.attributedTotalTimeNanos / 1_000_000 })), (value) => `${Number(value).toFixed(1)} ms`);
  renderTable("profilingPluginsTable", [
    { label: t("plugin"), render: (row) => row.pluginName },
    { label: t("events"), render: (row) => row.eventCount },
    { label: t("avgMs"), render: (row) => (row.averageAttributedTimeNanos / 1_000_000).toFixed(2) },
    { label: t("totalMs"), render: (row) => (row.attributedTotalTimeNanos / 1_000_000).toFixed(2) }
  ], profile.topPlugins || []);
  renderTable("profilingFrequentTable", [
    { label: t("event"), render: (row) => row.eventId },
    { label: t("class"), render: (row) => shortClass(row.eventClassName) },
    { label: t("count"), render: (row) => row.count },
    { label: t("plugins"), render: (row) => row.participatingPlugins.join(", ") || "-" }
  ], profile.topFrequentEvents || []);
  renderTable("profilingSlowTable", [
    { label: t("event"), render: (row) => row.eventId },
    { label: t("avgMs"), render: (row) => (row.averageTimeNanos / 1_000_000).toFixed(2) },
    { label: t("maxMs"), render: (row) => (row.maxTimeNanos / 1_000_000).toFixed(2) },
    { label: t("count"), render: (row) => row.count }
  ], profile.topSlowEvents || []);
  renderTable("profilingBurstTable", [
    { label: t("event"), render: (row) => row.eventId },
    { label: t("peakWindow"), render: (row) => `${row.maxWindowCount}/${Math.max(1, Math.round(row.burstWindowMillis / 1000))}s` },
    { label: t("burstScore"), render: (row) => Number(row.burstScore).toFixed(2) },
    { label: t("plugins"), render: (row) => row.participatingPlugins.join(", ") || "-" }
  ], profile.topSuspiciousBursts || []);
}

async function refreshFindings() {
  const [findings, alerts] = await Promise.all([api("/api/findings?limit=20"), api("/api/alerts?limit=20")]);
  renderTable("findingsTable", [
    { label: t("severity"), render: (row) => severityPill(row.severity), allowHtml: true },
    { label: t("finding"), render: (row) => row.title },
    { label: t("probableCause"), render: (row) => row.probableCause },
    { label: t("suggestedAction"), render: (row) => row.suggestedAction }
  ], findings);
  renderTable("alertsTable", [
    { label: t("severity"), render: (row) => severityPill(row.severity), allowHtml: true },
    { label: t("code"), render: (row) => row.code },
    { label: t("status"), render: (row) => translateStatus(row.status) },
    { label: t("message"), render: (row) => row.message }
  ], alerts);
}

function formatLabels(labels) {
  const entries = Object.entries(labels || {});
  if (!entries.length) return "-";
  return entries.map(([key, value]) => `${key}=${value}`).join(", ");
}

async function refreshCurrentPage() {
  try {
    connectionState.textContent = t("statusRefreshing");
    connectionState.className = "status-pill idle";
    if (state.activePage === "overview") {
      await refreshOverview();
    } else if (state.activePage === "metrics") {
      await refreshMetrics();
    } else if (state.activePage === "worlds") {
      await refreshWorlds();
    } else if (state.activePage === "profiling") {
      await refreshProfiling();
    } else if (state.activePage === "findings") {
      await refreshFindings();
    }
    connectionState.textContent = t("statusConnected");
    connectionState.className = "status-pill ok";
    lastUpdated.textContent = new Date().toLocaleTimeString(state.locale);
    globalMessage.textContent = t("globalConnected");
  } catch (error) {
    connectionState.textContent = t("statusError");
    connectionState.className = "status-pill error";
    globalMessage.textContent = error.message;
  }
}

saveTokenButton.addEventListener("click", () => {
  state.token = tokenInput.value.trim();
  refreshCurrentPage();
});

clearTokenButton.addEventListener("click", () => {
  state.token = "";
  tokenInput.value = "";
  refreshCurrentPage();
});

localeSelect.addEventListener("change", () => {
  state.locale = normalizeLocale(localeSelect.value);
  localStorage.setItem("serverscopeLocale", state.locale);
  applyStaticText();
  renderPageState();
  refreshCurrentPage();
});

document.querySelectorAll(".nav-link").forEach((button) => {
  button.addEventListener("click", () => {
    state.activePage = button.dataset.page;
    renderPageState();
    refreshCurrentPage();
  });
});

applyStaticText();
renderPageState();
refreshCurrentPage();
setInterval(refreshCurrentPage, state.refreshMs);
