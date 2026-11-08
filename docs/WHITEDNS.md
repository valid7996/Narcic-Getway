# WhiteDNS in Narcic Getway

Android only. Two engines, both shipped as native executables (`libstormdns_client.so`,
`libcottendns_client.so`) and run as a child process by `WhiteDnsController`.

## Where things are

| What | Where |
|---|---|
| Connection model (one server) | `domain/config/WhiteDnsProfile.kt` |
| Shared engine settings + clamping | `domain/config/WhiteDnsSettings.kt` |
| Engine TOML, config text <-> form, `stormdns://` / `cottendns://` import | `domain/config/WhiteDnsConfig.kt` |
| Connection editor (form or config text) | `ui/servers/WhiteDnsSheet.kt` |
| Settings page (every shared value + config editor + log button) | `ui/settings/WhiteDnsSettingsPage.kt` |
| Process, readiness, log mirroring | `app/.../core/WhiteDnsController.kt` |
| Start / stop / health | `NrgVpnService` (`startWhiteDns`), `StartPlanner` |
| Log | `LogCategory.WhiteDns` (tags `WhiteDns`, `WhiteDnsController`) |

## Config editing

* **Connection** ("Edit as config" in the sheet): `KEY = value` lines for the form's fields. Known keys fill
  the form, any other key is kept as an *engine override* and replaces the generated value of the same key.
* **Shared settings** (Settings -> WhiteDNS -> Edit engine config): real engine keys, one to one with the page.
  Keys that Narcic or a connection owns (`LISTEN_*`, `SOCKS5_*`, `LOCAL_DNS_*`, `DOMAINS`, ...) are reported
  as ignored.

## Resolver order

1. App-wide DNS override (Settings -> DNS), if on
2. The connection's own list
3. The WhiteDNS list in Settings -> WhiteDNS
4. The list that ships with the app (`whitedns_default_resolvers.txt`)

Only IP / IP:port / CIDR entries are used.

## Not included

* Desktop: the engines have no desktop build, so a WhiteDNS profile reports an error there.
* Cross-chain: WhiteDNS cannot be a carrier or dialer in a chain.
* Soar: WhiteDNS's Soar engine is not part of this port (its binary is not in the WhiteDNS sources).
* WhiteDNS's own app-level warm-up / auto-tune loops.
