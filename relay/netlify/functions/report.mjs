import { getStore } from "@netlify/blobs";

export const config = { path: "/api/report" };

const MAX_HTML = 5 * 1024 * 1024;
const MAX_CANONICAL = 1 * 1024 * 1024;
const INDEX_CAP = 500;
const ACCOUNT_TYPES = new Set(["Premium", "Cracked", "Unknown"]);

const SOURCE_TYPES = new Set(["MOD", "EXTERNAL", "UNKNOWN"]);
const FLAG_KINDS = new Set(["hotbar", "use", "attack", "keybind"]);
const MAX_SOURCES = 24;

const store = () => getStore({ name: "combat-tracker", consistency: "strong" });

const idSafe = (v) =>
  (typeof v === "string" ? v : "").replace(/[^A-Za-z0-9._-]/g, "").slice(0, 128);
const text = (v, max) => (typeof v === "string" ? v.slice(0, max) : "");
const count = (v) => {
  const n = Number.parseInt(v, 10);
  return Number.isFinite(n) && n > 0 ? Math.min(n, 1_000_000) : 0;
};
const json = (obj, status = 200) =>
  new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json" },
  });

const sourceEntry = (v) => {
  if (!v || typeof v !== "object") return null;
  if (!FLAG_KINDS.has(v.kind)) return null;
  return {
    kind: v.kind,
    type: SOURCE_TYPES.has(v.type) ? v.type : "UNKNOWN",
    modId: text(v.modId, 64),
    modName: text(v.modName, 64),
    modVersion: text(v.modVersion, 32),
    detail: text(v.detail, 160),
    count: count(v.count),
    suspects: Array.isArray(v.suspects)
      ? v.suspects.slice(0, 6).map((s) => text(s, 64)).filter(Boolean)
      : [],
  };
};

export default async (req) => {
  if (req.method !== "POST") return json({ error: "method not allowed" }, 405);

  let body;
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid json" }, 400);
  }

  const id = idSafe(body.sessionId);
  if (!id) return json({ error: "missing sessionId" }, 400);

  const html = typeof body.html === "string" ? body.html : "";
  if (!html) return json({ error: "missing html" }, 400);
  if (html.length > MAX_HTML) return json({ error: "html too large" }, 413);

  const f = body.flags && typeof body.flags === "object" ? body.flags : {};
  const meta = {
    id,
    player: text(body.player, 48) || "unknown",
    uuid: idSafe(body.uuid) || "unknown",
    accountType: ACCOUNT_TYPES.has(body.accountType) ? body.accountType : "Unknown",
    server: text(body.server, 120) || "unknown",
    startUtc: text(body.startUtc, 40),
    endUtc: text(body.endUtc, 40),
    detected: Array.isArray(body.detected)
      ? body.detected.slice(0, 8).map((d) => text(d, 40)).filter(Boolean)
      : [],
    flags: {
      hotbar: count(f.hotbar),
      use: count(f.use),
      attack: count(f.attack),
      keybind: count(f.keybind),
    },
    sources: Array.isArray(body.sources)
      ? body.sources.slice(0, MAX_SOURCES).map(sourceEntry).filter(Boolean)
      : [],
    updatedAt: new Date().toISOString(),
  };

  const s = store();
  await s.set(`report/${id}`, html);
  const canonical = text(body.canonical, MAX_CANONICAL);
  if (canonical) await s.set(`canonical/${id}`, canonical);

  const index = (await s.get("index", { type: "json" })) || [];
  const next = [meta, ...index.filter((e) => e && e.id !== id)].slice(0, INDEX_CAP);
  await s.setJSON("index", next);

  return json({ ok: true, id });
};
