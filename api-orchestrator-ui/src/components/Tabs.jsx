export default function Tabs({ tabs, active, onChange, size = "md" }) {
  return (
    <div className={`tabs tabs--${size}`} role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          className={`tabs__tab${active === tab.id ? " is-active" : ""}`}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
          {tab.badge ? <span className="tabs__badge">{tab.badge}</span> : null}
          {tab.dot ? <span className={`tabs__dot tabs__dot--${tab.dot}`} /> : null}
        </button>
      ))}
    </div>
  );
}
