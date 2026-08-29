import { useState } from "react";
import Modal from "./Modal.jsx";

export default function SaveRequestModal({ collections, draft, onSubmit, onClose }) {
  const [name, setName] = useState(draft.name || "");
  const [collectionId, setCollectionId] = useState(
    draft.collectionId ?? collections[0]?.id ?? "",
  );
  const [overwrite, setOverwrite] = useState(Boolean(draft.savedRequestId));

  const canOverwrite = Boolean(draft.savedRequestId && draft.collectionId);
  const submit = () => {
    if (!name.trim() || !collectionId) return;
    onSubmit({ name: name.trim(), collectionId, overwrite: overwrite && canOverwrite });
  };

  return (
    <Modal
      title="Save request"
      subtitle="Stores the method, URL, headers, params, body, assertions and extractions."
      onClose={onClose}
      footer={
        <>
          <button type="button" className="button button--ghost" onClick={onClose}>
            Cancel
          </button>
          <button
            type="button"
            className="button button--primary"
            onClick={submit}
            disabled={!name.trim() || !collectionId}
          >
            {overwrite && canOverwrite ? "Update request" : "Save to collection"}
          </button>
        </>
      }
    >
      <label className="field">
        <span className="field__label">Name</span>
        <input
          className="input"
          autoFocus
          value={name}
          placeholder="Get user by id"
          onChange={(event) => setName(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") submit();
          }}
        />
      </label>

      <label className="field">
        <span className="field__label">Collection</span>
        <select
          className="input input--select"
          value={collectionId}
          onChange={(event) => setCollectionId(event.target.value)}
        >
          {collections.map((collection) => (
            <option key={collection.id} value={collection.id}>
              {collection.name}
            </option>
          ))}
        </select>
      </label>

      {canOverwrite ? (
        <label className="checkbox checkbox--labelled checkbox--block">
          <input
            type="checkbox"
            checked={overwrite}
            onChange={(event) => setOverwrite(event.target.checked)}
          />
          <span />
          <em>Update the request this was opened from instead of creating a copy</em>
        </label>
      ) : null}
    </Modal>
  );
}
