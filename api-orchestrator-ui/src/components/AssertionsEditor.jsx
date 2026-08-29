import { ASSERTION_TYPES, assertionMeta } from "../lib/format.js";

export default function AssertionsEditor({ assertions, onChange, results }) {
  const update = (index, patch) =>
    onChange(assertions.map((a, i) => (i === index ? { ...a, ...patch } : a)));

  const add = () =>
    onChange([...assertions, { type: "STATUS_EQUALS", target: "", expected: "200" }]);

  const remove = (index) => onChange(assertions.filter((_, i) => i !== index));

  return (
    <div className="kv">
      <p className="hint">
        Expectations are checked against the response after every run. The result of each one is
        shown in the Assertions tab of the response panel.
      </p>

      {assertions.length === 0 ? (
        <div className="empty empty--inline">No assertions declared.</div>
      ) : (
        <div className="kv__rows">
          {assertions.map((assertion, index) => {
            const meta = assertionMeta(assertion.type);
            const result = results?.[index];
            return (
              <div className="assertion-row" key={index}>
                <select
                  className="input input--select"
                  value={assertion.type}
                  onChange={(event) => update(index, { type: event.target.value })}
                >
                  {ASSERTION_TYPES.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </select>
                <input
                  className="input input--mono"
                  placeholder={meta.needsTarget ? "$.data.id or Content-Type" : "not used"}
                  value={assertion.target ?? ""}
                  disabled={!meta.needsTarget}
                  onChange={(event) => update(index, { target: event.target.value })}
                />
                <input
                  className="input input--mono"
                  placeholder={meta.needsExpected ? "expected value" : "not used"}
                  value={assertion.expected ?? ""}
                  disabled={!meta.needsExpected}
                  onChange={(event) => update(index, { expected: event.target.value })}
                />
                {result ? (
                  <span
                    className={`pill pill--${result.passed ? "success" : "danger"} pill--sm`}
                    title={result.message}
                  >
                    {result.passed ? "pass" : "fail"}
                  </span>
                ) : (
                  <span className="pill pill--muted pill--sm">not run</span>
                )}
                <button
                  type="button"
                  className="icon-button icon-button--sm"
                  onClick={() => remove(index)}
                  aria-label="Remove assertion"
                >
                  &#10005;
                </button>
              </div>
            );
          })}
        </div>
      )}

      <button type="button" className="button button--ghost button--sm" onClick={add}>
        + Add assertion
      </button>
    </div>
  );
}
