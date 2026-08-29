import { useRef, useState } from "react";
import Tabs from "./Tabs.jsx";
import {
  formatMs,
  formatTimestamp,
  methodColor,
  shortenUrl,
  statusTone,
  statusText,
} from "../lib/format.js";

function MethodTag({ method }) {
  return (
    <span className="method-tag" style={{ color: methodColor(method) }}>
      {method}
    </span>
  );
}

function CollectionsPane({
  collections,
  activeRequestId,
  onOpenRequest,
  onCreateCollection,
  onRenameCollection,
  onDeleteCollection,
  onDeleteRequest,
  onExportCollection,
  onImportFile,
}) {
  const [expanded, setExpanded] = useState(() => new Set());
  const fileInputRef = useRef(null);

  const toggle = (id) => {
    setExpanded((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  return (
    <div className="pane">
      <div className="pane__actions">
        <button type="button" className="button button--ghost button--sm" onClick={onCreateCollection}>
          + New
        </button>
        <button
          type="button"
          className="button button--ghost button--sm"
          onClick={() => fileInputRef.current?.click()}
          title="Import a Postman v2.1 collection"
        >
          Import
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="application/json,.json"
          className="visually-hidden"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) onImportFile(file);
            event.target.value = "";
          }}
        />
      </div>

      {collections.length === 0 ? (
        <div className="empty empty--inline">
          No collections yet. Create one, or import a Postman file.
        </div>
      ) : (
        <ul className="tree">
          {collections.map((collection) => {
            const isOpen = expanded.has(collection.id);
            return (
              <li key={collection.id} className="tree__group">
                <div className="tree__node">
                  <button
                    type="button"
                    className="tree__toggle"
                    onClick={() => toggle(collection.id)}
                    aria-expanded={isOpen}
                  >
                    <span className={`caret${isOpen ? " caret--open" : ""}`} aria-hidden="true" />
                    <span className="tree__name">{collection.name}</span>
                    <span className="tree__count">{collection.requests.length}</span>
                  </button>
                  <div className="tree__tools">
                    <button
                      type="button"
                      className="icon-button icon-button--sm"
                      title="Rename"
                      onClick={() => onRenameCollection(collection)}
                    >
                      &#9998;
                    </button>
                    <button
                      type="button"
                      className="icon-button icon-button--sm"
                      title="Export as Postman v2.1"
                      onClick={() => onExportCollection(collection)}
                    >
                      &#8615;
                    </button>
                    <button
                      type="button"
                      className="icon-button icon-button--sm icon-button--danger"
                      title="Delete collection"
                      onClick={() => onDeleteCollection(collection)}
                    >
                      &#10005;
                    </button>
                  </div>
                </div>

                {isOpen ? (
                  <ul className="tree__children">
                    {collection.requests.length === 0 ? (
                      <li className="tree__empty">No requests saved here.</li>
                    ) : (
                      collection.requests.map((request) => (
                        <li key={request.id}>
                          <div
                            className={`tree__leaf${
                              activeRequestId === request.id ? " is-active" : ""
                            }`}
                          >
                            <button
                              type="button"
                              className="tree__leaf-main"
                              onClick={() => onOpenRequest(collection, request)}
                            >
                              <MethodTag method={request.method} />
                              <span className="tree__leaf-name">{request.name}</span>
                            </button>
                            <button
                              type="button"
                              className="icon-button icon-button--sm icon-button--danger"
                              title="Delete request"
                              onClick={() => onDeleteRequest(collection, request)}
                            >
                              &#10005;
                            </button>
                          </div>
                        </li>
                      ))
                    )}
                  </ul>
                ) : null}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

function HistoryPane({ history, onReplay, onClear }) {
  return (
    <div className="pane">
      <div className="pane__actions">
        <button
          type="button"
          className="button button--ghost button--sm"
          onClick={onClear}
          disabled={history.length === 0}
        >
          Clear history
        </button>
      </div>

      {history.length === 0 ? (
        <div className="empty empty--inline">Nothing executed yet.</div>
      ) : (
        <ul className="history">
          {history.map((entry) => (
            <li key={entry.id}>
              <button type="button" className="history__item" onClick={() => onReplay(entry)}>
                <div className="history__line">
                  <MethodTag method={entry.method} />
                  <span className="history__url" title={entry.resolvedUrl || entry.url}>
                    {shortenUrl(entry.resolvedUrl || entry.url)}
                  </span>
                </div>
                <div className="history__meta">
                  <span className={`pill pill--${statusTone(entry.status, entry.errorMessage)} pill--sm`}>
                    {statusText(entry.status, entry.errorMessage)}
                  </span>
                  <span>{formatMs(entry.responseTimeMs)}</span>
                  {entry.attempts > 1 ? <span>{entry.attempts} tries</span> : null}
                  {entry.assertionsPassed === true ? (
                    <span className="history__flag history__flag--pass">assertions ok</span>
                  ) : null}
                  {entry.assertionsPassed === false ? (
                    <span className="history__flag history__flag--fail">assertions failed</span>
                  ) : null}
                  <span className="history__time">{formatTimestamp(entry.createdAt)}</span>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function Sidebar(props) {
  const [tab, setTab] = useState("collections");

  const tabs = [
    { id: "collections", label: "Collections", badge: props.collections.length || undefined },
    { id: "history", label: "History", badge: props.history.length || undefined },
  ];

  return (
    <aside className="sidebar">
      <Tabs tabs={tabs} active={tab} onChange={setTab} size="sm" />
      {tab === "collections" ? (
        <CollectionsPane
          collections={props.collections}
          activeRequestId={props.activeRequestId}
          onOpenRequest={props.onOpenRequest}
          onCreateCollection={props.onCreateCollection}
          onRenameCollection={props.onRenameCollection}
          onDeleteCollection={props.onDeleteCollection}
          onDeleteRequest={props.onDeleteRequest}
          onExportCollection={props.onExportCollection}
          onImportFile={props.onImportFile}
        />
      ) : (
        <HistoryPane
          history={props.history}
          onReplay={props.onReplayHistory}
          onClear={props.onClearHistory}
        />
      )}
    </aside>
  );
}
