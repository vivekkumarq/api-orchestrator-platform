/** Presentation helpers shared by the response viewer and the sidebar. */

export const HTTP_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];

export const ASSERTION_TYPES = [
  { value: "STATUS_EQUALS", label: "Status equals", needsTarget: false, needsExpected: true },
  { value: "RESPONSE_TIME_UNDER", label: "Response time under (ms)", needsTarget: false, needsExpected: true },
  { value: "JSON_PATH_EQUALS", label: "JSONPath equals", needsTarget: true, needsExpected: true },
  { value: "JSON_PATH_CONTAINS", label: "JSONPath contains", needsTarget: true, needsExpected: true },
  { value: "HEADER_PRESENT", label: "Header present", needsTarget: true, needsExpected: false },
  { value: "HEADER_EQUALS", label: "Header equals", needsTarget: true, needsExpected: true },
  { value: "BODY_CONTAINS", label: "Body contains", needsTarget: false, needsExpected: true },
];

export function assertionMeta(type) {
  return ASSERTION_TYPES.find((t) => t.value === type) ?? ASSERTION_TYPES[0];
}

/** "GET" -> the CSS custom property that colours it. */
export function methodColor(method) {
  return `var(--method-${String(method || "get").toLowerCase()}, var(--text-muted))`;
}

export function statusTone(status, errorMessage) {
  if (errorMessage) return "danger";
  if (!status) return "muted";
  if (status < 300) return "success";
  if (status < 400) return "accent";
  if (status < 500) return "warning";
  return "danger";
}

export function statusText(status, errorMessage) {
  if (errorMessage) return "Failed";
  if (!status) return "No response";
  return String(status);
}

export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return "-";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

export function formatMs(ms) {
  if (ms === null || ms === undefined) return "-";
  if (ms < 1000) return `${ms} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
}

export function formatTimestamp(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

/** Shortens a URL for the sidebar without hiding which endpoint it is. */
export function shortenUrl(url) {
  if (!url) return "(no url)";
  return url.replace(/^https?:\/\//, "");
}

/**
 * Pretty-prints a body when it parses as JSON.
 * Returns the original text unchanged otherwise, so an HTML or plain-text
 * response is still readable rather than replaced by an error.
 */
export function prettyPrint(text) {
  if (typeof text !== "string" || text.trim() === "") {
    return { text: text ?? "", isJson: false };
  }
  try {
    return { text: JSON.stringify(JSON.parse(text), null, 2), isJson: true };
  } catch {
    return { text, isJson: false };
  }
}

const JSON_TOKEN =
  /("(?:\\.|[^"\\])*")\s*:|("(?:\\.|[^"\\])*")|\b(true|false)\b|\b(null)\b|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)/g;

/**
 * Splits pretty-printed JSON into typed tokens so the viewer can colour it with
 * real elements. Deliberately not regex-driven innerHTML: the highlighted text
 * is a third-party response and must never be interpreted as markup.
 */
export function tokenizeJson(source) {
  const tokens = [];
  let lastIndex = 0;
  let match;
  JSON_TOKEN.lastIndex = 0;

  while ((match = JSON_TOKEN.exec(source)) !== null) {
    if (match.index > lastIndex) {
      tokens.push({ type: "plain", text: source.slice(lastIndex, match.index) });
    }
    if (match[1] !== undefined) {
      tokens.push({ type: "key", text: match[1] });
      tokens.push({ type: "plain", text: match[0].slice(match[1].length) });
    } else if (match[2] !== undefined) {
      tokens.push({ type: "string", text: match[2] });
    } else if (match[3] !== undefined) {
      tokens.push({ type: "boolean", text: match[3] });
    } else if (match[4] !== undefined) {
      tokens.push({ type: "null", text: match[4] });
    } else {
      tokens.push({ type: "number", text: match[5] });
    }
    lastIndex = JSON_TOKEN.lastIndex;
  }
  if (lastIndex < source.length) {
    tokens.push({ type: "plain", text: source.slice(lastIndex) });
  }
  return tokens;
}

/** UI keeps ordered pairs so rows can be blank while being typed; the API wants an object. */
export function pairsToObject(pairs) {
  const result = {};
  (pairs || []).forEach(({ key, value, enabled }) => {
    if (enabled !== false && key && key.trim() !== "") {
      result[key.trim()] = value ?? "";
    }
  });
  return result;
}

export function objectToPairs(object) {
  return Object.entries(object || {}).map(([key, value]) => ({
    key,
    value: value ?? "",
    enabled: true,
  }));
}

export function isMac() {
  if (typeof navigator === "undefined") return false;
  return /mac|iphone|ipad/i.test(navigator.platform || navigator.userAgent || "");
}
