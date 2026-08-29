import { useState } from "react";
import Tabs from "./Tabs.jsx";
import CodeBlock from "./CodeBlock.jsx";
import { formatBytes, formatMs, statusText, statusTone } from "../lib/format.js";

function Metric({ label, value, tone }) {
  return (
    <div className="metric">
      <span className="metric__label">{label}</span>
      <span className={`metric__value${tone ? ` metric__value--${tone}` : ""}`}>{value}</span>
    </div>
  );
}

export default function ResponseViewer({ response, loading, error }) {
  const [tab, setTab] = useState("body");

  if (loading) {
    return (
      <section className="response response--center">
        <div className="loading-state">
          <span className="spinner spinner--lg" aria-hidden="true" />
          <p>Executing request…</p>
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="response response--center">
        <div className="empty empty--error">
          <h3>Could not run the request</h3>
          <p>{error}</p>
        </div>
      </section>
    );
  }

  if (!response) {
    return (
      <section className="response response--center">
        <div className="empty">
          <h3>No response yet</h3>
          <p>
            Enter a URL and hit <strong>Send</strong>. Status, timing, headers, assertion results
            and any extracted variables land here.
          </p>
        </div>
      </section>
    );
  }

  const tone = statusTone(response.status, response.errorMessage);
  const headerEntries = Object.entries(response.headers ?? {});
  const extractedEntries = Object.entries(response.extracted ?? {});
  const assertions = response.assertions ?? [];

  const tabs = [
    { id: "body", label: "Body" },
    { id: "headers", label: "Headers", badge: headerEntries.length || undefined },
  ];
  if (assertions.length) {
    tabs.push({
      id: "assertions",
      label: "Assertions",
      badge: `${assertions.filter((a) => a.passed).length}/${assertions.length}`,
      dot: response.assertionsPassed ? "success" : "danger",
    });
  }
  if (extractedEntries.length) {
    tabs.push({ id: "extracted", label: "Extracted", badge: extractedEntries.length });
  }

  const activeTab = tabs.some((t) => t.id === tab) ? tab : "body";

  return (
    <section className="response">
      <header className="response__head">
        <div className="response__metrics">
          <span className={`pill pill--${tone} pill--status`}>
            {statusText(response.status, response.errorMessage)}
          </span>
          <Metric label="Time" value={formatMs(response.responseTimeMs)} />
          <Metric label="Size" value={formatBytes(response.responseSizeBytes)} />
          {response.attempts > 1 ? (
            <Metric label="Attempts" value={response.attempts} tone="warning" />
          ) : null}
          {response.assertionsPassed !== null && response.assertionsPassed !== undefined ? (
            <Metric
              label="Assertions"
              value={response.assertionsPassed ? "all passed" : "failed"}
              tone={response.assertionsPassed ? "success" : "danger"}
            />
          ) : null}
        </div>
        {response.resolvedUrl ? (
          <p className="response__url" title={response.resolvedUrl}>
            <span className="response__url-label">Resolved</span>
            <code>{response.resolvedUrl}</code>
          </p>
        ) : null}
      </header>

      {response.errorMessage ? (
        <div className="banner banner--danger">
          <strong>Transport failure.</strong> {response.errorMessage}
        </div>
      ) : null}

      {response.bodyTruncated ? (
        <div className="banner banner--warning">
          The response exceeded the server's in-memory limit and was truncated. The reported size
          is the full length.
        </div>
      ) : null}

      <Tabs tabs={tabs} active={activeTab} onChange={setTab} size="sm" />

      <div className="response__panel">
        {activeTab === "body" ? (
          <CodeBlock content={response.body} emptyLabel="The response had no body." />
        ) : null}

        {activeTab === "headers" ? (
          headerEntries.length ? (
            <table className="table">
              <thead>
                <tr>
                  <th>Header</th>
                  <th>Value</th>
                </tr>
              </thead>
              <tbody>
                {headerEntries.map(([name, value]) => (
                  <tr key={name}>
                    <td className="table__key">{name}</td>
                    <td className="table__value">{value}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="empty empty--inline">No response headers.</div>
          )
        ) : null}

        {activeTab === "assertions" ? (
          <ul className="assertion-results">
            {assertions.map((assertion, index) => (
              <li
                key={index}
                className={`assertion-result assertion-result--${assertion.passed ? "pass" : "fail"}`}
              >
                <span className={`pill pill--${assertion.passed ? "success" : "danger"} pill--sm`}>
                  {assertion.passed ? "pass" : "fail"}
                </span>
                <div className="assertion-result__body">
                  <p className="assertion-result__title">
                    {assertion.type}
                    {assertion.target ? <code> {assertion.target}</code> : null}
                  </p>
                  <p className="assertion-result__message">{assertion.message}</p>
                  {assertion.actual !== null && assertion.actual !== undefined ? (
                    <p className="assertion-result__actual">
                      actual: <code>{assertion.actual}</code>
                    </p>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        ) : null}

        {activeTab === "extracted" ? (
          <table className="table">
            <thead>
              <tr>
                <th>Variable</th>
                <th>Value</th>
              </tr>
            </thead>
            <tbody>
              {extractedEntries.map(([name, value]) => (
                <tr key={name}>
                  <td className="table__key">{`{{${name}}}`}</td>
                  <td className="table__value">{value}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
  );
}
