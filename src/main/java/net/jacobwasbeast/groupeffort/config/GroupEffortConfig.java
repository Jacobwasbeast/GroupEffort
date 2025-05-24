package net.jacobwasbeast.groupeffort.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "groupeffort")
public class GroupEffortConfig implements ConfigData {

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.TransitiveObject
    public GeneralSettings generalSettings = new GeneralSettings();

    @ConfigEntry.Category("gracePeriod")
    @ConfigEntry.Gui.TransitiveObject
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = false)
    public GracePeriodSettings gracePeriod = new GracePeriodSettings();

    @ConfigEntry.Category("chatMessages")
    @ConfigEntry.Gui.TransitiveObject
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = false)
    public ChatMessageSettings chatMessages = new ChatMessageSettings();

    public static class GeneralSettings {
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
        public int minimumPlayers = 2;

        @ConfigEntry.Gui.Tooltip
        public boolean blockChatMessages = false;

        @ConfigEntry.Gui.Tooltip
        public LimboType limboType = LimboType.THE_VOID;
    }

    public static class GracePeriodSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 0, max = 3600)
        public int durationSeconds = 180;

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.Gui.PrefixText
        public String startMessage = "&ePlayer count has dropped below the minimum! Grace period of %duration% seconds has started before restrictions apply.";

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.Gui.PrefixText
        public String endMessageRestrictionsActive = "&cGrace period ended. Minimum player count not met. Server restrictions are now active.";
    }

    public static class ChatMessageSettings {
        @ConfigEntry.Gui.Tooltip(count = 3)
        @ConfigEntry.Gui.PrefixText
        public String playerJoinBelowMinimum = "&eWelcome, %player%! We need &a%needed%&e more player(s) to activate the server. Currently: &a%current%&e/&a%minimum%&e.";

        @ConfigEntry.Gui.Tooltip(count = 3)
        @ConfigEntry.Gui.PrefixText
        public String waitingForPlayersPeriodic = "&eThe server is waiting for &a%needed%&e more player(s). Current: &a%current%&e/&a%minimum%&e.";

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.Gui.PrefixText
        public String minimumPlayersMet = "&aMinimum player count of %minimum% has been reached! Server restrictions lifted. Enjoy!";

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.PrefixText
        public String playerEntersLimbo = "&cYou have been placed into limbo until enough players are online.";

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.PrefixText
        public String playerLeavesLimbo = "&aYou have been returned from limbo as the server is now active.";
    }

    public enum LimboType {
        /**
         * Player is visually teleported high in their current dimension.
         * World rendering below them is then hidden by S2C packet filtering.
         * Movement and interactions are blocked.
         */
        THE_VOID,

        /**
         * Player remains visually where they are (no teleportation packet sent to change their perceived dimension or major position).
         * Movement and interactions are blocked. Client sees their surroundings (if server ticks allow).
         * S2C packet filter for hiding the world is NOT active for this type.
         */
        LOCALIZED_FREEZE
    }
}