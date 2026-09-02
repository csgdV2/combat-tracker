import { card, avatar, localTime } from "/report-card.js";

const gate = document.getElementById("gate");
const gateForm = document.getElementById("gateForm");
const gateErr = document.getElementById("gateErr");
const gateSubmit = document.getElementById("gateSubmit");
const password = document.getElementById("password");

const panel = document.getElementById("panel");
const cardsEl = document.getElementById("cards");
const statusEl = document.getElementById("status");
const search = document.getElementById("search");
const selectAll = document.getElementById("selectAll");
const clearSel = document.getElementById("clearSel");
const bulkDelete = document.getElementById("bulkDelete");
const selCount = document.getElementById("selCount");
const logoutBtn = document.getElementById("logout");

const dlg = document.getElementById("confirmDlg");
const dlgForm = document.getElementById("dlgForm");
const dlgTitle = document.getElementById("dlgTitle");
const dlgWho = document.getElementById("dlgWho");
const dlgBody = document.getElementById("dlgBody");
const dlgScope = document.getElementById("dlgScope");
const scopePlayer = document.getElementById("scopePlayer");
const dlgOk = document.getElementById("dlgOk");
const dlgCancel = document.getElementById("dlgCancel");
const toasts = document.getElementById("toasts");

let all = [];
let shown = [];
const selected = new Set();

function toast(message, kind = "info") {
  const el = document.createElement("div");
  el.className = `toast toast--${kind}`;
  el.textContent = message;
  toasts.appendChild(el);
  setTimeout(() => {
    el.classList.add("toast--out");
    setTimeout(() => el.remove(), 250);
  }, 4200);
}

function playerKey(r) {
  const uuid = r.uuid && r.uuid !== "unknown" ? r.uuid : "";
  return uuid ? `u:${uuid}` : `n:${(r.player || "unknown").toLowerCase()}`;
}

function samePlayer(r) {
  const key = playerKey(r);
  return all.filter((x) => playerKey(x) === key);
}

function playerBody(r) {
  return r.uuid && r.uuid !== "unknown"
    ? { uuid: r.uuid }
    : { player: r.player };
}

function identity(r) {
  const line = document.createElement("div");
  line.className = "who-line";
  const who = document.createElement("div");
  who.className = "who";
  const name = document.createElement("div");
  name.className = "name";
  name.textContent = r.player || "unknown";
  const sub = document.createElement("div");
  sub.className = "server";
  sub.textContent = r.server || "unknown";
  who.append(name, sub);
  line.append(avatar(r, 36), who);
  return line;
}

function ask({ title, who, body, scopeLabel, okLabel }) {
  dlgTitle.textContent = title;
  dlgBody.textContent = body || "";
  dlgBody.hidden = !body;
  dlgWho.replaceChildren();
  dlgWho.hidden = !who;
  if (who) dlgWho.appendChild(who);
  dlgScope.hidden = !scopeLabel;
  dlgScope.disabled = !scopeLabel;
  if (scopeLabel) {
    scopePlayer.textContent = scopeLabel;
    dlgForm.elements.scope.value = "one";
  }
  dlgOk.textContent = okLabel || "Delete";
  return new Promise((resolve) => {
    const done = () => {
      dlg.removeEventListener("close", done);
      if (dlg.returnValue !== "confirm") return resolve(null);
      const scope = scopeLabel ? dlgForm.elements.scope.value : "one";
      resolve({ player: scope === "player" });
    };
    dlg.addEventListener("close", done);
    dlg.returnValue = "";
    dlg.showModal();
    dlgCancel.focus();
  });
}

dlg.addEventListener("click", (e) => {
  if (e.target === dlg) dlg.close("cancel");
});

async function fetchReports() {
  const res = await fetch("/api/reports");
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();
  return {
    admin: Boolean(data.admin),
    reports: Array.isArray(data.reports) ? data.reports : [],
  };
}

function openPanel(reports) {
  gate.hidden = true;
  panel.hidden = false;
  window.scrollTo(0, 0);
  all = reports;
  render();
}

function showGate(message) {
  panel.hidden = true;
  gate.hidden = false;
  if (message) {
    gateErr.textContent = message;
    gateErr.hidden = false;
  }
  password.focus();
}

function gateFail(message) {
  gateErr.textContent = message;
  gateErr.hidden = false;
  password.select();
}

gateForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  gateErr.hidden = true;
  gateSubmit.disabled = true;
  gateSubmit.textContent = "Checking…";
  try {
    const res = await fetch("/api/login", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ password: password.value }),
    });
    if (res.ok) {
      let data;
      try {
        data = await fetchReports();
      } catch {
        gateFail("Signed in, but the reports would not load. Reload to retry.");
        return;
      }
      if (!data.admin) {
        gateFail("Signed in, but this browser did not keep the session cookie.");
        return;
      }
      openPanel(data.reports);
      return;
    }
    gateFail(res.status === 401 ? "Wrong password." : "Could not sign in.");
  } catch {
    gateFail("Could not reach the server.");
  } finally {
    gateSubmit.disabled = false;
    gateSubmit.textContent = "Unlock";
  }
});

logoutBtn.addEventListener("click", async () => {
  logoutBtn.disabled = true;
  try {
    await fetch("/api/logout", { method: "POST" });
  } finally {
    location.href = "/";
  }
});

async function post(path, body) {
  try {
    const res = await fetch(path, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(body),
    });
    if (res.ok) return (await res.json().catch(() => ({}))) || {};
    if (res.status === 401) {
      password.value = "";
      showGate("Session expired — sign in again.");
      return null;
    }
    toast("Could not delete — the server refused.", "bad");
    return null;
  } catch {
    toast("Could not reach the server.", "bad");
    return null;
  }
}

