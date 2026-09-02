import { card } from "/report-card.js";

const grid = document.getElementById("grid");
const statusEl = document.getElementById("status");
const search = document.getElementById("search");

let all = [];

async function load() {
  try {
    const res = await fetch("/api/reports");
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    all = Array.isArray(data.reports) ? data.reports : [];
    render(all);
  } catch {
    statusEl.textContent = "Could not load reports.";
    statusEl.style.display = "";
  }
}

function render(list) {
  grid.replaceChildren();
  if (!list.length) {
    statusEl.textContent = all.length
      ? "No players match that search."
      : "No reports yet.";
    statusEl.style.display = "";
    return;
  }
  statusEl.style.display = "none";
  const frag = document.createDocumentFragment();
  for (const r of list) frag.appendChild(card(r));
  grid.appendChild(frag);
}

let debounce;
search.addEventListener("input", () => {
  clearTimeout(debounce);
  debounce = setTimeout(() => {
    const q = search.value.trim().toLowerCase();
    render(
      q ? all.filter((r) => (r.player || "").toLowerCase().includes(q)) : all,
    );
  }, 120);
});

load();
