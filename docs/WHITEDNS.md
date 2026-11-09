# WhiteDNS in Narcic Getway

Android only. Three engines, each a native executable run as a child process by `WhiteDnsController`:
`libstormdns_client.so`, `libcottendns_client.so` (shipped in `jniLibs`) and `libsoar_client.so` (built from
`tools/soar-engine`, see below).

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
* WhiteDNS's own app-level warm-up / auto-tune loops.

## Soar

Lantern Soar needs its own server and **public key** (not a Master/Cotten server). A Soar connection is a
WhiteDNS profile with engine "Soar": domain = the tunnel zone (one lowercase domain, max 45 characters), key =
the base64 32-byte server public key. `soar://` links import with their resolvers and settings.

* The engine is Go and is **not committed as a binary**. Build it with `tools/build-soar-engine.sh`
  (Go 1.25+, NDK) or let the `soar` job of `.github/workflows/android-natives.yml` do it; the release workflow
  already copies every `native-*` artifact into `jniLibs`. Without it a Soar connection reports that the
  engine is missing; StormDNS and CottenDNS are unaffected.
* Soar starts from the DNS servers of the phone's own network (never the tunnel's) and, with discovery on, finds
  more from its country list. It never receives the bundled StormDNS resolver list. Ranges (CIDR) are not accepted.
* The shared engine page (Settings -> WhiteDNS) does not apply to Soar; its options live in its connection.
* The SOCKS port opens only after the engine has authenticated with the server and passed an HTTPS probe
  (up to the start-up timeout, 90 s by default).
