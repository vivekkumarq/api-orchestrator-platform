import { useState } from "react";
import Modal from "./Modal.jsx";
import KeyValueEditor from "./KeyValueEditor.jsx";
import { objectToPairs, pairsToObject } from "../lib/format.js";

/**
 * The editing form for one environment.
 *
 * Rendered with a `key` tied to the selection, so switching environments
 * remounts it and the fields start from the new props. That keeps the initial
 * values out of an effect: syncing props into state after render is what
 * causes the cascading re-renders react-hooks warns about.
 */
function EnvironmentForm({ environment, isNew, busy, onSubmit }) {
  const [name, setName] = useState(environment?.name ?? "");
  const [pairs, setPairs] = useState(() => objectToPairs(environment?.variables));

  const submit = () => {
    const trimmed = name.trim();
    if (!trimmed) return;
    onSubmit({ name: trimmed, variables: pairsToObject(pairs) });
  };

  return (
    <div className="env-editor__detail">
      <label className="field">
        <span className="field__label">Name</span>
        <input
          className="input"
          value={name}
          placeholder="Local, Staging, Production…"
          onChange={(event) => setName(event.target.value)}
        />
      </label>

      <div className="field">
        <span className="field__label">Variables</span>
        <KeyValueEditor
          rows={pairs}
          onChange={setPairs}
          keyPlaceholder="baseUrl"
          valuePlaceholder="https://api.example.com"
          hint="Referenced as {{name}} in the URL, query parameters, headers and body."
        />
      </div>

      <button
        type="button"
        className="button button--primary"
        onClick={submit}
        disabled={busy || !name.trim()}
      >
        {isNew ? "Create environment" : "Save changes"}
      </button>
    </div>
  );
}

/**
 * Full environment editor: pick one on the left, edit its name and variables
 * on the right. Saving replaces the whole variable map, matching the PUT
 * semantics of the backend.
 */
export default function EnvironmentModal({
  environments,
  initialId,
  onClose,
  onCreate,
  onUpdate,
  onDelete,
  busy,
}) {
  const [selectedId, setSelectedId] = useState(initialId ?? environments[0]?.id ?? null);
  const [creating, setCreating] = useState(environments.length === 0);

  const selected = environments.find((environment) => environment.id === selectedId) ?? null;
  const isNew = creating || !selected;

  const submit = async (payload) => {
    if (isNew) {
      const created = await onCreate(payload);
      if (created) {
        setCreating(false);
        setSelectedId(created.id);
      }
    } else {
      await onUpdate(selected.id, payload);
    }
  };

  return (
    <Modal
      title="Environments"
      subtitle="Named variable sets. Requests resolve {{placeholders}} against the selected one."
      onClose={onClose}
      width={780}
      footer={
        <button type="button" className="button button--ghost" onClick={onClose}>
          Close
        </button>
      }
    >
      <div className="env-editor">
        <div className="env-editor__list">
          <button
            type="button"
            className={`env-editor__item${creating ? " is-active" : ""}`}
            onClick={() => setCreating(true)}
          >
            + New environment
          </button>
          {environments.map((environment) => (
            <div
              key={environment.id}
              className={`env-editor__row${
                !creating && environment.id === selectedId ? " is-active" : ""
              }`}
            >
              <button
                type="button"
                className="env-editor__item"
                onClick={() => {
                  setCreating(false);
                  setSelectedId(environment.id);
                }}
              >
                <span className="env-editor__name">{environment.name}</span>
                <span className="env-editor__count">
                  {Object.keys(environment.variables ?? {}).length}
                </span>
              </button>
              <button
                type="button"
                className="icon-button icon-button--sm icon-button--danger"
                title="Delete environment"
                onClick={() => onDelete(environment)}
              >
                &#10005;
              </button>
            </div>
          ))}
        </div>

        <EnvironmentForm
          key={isNew ? "new" : selected.id}
          environment={isNew ? null : selected}
          isNew={isNew}
          busy={busy}
          onSubmit={submit}
        />
      </div>
    </Modal>
  );
}
