import { useState } from "react";
import Tabs from "./Tabs.jsx";
import KeyValueEditor from "./KeyValueEditor.jsx";
import AssertionsEditor from "./AssertionsEditor.jsx";
import ExtractionsEditor from "./ExtractionsEditor.jsx";
import { HTTP_METHODS, isMac, methodColor } from "../lib/format.js";
import { activeCount } from "../lib/draft.js";

const BODY_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export default function RequestBuilder({
  draft,
  onDraftChange,
  onSend,
  onSave,
  loading,
  response,
  hasEnvironment,
  canSave,
}) {
  const [tab, setTab] = useState("params");
  const patch = (changes) => onDraftChange({ ...draft, ...changes });

  const sendHint = isMac() ? "⌘ Enter" : "Ctrl + Enter";
  const bodyAllowed = BODY_METHODS.has(draft.method);

  const tabs = [
    { id: "params", label: "Params", badge: activeCount(draft.queryParams) || undefined },
    { id: "headers", label: "Headers", badge: activeCount(draft.headers) || undefined },
    { id: "body", label: "Body", dot: draft.body?.trim() ? "accent" : undefined },
    {
      id: "assertions",
      label: "Assertions",
      badge: draft.assertions.length || undefined,
      dot:
        response?.assertionsPassed === undefined || response?.assertionsPassed === null
          ? undefined
          : response.assertionsPassed
            ? "success"
            : "danger",
    },
    { id: "extract", label: "Extract", badge: draft.extractions.length || undefined },
    { id: "settings", label: "Settings" },
  ];

  return (
    <section className="builder">
      <div className="builder__bar">
        <div className="method-select" style={{ "--method": methodColor(draft.method) }}>
          <select
            aria-label="HTTP method"
            value={draft.method}
            onChange={(event) => patch({ method: event.target.value })}
          >
            {HTTP_METHODS.map((method) => (
              <option key={method} value={method}>
                {method}
              </option>
            ))}
          </select>
        </div>

        <input
          className="input input--url"
          placeholder="{{baseUrl}}/posts/1"
          value={draft.url}
          spellCheck={false}
          onChange={(event) => patch({ url: event.target.value })}
        />

        <button
          type="button"
          className="button button--primary"
          onClick={onSend}
          disabled={loading || !draft.url.trim()}
          title={`Send (${sendHint})`}
        >
          {loading ? <span className="spinner" aria-hidden="true" /> : null}
          {loading ? "Sending" : "Send"}
          <kbd className="kbd">{sendHint}</kbd>
        </button>

        <button
          type="button"
          className="button button--ghost"
          onClick={onSave}
          disabled={!canSave || !draft.url.trim()}
          title={canSave ? "Save into a collection" : "Create a collection first"}
        >
          Save
        </button>
      </div>

      <Tabs tabs={tabs} active={tab} onChange={setTab} size="sm" />

      <div className="builder__panel">
        {tab === "params" ? (
          <KeyValueEditor
            rows={draft.queryParams}
            onChange={(queryParams) => patch({ queryParams })}
            keyPlaceholder="name"
            valuePlaceholder="value or {{variable}}"
            hint="Appended to the URL and percent-encoded for you."
          />
        ) : null}

        {tab === "headers" ? (
          <KeyValueEditor
            rows={draft.headers}
            onChange={(headers) => patch({ headers })}
            keyPlaceholder="Header-Name"
            valuePlaceholder="value or {{variable}}"
            hint="Both names and values go through variable substitution."
          />
        ) : null}

        {tab === "body" ? (
          <div className="body-editor">
            {!bodyAllowed ? (
              <p className="hint hint--warning">
                A {draft.method} request is sent without a body. Switch to POST, PUT, PATCH or
                DELETE to send one.
              </p>
            ) : (
              <p className="hint">Raw body. Variables are substituted before sending.</p>
            )}
            <textarea
              className="input input--mono textarea"
              rows={12}
              spellCheck={false}
              placeholder={'{\n  "name": "{{who}}"\n}'}
              value={draft.body}
              onChange={(event) => patch({ body: event.target.value })}
            />
          </div>
        ) : null}

        {tab === "assertions" ? (
          <AssertionsEditor
            assertions={draft.assertions}
            onChange={(assertions) => patch({ assertions })}
            results={response?.assertions}
          />
        ) : null}

        {tab === "extract" ? (
          <ExtractionsEditor
            extractions={draft.extractions}
            onChange={(extractions) => patch({ extractions })}
            extracted={response?.extracted}
            hasEnvironment={hasEnvironment}
          />
        ) : null}

        {tab === "settings" ? (
          <div className="settings-grid">
            <label className="field">
              <span className="field__label">Timeout (ms)</span>
              <input
                className="input"
                type="number"
                min="1"
                value={draft.timeoutMs}
                onChange={(event) => patch({ timeoutMs: event.target.value })}
              />
              <span className="field__hint">Clamped by the server's maximum.</span>
            </label>
            <label className="field">
              <span className="field__label">Retries</span>
              <input
                className="input"
                type="number"
                min="0"
                max="10"
                value={draft.maxRetries}
                onChange={(event) => patch({ maxRetries: event.target.value })}
              />
              <span className="field__hint">Retried on 5xx and transport errors only.</span>
            </label>
            <label className="field">
              <span className="field__label">Backoff (ms)</span>
              <input
                className="input"
                type="number"
                min="1"
                value={draft.retryBackoffMs}
                onChange={(event) => patch({ retryBackoffMs: event.target.value })}
              />
              <span className="field__hint">Base delay; doubles on each retry.</span>
            </label>
          </div>
        ) : null}
      </div>
    </section>
  );
}
