package surealmsforge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import surealmsforge.SurealmsForge;

import java.util.HashMap;
import java.util.UUID;

public class RepairGUI {
    private static final HashMap<UUID, ItemStack> repairingItems = new HashMap<>();
    private static final String TITLE = "Sửa Trang Bị";

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        // Khung trang trí
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < 27; i++) {
            if (i != 11 && i != 15 && i != 13) {
                gui.setItem(i, glass);
            }
        }

        // Slot 11: Trang bị đặt vào sửa
        // Slot 15: Trang bị đã sửa (hiện sau 20 phút)
        // Slot 13: Xác nhận

        player.openInventory(gui);
    }

    public static void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!e.getView().getTitle().equals(TITLE)) return;

        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot != 11 && slot != 13 && slot != 15) {
            e.setCancelled(true);
            return;
        }

        Inventory inv = e.getInventory();

        // Slot 13: Xác nhận sửa
        if (slot == 13) {
            ItemStack itemToRepair = inv.getItem(11);
            if (itemToRepair == null || itemToRepair.getType() == Material.AIR) {
                player.sendMessage("§cChưa đặt trang bị cần sửa!");
                e.setCancelled(true);
                return;
            }

            // TODO: Tính toán chi phí, trừ tiền/xp, lưu dữ liệu sửa vào manager, bắt đầu đếm thời gian

            repairingItems.put(player.getUniqueId(), itemToRepair.clone());
            inv.setItem(11, null);
            player.sendMessage("§aBắt đầu sửa trang bị. Vui lòng quay lại sau 20 phút!");
            e.setCancelled(true);
            return;
        }

        // Slot 15: Nhận lại trang bị sau khi hoàn thành
        if (slot == 15) {
            ItemStack completed = repairingItems.get(player.getUniqueId());
            if (completed != null) {
                ItemStack repaired = completed.clone();
                repaired.setDurability((short) 0); // Hoặc dùng meta nếu là 1.13+
                player.getInventory().addItem(repaired);
                repairingItems.remove(player.getUniqueId());
                player.sendMessage("§aTrang bị đã sửa xong và trả lại bạn!");
            } else {
                player.sendMessage("§cKhông có trang bị nào đang sửa!");
            }
            e.setCancelled(true);
        }
    }
}
