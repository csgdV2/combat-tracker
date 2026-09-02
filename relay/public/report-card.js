export const FLAG_ORDER = ["attack", "keybind", "hotbar", "use"];
export const FLAG_LABELS = {
  attack: "Attack",
  keybind: "Keybind",
  hotbar: "Hotbar",
  use: "Use",
};

const AVATAR_FALLBACK = "https://mc-heads.net/avatar/MHF_Steve/48";

export function localTime(s) {
  if (!s) return "";
  const iso = s.replace(" ", "T").replace(" UTC", "Z");
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? s : d.toLocaleString();
}

export function avatar(r, size = 48) {
  const img = document.createElement("img");
  img.className = "avatar";
  img.width = size;
  img.height = size;
  img.loading = "lazy";
  img.alt = "";
  const uuid = encodeURIComponent(r.uuid || "");
  img.src = `https://mc-heads.net/avatar/${uuid || "MHF_Steve"}/${size}`;
  img.addEventListener(
    "error",
    () => {
      img.src = AVATAR_FALLBACK;
    },
    { once: true },
  );
  return img;
}

export function sourceLabel(s) {
  if (s.type === "MOD") return s.modName || s.modId || "Unknown mod";
  if (s.type === "EXTERNAL") {
    return s.suspects && s.suspects.length
      ? `External · ${s.suspects.join(", ")}`
      : "External";
  }
  return "Unattributed";
}

function bySource(sources) {
  const groups = new Map();
  for (const s of sources) {
    if (!s || typeof s !== "object") continue;
    const type = s.type === "MOD" || s.type === "EXTERNAL" ? s.type : "UNKNOWN";
    const label = sourceLabel(s);
    const key = `${type}|${label}`;
    let g = groups.get(key);
    if (!g) {
      g = {
        label,
        type,
        count: 0,
        version: s.modVersion || "",
        kinds: [],
        details: new Set(),
      };
      groups.set(key, g);
    }
    const n = Number.parseInt(s.count, 10) || 0;
    g.count += n;
    const kind = FLAG_LABELS[s.kind];
    if (kind) g.kinds.push(n > 0 ? `${kind} ×${n}` : kind);
    if (s.detail) g.details.add(s.detail);
  }
  return [...groups.values()].sort((a, b) => b.count - a.count);
}

export function sourceChips(r) {
  const groups = bySource(Array.isArray(r.sources) ? r.sources : []);
  if (!groups.length) return null;
  const row = document.createElement("div");
  row.className = "sources";
  for (const g of groups) {
    const chip = document.createElement("span");
    chip.className = `chip chip--src src--${g.type.toLowerCase()}`;
    chip.textContent = g.count > 0 ? `${g.label} ×${g.count}` : g.label;
    const tip = [g.kinds.join(", "), g.version && `v${g.version}`, ...g.details]
      .filter(Boolean)
      .join(" — ");
    if (tip) chip.title = tip;
    row.appendChild(chip);
  }
  return row;
}

export function flagChips(r) {
  const chips = document.createElement("div");
  chips.className = "chips";
  const flags = r.flags || {};
  let any = false;
  for (const key of FLAG_ORDER) {
    const n = Number.parseInt(flags[key], 10) || 0;
    if (n > 0) {
      any = true;
      const chip = document.createElement("span");
      chip.className = `chip chip--${key}`;
      chip.textContent = `${FLAG_LABELS[key]} ×${n}`;
      chips.appendChild(chip);
    }
  }
  if (!any && Array.isArray(r.detected)) {
    for (const d of r.detected) {
      const chip = document.createElement("span");
      chip.className = "chip";
      chip.textContent = d;
      chips.appendChild(chip);
    }
  }
  return chips;
}

export function card(r, opts = {}) {
  const el = document.createElement("article");
  el.className = "card";
  el.dataset.id = r.id;

  const head = document.createElement("div");
  head.className = "card__head";

  if (opts.onSelect) {
    const box = document.createElement("input");
    box.type = "checkbox";
    box.className = "card-select";
    box.dataset.id = r.id;
    box.checked = Boolean(opts.selected && opts.selected(r.id));
    box.addEventListener("change", () => opts.onSelect(r.id, box.checked, el));
    head.appendChild(box);
  }

  const who = document.createElement("div");
  who.className = "who";
  const name = document.createElement("div");
  name.className = "name";
  name.textContent = r.player || "unknown";
  const server = document.createElement("div");
  server.className = "server";
  server.textContent = r.server || "unknown";
  who.append(name, server);

  const acctVal =
    r.accountType === "Premium" || r.accountType === "Cracked"
      ? r.accountType
      : "Unknown";
  const acct = document.createElement("span");
  acct.className = `acct acct--${acctVal.toLowerCase()}`;
  acct.textContent = acctVal;
  acct.title = "Account type";

  head.append(avatar(r), who, acct);

  const foot = document.createElement("div");
  foot.className = "card__foot";
  const time = document.createElement("span");
  time.className = "time";
  const rawTime = r.endUtc || r.startUtc || "";
  time.textContent = localTime(rawTime);
  time.title = rawTime;

  const actions = document.createElement("div");
  actions.className = "actions";
  if (opts.onDelete) {
    const del = document.createElement("button");
    del.className = "del";
    del.type = "button";
    del.textContent = "Delete";
    del.addEventListener("click", () => opts.onDelete(r, el, del));
    actions.appendChild(del);
  }
  const open = document.createElement("a");
  open.className = "open";
  open.textContent = "Open report";
  open.href = `/report/${encodeURIComponent(r.id || "")}`;
  open.target = "_blank";
  open.rel = "noopener";
  actions.appendChild(open);

  foot.append(time, actions);

  el.append(head, flagChips(r));
  const sources = sourceChips(r);
  if (sources) el.appendChild(sources);
  el.appendChild(foot);
  return el;
}
