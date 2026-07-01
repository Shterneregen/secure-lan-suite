# Compose Decision Trees

## MVI or MVVM

Use existing project convention.

If no convention exists:
- Complex screen, many events, strict state transitions → MVI.
- Simple form or settings screen → MVVM is acceptable.
- Team already uses one pattern → follow team pattern.

## StateFlow or SharedFlow

- Durable UI state → StateFlow.
- One-shot navigation/snackbar/toast → Channel or SharedFlow.
- Continuous event stream from external source → Flow.
- User input field value → StateFlow-backed state or local state depending persistence needs.

## ViewModel or Local State

Use ViewModel for:
- business-significant state;
- persisted state;
- network/persistence results;
- validation state;
- state needed after navigation/recomposition;
- data shared across children.

Use local state for:
- focus;
- scroll;
- transient expansion;
- hover;
- animation progress;
- temporary visual-only toggles.

## Column or LazyColumn

Use Column when:
- item count is small and fixed;
- content does not need virtualization.

Use LazyColumn when:
- list can grow;
- messages, files, peers, logs, or search results are shown;
- stable item keys exist.

## Dialog or Inline UI

Use inline UI for:
- validation;
- transfer progress;
- recoverable errors;
- contextual actions.

Use dialog for:
- destructive confirmation;
- permission request;
- blocking required decision.

Do not use dialogs for normal navigation.

## CommonMain or Platform Source Set

Put in commonMain:
- business logic;
- presentation state;
- validation;
- formatters;
- pure reducers;
- interfaces.

Use platform source sets for:
- platform APIs;
- file pickers;
- notifications;
- camera/microphone;
- OS integration;
- platform-specific storage.

## Add Dependency or Write Small Code

Add dependency only when:
- it solves a real recurring problem;
- target support is verified;
- API is stable enough;
- maintenance cost is justified.

Write small code when:
- the need is local;
- dependency would be heavier than the solution;
- target support is unclear.
