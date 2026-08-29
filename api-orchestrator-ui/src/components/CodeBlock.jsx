import { useMemo, useState } from "react";
import { prettyPrint, tokenizeJson } from "../lib/format.js";

/**
 * Renders a response body. JSON is pretty-printed and syntax-highlighted with
 * real elements rather than injected markup, so a response can never smuggle
 * HTML into the page.
 */
export default function CodeBlock({ content, emptyLabel = "No content" }) {
  const [raw, setRaw] = useState(false);
  const [copied, setCopied] = useState(false);

  const { text, isJson } = useMemo(() => prettyPrint(content), [content]);
  const tokens = useMemo(
    () => (isJson && !raw ? tokenizeJson(text) : null),
    [isJson, raw, text],
  );

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(content ?? "");
      setCopied(true);
      setTimeout(() => setCopied(false), 1400);
    } catch {
      setCopied(false);
    }
  };

  if (!content) {
    return <div className="empty empty--inline">{emptyLabel}</div>;
  }

  return (
    <div className="code">
      <div className="code__toolbar">
        {isJson ? (
          <button type="button" className="chip" onClick={() => setRaw((value) => !value)}>
            {raw ? "Pretty" : "Raw"}
          </button>
        ) : (
          <span className="chip chip--static">Plain text</span>
        )}
        <button type="button" className="chip" onClick={copy}>
          {copied ? "Copied" : "Copy"}
        </button>
      </div>
      <pre className="code__pre">
        <code>
          {tokens
            ? tokens.map((token, index) => (
                <span key={index} className={`tok tok--${token.type}`}>
                  {token.text}
                </span>
              ))
            : raw
              ? content
              : text}
        </code>
      </pre>
    </div>
  );
}
