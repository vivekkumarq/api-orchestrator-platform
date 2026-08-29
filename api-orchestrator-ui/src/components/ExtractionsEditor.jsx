export default function ExtractionsEditor({ extractions, onChange, extracted, hasEnvironment }) {
  const update = (index, patch) =>
    onChange(extractions.map((e, i) => (i === index ? { ...e, ...patch } : e)));

  const add = () => onChange([...extractions, { name: "", jsonPath: "$.", persist: true }]);

  const remove = (index) => onChange(extractions.filter((_, i) => i !== index));

  return (
    <div className="kv">
      <p className="hint">
        Pull a value out of the response and bind it to a variable. Persisted values are written
        into the selected environment, so the next request can use it as{" "}
        <code>{"{{name}}"}</code>.
      </p>

      {!hasEnvironment && extractions.length > 0 ? (
        <p className="hint hint--warning">
          No environment is selected, so nothing can be persisted. Extracted values will still be
          reported for this run.
        </p>
      ) : null}

      {extractions.length === 0 ? (
        <div className="empty empty--inline">No extractions declared.</div>
      ) : (
        <div className="kv__rows">
          {extractions.map((extraction, index) => (
            <div className="extraction-row" key={index}>
              <input
                className="input input--mono"
                placeholder="variable name"
                value={extraction.name ?? ""}
                onChange={(event) => update(index, { name: event.target.value })}
              />
              <input
                className="input input--mono"
                placeholder="$.token"
                value={extraction.jsonPath ?? ""}
                onChange={(event) => update(index, { jsonPath: event.target.value })}
              />
              <label className="checkbox checkbox--labelled" title="Write into the environment">
                <input
                  type="checkbox"
                  checked={extraction.persist !== false}
                  onChange={(event) => update(index, { persist: event.target.checked })}
                />
                <span />
                <em>persist</em>
              </label>
              {extracted?.[extraction.name] !== undefined ? (
                <code className="extraction-row__value" title={extracted[extraction.name]}>
                  {extracted[extraction.name]}
                </code>
              ) : (
                <span className="pill pill--muted pill--sm">not run</span>
              )}
              <button
                type="button"
                className="icon-button icon-button--sm"
                onClick={() => remove(index)}
                aria-label="Remove extraction"
              >
                &#10005;
              </button>
            </div>
          ))}
        </div>
      )}

      <button type="button" className="button button--ghost button--sm" onClick={add}>
        + Add extraction
      </button>
    </div>
  );
}
