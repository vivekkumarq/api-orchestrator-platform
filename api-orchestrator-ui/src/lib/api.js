/**
 * Thin wrapper over the backend HTTP API.
 *
 * Every call funnels through `request`, which turns a non-2xx response into an
 * ApiError carrying the RFC 7807 problem detail the backend sends, so callers
 * can surface the server's own explanation rather than a generic message.
 */

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(message, status, problem) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
  }
}

async function request(path, { method = "GET", body, signal } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      signal,
      headers: body === undefined ? undefined : { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    // fetch only rejects on a network-level failure, never on an HTTP error status.
    throw new ApiError(
      `Cannot reach the API at ${API_BASE_URL}. Is the backend running?`,
      0,
      null,
    );
  }

  if (response.status === 204) return null;

  const text = await response.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = text;
    }
  }

  if (!response.ok) {
    const detail =
      payload && typeof payload === "object"
        ? payload.detail || payload.title || payload.message
        : payload;
    throw new ApiError(detail || `Request failed with ${response.status}`, response.status, payload);
  }
  return payload;
}

export const api = {
  // ---- execution ------------------------------------------------------
  execute: (payload, signal) =>
    request("/api/requests/execute", { method: "POST", body: payload, signal }),

  history: (page = 0, size = 50) =>
    request(`/api/requests/history?page=${page}&size=${size}`),

  historyEntry: (id) => request(`/api/requests/history/${id}`),

  clearHistory: () => request("/api/requests/history", { method: "DELETE" }),

  // ---- collections ----------------------------------------------------
  listCollections: () => request("/api/collections"),

  createCollection: (dto) => request("/api/collections", { method: "POST", body: dto }),

  updateCollection: (id, dto) => request(`/api/collections/${id}`, { method: "PUT", body: dto }),

  deleteCollection: (id) => request(`/api/collections/${id}`, { method: "DELETE" }),

  addRequest: (collectionId, dto) =>
    request(`/api/collections/${collectionId}/requests`, { method: "POST", body: dto }),

  updateRequest: (collectionId, requestId, dto) =>
    request(`/api/collections/${collectionId}/requests/${requestId}`, { method: "PUT", body: dto }),

  deleteRequest: (collectionId, requestId) =>
    request(`/api/collections/${collectionId}/requests/${requestId}`, { method: "DELETE" }),

  importCollection: (collection, name) =>
    request(`/api/collections/import${name ? `?name=${encodeURIComponent(name)}` : ""}`, {
      method: "POST",
      body: collection,
    }),

  exportCollection: (id) => request(`/api/collections/${id}/export`),

  // ---- environments ---------------------------------------------------
  listEnvironments: () => request("/api/environments"),

  createEnvironment: (dto) => request("/api/environments", { method: "POST", body: dto }),

  updateEnvironment: (id, dto) => request(`/api/environments/${id}`, { method: "PUT", body: dto }),

  deleteEnvironment: (id) => request(`/api/environments/${id}`, { method: "DELETE" }),
};
