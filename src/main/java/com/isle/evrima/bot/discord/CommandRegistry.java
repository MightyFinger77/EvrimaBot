package com.isle.evrima.bot.discord;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Slash commands are split across roots so server owners can hide whole trees per role in
 * Discord: Server Settings → Integrations → [this bot] → Manage (Command Permissions v2).
 */
public final class CommandRegistry {

    /**
     * Subcommand names registered under {@code /evrima-admin}. Must stay disjoint from
     * {@link #EVRIMA_SERVER_SUBCOMMANDS} — each RCON-facing slash lives in exactly one tree.
     */
    private static final Set<String> EVRIMA_ADMIN_SUBCOMMANDS = Set.of(
            "announce", "playerlist", "kick", "ban", "dm", "getplayer", "wipecorpses", "reload", "save",
            "unlink", "give", "ai-toggle", "ai-density", "ai-classes", "ai-stop-spawns", "ai-wipe", "ai-learning",
            "species-control", "species-cap-set", "species-cap-clear", "species-cap-list",
            "corpse-wipe-control", "corpse-wipe-set", "corpse-wipe-clear");

    /**
     * Subcommand names registered under {@code /evrima-server} (runtime: {@code head_admin} only).
     * Must stay disjoint from {@link #EVRIMA_ADMIN_SUBCOMMANDS}.
     */
    private static final Set<String> EVRIMA_SERVER_SUBCOMMANDS = Set.of(
            "serverdetails", "getplayables", "updateplayables", "togglemigrations", "growth-toggle", "growth-set",
            "netdistance-toggle", "pause", "queue-status", "globalchat-toggle", "humans-toggle",
            "whitelist-toggle", "whitelist-add", "whitelist-remove");

    static {
        var dup = new HashSet<>(EVRIMA_ADMIN_SUBCOMMANDS);
        dup.retainAll(EVRIMA_SERVER_SUBCOMMANDS);
        if (!dup.isEmpty()) {
            throw new IllegalStateException(
                    "/evrima-admin and /evrima-server must not share subcommand names: " + dup);
        }
    }

    private CommandRegistry() {}

    public static List<CommandData> allCommands() {
        return List.of(
                publicEvrima(),
                modEvrima(),
                adminEvrima(),
                serverEvrima(),
                headEvrima()
        );
    }

    /** Everyone: linking, account, economy (balance/spin), dino slots. */
    private static CommandData publicEvrima() {
        return Commands.slash("evrima", "The Isle Evrima — linking, economy, parking (public)")
                .addSubcommands(
                        new SubcommandData("help", "Player commands — link, economy, dino park, ecosystem"))
                .addSubcommandGroups(
                        new SubcommandGroupData("link", "Steam account linking")
                                .addSubcommands(
                                        new SubcommandData("start", "DMs you a code to finish linking"),
                                        new SubcommandData("complete", "Finish linking with the code from /evrima link start")
                                                .addOption(OptionType.STRING, "code", "Code from your DM", true)
                                                .addOption(OptionType.STRING, "steam_id", "Your SteamID64", true)
                                ),
                        new SubcommandGroupData("account", "Your account")
                                .addSubcommands(
                                        new SubcommandData("show", "Show your linked SteamID64"),
                                        new SubcommandData("debug", "Show your Discord role IDs vs bot config (troubleshooting)")
                                ),
                        new SubcommandGroupData("eco", "Economy")
                                .addSubcommands(
                                        new SubcommandData("balance", "Check your points"),
                                        new SubcommandData("spin", "Daily spin (once per UTC day)"),
                                        new SubcommandData("parking", "Parking slot usage and next purchase price (when economy.parking_slots is enabled)"),
                                        new SubcommandData("parking-buy", "Buy +1 parking slot with points (when economy.parking_slots is enabled)")
                                ),
                        new SubcommandGroupData("dino", "Park dino — RCON snapshot + optional on-disk player file capture/restore")
                                .addSubcommands(
                                        new SubcommandData("park", "Save your current in-game character (RCON getplayerdata)")
                                                .addOption(OptionType.STRING, "label", "Label shown in lists", false),
                                        new SubcommandData("list", "List your parking slots"),
                                        new SubcommandData("delete", "Delete a parking slot by id")
                                                .addOption(OptionType.INTEGER, "id", "Id from /evrima dino list", true),
                                        new SubcommandData("retrieve", "Show snapshot; may restore server player file (see config dino_park.playerdata_file)")
                                                .addOption(OptionType.INTEGER, "id", "Id from /evrima dino list", true)
                                ),
                        new SubcommandGroupData("ecosystem", "Population dashboard (RCON playerlist + species taxonomy)")
                                .addSubcommands(
                                        new SubcommandData("dashboard", "Species counts, diet buckets, and percentages")
                                                .addOption(OptionType.BOOLEAN, "fresh",
                                                        "Bypass cache and query RCON again (default: use short-lived cache)", false)
                                )
                );
    }

