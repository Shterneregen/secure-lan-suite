# Desktop tray lifecycle

The primary Compose desktop client owns one long-lived application session. Hiding the main window
does not stop discovery, room hosting, chat, transfers, quick share, or realtime-media services.
Only the explicit **Exit SecureLanSuite** command performs adapter shutdown and exits the process.

## Application setting

Close-button behavior is stored with the other user preferences in the shared application settings:

```properties
# <application directory>/config/settings.properties
window.keepRunningAfterClose=true
```

The values are:

- `true` — save window geometry and hide the main window in the tray;
- `false` — save window geometry, stop services, and exit the application.

The setting defaults to `true` and is exposed as **Keep running after window closes** both in General
Settings and in the tray menu. Both controls update the same `DesktopAppSettingsController`, so a
change is applied immediately and persisted through the normal settings store.

If the operating system does not support a system tray, a minimize request safely falls back to
normal application exit. This prevents an invisible process that the user cannot reopen.

## Tray menu

The native, operating-system-styled menu is deliberately concise and lifecycle-focused:

1. a read-only live status (`Idle`, `Hosting a room`, `Connected to a room`, or both);
2. an actionable unread-message count when messages are waiting;
3. a contextual open/bring-to-front action, also bound to the platform tray-icon action;
4. **Hide main window**, disabled while the window is already hidden;
5. persisted toggles for chat notifications and close-button behavior;
6. **Exit SecureLanSuite**, the only tray command that shuts down services.

The menu receives a small immutable state object and callbacks. It does not own the window or network
services, so later quick actions (disconnect, stop hosting, pause discovery) can be added as explicit
commands without moving application-lifecycle logic into the UI menu.

## Chat notifications

Remote user messages create a platform tray notification containing the sender and a compact,
single-line preview. Notifications are suppressed while the main window is focused and are not
created for presence/system messages or messages echoed from the local user. The tray tooltip and
menu show the unread count until the main window regains focus.

The notification switch is available both in the tray and on the Notifications settings page. The
existing sound preference controls whether chat notifications request an informational sound from
the operating system.

## Lifecycle invariants

- Closing or hiding always persists the last window geometry.
- Hiding never calls `ComposeDesktopHostAdapter.shutdown()`.
- Explicit exit is idempotent because adapter shutdown already guards repeated calls.
- Restore clears the minimized state, makes the window visible, raises it, and requests focus.
- The tray is composed only when the current platform reports tray support.
