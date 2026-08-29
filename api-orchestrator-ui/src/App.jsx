import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Sidebar from "./components/Sidebar.jsx";
import RequestBuilder from "./components/RequestBuilder.jsx";
import ResponseViewer from "./components/ResponseViewer.jsx";
import EnvironmentBar from "./components/EnvironmentBar.jsx";
import EnvironmentModal from "./components/EnvironmentModal.jsx";
import PromptModal from "./components/PromptModal.jsx";
import SaveRequestModal from "./components/SaveRequestModal.jsx";
import Toasts from "./components/Toasts.jsx";
import { api, API_BASE_URL } from "./lib/api.js";
import {
  draftFromHistory,
  draftFromSavedRequest,
  draftToExecutePayload,
  draftToSavedRequestPayload,
  emptyDraft,
} from "./lib/draft.js";
import "./styles/app.css";

const THEME_KEY = "api-orchestrator.theme";
const ENVIRONMENT_KEY = "api-orchestrator.environment";

function readStoredTheme() {
  try {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored === "light" || stored === "dark") return stored;
  } catch {
    // Storage can be unavailable (private mode, blocked cookies). Fall through.
  }
  if (typeof window !== "undefined" && window.matchMedia) {
    return window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
  }
  return "dark";
}

export default function App() {
  const [theme, setTheme] = useState(readStoredTheme);

  const [collections, setCollections] = useState([]);
  const [environments, setEnvironments] = useState([]);
  const [history, setHistory] = useState([]);
  const [activeEnvironmentId, setActiveEnvironmentId] = useState(null);

  const [draft, setDraft] = useState(emptyDraft);
  const [response, setResponse] = useState(null);
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState(null);

  const [toasts, setToasts] = useState([]);
  const [modal, setModal] = useState(null);
  const [busy, setBusy] = useState(false);

  const draftRef = useRef(draft);
  draftRef.current = draft;
  const environmentRef = useRef(activeEnvironmentId);
  environmentRef.current = activeEnvironmentId;

  // ---- toasts ---------------------------------------------------------

  const dismissToast = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const toast = useCallback(
    (message, tone = "info") => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      setToasts((current) => [...current, { id, message, tone }]);
      setTimeout(() => dismissToast(id), tone === "danger" ? 7000 : 4000);
    },
    [dismissToast],
  );

  // ---- theme ----------------------------------------------------------

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch {
      // Persisting the preference is a convenience, not a requirement.
    }
  }, [theme]);

  // ---- data loading ---------------------------------------------------

  const refreshCollections = useCallback(async () => {
    setCollections(await api.listCollections());
  }, []);

  const refreshEnvironments = useCallback(async () => {
    const loaded = await api.listEnvironments();
    setEnvironments(loaded);
    return loaded;
  }, []);

  const refreshHistory = useCallback(async () => {
    const page = await api.history(0, 50);
    setHistory(page.content ?? []);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [, loadedEnvironments] = await Promise.all([
          refreshCollections(),
          refreshEnvironments(),
          refreshHistory(),
        ]);
        if (cancelled) return;
        try {
          const stored = localStorage.getItem(ENVIRONMENT_KEY);
          if (stored && loadedEnvironments.some((environment) => environment.id === stored)) {
            setActiveEnvironmentId(stored);
          }
        } catch {
          // No stored selection; start with none.
        }
      } catch (error) {
        if (!cancelled) toast(error.message, "danger");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [refreshCollections, refreshEnvironments, refreshHistory, toast]);

  useEffect(() => {
    try {
      if (activeEnvironmentId) localStorage.setItem(ENVIRONMENT_KEY, activeEnvironmentId);
      else localStorage.removeItem(ENVIRONMENT_KEY);
    } catch {
      // Selection simply will not survive a reload.
    }
  }, [activeEnvironmentId]);

  const activeEnvironment = useMemo(
    () => environments.find((environment) => environment.id === activeEnvironmentId) ?? null,
    [environments, activeEnvironmentId],
  );

  // ---- execution ------------------------------------------------------

  const send = useCallback(async () => {
    const current = draftRef.current;
    if (!current.url.trim()) return;

    setSending(true);
    setSendError(null);
    setResponse(null);
    try {
      const result = await api.execute(
        draftToExecutePayload(current, environmentRef.current),
      );
      setResponse(result);
      // An execution may have written extracted variables into the environment,
      // and it always adds a history row.
      await Promise.all([refreshHistory(), refreshEnvironments()]);
    } catch (error) {
      setSendError(error.message);
      toast(error.message, "danger");
    } finally {
      setSending(false);
    }
  }, [refreshEnvironments, refreshHistory, toast]);

  useEffect(() => {
    const onKeyDown = (event) => {
      if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
        event.preventDefault();
        send();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [send]);

  // ---- collection actions ---------------------------------------------

  const guard = useCallback(
    async (action, successMessage) => {
      setBusy(true);
      try {
        const result = await action();
        if (successMessage) toast(successMessage, "success");
        return result;
      } catch (error) {
        toast(error.message, "danger");
        return null;
      } finally {
        setBusy(false);
      }
    },
    [toast],
  );

  const openRequest = (collection, request) => {
    setDraft(draftFromSavedRequest(request, collection.id));
    setResponse(null);
    setSendError(null);
  };

  const replayHistory = async (entry) => {
    setDraft(draftFromHistory(entry));
    setSendError(null);
    const detail = await guard(() => api.historyEntry(entry.id));
    if (detail) {
      setResponse({
        status: detail.status,
        headers: detail.responseHeaders,
        body: detail.responseBody,
        responseTimeMs: detail.responseTimeMs,
        responseSizeBytes: detail.responseSizeBytes,
        bodyTruncated: detail.responseBodyTruncated,
        attempts: detail.attempts,
        errorMessage: detail.errorMessage,
        resolvedUrl: detail.resolvedUrl,
        assertions: detail.assertions,
        assertionsPassed: detail.assertionsPassed,
        extracted: {},
      });
    }
  };

  const importFile = async (file) => {
    let parsed;
    try {
      parsed = JSON.parse(await file.text());
    } catch {
      toast("That file is not valid JSON.", "danger");
      return;
    }
    const result = await guard(() => api.importCollection(parsed), null);
    if (result) {
      await refreshCollections();
      await refreshEnvironments();
      toast(
        `Imported ${result.importedRequests} request${result.importedRequests === 1 ? "" : "s"} into "${result.collection.name}".`,
        "success",
      );
    }
  };

  const exportCollection = async (collection) => {
    const exported = await guard(() => api.exportCollection(collection.id));
    if (!exported) return;
    const blob = new Blob([JSON.stringify(exported, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `${collection.name.replace(/[^\w.-]+/g, "-")}.postman_collection.json`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    toast("Collection exported.", "success");
  };

  const saveRequest = async ({ name, collectionId, overwrite }) => {
    const payload = draftToSavedRequestPayload(draft, name);
    const saved = overwrite
      ? await guard(
          () => api.updateRequest(draft.collectionId, draft.savedRequestId, payload),
          "Request updated.",
        )
      : await guard(() => api.addRequest(collectionId, payload), "Request saved.");
    if (saved) {
      setDraft((current) => ({
        ...current,
        name: saved.name,
        savedRequestId: saved.id,
        collectionId: overwrite ? current.collectionId : collectionId,
      }));
      await refreshCollections();
      setModal(null);
    }
  };

  // ---- render ---------------------------------------------------------

  const environmentVariableCount = Object.keys(activeEnvironment?.variables ?? {}).length;

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="brand__mark" aria-hidden="true" />
          <div>
            <h1 className="brand__name">API Orchestrator</h1>
            <p className="brand__tagline">Postman-lite execution engine</p>
          </div>
        </div>

        <div className="topbar__right">
          <EnvironmentBar
            environments={environments}
            activeId={activeEnvironmentId}
            variableCount={environmentVariableCount}
            onSelect={setActiveEnvironmentId}
            onManage={() => setModal({ type: "environments" })}
          />
          <a
            className="button button--ghost button--sm"
            href={`${API_BASE_URL}/swagger-ui.html`}
            target="_blank"
            rel="noreferrer"
          >
            API docs
          </a>
          <button
            type="button"
            className="icon-button"
            title={theme === "dark" ? "Switch to light" : "Switch to dark"}
            onClick={() => setTheme((current) => (current === "dark" ? "light" : "dark"))}
          >
            {theme === "dark" ? "☀" : "☾"}
          </button>
        </div>
      </header>

      <div className="layout">
        <Sidebar
          collections={collections}
          history={history}
          activeRequestId={draft.savedRequestId}
          onOpenRequest={openRequest}
          onReplayHistory={replayHistory}
          onClearHistory={async () => {
            const done = await guard(() => api.clearHistory(), "History cleared.");
            if (done !== null) await refreshHistory();
          }}
          onCreateCollection={() => setModal({ type: "new-collection" })}
          onRenameCollection={(collection) => setModal({ type: "rename-collection", collection })}
          onDeleteCollection={async (collection) => {
            const done = await guard(
              () => api.deleteCollection(collection.id),
              `Deleted "${collection.name}".`,
            );
            if (done !== null) {
              await refreshCollections();
              if (draft.collectionId === collection.id) {
                setDraft((current) => ({ ...current, collectionId: null, savedRequestId: null }));
              }
            }
          }}
          onDeleteRequest={async (collection, request) => {
            const done = await guard(
              () => api.deleteRequest(collection.id, request.id),
              `Deleted "${request.name}".`,
            );
            if (done !== null) {
              await refreshCollections();
              if (draft.savedRequestId === request.id) {
                setDraft((current) => ({ ...current, savedRequestId: null }));
              }
            }
          }}
          onExportCollection={exportCollection}
          onImportFile={importFile}
        />

        <main className="workspace">
          <RequestBuilder
            draft={draft}
            onDraftChange={setDraft}
            onSend={send}
            onSave={() => setModal({ type: "save-request" })}
            loading={sending}
            response={response}
            hasEnvironment={Boolean(activeEnvironmentId)}
            canSave={collections.length > 0}
          />
          <ResponseViewer response={response} loading={sending} error={sendError} />
        </main>
      </div>

      {modal?.type === "environments" ? (
        <EnvironmentModal
          environments={environments}
          initialId={activeEnvironmentId}
          busy={busy}
          onClose={() => setModal(null)}
          onCreate={async (payload) => {
            const created = await guard(
              () => api.createEnvironment(payload),
              `Created "${payload.name}".`,
            );
            if (created) {
              await refreshEnvironments();
              setActiveEnvironmentId(created.id);
            }
            return created;
          }}
          onUpdate={async (id, payload) => {
            const updated = await guard(() => api.updateEnvironment(id, payload), "Environment saved.");
            if (updated) await refreshEnvironments();
            return updated;
          }}
          onDelete={async (environment) => {
            const done = await guard(
              () => api.deleteEnvironment(environment.id),
              `Deleted "${environment.name}".`,
            );
            if (done !== null) {
              await refreshEnvironments();
              if (activeEnvironmentId === environment.id) setActiveEnvironmentId(null);
            }
          }}
        />
      ) : null}

      {modal?.type === "new-collection" ? (
        <PromptModal
          title="New collection"
          label="Name"
          placeholder="Users API"
          confirmLabel="Create"
          onClose={() => setModal(null)}
          onSubmit={async (name) => {
            const created = await guard(() => api.createCollection({ name }), `Created "${name}".`);
            if (created) {
              await refreshCollections();
              setModal(null);
            }
          }}
        />
      ) : null}

      {modal?.type === "rename-collection" ? (
        <PromptModal
          title="Rename collection"
          label="Name"
          initialValue={modal.collection.name}
          confirmLabel="Rename"
          onClose={() => setModal(null)}
          onSubmit={async (name) => {
            const updated = await guard(
              () =>
                api.updateCollection(modal.collection.id, {
                  name,
                  description: modal.collection.description,
                }),
              "Collection renamed.",
            );
            if (updated) {
              await refreshCollections();
              setModal(null);
            }
          }}
        />
      ) : null}

      {modal?.type === "save-request" ? (
        <SaveRequestModal
          collections={collections}
          draft={draft}
          onClose={() => setModal(null)}
          onSubmit={saveRequest}
        />
      ) : null}

      <Toasts toasts={toasts} onDismiss={dismissToast} />
    </div>
  );
}