    /** Staff: configure who sees this under Integrations → command permissions. */
    private static CommandData modEvrima() {
        return Commands.slash("evrima-mod", "Moderation — whois, Discord timeouts (bot checks config roles)")
                .addSubcommands(
                        new SubcommandData("adminhelp", "Staff/admin slash commands (moderator+); no secrets"),
                        new SubcommandData("whois", "Linked Steam + getplayerdata — Discord **or** in-game / SteamID")
                                .addOption(OptionType.USER, "user", "Discord user (leave empty if you use player)", false)
                                .addOption(OptionType.STRING, "player", "SteamID64 or in-game name from live playerlist (omit if you use user)", false),
                        new SubcommandData("timeout", "Discord timeout (not in-game)")
                                .addOption(OptionType.USER, "user", "Member to timeout", true)
                                .addOption(OptionType.INTEGER, "minutes", "1–40320 (28d max)", true)
                );
    }

    private static CommandData adminEvrima() {
        return Commands.slash("evrima-admin", "RCON admin + grant points (bot checks config admin/head_admin)")
                .addSubcommands(
                        new SubcommandData("announce", "In-game broadcast (RCON announce)")
                                .addOption(OptionType.STRING, "message", "Message text", true),
                        new SubcommandData("playerlist", "Fetch connected players (RCON playerlist; raw text)"),
                        new SubcommandData("kick", "Kick a player (RCON kick)")
                                .addOption(OptionType.STRING, "player", "SteamID64 or in-game name (from live playerlist)", true)
                                .addOption(OptionType.STRING, "reason", "Reason", true),
                        new SubcommandData("ban", "Ban a player (RCON ban; format is game-specific)")
                                .addOption(OptionType.STRING, "player", "SteamID64 or in-game name (from live playerlist)", true)
                                .addOption(OptionType.STRING, "reason", "Reason", true)
                                .addOption(OptionType.INTEGER, "minutes", "Ban minutes (0 = server default style)", false),
                        new SubcommandData("dm", "Private in-game message (RCON directmessage)")
                                .addOption(OptionType.STRING, "player", "SteamID64 or in-game name (from live playerlist)", true)
                                .addOption(OptionType.STRING, "message", "Message", true),
                        new SubcommandData("getplayer", "Dump player/server fields (RCON getplayerdata)")
                                .addOption(OptionType.STRING, "player", "SteamID64 or in-game name (from live playerlist)", true),
                        new SubcommandData("wipecorpses", "Remove corpses / cleanup bodies (RCON wipecorpses)"),
                        new SubcommandData("reload", "Reload config.yml, species-taxonomy.yml, and kill-flavor.yml from disk"),
                        new SubcommandData("save", "Tell the game to save (RCON save)"),
                        new SubcommandData("unlink", "Remove this bot’s stored Steam link for a Discord user (not in-game)")
                                .addOption(OptionType.USER, "user", "Discord user", true),
                        new SubcommandData("give", "Add economy points (Discord bot DB only; not RCON)")
                                .addOption(OptionType.USER, "user", "Discord user", true)
                                .addOption(OptionType.INTEGER, "amount", "Points to add (0–1000000000)", true),
                        new SubcommandData("ai-toggle", "Flip the AI master switch (RCON toggleai — On↔Off; run again to undo)"),
                        new SubcommandData("ai-density", "Set AI spawn density multiplier (RCON aidensity; 0 = no new spawns)")
                                .addOption(OptionType.NUMBER, "value", "Multiplier (e.g. 0–1; see your host docs)", true),
                        new SubcommandData("ai-classes", "Disable AI creature types — NOT a toggle (RCON disableaiclasses)")
                                .addOption(OptionType.STRING, "classes", "Internal names, comma-separated (e.g. boar,Compsognathus)", true),
                        new SubcommandData("ai-stop-spawns", "Stop **new** AI spawns only: RCON aidensity 0 (use wipecorpses / ai-toggle separately)"),
                        new SubcommandData("ai-wipe", "Info: Evrima RCON has no opcode for admin-panel Wipe AI (no custom exec)"),
                        new SubcommandData("ai-learning", "Flip AI learning flag if your build supports it (RCON toggleailearning)"),
                        new SubcommandData("species-control", "Toggle species_population_control.enabled (writes config.yml)")
                                .addOption(OptionType.STRING, "mode", "on, off, or status", true),
                        new SubcommandData("species-cap-set", "Set species_population_control.caps entry (writes config.yml)")
                                .addOption(OptionType.STRING, "species", "Species display name", true)
                                .addOption(OptionType.INTEGER, "cap", "Cap value (0..500)", true),
                        new SubcommandData("species-cap-clear", "Reset one cap from bundled defaults (writes config.yml)")
                                .addOption(OptionType.STRING, "species", "Species display name", true),
                        new SubcommandData("species-cap-list", "List species caps from loaded config.yml"),
                        new SubcommandData("corpse-wipe-control", "Set scheduled_wipecorpses.enabled (writes config.yml)")
                                .addOption(OptionType.STRING, "mode", "on, off, dynamic, or status", true),
                        new SubcommandData("corpse-wipe-set", "Set a scheduled_wipecorpses field (writes config.yml)")
                                .addOption(OptionType.STRING, "key",
                                        "interval_minutes, warn_before_minutes, announce_message, dynamic_*", true)
                                .addOption(OptionType.STRING, "value", "Value for the key", true),
                        new SubcommandData("corpse-wipe-clear", "Reset wipe fields to bundled defaults (writes config.yml)")
                                .addOption(OptionType.STRING, "key",
                                        "enabled, interval_minutes, warn_before_minutes, announce_message, dynamic_*, or all", true)
                );
    }

