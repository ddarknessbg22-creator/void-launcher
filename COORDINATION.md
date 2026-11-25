# VOID multi-agent coordination

Two agents work in parallel. These boundaries are mandatory.

## Ownership

| Agent | Workspace | Owns | Must not touch |
| --- | --- | --- | --- |
| A — Launcher agent | workspace 1 (this chat) | `ddarknessbg22-creator/void-launcher` | `void-launcher-engine` |
| B — Engine agent | fresh chat/workspace | `ddarknessbg22-creator/void-launcher-engine` | `void-launcher` |

Both agents work on `main` of their own repo. Neither agent pushes to the other repo.
If you need a change in the other repo, write `CROSS_AGENT_REQUESTS.md` in your own repo and tell the user. Do not edit the other repo.

## Device lock (mandatory)

Only one agent may control the phone at a time. Phone control includes `pm install`,
`am start`, `am startservice`, `input`, `screencap`, `uiautomator`, `dumpsys`, and
`read_logcat` for device diagnostics.

Shared lock path: `/sdcard/void-agent-device.lock`

Acquire before phone control:

```sh
if mkdir /sdcard/void-agent-device.lock 2>/dev/null; then
  echo "$(date -u +%FT%TZ) AGENT_NAME" > /sdcard/void-agent-device.lock/owner
else
  echo "Device busy: $(cat /sdcard/void-agent-device.lock/owner 2>/dev/null)"
fi
```

Release only if you own it:

```sh
grep -q AGENT_NAME /sdcard/void-agent-device.lock/owner 2>/dev/null && \
  rm -rf /sdcard/void-agent-device.lock
```

If the lock exists, keep doing non-device work. Do not delete someone else's lock.

## Build ownership

- Agent A: local Gradle builds/tests for `void-launcher`; produce the launcher APK.
- Agent B: GitHub Actions `Build CI` for `void-launcher-engine`; produce the engine APK.
- Neither agent triggers or edits the other repo's CI.

## Filesystem

- Agent A's workspace: current workspace 1.
- Agent B must use its own fresh workspace for all downloads and build outputs.
- Do not write into another agent's workspace.

## Visual verification

The phone was pattern-locked. Neither agent attempts to bypass it. Ask the user to unlock
before any screenshot-based gameplay verification.

## Balanced assignments (equal priority)

Agent A — Launcher agent (this chat):
- Own `void-launcher` repo and `VOIDLauncher/` files only.
- Keep launcher building/tests passing; produce launcher APK + SHA-256.
- Finish engine detection, VOID Engine Download button, handoff from Launch, account/version/mods/Java flows.
- Fix launcher-side bugs, remove dead UI/code, update README/VERIFICATION.md.
- Do not edit `void-launcher-engine` or its CI.

Agent B — Engine agent (fresh chat):
- Own `void-launcher-engine` repo and `VOIDEngine/` files only.
- Verify/fix automatic first-run component extraction; rebuild via existing `Build CI`.
- Remove remaining runtime Zalith branding; keep GPL notice and attribution.
- Produce engine APK + SHA-256; install and attempt real Minecraft launch when device is unlocked.
- Report exact blockers (JRE, LWJGL, GL renderer, first-run UI) from logcat/screenshots.
- Do not edit `void-launcher` repo.

Neither assignment is subordinate. Both must commit/push their own repo and report honest status.

## Shared goal

Single installable VOID app that actually launches Minecraft. Until the native engine
can be merged safely, VOID Engine is the game app and VOID Launcher is the config/handoff app.
Report VERIFIED / BUILT-NOT-DEVICE-VERIFIED / BLOCKED / NOT IMPLEMENTED honestly.
