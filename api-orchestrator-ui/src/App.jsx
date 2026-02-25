import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const [url, setUrl] = useState("");
  const [method, setMethod] = useState("GET");
  const [headers, setHeaders] = useState("{}");
  const [body, setBody] = useState("");
  const [timeoutMs, setTimeoutMs] = useState(3000);

  const [response, setResponse] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadHistory = async () => {
    try {
      const res = await fetch("http://localhost:8080/requests/history");
      const data = await res.json();
      setHistory(data.reverse()); // latest first
    } catch (e) {
      console.error("Failed to load history", e);
    }
  };

  useEffect(() => {
    loadHistory();
  }, []);

  const executeRequest = async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    let parsedHeaders = {};
    try {
      parsedHeaders = headers ? JSON.parse(headers) : {};
    } catch (e) {
      setError("Headers must be valid JSON");
      setLoading(false);
      return;
    }

    const payload = {
      url,
      method,
      headers: parsedHeaders,
      body: body || null,
      timeoutMs: Number(timeoutMs),
    };

    try {
      const res = await fetch("http://localhost:8080/requests/execute", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      setResponse(data);
      await loadHistory(); // refresh history after execution
    } catch (err) {
      setError("Failed to call backend: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: "20px", maxWidth: "1100px", margin: "0 auto" }}>
      <h1>API Orchestrator (Postman Lite)</h1>

      <div style={{ display: "flex", gap: "30px" }}>
        {/* Left Panel */}
        <div style={{ flex: 1 }}>
          <h2>Request</h2>

          <div style={{ marginBottom: "10px" }}>
            <label>URL:</label>
            <input
              style={{ width: "100%" }}
              value={url}
              onChange={(e) => setUrl(e.target.value)}
            />
          </div>

          <div style={{ marginBottom: "10px" }}>
            <label>Method:</label>
            <select value={method} onChange={(e) => setMethod(e.target.value)}>
              <option>GET</option>
              <option>POST</option>
              <option>PUT</option>
              <option>DELETE</option>
            </select>
          </div>

          <div style={{ marginBottom: "10px" }}>
            <label>Headers (JSON):</label>
            <textarea
              rows={4}
              style={{ width: "100%" }}
              value={headers}
              onChange={(e) => setHeaders(e.target.value)}
            />
          </div>

          <div style={{ marginBottom: "10px" }}>
            <label>Body:</label>
            <textarea
              rows={6}
              style={{ width: "100%" }}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          </div>

          <div style={{ marginBottom: "10px" }}>
            <label>Timeout (ms):</label>
            <input
              type="number"
              value={timeoutMs}
              onChange={(e) => setTimeoutMs(e.target.value)}
            />
          </div>

          <button onClick={executeRequest} disabled={loading}>
            {loading ? "Sending..." : "Send Request"}
          </button>

          {error && <p style={{ color: "red" }}>{error}</p>}

          {response && (
            <div style={{ marginTop: "20px" }}>
              <h3>Response</h3>
              <p><b>Status:</b> {response.status}</p>
              <p><b>Time:</b> {response.responseTimeMs} ms</p>
              <pre>{response.body}</pre>
            </div>
          )}
        </div>

        {/* Right Panel */}
        <div style={{ flex: 1 }}>
          <h2>History</h2>
          <div style={{ maxHeight: "600px", overflowY: "auto" }}>
            {history.map((h) => (
              <div
  key={h.id}
  className="history-card"
  onClick={() => {
    setUrl(h.url);
    setMethod(h.method);
    setHeaders(h.requestHeaders || "{}");
    setBody(h.requestBody || "");
  }}
>
                <div><b>{h.method}</b> {h.url}</div>
                <div>Status: {h.status}</div>
                <div>Time: {h.responseTimeMs} ms</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default App; 