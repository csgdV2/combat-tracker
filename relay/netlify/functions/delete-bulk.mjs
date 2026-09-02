import { getStore } from "@netlify/blobs";
import { isAdmin } from "../lib/auth.mjs";

export const config = { path: "/api/delete-bulk" };

const MAX_IDS = 500;

const store = () => getStore({ name: "combat-tracker", consistency: "strong" });

const idSafe = (v) =>
  (typeof v === "string" ? v : "").replace(/[^A-Za-z0-9._-]/g, "").slice(0, 128);

const json = (obj, status = 200) =>
  new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json" },
  });

export default async (req) => {
  if (req.method !== "POST") return json({ error: "method not allowed" }, 405);

  if (!isAdmin(req)) return json({ error: "admin only" }, 401);

  let body;
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid json" }, 400);
  }

  const ids = Array.isArray(body?.ids)
    ? [...new Set(body.ids.map(idSafe).filter(Boolean))].slice(0, MAX_IDS)
    : [];
  if (!ids.length) return json({ error: "missing ids" }, 400);

  const s = store();
  const index = (await s.get("index", { type: "json" })) || [];
  const live = Array.isArray(index) ? index.filter(Boolean) : [];
  const gone = new Set(ids);
  const deleted = live.filter((e) => gone.has(e.id)).map((e) => e.id);

  for (let i = 0; i < ids.length; i += 20) {
    const batch = ids.slice(i, i + 20);
    await Promise.all(
      batch.flatMap((id) => [s.delete(`report/${id}`), s.delete(`canonical/${id}`)]),
    );
  }

  await s.setJSON(
    "index",
    live.filter((e) => !gone.has(e.id)),
  );

  return json({ ok: true, deleted });
};
