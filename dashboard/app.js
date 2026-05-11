/**
 * Datos: si la pagina se sirve por HTTP desde la app, /api/rows actualiza en vivo.
 * window.TROQUELES_DATA (data.js) es respaldo offline y semilla hasta el primer fetch OK.
 *
 * Categorias admitidas en vista: Troqueles, Medidas, X, Y, Madera, Corte, Hendido, Goma.
 */
const fileInput = document.getElementById("csvFile");

const TABLE_TARGETS = [
  { id: "troquelesBody", category: "Troqueles" },
  { id: "medidasBody", category: "Medidas" },
  { id: "medidasXBody", category: "X" },
  { id: "medidasYBody", category: "Y" },
  { id: "maderaBody", category: "Madera" },
  { id: "corteBody", category: "Corte" },
  { id: "hendidoBody", category: "Hendido" },
  { id: "gomaBody", category: "Goma" },
];

const API_REFRESH_MS = 2500;
let apiIntervalId = null;

initializeDashboard();

fileInput.addEventListener("change", async (event) => {
  const file = event.target.files?.[0];
  if (!file) return;

  if (apiIntervalId !== null) {
    clearInterval(apiIntervalId);
    apiIntervalId = null;
  }
  const text = await file.text();
  const rows = normalizeRows(parseCsv(text));
  render(rows);
});

async function initializeDashboard() {
  const loadedFromApi = await refreshFromApi();
  if (loadedFromApi) return;
  if (Array.isArray(window.TROQUELES_DATA) && window.TROQUELES_DATA.length > 0) {
    render(normalizeRows(window.TROQUELES_DATA));
    return;
  }
  render([]);
}

async function refreshFromApi() {
  const protocol = window.location.protocol;
  const canUseApi = protocol === "http:" || protocol === "https:";
  if (!canUseApi) return false;

  try {
    const response = await fetch("/api/rows", { cache: "no-store" });
    if (!response.ok) return false;
    const data = await response.json();
    if (!Array.isArray(data)) return false;
    render(normalizeRows(data));
    if (apiIntervalId === null) {
      apiIntervalId = setInterval(() => {
        void refreshFromApi();
      }, API_REFRESH_MS);
    }
    return true;
  } catch (error) {
    return false;
  }
}

function parseCsv(text) {
  const lines = text.split(/\r?\n/).filter(Boolean);
  if (lines.length === 0) return [];

  const parsed = [];
  const first = parseLine(lines[0]).map((v) => v.toLowerCase());
  const startsWithHeader = first.includes("concepto") && first.includes("valor");
  const start = startsWithHeader ? 1 : 0;

  for (let i = start; i < lines.length; i++) {
    const cols = parseLine(lines[i]);
    if (cols.length < 3) continue;
    const value = Number(String(cols[1]).replace(",", "."));
    parsed.push({
      concepto: cols[0],
      valor: Number.isFinite(value) ? value : 0,
      categoria: cols[2] || "General",
    });
  }
  return parsed;
}

function normalizeRows(rows) {
  return rows.map((row) => {
    const value = Number(String(row.valor).replace(",", "."));
    return {
      concepto: row.concepto ?? "Sin concepto",
      valor: Number.isFinite(value) ? value : 0,
      categoria: row.categoria ?? "General",
    };
  });
}

function parseLine(line) {
  const result = [];
  let current = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (ch === "," && !inQuotes) {
      result.push(current);
      current = "";
    } else {
      current += ch;
    }
  }
  result.push(current);
  return result;
}

function matchesCategory(rowCategory, expected) {
  return String(rowCategory).trim().toLowerCase() === String(expected).trim().toLowerCase();
}

function filterByCategory(rows, category) {
  return rows.filter((row) => matchesCategory(row.categoria, category));
}

function render(rows) {
  const safeRows = Array.isArray(rows) ? rows : [];

  TABLE_TARGETS.forEach(({ id, category }) => {
    const tbody = document.getElementById(id);
    if (!tbody) return;
    tbody.innerHTML = "";
    const subset = filterByCategory(safeRows, category);
    if (subset.length === 0) {
      tbody.innerHTML = `<tr><td class="empty" colspan="4">Sin datos</td></tr>`;
      return;
    }
    subset.forEach((row, index) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td class="row-index">${index + 1}</td>
        <td>${escapeHtml(row.concepto)}</td>
        <td><span class="value-chip">${row.valor.toFixed(2)}</span></td>
        <td><span class="category-chip">${escapeHtml(row.categoria)}</span></td>
      `;
      tbody.appendChild(tr);
    });
  });
}

function escapeHtml(text) {
  return String(text)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
