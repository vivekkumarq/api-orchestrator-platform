export default function Toasts({ toasts, onDismiss }) {
  if (!toasts.length) return null;
  return (
    <div className="toasts" role="status" aria-live="polite">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast--${toast.tone}`}>
          <span className="toast__text">{toast.message}</span>
          <button
            type="button"
            className="icon-button icon-button--sm"
            onClick={() => onDismiss(toast.id)}
            aria-label="Dismiss"
          >
            &#10005;
          </button>
        </div>
      ))}
    </div>
  );
}
