# BoltReset

[日本語](README.ja.md)

A Paper plugin that clears [Bolt](https://modrinth.com/plugin/bolt) block protections in a world after the world has been regenerated.

When you reset a resource world by deleting and regenerating it, Bolt still keeps the old protections for that world name. Players then find chests and doors in the new world locked by protections from the old one. BoltReset removes every block protection in that world from the console, and only after checking that the world really was regenerated.

- Console and RCON only. Players cannot run it.
- Only worlds you list in `config.yml` can be cleared.
- Refuses unless the world's UID has changed to the one you give, so it never clears a world that was not regenerated.
- By default, refuses unless the whitelist is on and nobody is online.
- Prints one machine-readable line (`BOLT_RESET_OK ...` or `BOLT_RESET_ERROR ...`), so reset scripts can check the result.
- Entity protections and Bolt's per-player trust settings are not touched.

## Requirements

- Paper 1.20.6 or later (tested on Paper 26.2 and Paper 26.3)
- Java 21 or later (whatever your server version needs)
- [Bolt](https://modrinth.com/plugin/bolt) 1.2.22 or later

## Installation

1. Download `bolt-reset-1.0.0.jar` from [Releases](https://github.com/spa77k/BoltReset/releases).
2. Put it in your server's `plugins/` folder next to Bolt.
3. Restart the server. `plugins/BoltReset/config.yml` is created.
4. List the worlds you reset under `worlds` and restart the server.

## Commands

Run these from the server console or RCON.

| Command | What it does |
| --- | --- |
| `boltreset status` | Prints `BOLT_RESET_READY` when Bolt is available |
| `boltreset clear <world> <old-uid> <new-uid>` | Clears every block protection in `<world>` |

`<old-uid>` is the world's UID before regeneration and `<new-uid>` is the UID after it. Save the old UID before deleting the world (for example, from `uid.dat` in the world folder), then pass both after the new world is loaded.

### Output

| Output | Meaning |
| --- | --- |
| `BOLT_RESET_OK world=<world> removed=<n> remaining=0` | Cleared and confirmed that no protections are left |
| `BOLT_RESET_ERROR world-not-allowed` | The world is not in `worlds` |
| `BOLT_RESET_ERROR world-not-regenerated` | The UIDs are the same, or the loaded world's UID is not `<new-uid>` |
| `BOLT_RESET_ERROR players-not-blocked` | The whitelist is off or someone is online |
| `BOLT_RESET_ERROR console-only` | Sent by a player |
| `BOLT_RESET_ERROR Bolt-unavailable` | Bolt is not enabled |
| `BOLT_RESET_ERROR remaining=<n>` | Some protections were still there after clearing |

Running `clear` again on the same world succeeds with `removed=0`.

## Configuration

`plugins/BoltReset/config.yml`

| Key | Default | Description |
| --- | --- | --- |
| `worlds` | `[]` | Worlds that may be cleared. Every request is refused while this is empty. |
| `require-whitelist` | `true` | Refuse unless the whitelist is on. |
| `require-empty-server` | `true` | Refuse while any player is online. |

Restart the server to apply changes.

## License

[MIT](LICENSE)
