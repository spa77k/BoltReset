package io.github.spa77k.boltreset;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.protection.BlockProtection;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** 再生成した許可済みワールドだけを対象に、Boltのブロック保護をコンソールから消す。 */
public final class BoltResetPlugin extends JavaPlugin {
    private static final int TIMEOUT_SECONDS = 10;
    private BoltPlugin bolt;
    private Set<String> worlds;
    private boolean requireWhitelist;
    private boolean requireEmptyServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        worlds = Set.copyOf(getConfig().getStringList("worlds"));
        requireWhitelist = getConfig().getBoolean("require-whitelist", true);
        requireEmptyServer = getConfig().getBoolean("require-empty-server", true);
        bolt = (BoltPlugin) getServer().getPluginManager().getPlugin("Bolt");
        if (worlds.isEmpty()) {
            getLogger().warning("No worlds are listed in config.yml; every clear request will be refused.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
            sender.sendMessage("BOLT_RESET_ERROR console-only");
            return true;
        }
        if (bolt == null || !bolt.isEnabled()) {
            sender.sendMessage("BOLT_RESET_ERROR Bolt-unavailable");
            return true;
        }
        if (args.length == 1 && args[0].equals("status")) {
            sender.sendMessage("BOLT_RESET_READY");
            return true;
        }
        if (args.length != 4 || !args[0].equals("clear")) {
            sender.sendMessage("BOLT_RESET_ERROR usage: clear <world> <old-uid> <new-uid>");
            return true;
        }
        String name = args[1];
        if (!worlds.contains(name)) {
            sender.sendMessage("BOLT_RESET_ERROR world-not-allowed");
            return true;
        }
        try {
            World world = getServer().getWorld(name);
            UUID oldUid = UUID.fromString(args[2]);
            UUID newUid = UUID.fromString(args[3]);
            // 再生成前後のUIDが違い、今のワールドが再生成後のものであることを確かめる
            if (world == null || oldUid.equals(newUid) || !world.getUID().equals(newUid)) {
                sender.sendMessage("BOLT_RESET_ERROR world-not-regenerated");
                return true;
            }
            if ((requireWhitelist && !getServer().hasWhitelist())
                    || (requireEmptyServer && !getServer().getOnlinePlayers().isEmpty())) {
                sender.sendMessage("BOLT_RESET_ERROR players-not-blocked");
                return true;
            }
            Store store = bolt.getBolt().getStore();
            // 先に未保存の保護も確定し、漏れなく対象を取得する
            store.flush().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Collection<BlockProtection> all = store.loadBlockProtections().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            int removed = 0;
            for (BlockProtection protection : all) {
                if (name.equals(protection.getWorld())) {
                    bolt.removeProtection(protection);
                    removed++;
                }
            }
            store.flush().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long remaining = store.loadBlockProtections().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).stream()
                    .filter(p -> name.equals(p.getWorld())).count();
            if (remaining != 0) {
                sender.sendMessage("BOLT_RESET_ERROR remaining=" + remaining);
                return true;
            }
            getLogger().info("Cleared " + removed + " block protections in " + name);
            sender.sendMessage("BOLT_RESET_OK world=" + name + " removed=" + removed + " remaining=0");
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            getLogger().log(Level.SEVERE, "Could not confirm that the Bolt protections were cleared.", e);
            sender.sendMessage("BOLT_RESET_ERROR " + e.getClass().getSimpleName());
        }
        return true;
    }
}
