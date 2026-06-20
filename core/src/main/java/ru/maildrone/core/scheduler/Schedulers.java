package ru.maildrone.core.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Единая Folia-безопасная обёртка над планировщиками.
 *
 * <p>На Folia задачи раскладываются по корректным потокам (region / entity /
 * global / async), как того требует регионная многопоточность. На обычном
 * Paper и форках без регионов используется классический {@code BukkitScheduler}
 * — он есть всегда, даже на старых 1.21.x, где Folia-планировщиков ещё не было.
 *
 * <p>Это даёт максимальную совместимость: Paper, Folia, Purpur, Leaf, Canvas,
 * ShreddedPaper и т.д., обе линейки 1.21.x и 26.x.
 */
public final class Schedulers {

    private final Plugin plugin;
    private final boolean folia;

    public Schedulers(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    public boolean isFolia() {
        return folia;
    }

    public static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // ---- Глобальные задачи (не привязаны к месту/сущности) ----

    public MailTask global(Runnable task) {
        if (folia) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler().run(plugin, st -> task.run());
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTask(plugin, task);
        return t::cancel;
    }

    public MailTask globalLater(Runnable task, long delayTicks) {
        long d = Math.max(1L, delayTicks);
        if (folia) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, st -> task.run(), d);
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, task, d);
        return t::cancel;
    }

    public MailTask globalTimer(Runnable task, long initialDelayTicks, long periodTicks) {
        long init = Math.max(1L, initialDelayTicks);
        long per = Math.max(1L, periodTicks);
        if (folia) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, st -> task.run(), init, per);
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, task, init, per);
        return t::cancel;
    }

    // ---- Привязка к локации (регион, владеющий чанком) ----

    public MailTask atLocation(Location loc, Runnable task) {
        if (folia) {
            ScheduledTask t = Bukkit.getRegionScheduler().run(plugin, loc, st -> task.run());
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTask(plugin, task);
        return t::cancel;
    }

    public MailTask atLocationLater(Location loc, Runnable task, long delayTicks) {
        long d = Math.max(1L, delayTicks);
        if (folia) {
            ScheduledTask t = Bukkit.getRegionScheduler().runDelayed(plugin, loc, st -> task.run(), d);
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, task, d);
        return t::cancel;
    }

    // ---- Привязка к сущности (планировщик следует за ней через регионы) ----

    public MailTask onEntity(Entity entity, Runnable task, Runnable retired) {
        if (entity == null) {
            return MailTask.NONE;
        }
        if (folia) {
            ScheduledTask t = entity.getScheduler().run(plugin, st -> task.run(), retired);
            return t == null ? MailTask.NONE : t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTask(plugin, task);
        return t::cancel;
    }

    /**
     * Повторяющаяся задача, привязанная к сущности. В колбэк передаётся
     * {@link MailTask} для самоотмены. {@code retired} вызывается, если
     * сущность была удалена (Folia).
     */
    public MailTask onEntityTimer(Entity entity, Consumer<MailTask> task, Runnable retired,
                                  long initialDelayTicks, long periodTicks) {
        if (entity == null) {
            if (retired != null) {
                retired.run();
            }
            return MailTask.NONE;
        }
        long init = Math.max(1L, initialDelayTicks);
        long per = Math.max(1L, periodTicks);
        if (folia) {
            final ScheduledTask[] ref = new ScheduledTask[1];
            MailTask handle = () -> {
                if (ref[0] != null) {
                    ref[0].cancel();
                }
            };
            ScheduledTask t = entity.getScheduler().runAtFixedRate(plugin, st -> task.accept(handle), retired, init, per);
            if (t == null) {
                if (retired != null) {
                    retired.run();
                }
                return MailTask.NONE;
            }
            ref[0] = t;
            return handle;
        }
        final BukkitTask[] ref = new BukkitTask[1];
        MailTask handle = () -> {
            if (ref[0] != null) {
                ref[0].cancel();
            }
        };
        ref[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> task.accept(handle), init, per);
        return handle;
    }

    // ---- Асинхронные задачи (I/O, без обращения к API мира) ----

    public MailTask async(Runnable task) {
        if (folia) {
            ScheduledTask t = Bukkit.getAsyncScheduler().runNow(plugin, st -> task.run());
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        return t::cancel;
    }

    public MailTask asyncLater(Runnable task, long delayMillis) {
        long ms = Math.max(1L, delayMillis);
        if (folia) {
            ScheduledTask t = Bukkit.getAsyncScheduler().runDelayed(plugin, st -> task.run(), ms, TimeUnit.MILLISECONDS);
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, Math.max(1L, ms / 50L));
        return t::cancel;
    }
}
