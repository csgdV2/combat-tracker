import { getStore } from "@netlify/blobs";

export const config = { path: "/report/:id" };

const idSafe = (v) =>
  (typeof v === "string" ? v : "").replace(/[^A-Za-z0-9._-]/g, "").slice(0, 128);

export default async (req, context) => {
  const id = idSafe(context.params?.id);
  if (!id) return new Response("Not found", { status: 404 });

  const store = getStore({ name: "combat-tracker", consistency: "strong" });
  const html = await store.get(`report/${id}`, { type: "text" });
  if (!html) return new Response("Report not found", { status: 404 });

  return new Response(html, {
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "public, max-age=60",
    },
  });
};
