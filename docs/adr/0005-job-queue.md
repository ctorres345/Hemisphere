---
status: accepted
---

# Our own persisted job queue, run by a foreground service, instead of WorkManager

Copy and move need live per-file progress, pause, cancel, conflicts that wait for the user, and
must survive process death. We keep the queue in Room and run it from a foreground service of
type `dataSync`. The queue logic lives in `core:jobs` and does not know about the service, so the
host can change later.

## Considered options

- **WorkManager with `setForeground`.** Survives process death for free, but pausing,
  re-ordering, waiting for a conflict answer and streaming fine-grained progress all fight its
  model of independent, retryable work. Rejected for the MVP.
- **User-initiated data transfer jobs (Android 14+).** Missing on 8 to 13, and they require a
  network constraint, so they do not fit local copies. They may host network-share jobs in v1.x
  without touching `core:jobs`; local jobs stay on the `dataSync` service.

## Consequences

- Android 15 limits `dataSync` services to 6 hours a day. The service handles `onTimeout` by
  pausing running jobs and stopping itself at once.
- Android's documentation says `dataSync` will be deprecated in a future release. That is why the
  host stays swappable: re-check it at every `targetSdk` bump.
- Google Play requires a declaration for the `dataSync` service type (ADR 0003).
- On Android 13+ the notification needs `POST_NOTIFICATIONS`. The in-app progress chip and queue
  sheet offer every action, so a denied permission breaks nothing.
- After process death, running jobs come back as `Paused`. Nothing resumes without the user.
