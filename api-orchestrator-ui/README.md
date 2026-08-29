# api-orchestrator-ui

React + Vite front end for [API Orchestrator](../README.md). It is a thin client:
all execution, variable substitution, assertion evaluation and persistence happen
in the Spring Boot backend.

## Running

```bash
npm install
npm run dev      # http://localhost:5173
```

The dev server expects the backend on `http://localhost:8080`. Point it elsewhere
with `VITE_API_BASE_URL`:

```bash
VITE_API_BASE_URL=http://localhost:9000 npm run dev
```

Vite inlines that value at build time, so a deployed bundle needs a rebuild — not
a restart — to change it.

## Scripts

| Script            | What it does                         |
| ----------------- | ------------------------------------ |
| `npm run dev`     | Dev server with hot module reloading |
| `npm run build`   | Production bundle into `dist/`       |
| `npm run preview` | Serve the built bundle locally       |
| `npm run lint`    | ESLint over the whole project        |

## Layout

```
src/
├── App.jsx                 Application shell: state, data loading, actions
├── main.jsx                Entry point
├── components/
│   ├── Sidebar.jsx           Collections tree and execution history
│   ├── RequestBuilder.jsx    Method, URL, and the params/headers/body/assertions tabs
│   ├── ResponseViewer.jsx    Status, timing, size, body, headers, assertion results
│   ├── CodeBlock.jsx         JSON pretty-printing and syntax highlighting
│   ├── EnvironmentBar.jsx    Environment picker in the top bar
│   ├── EnvironmentModal.jsx  Environment and variable editor
│   ├── KeyValueEditor.jsx    Shared key/value row editor
│   ├── AssertionsEditor.jsx  Assertion declaration with inline pass/fail
│   ├── ExtractionsEditor.jsx JSONPath extraction for request chaining
│   └── Modal.jsx, PromptModal.jsx, SaveRequestModal.jsx, Tabs.jsx, Toasts.jsx
├── lib/
│   ├── api.js              Backend client; turns problem responses into errors
│   ├── draft.js            The in-progress request and its DTO conversions
│   └── format.js           Formatting, JSON tokenising, pair/object helpers
└── styles/
    ├── tokens.css          Design tokens and reset, light and dark palettes
    └── app.css             Layout and component styles
```

There is no CSS framework and no runtime dependency beyond React: the styling is
plain CSS driven by custom properties, and the JSON highlighter is a small
tokeniser in `lib/format.js` that emits elements rather than injected markup.
