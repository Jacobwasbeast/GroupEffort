# GroupEffort

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-blue)](https://www.minecraft.net)
[![Requires Fabric API](https://img.shields.io/badge/Requires-Fabric%20API-orange)](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
[![Requires Cloth Config](https://img.shields.io/badge/Requires-Cloth%20Config-blueviolet)](https://www.curseforge.com/minecraft/mc-mods/cloth-config)
[![CurseForge](https://img.shields.io/badge/CurseForge-Group%20Effort-red)](https://www.curseforge.com/minecraft/mc-mods/group-effort)

**GroupEffort** is a server-side Fabric mod for Minecraft 1.20.1, designed to encourage cooperative play by making server activity contingent on a minimum number of online players. When the player count falls below a configurable threshold, the server enters a restricted "limbo" state until enough players join the effort!

## Core Concept

GroupEffort aims to make your server truly come alive when a community gathers. Instead of a quiet server, it will wait for a quorum of players before fully unlocking its features. This can foster a sense of community, encourage players to join together, and can also help manage server resources during off-peak hours by restricting non-essential activities.

## Features

* **Minimum Player Quota:** Configure the number of players required for the server to be fully active.
* **Configurable Limbo States:** Choose how players experience the server when the quota isn't met:
    * **`THE_VOID`**: Players are visually teleported very high up. The client renders The End skybox (if its installed on the client), creating a distinct visual void experience, regardless of the player's actual current dimension. The world below is hidden by server-side packet filtering.
    * **`LOCALIZED_FREEZE`**: Players remain visually where they are when limbo activates.
* **Movement & Interaction Blocking:** In either limbo state, all player movement, block interactions, and entity interactions are blocked.
* **Grace Period:** Set a configurable grace period after the player count drops, allowing time for others to join before restrictions apply.
* **Customizable Chat Messages:** Tailor all mod-related messages with color codes and placeholders.
* **Configurable Chat Blocking:** Optionally block chat messages for players who are in a limbo state.
* **Operator Commands:** Ops can adjust key settings on-the-fly without server restarts.
* **Server-Side Core Logic:** The primary logic operates on the server.

## Limbo Types Explained

When the minimum player quota is not met (and the grace period, if enabled, has expired), online players will be placed into a "limbo" state.

* **`THE_VOID`**:
    * The player is visually teleported to a very high Y-coordinate by the server.
    * The client will render **The End skybox**, providing a distinct "void" atmosphere, as if they are in The End void.
    * The mod also hides the world below them by filtering outgoing server-to-client packets.
    * All player movement and interactions are blocked. Player abilities are modified via packets to prevent falling and restrict movement.

* **`LOCALIZED_FREEZE`**:
    * Players remain visually at their exact location when limbo activates. No visual teleportation occurs.
    * All player movement and interactions are blocked. Player abilities are modified via packets to prevent movement.
    * Players will see their surroundings. Whether these surroundings continue to update (e.g., other entities moving) depends on whether server ticks for their area are also paused by the mod's tick-stopping mechanisms.

## Installation

1.  Ensure you have the [Fabric Loader](https://fabricmc.net/use/) (for Minecraft 1.20.1) installed on your server.
2.  Download the **GroupEffort** mod `.jar` file from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/group-effort).
3.  Place the mod `.jar` file into your server's `mods` folder.
4.  **Dependencies:** You **MUST** also install the following in the `mods` folder:
    * [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) (for Minecraft 1.20.1)
    * [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) (for configuration; specifically `cloth-config-fabric`)
5.  For easy configuration, it's recommended that server admins (or clients connecting to a server that syncs configs) have [ModMenu](https://www.curseforge.com/minecraft/mc-mods/modmenu) installed on their client.

## Configuration

GroupEffort's settings can be configured via:

* **ModMenu:** If you have ModMenu installed on a client (and the server's config is accessible or synced), you can configure it through the in-game Mods menu.
* **JSON File:** Directly edit the configuration file located at `config/groupeffort.json` in your server's directory after running the server with the mod at least once.

**Key Configuration Options (see `GroupEffortConfig.java` for details):**

* **General Settings:**
    * `minimumPlayers`: The number of online players required for the server to be fully active (default `2`, 0 to effectively disable quota).
    * `blockChatMessages`: If `true`, players in limbo cannot send chat messages (default `false`).
    * `limboType`: Choose between `THE_VOID` or `LOCALIZED_FREEZE` (default `THE_VOID`).
* **Grace Period:**
    * `enabled`: Set to `true` or `false` to enable/disable the grace period (default `true`).
    * `durationSeconds`: How long the grace period lasts in seconds (default `180`).
* **Chat Messages:**
    * Customize all messages for events like player join below minimum, grace period start/end, limbo entry/exit, etc.
    * Supports standard Minecraft color codes (e.g., `&c` for red).
    * Placeholders (e.g., `%player%`, `%needed%`, `%current%`, `%minimum%`, `%duration%`) are available.

## Operator Commands

Server operators (default permission level 2) can modify settings in-game.
The base command is `/groupeffort`. *(You might want to add an alias like `/ge` for convenience).*

* `/groupeffort set minimumPlayers <count>`
    * Example: `/groupeffort set minimumPlayers 3`
* `/groupeffort set limboType <THE_VOID | LOCALIZED_FREEZE>`
    * Example: `/groupeffort set limboType LOCALIZED_FREEZE`
* `/groupeffort set blockChatMessages <true | false>`
    * Example: `/groupeffort set blockChatMessages true`
* `/groupeffort set graceEnabled <true | false>`
    * Example: `/groupeffort set graceEnabled true`
* `/groupeffort set graceDuration <seconds>`
    * Example: `/groupeffort set graceDuration 120`
* `/groupeffort status`: Displays the current key settings of the mod.

*(Command arguments for counts/durations respect the bounds defined in the configuration annotations).*

## Dependencies

* **Fabric API** (Required)
* **Cloth Config API (via AutoConfig)** (Required for configuration)
* **ModMenu** (Client-side, Optional, for in-game config screen)

## How It Works (Briefly)

GroupEffort is primarily a server-side mod that monitors the online player count.
* When the count is below `minimumPlayers` (after any grace period), it activates "limbo":
    * Player actions (movement, interaction, optionally chat) are blocked by cancelling their incoming Client-to-Server (C2S) packets.
    * Player abilities are modified via Server-to-Client (S2C) packets to restrict movement and, for `THE_VOID`, to enable flight to prevent falling from the visual high Y-coordinate.
    * For `THE_VOID`, the client is visually teleported high up in their current dimension, and S2C packets that would render the world (chunks, entities) are filtered out to achieve the "sky-only" view, which presents as The End skybox.
    * For `LOCALIZED_FREEZE`, the player's position is reinforced via packets, and abilities are set to prevent movement.
    * Server game ticks for worlds may also be restricted when the server is in a global limbo state to save resources.
* When the player quota is met, these restrictions are lifted, and players are restored from limbo.

## Reporting Bugs & Issues

If you encounter any bugs or unexpected behavior, please report them on the [GitHub Issues page](https://github.com/jacobwasbeast/groupeffort/issues).
Please include:
* Minecraft version (1.20.1)
* Fabric Loader version
* GroupEffort mod version
* Fabric API version
* Cloth Config API version
* List of other mods (if any)
* A clear description of the issue and steps to reproduce it.
* Any relevant logs or crash reports (use a service like [Pastebin](https://pastebin.com/)).

## Contributing

Contributions are welcome! Please fork the repository, make your changes, and submit a Pull Request on GitHub.
