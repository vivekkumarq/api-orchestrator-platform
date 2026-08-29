# Contributing

Thanks for taking a look. This is a portfolio project, so the bar is simple:
changes should keep the build green and the README honest.

## Getting set up

You need **JDK 17** and **Node.js 20+**. Maven comes from the wrapper in the
repository, so there is nothing else to install.

```bash
git clone https://github.com/vivekkumarq/api-orchestrator-platform.git
cd api-orchestrator-platform

# backend
cd api-orchestrator && ./mvnw spring-boot:run

# frontend, in another terminal
cd api-orchestrator-ui && npm install && npm run dev
```

## Before you open a pull request

Both of these must pass. CI runs exactly the same commands.

```bash
cd api-orchestrator     && ./mvnw -B clean verify
cd api-orchestrator-ui  && npm run lint && npm run build
```

Do not reach for `-DskipTests` to get a green build. If a test is wrong, fix the
test and say why in the commit message.

## Project conventions

**Backend**

- Plain constructor injection. Lombok is on the classpath but the code does not
  use it for DTOs or entities — explicit getters and setters keep the JSON
  contract obvious.
- Services own the logic; controllers only bind HTTP to a service call and add
  OpenAPI annotations.
- Entities never leave the controller layer. Map to a DTO.
- New configuration goes in `AppProperties` under the `app.*` prefix, with a
  default in `application.yaml` that is overridable by an environment variable,
  and a row in the README's configuration table plus `.env.example`.
- Anything that touches the executor needs a MockWebServer test. That class is
  the heart of the application.

**Frontend**

- Components are presentational; data loading and mutations live in `App.jsx`.
- No new runtime dependencies without a good reason. The UI deliberately ships
  React and nothing else.
- Styling is plain CSS using the custom properties in `styles/tokens.css`. If
  you add a colour, add it to both the light and dark palettes.
- Never build markup from a response body. The JSON highlighter emits React
  elements precisely so a third-party response cannot inject HTML.
- Guard every `localStorage` access — it throws in some privacy modes.

## Commit messages

Conventional commits: `feat:`, `fix:`, `test:`, `docs:`, `chore:`, `build:`,
`refactor:`. Explain *why* in the body when the change is not obvious. Scope
with the affected half where it helps — `feat(backend):`, `feat(ui):`.

## The honesty rule

The README documents only what exists and works. If you add a feature, add it to
the README; if you find something documented that does not work, fix one or the
other. Anything unverified — the Docker images, at the time of writing — must
say so where it is documented.

## Reporting a bug

Include the request you sent, what came back, and what you expected. For an
execution problem, the `resolvedUrl`, `status`, `attempts` and `errorMessage`
fields from the response, plus the relevant history entry, are usually enough to
reproduce it.

## Security

The application makes HTTP requests to URLs the caller supplies, which is
deliberate. Read the [security section](README.md#security-this-tool-fetches-urls-you-give-it)
before reporting SSRF-shaped findings — the exposure is known and documented,
and the useful reports are ones that defeat the configured policy (for example,
reaching a private address while `ALLOW_PRIVATE_NETWORKS` is `false`). Please
report those privately rather than in a public issue.
