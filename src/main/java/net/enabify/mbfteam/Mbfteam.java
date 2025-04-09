package net.enabify.mbfteam;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Mbfteam extends JavaPlugin implements Listener, CommandExecutor {

    // 各チームのメンバーをUUIDで管理
    private final Map<String, Set<UUID>> teamMembers = new HashMap<>();
    // 各プレイヤーの所属チームを管理
    private final Map<UUID, String> playerTeams = new HashMap<>();

    // 有効なチームカラー（全て小文字で扱う）
    private final List<String> validTeams = Arrays.asList("red", "blue", "green", "yellow", "purple", "white");

    // Foliaのスケジューラ
    private AsyncScheduler asyncScheduler;
    private RegionScheduler regionScheduler;

        @Override
        public void onEnable() {
            // Foliaスケジューラの初期化
            this.asyncScheduler = getServer().getAsyncScheduler();
            this.regionScheduler = getServer().getRegionScheduler();
        
            // 各チームのセットを初期化
            for (String team : validTeams) {
                teamMembers.put(team, new HashSet<>());
            }
        
            // イベントリスナーおよびコマンドの登録
            getServer().getPluginManager().registerEvents(this, this);
            getCommand("mbfteam").setExecutor(this);
        
            getLogger().info("MBFTeamPlugin has been enabled.");
        }

    @Override
    public void onDisable() {
        // メモリ上の情報をクリア（再起動時にリセット）
        playerTeams.clear();
        for (Set<UUID> set : teamMembers.values()) {
            set.clear();
        }
        getLogger().info("MBFTeam has been disabled.");
    }

    // /mbfteam コマンドの処理
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "使い方: /mbfteam <join | leave | tp> [引数]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // チームメンバー一覧を表示
        if (subCommand.equals("list")) {
            handleListCommand(sender, args);
            return true;
        }

        // 管理者パーミッションのチェック
        if (!sender.hasPermission("mbfteam.admin")) {
            sender.sendMessage(ChatColor.RED + "イベント主催者のみが使用できるコマンドです.");
            return true;
        }

        switch (subCommand) {
            case "join":
                handleJoinCommand(sender, args);
                break;

            case "leave":
                handleLeaveCommand(sender);
                break;

            case "tp":
                handleTpCommand(sender, args);
                break;
            
            case "start":
                handleStartCommand(sender, args);
                break;
            
            case "end":
                handleEndCommand(sender, args);
                break;

            case "list":
                handleListCommand(sender, args);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "使い方: /mbfteam <join | leave | tp> [引数]");
                break;
        }
        return true;
    }

        private void handleJoinCommand(CommandSender sender, String[] args) {
        // /mbfteam join <プレイヤー名> <チームカラー>
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "使い方: /mbfteam join <プレイヤー名> <チーム名>");
            return;
        }
        
        String targetName = args[1];
        String teamColor = args[2].toLowerCase();
        
        if (!validTeams.contains(teamColor)) {
            sender.sendMessage(ChatColor.RED + "間違ったチーム名です.正しいチーム名: red, blue, green, yellow, purple.");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + targetName + "さんはオンラインではありません.");
            return;
        }
        
        // プレイヤーのEntitySchedulerを使用して安全に処理
        EntityScheduler scheduler = targetPlayer.getScheduler();
        scheduler.run(this, task -> {
            // 既に他のチームに所属している場合は削除
            if (playerTeams.containsKey(targetPlayer.getUniqueId())) {
                String oldTeam = playerTeams.get(targetPlayer.getUniqueId());
                teamMembers.get(oldTeam).remove(targetPlayer.getUniqueId());
            }
        
            // 指定チームに追加
            teamMembers.get(teamColor).add(targetPlayer.getUniqueId());
            playerTeams.put(targetPlayer.getUniqueId(), teamColor);
        
            sender.sendMessage(ChatColor.GREEN + targetPlayer.getName() + "さんを、" + teamColor + "に配属しました.");
            targetPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "あなたは、" + teamColor + "に配属されました.");
        }, null); // 第3引数にnullを渡す
    }

        private void handleLeaveCommand(CommandSender sender) {
        // /mbfteam leave : 全プレイヤーをチームから退出させる
        Bukkit.getServer().getGlobalRegionScheduler().execute(this, () -> {
            for (UUID uuid : new HashSet<>(playerTeams.keySet())) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "あなたはチームから退出しました.");
                }
            }
            
            playerTeams.clear();
            for (String key : teamMembers.keySet()) {
                teamMembers.get(key).clear();
            }
            
            sender.sendMessage(ChatColor.GREEN + "すべてのプレイヤーをチームから脱退させました.");
        });
    }

    private void handleTpCommand(CommandSender sender, String[] args) {
        // /mbfteam tp <チームカラー> : コマンド実行者の座標に指定チームのオンラインプレイヤーをテレポート
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "使い方: /mbfteam tp <チーム名>");
            return;
        }
        
        String tpTeam = args[1].toLowerCase();
        if (!validTeams.contains(tpTeam)) {
            sender.sendMessage(ChatColor.RED + "間違ったチーム名です.正しいチーム名: red, blue, green, yellow, purple.");
            return;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみが実行できます.");
            return;
        }
        
        Player executor = (Player) sender;
        Location targetLocation = executor.getLocation();
        Set<UUID> members = teamMembers.get(tpTeam);
        
        if (members == null || members.isEmpty()) {
            sender.sendMessage(ChatColor.RED + tpTeam + "チームのプレイヤーでオンラインのプレイヤーが見つかりません.");
            return;
        }
        
        // 非同期でテレポート処理を実行
        asyncScheduler.runNow(this, task -> {
            int teleportedCount = 0;
            for (UUID uuid : members) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null && member.isOnline()) {
                    // プレイヤーごとのEntitySchedulerを使用してテレポート
                    member.getScheduler().run(this, innerTask -> {
                        member.teleportAsync(targetLocation).thenAccept(result -> {
                            member.sendMessage(ChatColor.LIGHT_PURPLE + "試合会場にテレポートしました！");
                        });
                    }, null); // 第3引数にnullを渡す
                    teleportedCount++;
                }
            }
            
            final int finalCount = teleportedCount;
            Bukkit.getServer().getGlobalRegionScheduler().execute(this, () -> {
                sender.sendMessage(ChatColor.GREEN + tpTeam + "チームに所属している" + finalCount + "人のプレイヤーをあなたの場所にテレポートさせました.");
            });
        });
    }

    private void handleStartCommand(CommandSender sender, String[] args) {
        // /mbfteam start : 全プレイヤー(チームに参加している人のみ)に、ゲーム開始のタイトルを表示し、色付き革チェストプレートを与える.
        Bukkit.getServer().getGlobalRegionScheduler().execute(this, () -> {
            for (UUID uuid : new HashSet<>(playerTeams.keySet())) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    String team = playerTeams.get(uuid);

                    // ゲーム開始のタイトルを表示
                    player.sendTitle(
                        ChatColor.GOLD + "ゲーム開始！",
                        ChatColor.GREEN + "あなたのチーム: " + team,
                        10, 70, 20
                    );

                    player.sendMessage(ChatColor.LIGHT_PURPLE + "ゲームが開始されました！\nあなたのチーム: " + team);
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "試合中は、色付き革チェストプレートを着たままにしてください。");

                    // チームカラーに応じた色付き革チェストプレートを与える
                    org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE);
                    org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate.getItemMeta();

                    switch (team) {
                        case "red":
                            meta.setColor(org.bukkit.Color.RED);
                            break;
                        case "blue":
                            meta.setColor(org.bukkit.Color.BLUE);
                            break;
                        case "green":
                            meta.setColor(org.bukkit.Color.GREEN);
                            break;
                        case "yellow":
                            meta.setColor(org.bukkit.Color.YELLOW);
                            break;
                        case "purple":
                            meta.setColor(org.bukkit.Color.PURPLE);
                            break;
                        case "white":
                            meta.setColor(org.bukkit.Color.WHITE);
                            break;
                        default:
                            meta.setColor(org.bukkit.Color.WHITE);
                            break;
                    }

                    chestplate.setItemMeta(meta);
                    player.getInventory().addItem(chestplate);
                }
            }

            sender.sendMessage(ChatColor.GREEN + "ゲームを開始しました！全プレイヤーに通知し、装備を配布しました.");
        });
    }


    private void handleEndCommand(CommandSender sender, String[] args) {
        // /mbfteam end : 全プレイヤー(チームに参加している人のみ)に、ゲーム終了のタイトルを表示し、革のチェストプレートを削除する.
        Bukkit.getServer().getGlobalRegionScheduler().execute(this, () -> {
            for (UUID uuid : new HashSet<>(playerTeams.keySet())) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    // ゲーム終了のタイトルを表示
                    player.sendTitle(
                        ChatColor.RED + "ゲーム終了！",
                        ChatColor.GREEN + "お疲れ様でした！",
                        10, 70, 20
                    );

                    // 革のチェストプレートを削除
                    player.getInventory().remove(org.bukkit.Material.LEATHER_CHESTPLATE);
                }
            }

            sender.sendMessage(ChatColor.GREEN + "ゲームを終了しました.全プレイヤーに通知し、装備を削除しました.");
        });
    }


    private void handleListCommand(CommandSender sender, String[] args) {
        // /mbfteam list : すべてのチームのメンバーをチャット欄に表示
        Bukkit.getServer().getGlobalRegionScheduler().execute(this, () -> {
            for (String team : validTeams) {
                Set<UUID> members = teamMembers.get(team);
                if (members == null || members.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + team + "チームにはメンバーがいません.");
                } else {
                    StringBuilder memberNames = new StringBuilder();
                    for (UUID uuid : members) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            memberNames.append(player.getName()).append(", ");
                        }
                    }
                    if (memberNames.length() > 0) {
                        memberNames.setLength(memberNames.length() - 2); // 最後のカンマとスペースを削除
                    }
                    sender.sendMessage(ChatColor.GREEN + team + "チームのメンバー: " + memberNames);
                }
            }
        });
    }

    // 同じチーム内では攻撃をキャンセルする（フレンドリーファイアOFF）
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        UUID victimId = victim.getUniqueId();
        UUID attackerId = attacker.getUniqueId();
        
        if (playerTeams.containsKey(victimId) && playerTeams.containsKey(attackerId)) {
            String teamVictim = playerTeams.get(victimId);
            String teamAttacker = playerTeams.get(attackerId);
            
            if (teamVictim.equals(teamAttacker)) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "チームメイトを攻撃してはいけません！");
            }
        }
    }
}