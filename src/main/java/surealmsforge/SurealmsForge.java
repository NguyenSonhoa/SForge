package surealmsforge;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SurealmsForge extends JavaPlugin {
    private static Economy econ = null;
    public static MiniMessage miniMessage = MiniMessage.miniMessage();
    private static SurealmsForge instance;
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Không tìm thấy Vault hoặc không hook được Economy!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        new SkinChangeListener(this, econ);
        getLogger().info("SurealmsForge enabled!");
    }

    public static SurealmsForge getInstance() {
        return instance;
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
    public static Economy getEconomy() {
        return econ;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("skinforge")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                SkinChangeGUI.open(player, this);
                return true;
            } else {
                String msg = getConfig().getString("messages.only_player", "<red>Chỉ người chơi mới dùng được lệnh này!</red>");
                sender.sendMessage(miniMessage.deserialize(msg));
                return true;
            }
        }
        return false;
    }
}