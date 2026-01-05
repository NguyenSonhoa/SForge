package surealmsforge;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public class SkinChangeListener implements Listener {
    private final JavaPlugin plugin;
    private final Economy economy;

    public SkinChangeListener(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory(); // Retrieve the inventory from the event

        // Kiểm tra title thay vì loại inventory
        String inventoryTitle = plugin.getConfig().getString("gui.title", "§6Đổi Skin Trang Bị");
        if (!e.getView().getTitle().equals(inventoryTitle)) return;

        e.setCancelled(true);
        FileConfiguration config = plugin.getConfig();
        int slotItem = config.getInt("gui.slots.item", 0);
        int slotToken = config.getInt("gui.slots.token", 1);
        int slotResult = config.getInt("gui.slots.result", 3);
        int slotTip = config.getInt("gui.slots.tip", 2);
        int slot = e.getRawSlot();

        // Chỉ cho phép thao tác ở các slot cấu hình
        if (slot != slotItem && slot != slotToken && slot != slotResult) return;
        if (slot == slotResult) {
            ItemStack item = inv.getItem(slotItem);
            ItemStack token = inv.getItem(slotToken);
            if (item == null || token == null) return;
            boolean matched = false;
            String matchedKey = null;
            int resultCmd = 0;
            List<String> resultLore = null;

            for (String key : config.getConfigurationSection("skins").getKeys(false)) {
                String itemType = config.getString("skins." + key + ".item");
                if (itemType == null) continue;
                if (!item.getType().toString().equalsIgnoreCase(itemType)) continue;
                if (!config.isConfigurationSection("skins." + key + ".tokens")) continue;

                for (String tokenKey : config.getConfigurationSection("skins." + key + ".tokens").getKeys(false)) {
                    String tokenMat = config.getString("skins." + key + ".tokens." + tokenKey + ".material", "PAPER");
                    int tokenCmd = config.getInt("skins." + key + ".tokens." + tokenKey + ".custommodeldata", 1001);
                    if (token.getType() != Material.valueOf(tokenMat)) continue;
                    ItemMeta tokenMeta = token.getItemMeta();
                    if (tokenMeta == null || !tokenMeta.hasCustomModelData() || tokenMeta.getCustomModelData() != tokenCmd) continue;
                    matched = true;
                    matchedKey = key;
                    resultCmd = config.getInt("skins." + key + ".tokens." + tokenKey + ".result_custommodeldata", 1234);

                    // Thêm lore nếu có
                    if (config.contains("skins." + key + ".tokens." + tokenKey + ".result_lore")) {
                        resultLore = config.getStringList("skins." + key + ".tokens." + tokenKey + ".result_lore");
                    }
                    break;
                }
                if (matched) break;
            }

            if (!matched || matchedKey == null) return;
            ItemStack result = item.clone();
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(resultCmd);
                if (resultLore != null) meta.setLore(resultLore);
                result.setItemMeta(meta);
            }

            double price = config.getDouble("price", 1000);
            int exp = config.getInt("exp", 30);
            if (economy.getBalance(player) < price) {
                String msg = config.getString("messages.not_enough_money", "<red>Bạn không đủ tiền!</red>");
                player.sendMessage(SurealmsForge.miniMessage.deserialize(msg));
                return;
            }
            if (player.getLevel() < exp) {
                String msg = config.getString("messages.not_enough_exp", "<red>Bạn không đủ exp!</red>");
                player.sendMessage(SurealmsForge.miniMessage.deserialize(msg));
                return;
            }

            economy.withdrawPlayer(player, price);
            player.setLevel(player.getLevel() - exp);
            inv.setItem(slotItem, null);
            inv.setItem(slotToken, null);
            inv.setItem(slotResult, result);
            String msg = config.getString("messages.success", "<green>Đổi skin thành công!</green>");
            player.sendMessage(SurealmsForge.miniMessage.deserialize(msg));
        } else {
            // Cho phép đặt item vào slot item, token
            e.setCancelled(false);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Kiểm tra title thay vì loại inventory
        String inventoryTitle = plugin.getConfig().getString("gui.title", "§6Đổi Skin Trang Bị");
        if (!e.getView().getTitle().equals(inventoryTitle)) return;

        Inventory inv = e.getInventory(); // Retrieve the inventory from the event
        FileConfiguration config = plugin.getConfig();
        int slotItem = config.getInt("gui.slots.item", 0);
        int slotToken = config.getInt("gui.slots.token", 1);

        // Trả lại item còn lại cho người chơi
        for (int i : new int[]{slotItem, slotToken}) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                e.getPlayer().getInventory().addItem(item);
            }
        }
    }
}
