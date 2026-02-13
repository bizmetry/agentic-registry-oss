import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_REGISTRY_API_BASE,
  headers: { "Content-Type": "application/json" }
});

export async function safe(fn) {
  try {
    return { ok: true, data: await fn() };
  } catch (e) {
    return {
      ok: false,
      error: e?.response?.data?.message || e.message || "Error"
    };
  }
}
