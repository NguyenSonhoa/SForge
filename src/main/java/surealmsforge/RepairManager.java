package surealmsforge;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.milkbowl.vault.economy.Economy;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RepairManager {
    private static RepairManager instance;
    private JavaPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, RepairTask> repairTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> repairingItems = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> notificationTasks = new ConcurrentHashMap<>();

    private RepairManager() {
    }

    public static RepairManager getInstance() {
        if (instance == null) {
            instance = new RepairManager();
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "repair_data.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadRepairTasks();
    }

    public void shutdown() {
        saveRepairTasks();
    }

    public void startRepair(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();

        // Kiểm tra xem người chơi đã có trang bị đang sửa hay chưa
        if (repairingItems.containsKey(playerId)) {
            String message = plugin.getConfig().getString("messages.already_repairing",
                    "<red>Bạn đã có một trang bị đang sửa chữa!</red>");
            player.sendMessage(SurealmsForge.miniMessage.deserialize(message));
            return;
        }

        // Kiểm tra nếu item không có độ bền
        if (!(item.getItemMeta() instanceof Damageable)) {
            String message = plugin.getConfig().getString("messages.not_repairable",
                    "<red>Trang bị này không thể sửa chữa!</red>");
            player.sendMessage(SurealmsForge.miniMessage.deserialize(message));
            return;
        }

        // Kiểm tra nếu item đã đầy độ bền
        Damageable damageable = (Damageable) item.getItemMeta();
        if (damageable.getDamage() == 0) {
            String message = plugin.getConfig().getString("messages.already_repaired",
                    "<red>Trang bị này không cần sửa chữa!</red>");
            player.sendMessage(SurealmsForge.miniMessage.deserialize(message));
            return;
        }

        // Tính phí sửa chữa
        int maxDurability = item.getType().getMaxDurability();
        int damage = damageable.getDamage();
        double damagePercent = (double) damage / maxDurability * 100;
        int expCost = (int) (plugin.getConfig().getDouble("repair.base_exp_cost", 5) * damagePercent / 100);
        double moneyCost = plugin.getConfig().getDouble("repair.base_money_cost", 100) * damagePercent / 100;

        // Kiểm tra tiền và exp
        if (SurealmsForge.getEconomy().getBalance(player) < moneyCost) {
            String message = plugin.getConfig().getString("messages.not_enough_money",
                    "<red>Bạn không đủ tiền để sửa chữa trang bị này!</red>");
            player.sendMessage(SurealmsForge.miniMessage.deserialize(message));
            return;
        }

        if (player.getLevel() < expCost) {
            String message = plugin.getConfig().getString("messages.not_enough_exp",
                    "<red>Bạn không đủ exp để sửa chữa trang bị này!</red>");
            player.sendMessage(SurealmsForge.miniMessage.deserialize(message));
            return;
        }

        // Trừ tiền và exp
        SurealmsForge.getEconomy().withdrawPlayer(player, moneyCost);
        player.setLevel(player.getLevel() - expCost);

        // Tạo task sửa chữa
        long repairTime = plugin.getConfig().getLong("repair.time_minutes", 20) * 60 * 20; // Chuyển phút thành ticks
        long finishTime = System.currentTimeMillis() + (repairTime * 50); // Chuyển ticks thành milis

        RepairTask task = new RepairTask(player.getUniqueId(), item.clone(), finishTime);
        repairTasks.put(playerId, task);
        repairingItems.put(playerId, item.clone());

        // Lưu dữ liệu
        saveRepairTasks();

        // Thông báo bắt đầu sửa chữa
        String message = plugin.getConfig().getString("messages.repair_started",
                "<green>Bắt đầu sửa chữa trang bị. Quá trình sẽ hoàn thành sau %time% phút.</green>");
        message = message.replace("%time%", String.valueOf(plugin.getConfig().getLong("repair.time_minutes", 20)));
        player.sendMessage(SurealmsForge.miniMessage.deserialize(message));

        // Lên lịch thông báo khi hoàn thành
        scheduleNotification(player);
    }

    private void scheduleNotification(Player player) {
        UUID playerId = player.getUniqueId();
        RepairTask task = repairTasks.get(playerId);

        if (task == null) return;

        long currentTime = System.currentTimeMillis();
        long finishTime = task.getFinishTime();
        long delayTicks = (finishTime - currentTime) / 50; // Chuyển milis thành ticks

        if (delayTicks <= 0) {
            // Đã hoàn thành sửa chữa
            notifyRepairComplete(player);
            return;
        }

        // Hủy task cũ nếu có
        if (notificationTasks.containsKey(playerId)) {
            notificationTasks.get(playerId).cancel();
        }

        // Lên lịch thông báo mới
        BukkitTask notifyTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            notifyRepairComplete(player);
        }, delayTicks);

        notificationTasks.put(playerId, notifyTask);
    }

    private void notifyRepairComplete(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID playerId = player.getUniqueId();

        // Kiểm tra xem có đang sửa chữa không
        if (!repairTasks.containsKey(playerId)) return;

        // Kiểm tra xem đã hoàn thành chưa
        RepairTask task = repairTasks.get(playerId);
        if (System.currentTimeMillis() < task.getFinishTime()) return;

        // Thông báo hoàn thành
        String message = plugin.getConfig().getString("messages.repair_completed",
                "<green>Trang bị của bạn đã được sửa chữa hoàn tất! Hãy mở giao diện sửa chữa để nhận lại.</green>");
        player.sendMessage(SurealmsForge.miniMessage.deserialize(message));

        // Xóa task thông báo
        notificationTasks.remove(playerId);
    }

    public boolean canCompleteRepair(UUID playerId) {
        RepairTask task = repairTasks.get(playerId);
        return task != null && System.currentTimeMillis() >= task.getFinishTime();
    }

    public ItemStack getRepairingItem(UUID playerId) {
        return repairingItems.get(playerId);
    }

    public ItemStack getRepairedItem(UUID playerId) {
        RepairTask task = repairTasks.get(playerId);
        if (task == null) return null;

        ItemStack item = task.getItem().clone();
        if (item.getItemMeta() instanceof Damageable) {
            ItemMeta meta = item.getItemMeta();
            ((Damageable) meta).setDamage(0);
            item.setItemMeta(meta);
        }

        return item;
    }

    public void completeRepair(Player player) {
        UUID playerId = player.getUniqueId();

        // Xóa dữ liệu sửa chữa
        repairTasks.remove(playerId);
        repairingItems.remove(playerId);

        // Huỷ task thông báo nếu có
        if (notificationTasks.containsKey(playerId)) {
            notificationTasks.get(playerId).cancel();
            notificationTasks.remove(playerId);
        }

        // Lưu dữ liệu
        saveRepairTasks();

        // Thông báo
        String message = plugin.getConfig().getString("messages.repair_claimed",
                "<green>Bạn đã nhận lại trang bị đã sửa!</green>");
        player.sendMessage(SurealmsForge.miniMessage.deserialize(message));
    }

    public void cancelRepair(Player player) {
        UUID playerId = player.getUniqueId();

        // Kiểm tra xem có đang sửa chữa không
        if (!repairTasks.containsKey(playerId)) return;

        // Hoàn trả một phần tiền và exp (tùy chọn)
        // Ở đây bạn có thể thêm code để hoàn trả tiền và exp

        // Xóa dữ liệu sửa chữa
        repairTasks.remove(playerId);
        repairingItems.remove(playerId);

        // Huỷ task thông báo nếu có
        if (notificationTasks.containsKey(playerId)) {
            notificationTasks.get(playerId).cancel();
            notificationTasks.remove(playerId);
        }

        // Lưu dữ liệu
        saveRepairTasks();

        // Thông báo
        String message = plugin.getConfig().getString("messages.repair_cancelled",
                "<yellow>Bạn đã hủy quá trình sửa chữa trang bị!</yellow>");
        player.sendMessage(SurealmsForge.miniMessage.deserialize(message));
    }

    public long getRemainingTime(UUID playerId) {
        RepairTask task = repairTasks.get(playerId);
        if (task == null) return 0;

        long remainingMillis = task.getFinishTime() - System.currentTimeMillis();
        if (remainingMillis <= 0) return 0;

        return remainingMillis / 1000 / 60; // Chuyển milis thành phút
    }

    public double getRepairProgress(UUID playerId) {
        RepairTask task = repairTasks.get(playerId);
        if (task == null) return 0;

        long totalTime = plugin.getConfig().getLong("repair.time_minutes", 20) * 60 * 1000; // Tổng thời gian sửa chữa (milis)
        long remainingMillis = task.getFinishTime() - System.currentTimeMillis();

        if (remainingMillis <= 0) return 100;

        double progress = 100 - (remainingMillis * 100.0 / totalTime);
        return Math.min(100, Math.max(0, progress));
    }

    private void loadRepairTasks() {
        if (!dataFile.exists()) return;

        ConfigurationSection taskSection = dataConfig.getConfigurationSection("tasks");
        if (taskSection == null) return;

        for (String key : taskSection.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                ConfigurationSection playerSection = taskSection.getConfigurationSection(key);

                if (playerSection != null) {
                    ItemStack item = playerSection.getItemStack("item");
                    long finishTime = playerSection.getLong("finish_time");

                    if (item != null) {
                        RepairTask task = new RepairTask(playerId, item, finishTime);
                        repairTasks.put(playerId, task);
                        repairingItems.put(playerId, item);

                        // Lên lịch thông báo nếu người chơi online
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            scheduleNotification(player);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Không thể tải dữ liệu sửa chữa cho: " + key);
                e.printStackTrace();
            }
        }
    }

    private void saveRepairTasks() {
        dataConfig.set("tasks", null);

        for (Map.Entry<UUID, RepairTask> entry : repairTasks.entrySet()) {
            UUID playerId = entry.getKey();
            RepairTask task = entry.getValue();

            dataConfig.set("tasks." + playerId + ".item", task.getItem());
            dataConfig.set("tasks." + playerId + ".finish_time", task.getFinishTime());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Không thể lưu dữ liệu sửa chữa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class RepairTask {
        private final UUID playerId;
        private final ItemStack item;
        private final long finishTime;

        public RepairTask(UUID playerId, ItemStack item, long finishTime) {
            this.playerId = playerId;
            this.item = item;
            this.finishTime = finishTime;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public ItemStack getItem() {
            return item;
        }

        public long getFinishTime() {
            return finishTime;
        }
    }
}