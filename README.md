# MCXboxBroadcastAutomaticUpdates
This is a plugin that automatically updates Geyser's MCXboxBroadcastExtension. It supports Spigot, Paper, BungeeCord, and Velocity.

## Supported Servers and Proxies
Spigot, Paper, BungeeCord, Velocity  
Supported Servers and Proxies are those that I have confirmed to be working.

---
## Configuration (en_us)
The language file is located in the `_Latest build_/language` directory within the project; the `en_us` language file is there.  
The default build language is Japanese, so please replace it manually.
```yaml
# ==============================================================================
# MCXboxBroadcastAutoUpdate Configuration File
# Developer: RedCatKnights
# ==============================================================================

auto-update:
  # Whether to automatically check for updates on server startup (true: Enabled / false: Disabled)
  check-on-startup: true

  # Interval for periodic update checks (in hours)
  # Default: Checks once every 5 hours
  check-interval-hours: 5

  # Modrinth Project API URL
  # Endpoint used to retrieve version information for the target plugin/extension.
  modrinth-api-url: "https://api.modrinth.com/v2/project/mcxboxbroadcast/version"

  # Target platform selection ("Automatic", "Geyser-Spigot", "Geyser-Velocity", "Geyser-Bungeecord")
  # If set to "Automatic", it will be automatically determined based on the running server platform.
  platform: "Automatic"

  # Path to the target JAR file to be updated
  # %automatic% will be replaced with the folder name based on the platform setting (or auto-detected result) above.
  target-jar-path: "plugins/%automatic%/extensions/MCXboxBroadcastExtension.jar"

restart-settings:
  # Whether to automatically restart/stop the server after the update download completes (true: Enabled / false: Disabled)
  auto-restart: true

  # Countdown announcement timing when stopping the server (list of seconds)
  # Warning messages will be broadcast in-game and to the console at the specified seconds.
  # Including 0 or setting an empty list [] will shut down immediately without a countdown.
  announcements: [120, 60, 30, 10, 5, 4, 3, 2, 1]

  # Whether to use a custom shutdown command (true: Use custom command / false: Execute standard shutdown procedure)
  use-custom-shutdown-command: false

  # Console command executed when use-custom-shutdown-command is set to true
  # Example: Can be set to commands for integration with external restart scripts
  custom-shutdown-command: "stop"
``` 
---
## Language (en_us)
```yaml
prefix: "§8[§bMCXboxBroadcastAutoUpdate§8] "

messages:
  checking: "%prefix%§7Checking for updates..."
  latest-version: "%prefix%§aThe plugin is already up to date."
  update-found: "%prefix%§eA new version was found (v%remote_version%). Starting download..."
  download-success: "%prefix%§aSuccessfully downloaded the latest version. Restarting to apply the update."
  download-success-manual: "%prefix%§aSuccessfully downloaded the latest version. Please restart the server manually to apply the update."
  download-failed: "%prefix%§cFailed to download the update file."
  announcement: "%prefix%§eThe server will restart in %seconds% seconds to apply updates."
  stopping: "%prefix%§cStopping/Restarting the server..."
  target-jar-missing: "%prefix%§eTarget JAR file not found. Starting initial download: %path%"
  fetch-latest-failed: "%prefix%§cFailed to fetch the latest version."
  initial-download-failed-remote: "%prefix%§cInitial download failed (Failed to fetch remote version)."
  initial-download-success: "%prefix%§aInitial JAR file download completed: %file%"
  initial-download-failed: "%prefix%§cFailed to perform initial JAR file download."
  config-load-failed: "%prefix%§cFailed to load config.yml: %error%"
  lang-load-failed: "%prefix%§cFailed to load language.yml: %error%"
  default-copy-failed: "%prefix%§cFailed to copy default file: %resource%"
```
---

## ! ! Important Notes ! !
- Please note that this code was created and modified with the assistance of the AI, Gemini.
- I have basic programming knowledge, but I am unable to develop code entirely from scratch, so I am very grateful for Gemini's help.
- Please be aware that this code has not been tested in any environment other than my own.

---

## Future Updates & Maintenance
- Future updates and maintenance for this code are not guaranteed.
- If the plugin stops working due to future updates, it may not be fixed.
- Bug reports are not actively supported, but I might look into them if time permits or if I feel like it.
- This code is published primarily as a personal note/backup, and for anyone who might be looking for something similar.

---

  
