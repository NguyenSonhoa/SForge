package surealmsforge.task;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;
import surealmsforge.SurealmsForge;

import java.util.HashMap;
import java.util.UUID;

public class RepairTaskManager {
    public static final long REPAIR_TIME = 20 * 60 * 20; // 20 phút = 20 phút thực tế = 1 ngày Minecraft
    private static final HashMap<UUID, RepairTask> tasks = new HashMap<>();

    public static boolean startRepair(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) return false;

        int costMoney = calculateCost(item);
        int costExp = costMoney / 10;

        if (!SurealmsForge.getEconomy().has(player, costMoney)) {
            player.sendMessage("§cKhông đủ tiền để sửa!");
            return false;
        }
        if (player.getLevel() < costExp) {
            player.sendMessage("§cKhông đủ kinh nghiệm để sửa!");
            return false;
        }

        SurealmsForge.getEconomy().withdrawPlayer(player, costMoney);
        player.setLevel(player.getLevel() - costExp);

        RepairTask task = new RepairTask(player, item);
        task.runTaskLater(SurealmsForge.getInstance(), REPAIR_TIME);
        tasks.put(uuid, task);

        player.sendMessage("§aBắt đầu sửa. Vui lòng quay lại sau 20 phút.");
        return true;
    }

    public static ItemStack getCompletedItem(UUID uuid) {
        RepairTask task = tasks.get(uuid);
        if (task != null && task.isFinished()) {
            tasks.remove(uuid);
            return task.getRepairedItem();
        }
        return null;
    }

    public static int calculateCost(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;

        if (!(item.getItemMeta() instanceof Damageable meta)) return 0;
        if (!meta.hasDamage()) return 0;

        int maxDurability = item.getType().getMaxDurability();
        int currentDurability = meta.getDamage();
        double percent = (double) currentDurability / maxDurability;

        return (int) Math.ceil(percent * 100); // 0-100 tiền tùy độ hư
    }

    static class RepairTask extends BukkitRunnable {
        private final Player player;
        private final ItemStack item;
        private boolean finished = false;

        RepairTask(Player player, ItemStack item) {
            this.player = player;
            this.item = item.clone();
        }

        @Override
        public void run() {
            finished = true;
            player.sendMessage("§aTrang bị của bạn đã được sửa xong!");
        }

        public boolean isFinished() {
            return finished;
        }

        public ItemStack getRepairedItem() {
            ItemStack result = item.clone();
            if (result.getItemMeta() instanceof Damageable meta) {
                meta.setDamage(0);
                result.setItemMeta(meta);
            }
            return result;
        }
    }
}
