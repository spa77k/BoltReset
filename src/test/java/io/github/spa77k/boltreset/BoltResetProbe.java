package io.github.spa77k.boltreset;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.Store;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/** 隔離したPaperサーバーで BoltReset のコマンドを実行し、結果をログに出す。 */
public final class BoltResetProbe extends JavaPlugin {
    private static final List<String> WORLDS = List.of("reset_a", "reset_end", "keep");
    private Store store;
    private BoltPlugin bolt;
    private BoltResetPlugin plugin;

    private long count(String name) {
        return store.loadBlockProtections().join().stream().filter(p -> p.getWorld().equals(name)).count();
    }

    private void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
        getLogger().info("ok: " + message);
    }

    private String command(boolean remoteConsole, String... args) {
        List<String> messages = new ArrayList<>();
        Class<?> senderType = remoteConsole ? RemoteConsoleCommandSender.class : CommandSender.class;
        CommandSender sender = (CommandSender) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{senderType}, (proxy, method, values) -> {
                    if (method.getName().equals("sendMessage")) {
                        for (Object value : values) {
                            if (value instanceof String text) messages.add(text);
                            if (value instanceof String[] texts) messages.addAll(List.of(texts));
                        }
                    }
                    return null;
                });
        plugin.onCommand(sender, plugin.getCommand("boltreset"), "boltreset", args);
        return String.join("\n", messages);
    }

    private String[] clear(String name) {
        return new String[]{"clear", name, UUID.randomUUID().toString(), Bukkit.getWorld(name).getUID().toString()};
    }

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                bolt = (BoltPlugin) Bukkit.getPluginManager().getPlugin("Bolt");
                plugin = (BoltResetPlugin) Bukkit.getPluginManager().getPlugin("BoltReset");
                store = bolt.getBolt().getStore();
                for (String name : WORLDS) {
                    WorldCreator creator = new WorldCreator(name).type(WorldType.FLAT);
                    if (name.equals("reset_end")) creator.environment(World.Environment.THE_END);
                    Bukkit.createWorld(creator);
                }
                if (Files.exists(Path.of("phase1-passed"))) {
                    require(count("reset_a") == 0 && count("reset_end") == 0,
                            "cleared worlds stay unprotected after restart");
                    require(count("keep") == 1, "other world keeps its protection after restart");
                    getLogger().info("BOLT_PROBE_PASS");
                    Bukkit.shutdown();
                    return;
                }
                for (String name : WORLDS) {
                    var chest = Bukkit.getWorld(name).getBlockAt(0, 100, 0);
                    chest.setType(Material.CHEST);
                    bolt.saveProtection(bolt.createProtection(chest, UUID.randomUUID(), "private"));
                }
                store.flush().join();
                require(count("reset_a") == 1 && count("reset_end") == 1 && count("keep") == 1,
                        "all three test chests protected");
                require(command(true, "status").equals("BOLT_RESET_READY"), "status reports ready");
                require(command(false, clear("reset_a")).contains("console-only"), "non-console sender refused");
                require(command(true, "clear").contains("usage:"), "wrong arguments show usage");
                require(command(true, clear("keep")).contains("world-not-allowed"), "unlisted world refused");
                String uid = Bukkit.getWorld("reset_a").getUID().toString();
                require(command(true, "clear", "reset_a", uid, uid).contains("world-not-regenerated"),
                        "unchanged world UID refused");
                require(command(true, "clear", "reset_a", UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        .contains("world-not-regenerated"), "wrong new UID refused");
                require(command(true, clear("reset_a")).contains("players-not-blocked"),
                        "refused while whitelist is off");
                Bukkit.getServer().setWhitelist(true);
                require(command(true, clear("reset_end")).equals("BOLT_RESET_OK world=reset_end removed=1 remaining=0"),
                        "end world cleared");
                require(count("reset_a") == 1 && count("keep") == 1, "other worlds untouched");
                require(command(true, clear("reset_a")).equals("BOLT_RESET_OK world=reset_a removed=1 remaining=0"),
                        "second world cleared");
                require(bolt.loadProtection(Bukkit.getWorld("reset_a").getBlockAt(0, 100, 0)) == null
                        && bolt.loadProtection(Bukkit.getWorld("reset_end").getBlockAt(0, 100, 0)) == null,
                        "cleared chests are unlocked");
                require(bolt.loadProtection(Bukkit.getWorld("keep").getBlockAt(0, 100, 0)) != null,
                        "chest in other world stays locked");
                require(command(true, clear("reset_a")).equals("BOLT_RESET_OK world=reset_a removed=0 remaining=0"),
                        "clearing again succeeds with zero removals");
                Files.writeString(Path.of("phase1-passed"), "PASS\n");
                getLogger().info("BOLT_PROBE_PHASE1_PASS");
                Bukkit.shutdown();
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "BOLT_PROBE_FAIL", e);
                Bukkit.shutdown();
            }
        });
    }
}
