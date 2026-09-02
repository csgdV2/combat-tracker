import { getStore } from "@netlify/blobs";
import { isAdmin } from "../lib/auth.mjs";

export const config = { path: "/api/delete" };

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

  const id = idSafe(body && body.id);
  if (!id) return json({ error: "missing id" }, 400);

  const s = store();
  await s.delete(`report/${id}`);
  await s.delete(`canonical/${id}`);

  const index = (await s.get("index", { type: "json" })) || [];
  await s.setJSON(
    "index",
    index.filter((e) => e && e.id !== id),
  );

  return json({ ok: true, id });
};
