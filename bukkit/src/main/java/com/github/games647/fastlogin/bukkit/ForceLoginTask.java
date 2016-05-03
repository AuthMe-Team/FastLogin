package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class ForceLoginTask implements Runnable {

    protected final FastLoginBukkit plugin;
    private final Player player;

    public ForceLoginTask(FastLoginBukkit plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            return;
        }

        //remove the bungeecord identifier
        String id = '/' + player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort();
        PlayerSession session = plugin.getSessions().get(id);

        //blacklist this target player for BungeeCord Id brute force attacks
        player.setMetadata(plugin.getName(), new FixedMetadataValue(plugin, true));
        //check if it's the same player as we checked before

        final BukkitAuthPlugin authPlugin = plugin.getAuthPlugin();
        if (session == null || !player.getName().equals(session.getUsername()) || authPlugin == null) {
            return;
        }

        Storage storage = plugin.getStorage();
        PlayerProfile playerProfile = null;
        if (storage != null) {
            playerProfile = storage.getProfile(session.getUsername(), false);
        }

        if (session.isVerified()) {
            boolean success = true;
            if (playerProfile != null) {
                playerProfile.setUuid(session.getUuid());
                playerProfile.setPremium(true);
            }

            if (success) {
                if (session.needsRegistration()) {
                    if (forceRegister(authPlugin, player)) {
                        storage.save(playerProfile);
                    }
                } else {
                    if (forceLogin(authPlugin, player)) {
                        storage.save(playerProfile);
                    }
                }
            }
        } else if (playerProfile != null) {
            storage.save(playerProfile);
        }
    }

    private boolean forceRegister(BukkitAuthPlugin authPlugin, Player player) {
        plugin.getLogger().log(Level.FINE, "Register player {0}", player.getName());

        String generatedPassword = plugin.generateStringPassword();
        boolean success = authPlugin.forceRegister(player, generatedPassword);
        player.sendMessage(ChatColor.DARK_GREEN + "Auto registered with password: " + generatedPassword);
        player.sendMessage(ChatColor.DARK_GREEN + "You may want change it?");
        return success;
    }

    private boolean forceLogin(BukkitAuthPlugin authPlugin, Player player) {
        plugin.getLogger().log(Level.FINE, "Logging player {0} in", player.getName());
        boolean success = authPlugin.forceLogin(player);
        player.sendMessage(ChatColor.DARK_GREEN + "Auto logged in");
        return success;
    }
}
