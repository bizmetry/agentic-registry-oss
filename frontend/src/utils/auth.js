const KEY = "registry_session";

export function loginStatic(u, p) {
  if (u === "admin" && p === "admin") {
    sessionStorage.setItem(KEY, "1");
    return true;
  }
  return false;
}

export function isAuthed() {
  return !!sessionStorage.getItem(KEY);
}

export function logout() {
  sessionStorage.removeItem(KEY);
}
