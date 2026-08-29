/** Shape of the request being edited, and conversions to and from the API DTOs. */

import { objectToPairs, pairsToObject } from "./format.js";

export function emptyDraft() {
  return {
    savedRequestId: null,
    collectionId: null,
    name: "",
    method: "GET",
    url: "",
    queryParams: [],
    headers: [],
    body: "",
    assertions: [],
    extractions: [],
    timeoutMs: 10000,
    maxRetries: 0,
    retryBackoffMs: 200,
  };
}

export function draftFromSavedRequest(saved, collectionId) {
  return {
    savedRequestId: saved.id,
    collectionId,
    name: saved.name ?? "",
    method: saved.method ?? "GET",
    url: saved.url ?? "",
    queryParams: objectToPairs(saved.queryParams),
    headers: objectToPairs(saved.headers),
    body: saved.body ?? "",
    assertions: (saved.assertions ?? []).map((a) => ({ ...a })),
    extractions: (saved.extractions ?? []).map((e) => ({ ...e })),
    timeoutMs: saved.timeoutMs ?? 10000,
    maxRetries: saved.maxRetries ?? 0,
    retryBackoffMs: saved.retryBackoffMs ?? 200,
  };
}

/** History entries carry only what was sent, so they replay as an unsaved draft. */
export function draftFromHistory(entry) {
  return {
    ...emptyDraft(),
    name: "",
    method: entry.method ?? "GET",
    url: entry.url ?? "",
    headers: objectToPairs(entry.requestHeaders),
    body: entry.requestBody ?? "",
  };
}

export function draftToExecutePayload(draft, environmentId) {
  const payload = {
    method: draft.method,
    url: draft.url,
    headers: pairsToObject(draft.headers),
    queryParams: pairsToObject(draft.queryParams),
    body: draft.body?.length ? draft.body : null,
    timeoutMs: Number(draft.timeoutMs) || undefined,
    maxRetries: Number(draft.maxRetries) || 0,
    retryBackoffMs: Number(draft.retryBackoffMs) || undefined,
  };
  if (environmentId) payload.environmentId = environmentId;

  const assertions = (draft.assertions ?? []).filter((a) => a.type);
  if (assertions.length) payload.assertions = assertions;

  const extractions = (draft.extractions ?? []).filter((e) => e.name && e.jsonPath);
  if (extractions.length) payload.extractions = extractions;

  return payload;
}

export function draftToSavedRequestPayload(draft, name) {
  return {
    name: name ?? draft.name,
    method: draft.method,
    url: draft.url,
    headers: pairsToObject(draft.headers),
    queryParams: pairsToObject(draft.queryParams),
    body: draft.body?.length ? draft.body : null,
    assertions: (draft.assertions ?? []).filter((a) => a.type),
    extractions: (draft.extractions ?? []).filter((e) => e.name && e.jsonPath),
    timeoutMs: Number(draft.timeoutMs) || null,
    maxRetries: Number(draft.maxRetries) || null,
    retryBackoffMs: Number(draft.retryBackoffMs) || null,
  };
}

/** Counts the rows that would actually be sent, for the little tab badges. */
export function activeCount(pairs) {
  return (pairs ?? []).filter((p) => p.enabled !== false && p.key?.trim()).length;
}
