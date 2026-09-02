import { clearCookie } from "../lib/auth.mjs";

export const config = { path: "/api/logout" };

export default async () =>
  new Response(JSON.stringify({ ok: true }), {
    status: 200,
    headers: {
      "content-type": "application/json",
      "set-cookie": clearCookie(),
    },
  });
