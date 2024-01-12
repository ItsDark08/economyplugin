package net.dark.economyplugin;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class EconomyPlugin extends JavaPlugin {

    private Map<String, Double> balances = new HashMap<>();
    private FileConfiguration config;
    private double massimoSoldi = 1_000_000.0;

    @Override
    public void onEnable() {
        caricaDati();
    }

    @Override
    public void onDisable() {
        salvaDati();
    }

    private void caricaDati() {
        saveDefaultConfig();
        config = getConfig();

        ConfigurationSection balancesSection = config.getConfigurationSection("balances");
        if (balancesSection != null) {
            for (String playerName : balancesSection.getKeys(false)) {
                double saldo = balancesSection.getDouble(playerName);
                balances.put(playerName, saldo);
            }
        }
    }

    private void salvaDati() {
        ConfigurationSection balancesSection = config.createSection("balances");
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            balancesSection.set(entry.getKey(), entry.getValue());
        }

        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Questo comando può essere eseguito solo da un giocatore.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("balance")) {
            if (args.length != 1) {
                player.sendMessage("Utilizzo: /balance <player>");
                return true;
            }

            String playerName = args[0];
            double saldo = getSaldo(playerName);

            mostraBossBar(player, playerName, saldo);
        } else if (command.getName().equalsIgnoreCase("setbalance")) {
            if (args.length != 2) {
                player.sendMessage("Utilizzo: /setbalance <player> <soldi>");
                return true;
            }

            String playerName = args[0];
            try {
                double nuovoSaldo = Double.parseDouble(args[1]);
                if (nuovoSaldo > massimoSoldi) {
                    player.sendMessage("Il saldo massimo consentito è 1 milione.");
                    return true;
                }
                setSaldo(playerName, nuovoSaldo);
                player.sendMessage("Hai impostato il saldo di " + playerName + " a " + nuovoSaldo + " euro");
            } catch (NumberFormatException e) {
                player.sendMessage("Quantità non valida.");
            }
        } else if (command.getName().equalsIgnoreCase("addbalance")) {
            if (args.length != 2) {
                player.sendMessage("Utilizzo: /addbalance <player> <soldi>");
                return true;
            }

            String playerName = args[0];
            try {
                double saldoDaAggiungere = Double.parseDouble(args[1]);
                if (getSaldo(playerName) + saldoDaAggiungere > massimoSoldi) {
                    player.sendMessage("Il saldo massimo consentito è 1 milione.");
                    return true;
                }
                aggiungiSaldo(playerName, saldoDaAggiungere);
                player.sendMessage("Hai aggiunto " + saldoDaAggiungere + " euro" + " al saldo di " + playerName);
            } catch (NumberFormatException e) {
                player.sendMessage("Quantità non valida.");
            }
        } else if (command.getName().equalsIgnoreCase("pay")) {
            if (args.length != 2) {
                player.sendMessage("Utilizzo: /pay <player> <soldi>");
                return true;
            }

            String targetPlayerName = args[0];
            double amount = Double.parseDouble(args[1]);

            if (amount <= 0) {
                player.sendMessage("La quantità deve essere maggiore di zero.");
                return true;
            }

            double playerBalance = getSaldo(player.getName());

            if (amount > playerBalance) {
                player.sendMessage("Non hai abbastanza soldi per effettuare questo pagamento.");
                return true;
            }

            double targetPlayerBalance = getSaldo(targetPlayerName);

            aggiungiSaldo(targetPlayerName, amount);
            rimuoviSaldo(player.getName(), amount);

            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
            if (targetPlayer != null) {
                targetPlayer.sendMessage("Hai ricevuto " + amount + " euro da " + player.getName());
            }

            player.sendMessage("Hai pagato " + amount + " euro a " + targetPlayerName);

            double updatedPlayerBalance = getSaldo(player.getName());
            mostraBossBar(player, player.getName(), updatedPlayerBalance);

            if (targetPlayer != null) {
                double updatedTargetPlayerBalance = getSaldo(targetPlayerName);
                mostraBossBar(targetPlayer, targetPlayerName, updatedTargetPlayerBalance);
            }
        }

        return true;
    }

    private double getSaldo(String playerName) {
        return balances.getOrDefault(playerName, 0.0);
    }

    private void setSaldo(String playerName, double nuovoSaldo) {
        balances.put(playerName, nuovoSaldo);
        salvaDati();
    }

    private void aggiungiSaldo(String playerName, double saldoDaAggiungere) {
        double saldoAttuale = getSaldo(playerName);
        double nuovoSaldo = saldoAttuale + saldoDaAggiungere;
        balances.put(playerName, nuovoSaldo);
        salvaDati();
    }

    private void rimuoviSaldo(String playerName, double amount) {
        double saldoAttuale = getSaldo(playerName);
        double nuovoSaldo = saldoAttuale - amount;
        balances.put(playerName, nuovoSaldo);
        salvaDati();
    }

    private void mostraBossBar(Player sender, String playerName, double saldo) {
        BossBar bossBar = Bukkit.createBossBar("Bilancio di " + playerName + ": $" + saldo,
                BarColor.GREEN, BarStyle.SEGMENTED_10);

        double percentualeProgresso = Math.min(saldo / massimoSoldi, 1.0);

        bossBar.setProgress(percentualeProgresso);
        bossBar.addPlayer(sender);
        Bukkit.getScheduler().runTaskLater(this, bossBar::removeAll, 100);
    }
}