function drop(ids) {
  const gone = new Set(ids);
  all = all.filter((r) => !gone.has(r.id));
  for (const id of ids) selected.delete(id);
  render();
}

function plural(n) {
  return n === 1 ? "report" : "reports";
}

function breakdown(list) {
  const counts = new Map();
  for (const r of list) {
    const name = r.player || "unknown";
    counts.set(name, (counts.get(name) || 0) + 1);
  }
  const parts = [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .map(([name, n]) => (n > 1 ? `${name} ×${n}` : name));
  if (parts.length <= 6) return parts.join(", ");
  return `${parts.slice(0, 6).join(", ")} and ${parts.length - 6} more`;
}

async function removeOne(r, el, btn) {
  const mates = samePlayer(r);
  const when = localTime(r.endUtc || r.startUtc);
  const answer = await ask({
    title: "Delete report",
    who: identity(r),
    body: when ? `Session ended ${when}.` : "",
    scopeLabel:
      mates.length > 1
        ? `All ${mates.length} reports from ${r.player || "this player"}`
        : "",
    okLabel: "Delete",
  });
  if (!answer) return;

  btn.disabled = true;
  btn.textContent = "Deleting…";
  el.classList.add("card--busy");
  const data = answer.player
    ? await post("/api/delete-user", playerBody(r))
    : await post("/api/delete", { id: r.id });
  if (!data) {
    btn.disabled = false;
    btn.textContent = "Delete";
    el.classList.remove("card--busy");
    return;
  }
  const ids = answer.player
    ? data.deleted?.length
      ? data.deleted
      : mates.map((x) => x.id)
    : [r.id];
  drop(ids);
  toast(`${ids.length} ${plural(ids.length)} deleted.`, "ok");
}

bulkDelete.addEventListener("click", async () => {
  const ids = [...selected];
  if (!ids.length) return;
  const picked = all.filter((r) => selected.has(r.id));
  const wholePlayer =
    new Set(picked.map(playerKey)).size === 1 &&
    picked.length === samePlayer(picked[0]).length;

  const answer = await ask({
    title: `Delete ${ids.length} ${plural(ids.length)}`,
    body: breakdown(picked),
    okLabel: `Delete ${ids.length}`,
  });
  if (!answer) return;

  bulkDelete.disabled = true;
  bulkDelete.textContent = "Deleting…";
  const data = wholePlayer
    ? await post("/api/delete-user", playerBody(picked[0]))
    : await post("/api/delete-bulk", { ids });
  if (!data) {
    updateBulkUI();
    return;
  }
  const gone = data.deleted?.length ? data.deleted : ids;
  drop(gone);
  toast(`${gone.length} ${plural(gone.length)} deleted.`, "ok");
});

function paint(box, checked) {
  box.checked = checked;
  box.closest(".card")?.classList.toggle("card--selected", checked);
}

function onSelect(id, checked, el) {
  if (checked) selected.add(id);
  else selected.delete(id);
  el?.classList.toggle("card--selected", checked);
  updateBulkUI();
}

selectAll.addEventListener("change", () => {
  for (const box of cardsEl.querySelectorAll(".card-select")) {
    if (selectAll.checked) selected.add(box.dataset.id);
    else selected.delete(box.dataset.id);
    paint(box, selectAll.checked);
  }
  updateBulkUI();
});

clearSel.addEventListener("click", () => {
  selected.clear();
  for (const box of cardsEl.querySelectorAll(".card-select")) paint(box, false);
  updateBulkUI();
});

function updateBulkUI() {
  const boxes = [...cardsEl.querySelectorAll(".card-select")];
  const on = boxes.filter((b) => selected.has(b.dataset.id)).length;
  selectAll.checked = boxes.length > 0 && on === boxes.length;
  selectAll.indeterminate = on > 0 && on < boxes.length;
  selectAll.disabled = boxes.length === 0;
  const n = selected.size;
  bulkDelete.disabled = n === 0;
  bulkDelete.textContent = n > 0 ? `Delete selected (${n})` : "Delete selected";
  clearSel.hidden = n === 0;
  selCount.textContent = n > 0
    ? `${n} of ${shown.length} selected`
    : `${shown.length} ${plural(shown.length)}`;
}

function render() {
  const q = search.value.trim().toLowerCase();
  shown = q ? all.filter((r) => (r.player || "").toLowerCase().includes(q)) : all;

  const visible = new Set(shown.map((r) => r.id));
  for (const id of [...selected]) if (!visible.has(id)) selected.delete(id);

  cardsEl.replaceChildren();
  if (!shown.length) {
    statusEl.textContent = all.length
      ? "No players match that search."
      : "No reports yet.";
    statusEl.style.display = "";
    updateBulkUI();
    return;
  }
  statusEl.style.display = "none";
  const opts = {
    selected: (id) => selected.has(id),
    onSelect,
    onDelete: removeOne,
  };

  const frag = document.createDocumentFragment();
  for (const r of shown) {
    const el = card(r, opts);
    if (selected.has(r.id)) el.classList.add("card--selected");
    frag.appendChild(el);
  }
  cardsEl.appendChild(frag);
  updateBulkUI();
}

let debounce;
search.addEventListener("input", () => {
  clearTimeout(debounce);
  debounce = setTimeout(render, 120);
});

async function boot() {
  try {
    const data = await fetchReports();
    if (!data.admin) {
      showGate();
      return;
    }
    openPanel(data.reports);
  } catch {
    showGate("Could not reach the server — try signing in.");
  }
}

boot();

