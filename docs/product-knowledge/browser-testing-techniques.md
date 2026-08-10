# Browser testing techniques that work on this SPA

**Verified:** 2026-08-10, QA V1.36. These are the manoeuvres that let us test things the UI
does not expose — and the disciplines that stop a test from lying.

## 1. Control-first: prove the thing CAN appear before proving it doesn't

The single most valuable habit here. Before asserting "the invite is suppressed during a job",
first prove the invite **appears** in the same place with the job condition removed. Otherwise
"not visible" may simply mean "never visible", and the test passes for the wrong reason.

This is not hypothetical. Testing PR #1127, the Dashboard invite appeared suppressed under a
running job — and also under **no** job. The gate was unobservable there; the "passing" check
was vacuous. Only the control exposed it.

**Rule:** every negative assertion needs a positive control in the same session, same site,
same page.

## 2. A request recorder that survives SPA navigation

`performance.getEntriesByType('resource')` gives you URLs only — useless when scope travels in
a **POST body**. Wrap `fetch` and `XMLHttpRequest` instead:

```js
window.__cap = [];
const of = window.fetch;
window.fetch = function (input, init) {
  const url = typeof input === 'string' ? input : (input && input.url) || '';
  const body = (init && init.body) || null;
  if (String(url).includes('/api/')) window.__cap.push({url: String(url), body});
  return of.apply(this, arguments);
};
// same idea for XMLHttpRequest.prototype.open/send
```

**The catch:** a full page load (`driver.get`) wipes the shim, and the request you care about
fires during that load. Solution: install the shim once, then move **within the SPA** by
clicking sidebar anchors — the document never reloads, so the shim survives:

```js
document.querySelector("a[href='/opportunities']").click();
```

This is also a more honest test: it is the path a real user takes.

## 3. Fault injection on a single endpoint

Same wrapper, but return a synthetic response for one URL. Switch behaviour at runtime through
a global so you can exercise several cases without reinstalling:

| Mode | Implementation | Tests |
|---|---|---|
| stub status | resolve a `Response` with your own JSON | job `running` / `pending` |
| network failure | `Promise.reject(new TypeError('Failed to fetch'))` | fail-open behaviour |
| server error | resolve a `Response` with `{status: 500}` | fail-open behaviour |
| slow answer | resolve inside `setTimeout` | **cross-site race conditions** |
| pass-through | delegate to the original `fetch` | the control |

A *delayed* response is what makes race guards testable: give site A a 2.5 s answer, switch to
site B immediately, and check B never inherits A's answer.

## 4. Switching sites programmatically

React ignores `input.value = x`. Use the native setter, then dispatch `input`:

```js
const inp = document.querySelector("input[placeholder='Select facility']");
const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
inp.focus(); setter.call(inp, 'Test without location');
inp.dispatchEvent(new Event('input', {bubbles: true}));
// then: document.querySelector('li[role="option"]').click()
```

Gotchas: typing the **currently selected** site yields no dropdown option (nothing to pick);
and duplicate site names exist, so the first option may not be the site you meant.

## 5. Remounting a component to re-trigger its fetch

Data fetches fire on mount. To re-run one under new injected conditions, navigate away and back
inside the SPA (`/issues` → `/assets`). Cheaper and more reliable than a reload, and it keeps
the shim alive.

## 6. Never assert on an empty capture

If the route's own data call never arrives, a "no scoped requests" assertion passes trivially.
Always assert the expected endpoint **was observed** before judging its content — otherwise a
broken page reads as a clean pass. `ForcedAllPagesScopeTestNG` fails loudly in that case.

## 7. Reading CI job logs

GitHub job logs often contain bytes that make `grep` treat them as **binary and skip silently**
(you get no output and no error). Use Python to search them, or `grep -a`. This cost real time
during an investigation where "no matches" looked like "the thing never happened".
