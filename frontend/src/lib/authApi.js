const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

async function requestJson(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers: {
      "Content-Type": "application/json",
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
      ...(options.headers ?? {}),
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  const text = await response.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  return { response, data };
}

export function register(body) {
  return requestJson("/auth/register", {
    method: "POST",
    body,
  });
}

export function login(body) {
  return requestJson("/auth/login", {
    method: "POST",
    body,
  });
}

export function verifyTwoFactor(body) {
  return requestJson("/auth/verify-2fa", {
    method: "POST",
    body,
  });
}

export function enableTwoFactor(token) {
  return requestJson("/auth/2fa/enable", {
    method: "POST",
    token,
  });
}

export function confirmEnableTwoFactor(body, token) {
  return requestJson("/auth/2fa/enable/confirm", {
    method: "POST",
    body,
    token,
  });
}

export function disableTwoFactor(token) {
  return requestJson("/auth/2fa/disable", {
    method: "POST",
    token,
  });
}

export function logout(token) {
  return requestJson("/auth/logout", {
    method: "POST",
    token,
  });
}