/**
 * Editable list of key/value rows, used for query parameters and headers.
 * Rows can be disabled rather than deleted, which is how you keep a header
 * around while testing without it.
 */
export default function KeyValueEditor({ rows, onChange, keyPlaceholder, valuePlaceholder, hint }) {
  const update = (index, patch) => {
    const next = rows.map((row, i) => (i === index ? { ...row, ...patch } : row));
    onChange(next);
  };

  const add = () => onChange([...rows, { key: "", value: "", enabled: true }]);

  const remove = (index) => onChange(rows.filter((_, i) => i !== index));

  return (
    <div className="kv">
      {hint ? <p className="hint">{hint}</p> : null}

      {rows.length === 0 ? (
        <div className="empty empty--inline">Nothing here yet.</div>
      ) : (
        <div className="kv__rows">
          {rows.map((row, index) => (
            <div className="kv__row" key={index}>
              <label className="checkbox" title="Include this row">
                <input
                  type="checkbox"
                  checked={row.enabled !== false}
                  onChange={(event) => update(index, { enabled: event.target.checked })}
                />
                <span />
              </label>
              <input
                className="input input--mono"
                value={row.key}
                placeholder={keyPlaceholder}
                onChange={(event) => update(index, { key: event.target.value })}
              />
              <input
                className="input input--mono"
                value={row.value}
                placeholder={valuePlaceholder}
                onChange={(event) => update(index, { value: event.target.value })}
              />
              <button
                type="button"
                className="icon-button icon-button--sm"
                onClick={() => remove(index)}
                aria-label="Remove row"
              >
                &#10005;
              </button>
            </div>
          ))}
        </div>
      )}

      <button type="button" className="button button--ghost button--sm" onClick={add}>
        + Add row
      </button>
    </div>
  );
}
