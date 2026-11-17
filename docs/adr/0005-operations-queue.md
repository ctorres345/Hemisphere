---
status: proposed
---

# Our own persisted queue, run by a foreground service, instead of WorkManager

Copy and move need live per-file progress, pause, cancel, conflicts that wait for the user, and
must survive process death. We keep the queue in Room and run it from a foreground service of
type `dataSync`. The queue logic lives in `core:ops` and does not know about the service, so the
host can change later.

## Considered options

- **WorkManager with `setForeground`.** Survives process death for free, but pausing,
  re-ordering, waiting for a conflict answer and streaming fine-grained progress all fight its
  model of independent, retryable work. Rejected for the MVP.
- **User-initiated data transfer jobs (Android 14+).** The recommended API for this on new
  Android, but missing on 8 to 13. Can replace the service host on 14+ later without touching
  `core:ops`.

## Consequences

- Android 15 limits `dataSync` services to 6 hours a day. When the system stops the service,
  running jobs move to `Paused` with a notification.
- After process death, running jobs come back as `Paused`. Nothing resumes without the user.
