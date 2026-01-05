package surealmsforge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public class SkinChangeGUI {
    public static void open(Player player, JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String title = config.getString("gui.title", "§6Đổi Skin Trang Bị");
        Inventory gui = Bukkit.createInventory(null, InventoryType.HOPPER, title);
        int slotItem = config.getInt("gui.slots.item", 0);
        int slotToken = config.getInt("gui.slots.token", 1);
        int slotResult = config.getInt("gui.slots.result", 3);
        int slotTip = config.getInt("gui.slots.tip", 2);
        // Đặt tip item nếu có
        if (config.isConfigurationSection("gui.tip_item")) {
            ItemStack tip = getTipItem(config);
            gui.setItem(slotTip, tip);
        }
        player.openInventory(gui);
    }

    private static ItemStack getTipItem(FileConfiguration config) {
        String path = "gui.tip_item";
        Material mat = Material.valueOf(config.getString(path + ".material", "PAPER"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.contains(path + ".name")) meta.setDisplayName(config.getString(path + ".name"));
            if (config.contains(path + ".custommodeldata")) meta.setCustomModelData(config.getInt(path + ".custommodeldata"));
            if (config.contains(path + ".lore")) meta.setLore(config.getStringList(path + ".lore"));
            item.setItemMeta(meta);
        }
        return item;
    }
} 