import crypto from "node:crypto";

const COOKIE = "ct_admin";

export const TTL_SECONDS = 24 * 60 * 60;

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || "thispasswordisverysecure";

function secret() {
  const explicit = process.env.AUTH_SECRET;
  if (explicit && explicit.length >= 16) return explicit;
  return crypto
    .createHash("sha256")
    .update(`combat-tracker|admin|${ADMIN_PASSWORD}`)
    .digest("hex");
}

function safeEqual(a, b) {
  const ab = Buffer.from(String(a), "utf8");
  const bb = Buffer.from(String(b), "utf8");
  if (ab.length !== bb.length) return false;
  return crypto.timingSafeEqual(ab, bb);
}

export function isAdminPassword(password) {
  if (typeof password !== "string" || !password) return false;
  return safeEqual(password, ADMIN_PASSWORD);
}

function sign() {
  const payload = Buffer.from(
    JSON.stringify({ a: 1, exp: Date.now() + TTL_SECONDS * 1000 }),
    "utf8",
  ).toString("base64url");
  const sig = crypto
    .createHmac("sha256", secret())
    .update(payload)
    .digest("base64url");
  return `${payload}.${sig}`;
}

function verify(token) {
  if (typeof token !== "string" || !token.includes(".")) return false;
  const [payload, sig] = token.split(".");
  const expected = crypto
    .createHmac("sha256", secret())
    .update(payload)
    .digest("base64url");
  if (!safeEqual(sig, expected)) return false;
  try {
    const obj = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    return Boolean(obj) && obj.a === 1 && typeof obj.exp === "number" && obj.exp > Date.now();
  } catch {
    return false;
  }
}

function readCookie(req, name) {
  const header = req.headers.get("cookie") || "";
  for (const part of header.split(/;\s*/)) {
    const eq = part.indexOf("=");
    if (eq > 0 && part.slice(0, eq) === name) {
      return decodeURIComponent(part.slice(eq + 1));
    }
  }
  return null;
}

export function isAdmin(req) {
  const token = readCookie(req, COOKIE);
  return token ? verify(token) : false;
}

export function adminCookie() {
  return (
    `${COOKIE}=${sign()}; HttpOnly; Secure; SameSite=Lax; Path=/; ` +
    `Max-Age=${TTL_SECONDS}`
  );
}

export function clearCookie() {
  return `${COOKIE}=; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=0`;
}
