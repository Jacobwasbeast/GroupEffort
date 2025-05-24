package net.jacobwasbeast.groupeffort.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.jacobwasbeast.groupeffort.GroupEffort;
import net.jacobwasbeast.groupeffort.config.GroupEffortConfig;
import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class GroupEffortCommands {

    private static final SimpleCommandExceptionType INVALID_LIMBO_TYPE_EXCEPTION = new SimpleCommandExceptionType(Text.literal("Invalid limbo type."));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> rootNode = CommandManager.literal("groupeffort")
                .requires(source -> source.hasPermissionLevel(2));

        LiteralArgumentBuilder<ServerCommandSource> setNode = CommandManager.literal("set");

        setNode.then(CommandManager.literal("minimumPlayers")
                .then(CommandManager.argument("count", IntegerArgumentType.integer(CONFIG_MIN_PLAYERS, CONFIG_MAX_PLAYERS))
                        .executes(GroupEffortCommands::setMinimumPlayers)));

        setNode.then(CommandManager.literal("limboType")
                .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests(GroupEffortCommands::suggestLimboTypes)
                        .executes(GroupEffortCommands::setLimboType)));

        setNode.then(CommandManager.literal("chatBlocked")
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(GroupEffortCommands::setChatBlockedEnabled)));

        setNode.then(CommandManager.literal("graceEnabled")
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(GroupEffortCommands::setGraceEnabled)));

        setNode.then(CommandManager.literal("graceDuration")
                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(CONFIG_MIN_GRACE_SECONDS, CONFIG_MAX_GRACE_SECONDS))
                        .executes(GroupEffortCommands::setGraceDuration)));

        rootNode.then(setNode);

        // --- Status Command ---
        rootNode.then(CommandManager.literal("status")
                .executes(GroupEffortCommands::showStatus));

        dispatcher.register(rootNode);
    }

    private static final int CONFIG_MIN_PLAYERS = 1;
    private static final int CONFIG_MAX_PLAYERS = 200;
    private static final int CONFIG_MIN_GRACE_SECONDS = 0;
    private static final int CONFIG_MAX_GRACE_SECONDS = 3600;


    private static int setMinimumPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int newMinPlayers = IntegerArgumentType.getInteger(context, "count");
        GroupEffortConfig config = GroupEffort.getConfig();
        config.generalSettings.minimumPlayers = newMinPlayers;
        saveConfigAndNotify(context, "Minimum players set to " + newMinPlayers);
        GroupEffortManager.updateGlobalLimboState();
        return 1;
    }

    private static int setLimboType(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String typeStr = StringArgumentType.getString(context, "type").toUpperCase(Locale.ROOT);
        GroupEffortConfig.LimboType newLimboType;
        try {
            newLimboType = GroupEffortConfig.LimboType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw INVALID_LIMBO_TYPE_EXCEPTION.create();
        }

        GroupEffortConfig config = GroupEffort.getConfig();
        GroupEffortConfig.LimboType oldLimboType = config.generalSettings.limboType;
        config.generalSettings.limboType = newLimboType;

        saveConfigAndNotify(context, "Limbo type set to " + newLimboType);

        if (oldLimboType != newLimboType) {
            GroupEffortManager.updateLimboTypeOfActivePlayers();
        }
        return 1;
    }

    private static int setGraceEnabled(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        GroupEffortConfig config = GroupEffort.getConfig();
        config.gracePeriod.enabled = enabled;
        saveConfigAndNotify(context, "Grace period " + (enabled ? "enabled" : "disabled"));
        GroupEffortManager.updateGlobalLimboState();
        return 1;
    }

    private static int setChatBlockedEnabled(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        GroupEffortConfig config = GroupEffort.getConfig();
        config.generalSettings.blockChatMessages = enabled;
        saveConfigAndNotify(context, "Blocking chat messages " + (enabled ? "enabled" : "disabled"));
        GroupEffortManager.updateGlobalLimboState();
        return 1;
    }

    private static int setGraceDuration(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int duration = IntegerArgumentType.getInteger(context, "seconds");
        GroupEffortConfig config = GroupEffort.getConfig();
        config.gracePeriod.durationSeconds = duration;
        saveConfigAndNotify(context, "Grace period duration set to " + duration + " seconds");
        GroupEffortManager.updateGlobalLimboState();
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        GroupEffortConfig config = GroupEffort.getConfig();
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("--- GroupEffort Status ---"), false);
        source.sendFeedback(() -> Text.literal("Minimum Players: " + config.generalSettings.minimumPlayers), false);
        source.sendFeedback(() -> Text.literal("Limbo Type: " + config.generalSettings.limboType), false);
        source.sendFeedback(() -> Text.literal("Grace Period Enabled: " + config.gracePeriod.enabled), false);
        source.sendFeedback(() -> Text.literal("Grace Period Duration: " + config.gracePeriod.durationSeconds + "s"), false);
        source.sendFeedback(() -> Text.literal("Server Ticks Allowed (Limbo Active for Ticks): " + GroupEffortManager.isServerTickAllowed()), false);
        source.sendFeedback(() -> Text.literal("Players currently in Limbo state: " + GroupEffortManager.getPlayersInLimboCount()), false);
        return 1;
    }


    private static CompletableFuture<Suggestions> suggestLimboTypes(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(
                Arrays.stream(GroupEffortConfig.LimboType.values()).map(Enum::name),
                builder
        );
    }

    private static void saveConfigAndNotify(CommandContext<ServerCommandSource> context, String successMessage) {
        AutoConfig.getConfigHolder(GroupEffortConfig.class).save();
        GroupEffort.LOGGER.info("ServerQuota config saved via command: " + successMessage);
        context.getSource().sendFeedback(() -> Text.literal(successMessage), true);
    }
}