    /**
     * Server-rule RCON verbs that do not live under {@code /evrima-admin}. Requires {@code head_admin}
     * at runtime. Kept separate so {@code /evrima-admin} stays readable and under Discord limits.
     */
    private static CommandData serverEvrima() {
        return Commands.slash("evrima-server", "Server rules RCON — head_admin only (playables, pause, whitelist, …)")
                .addSubcommands(
                        new SubcommandData("serverdetails", "Get current server settings (RCON serverdetails)"),
                        new SubcommandData("getplayables", "Get current playable species list (RCON getplayables)"),
                        new SubcommandData("updateplayables", "Set playable species list (RCON updateplayables)")
                                .addOption(OptionType.STRING, "classes", "Comma-separated class names", true),
                        new SubcommandData("togglemigrations", "Toggle migrations (RCON togglemigrations)"),
                        new SubcommandData("growth-toggle", "Toggle growth multiplier mode (RCON togglegrowthmultiplier)"),
                        new SubcommandData("growth-set", "Set growth multiplier (RCON setgrowthmultiplier)")
                                .addOption(OptionType.NUMBER, "value", "Growth multiplier value", true),
                        new SubcommandData("netdistance-toggle",
                                "Toggle net update distance checks (RCON togglenetupdatedistancechecks)"),
                        new SubcommandData("pause", "Pause/unpause server simulation (RCON pause)"),
                        new SubcommandData("queue-status", "Get queue status (RCON getqueuestatus)"),
                        new SubcommandData("globalchat-toggle", "Toggle global chat (RCON toggleglobalchat)"),
                        new SubcommandData("humans-toggle", "Toggle humans (RCON togglehumans)"),
                        new SubcommandData("whitelist-toggle", "Toggle whitelist (RCON togglewhitelist)"),
                        new SubcommandData("whitelist-add", "Add SteamID64 to whitelist (RCON addwhitelist)")
                                .addOption(OptionType.STRING, "steam_id", "SteamID64", true),
                        new SubcommandData("whitelist-remove", "Remove SteamID64 from whitelist (RCON removewhitelist)")
                                .addOption(OptionType.STRING, "steam_id", "SteamID64", true)
                );
    }

    private static CommandData headEvrima() {
        return Commands.slash("evrima-head", "Head admin only (configure visibility in Integrations)")
                .addSubcommands(
                        new SubcommandData("check", "Verify head-admin tier (placeholder for future server tools)")
                );
    }
}
