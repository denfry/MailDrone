package ru.maildrone.core.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.maildrone.core.MailDrone;
import ru.maildrone.core.config.Messages;
import ru.maildrone.core.parcel.Parcel;
import ru.maildrone.core.parcel.ParcelStatus;
import ru.maildrone.core.parcel.TrackingNumbers;
import ru.maildrone.core.util.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Точка входа команды {@code /post}. */
public final class PostCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_SEND = "maildrone.send";
    private static final String PERM_USE = "maildrone.use";
    private static final String PERM_ADMIN = "maildrone.admin";

    private final MailDrone mail;

    public PostCommand(MailDrone mail) {
        this.mail = mail;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "send" -> doSend(sender, args);
            case "box" -> doBox(sender);
            case "track" -> doTrack(sender, args);
            case "list" -> doList(sender);
            case "admin" -> doAdmin(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void doSend(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mail.messages().msg("only-player"));
            return;
        }
        if (!player.hasPermission(PERM_SEND)) {
            player.sendMessage(mail.messages().msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mail.messages().msg("usage-send"));
            return;
        }
        String name = args[1];
        UUID recipientId;
        String recipientName;
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            recipientId = online.getUniqueId();
            recipientName = online.getName();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(name);
            if (off == null || off.getUniqueId() == null) {
                player.sendMessage(mail.messages().msg("player-not-found", Messages.ph("name", name)));
                return;
            }
            recipientId = off.getUniqueId();
            recipientName = off.getName() != null ? off.getName() : name;
        }
        if (recipientId.equals(player.getUniqueId())) {
            player.sendMessage(mail.messages().msg("cant-send-self"));
            return;
        }
        if (mail.config().requireOffice() && mail.postPoints().hasOffices()
                && !mail.postPoints().hasOfficeNear(player.getLocation(), mail.config().officeRadius())) {
            player.sendMessage(mail.messages().msg("need-office"));
            return;
        }
        mail.openPacking(player, recipientId, recipientName);
    }

    private void doBox(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mail.messages().msg("only-player"));
            return;
        }
        if (!player.hasPermission(PERM_USE)) {
            player.sendMessage(mail.messages().msg("no-permission"));
            return;
        }
        mail.openMailbox(player);
    }

    private void doTrack(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_USE)) {
            sender.sendMessage(mail.messages().msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mail.messages().msg("usage-track"));
            return;
        }
        String tracking = TrackingNumbers.normalize(args[1]);
        Parcel p = mail.store().get(tracking);
        if (p == null) {
            sender.sendMessage(mail.messages().msg("track-not-found", Messages.ph("tracking", tracking)));
            return;
        }
        long now = System.currentTimeMillis();
        Messages m = mail.messages();
        sender.sendMessage(m.get("track-header", Messages.ph("tracking", p.tracking())));
        sender.sendMessage(m.get("track-status", Messages.phc("status", m.status(p.status()))));
        sender.sendMessage(m.get("track-parties",
                Messages.ph("sender", p.senderName()), Messages.ph("recipient", p.recipientName())));
        sender.sendMessage(m.get("track-route",
                Messages.ph("origin", p.originName()), Messages.ph("dest", p.destName())));
        if (!p.note().isEmpty()) {
            sender.sendMessage(m.get("track-note", Messages.ph("note", p.note())));
        }
        if (p.status().inDelivery()) {
            sender.sendMessage(m.get("track-eta", Messages.ph("eta", Format.eta(p.deliverAt(), now))));
        }
        sender.sendMessage(m.get("track-items", Messages.ph("count", String.valueOf(p.itemCount()))));
    }

    private void doList(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mail.messages().msg("only-player"));
            return;
        }
        if (!player.hasPermission(PERM_USE)) {
            player.sendMessage(mail.messages().msg("no-permission"));
            return;
        }
        UUID id = player.getUniqueId();
        List<Parcel> mine = mail.store().activeFor(id);
        Messages m = mail.messages();
        if (mine.isEmpty()) {
            player.sendMessage(m.msg("list-empty"));
            return;
        }
        player.sendMessage(m.get("list-header"));
        for (Parcel p : mine) {
            if (p.sender().equals(id)) {
                player.sendMessage(m.get("list-sent",
                        Messages.ph("tracking", p.tracking()),
                        Messages.ph("recipient", p.recipientName()),
                        Messages.phc("status", m.status(p.status()))));
            } else {
                player.sendMessage(m.get("list-incoming",
                        Messages.ph("tracking", p.tracking()),
                        Messages.ph("sender", p.senderName()),
                        Messages.phc("status", m.status(p.status()))));
            }
        }
    }

    private void doAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage(mail.messages().msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mail.messages().msg("admin-usage"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "office" -> togglePoint(sender, true);
            case "postomat" -> togglePoint(sender, false);
            case "list" -> adminList(sender);
            case "reload" -> {
                mail.reload();
                sender.sendMessage(mail.messages().msg("reloaded"));
            }
            default -> sender.sendMessage(mail.messages().msg("admin-usage"));
        }
    }

    private void togglePoint(CommandSender sender, boolean office) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mail.messages().msg("only-player"));
            return;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null || target.getType().isAir()) {
            player.sendMessage(mail.messages().msg("admin-not-looking"));
            return;
        }
        Location loc = target.getLocation();
        String pos = Format.pos(loc);
        if (office) {
            boolean added = mail.postPoints().toggleOffice(loc);
            player.sendMessage(mail.messages().msg(added ? "office-added" : "office-removed", Messages.ph("pos", pos)));
        } else {
            boolean added = mail.postPoints().togglePostomat(loc);
            player.sendMessage(mail.messages().msg(added ? "postomat-added" : "postomat-removed", Messages.ph("pos", pos)));
        }
    }

    private void adminList(CommandSender sender) {
        Messages m = mail.messages();
        sender.sendMessage(m.get("points-header"));
        List<Location> offices = mail.postPoints().officeLocations();
        List<Location> postomats = mail.postPoints().postomatLocations();
        if (offices.isEmpty() && postomats.isEmpty()) {
            sender.sendMessage(m.msg("points-empty"));
            return;
        }
        for (Location l : offices) {
            sender.sendMessage(m.get("points-office", Messages.ph("pos", Format.pos(l))));
        }
        for (Location l : postomats) {
            sender.sendMessage(m.get("points-postomat", Messages.ph("pos", Format.pos(l))));
        }
    }

    private void help(CommandSender sender) {
        Messages m = mail.messages();
        sender.sendMessage(m.get("help-header"));
        sender.sendMessage(m.get("help-send"));
        sender.sendMessage(m.get("help-box"));
        sender.sendMessage(m.get("help-track"));
        sender.sendMessage(m.get("help-list"));
        if (sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage(m.get("help-admin"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("send", "box", "track", "list", "help"));
            if (sender.hasPermission(PERM_ADMIN)) {
                subs.add("admin");
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("send")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    names.add(p.getName());
                }
                return filter(names, args[1]);
            }
            if (sub.equals("admin") && sender.hasPermission(PERM_ADMIN)) {
                return filter(List.of("office", "postomat", "list", "reload"), args[1]);
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
