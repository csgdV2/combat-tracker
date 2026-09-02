import { isAdminPassword, adminCookie } from "../lib/auth.mjs";

export const config = { path: "/api/login" };

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const json = (obj, status, extraHeaders = {}) =>
  new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json", ...extraHeaders },
  });

export default async (req) => {
  if (req.method !== "POST") return json({ error: "method not allowed" }, 405);

  let body;
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid json" }, 400);
  }

  if (!isAdminPassword(body && body.password)) {
    await sleep(400);
    return json({ error: "wrong password" }, 401);
  }

  return json({ ok: true }, 200, { "set-cookie": adminCookie() });
};
