/**
 * Environment picker in the top bar. Selecting one makes its variables
 * available to every {{placeholder}} in the request, and makes persisted
 * extractions write back into it.
 */
export default function EnvironmentBar({
  environments,
  activeId,
  onSelect,
  onManage,
  variableCount,
}) {
  return (
    <div className="env-bar">
      <label className="env-bar__label" htmlFor="environment-select">
        Environment
      </label>
      <select
        id="environment-select"
        className="input input--select input--sm"
        value={activeId ?? ""}
        onChange={(event) => onSelect(event.target.value || null)}
      >
        <option value="">No environment</option>
        {environments.map((environment) => (
          <option key={environment.id} value={environment.id}>
            {environment.name}
          </option>
        ))}
      </select>
      {activeId ? (
        <span className="env-bar__count" title="Variables available to this request">
          {variableCount} vars
        </span>
      ) : null}
      <button type="button" className="button button--ghost button--sm" onClick={onManage}>
        Manage
      </button>
    </div>
  );
}
