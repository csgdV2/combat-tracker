import { getStore } from "@netlify/blobs";
import { isAdmin } from "../lib/auth.mjs";

export const config = { path: "/api/delete-user" };

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

  const uuid = idSafe(body && body.uuid);
  const player = (typeof body?.player === "string" ? body.player : "")
    .slice(0, 48)
    .toLowerCase();

  const byUuid = Boolean(uuid) && uuid !== "unknown";
  if (!byUuid && !player) return json({ error: "missing uuid or player" }, 400);

  const s = store();
  const index = (await s.get("index", { type: "json" })) || [];
  const live = Array.isArray(index) ? index.filter(Boolean) : [];

  const mine = (e) =>
    byUuid ? e.uuid === uuid : (e.player || "").toLowerCase() === player;

  const ids = live.filter(mine).map((e) => e.id).filter(Boolean);
  if (!ids.length) return json({ ok: true, deleted: [] });

  for (let i = 0; i < ids.length; i += 20) {
    const batch = ids.slice(i, i + 20);
    await Promise.all(
      batch.flatMap((id) => [s.delete(`report/${id}`), s.delete(`canonical/${id}`)]),
    );
  }

  await s.setJSON("index", live.filter((e) => !mine(e)));

  return json({ ok: true, deleted: ids });
};
