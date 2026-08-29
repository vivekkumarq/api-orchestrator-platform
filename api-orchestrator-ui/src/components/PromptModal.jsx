import { useState } from "react";
import Modal from "./Modal.jsx";

/** Small single-field prompt used for naming and renaming collections and saved requests. */
export default function PromptModal({
  title,
  subtitle,
  label,
  placeholder,
  initialValue = "",
  confirmLabel = "Save",
  extra,
  onSubmit,
  onClose,
}) {
  const [value, setValue] = useState(initialValue);

  const submit = () => {
    const trimmed = value.trim();
    if (trimmed) onSubmit(trimmed);
  };

  return (
    <Modal
      title={title}
      subtitle={subtitle}
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
            disabled={!value.trim()}
          >
            {confirmLabel}
          </button>
        </>
      }
    >
      <label className="field">
        <span className="field__label">{label}</span>
        <input
          className="input"
          autoFocus
          value={value}
          placeholder={placeholder}
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") submit();
          }}
        />
      </label>
      {extra}
    </Modal>
  );
}
