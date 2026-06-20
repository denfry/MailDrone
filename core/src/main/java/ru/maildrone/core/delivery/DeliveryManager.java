package ru.maildrone.core.delivery;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.maildrone.adapter.ParticleKind;
import ru.maildrone.adapter.PlatformAdapter;
import ru.maildrone.core.config.MailConfig;
import ru.maildrone.core.config.Messages;
import ru.maildrone.core.block.PostPointManager;
import ru.maildrone.core.parcel.Parcel;
import ru.maildrone.core.parcel.ParcelStatus;
import ru.maildrone.core.parcel.TrackingNumbers;
import ru.maildrone.core.scheduler.MailTask;
import ru.maildrone.core.scheduler.Schedulers;
import ru.maildrone.core.storage.ParcelStore;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет жизненным циклом посылки: приём → вылет дрона → (сортировка) →
 * прибытие. Весь полёт идёт на Folia-безопасных планировщиках.
 */
public final class DeliveryManager {

    private static final long ACCEPTANCE_TICKS = 60L; // ~3 с в отделении
    private static final long ANIM_PERIOD = 2L;       // кадр анимации раз в 2 тика

    private final MailConfig config;
    private final Messages messages;
    private final Schedulers schedulers;
    private final ParcelStore store;
    private final PlatformAdapter adapter;
    private final NotificationService notifications;
    private final PostPointManager postPoints;

    private final ConcurrentHashMap<String, Flight> active = new ConcurrentHashMap<>();
    private volatile boolean particlesBroken = false;

    public DeliveryManager(MailConfig config, Messages messages,
                           Schedulers schedulers, ParcelStore store, PlatformAdapter adapter,
                           NotificationService notifications, PostPointManager postPoints) {
        this.config = config;
        this.messages = messages;
        this.schedulers = schedulers;
        this.store = store;
        this.adapter = adapter;
        this.notifications = notifications;
        this.postPoints = postPoints;
    }

    /** Создаёт посылку и запускает доставку. Возвращает трек-номер. */
    public String createAndSend(Player sender, UUID recipientId, String recipientName, List<ItemStack> contents) {
        String tracking = uniqueTracking();
        long now = System.currentTimeMillis();

        Location senderLoc = sender.getLocation();
        Location origin = resolveOrigin(senderLoc);
        Location dest = resolveDestination(origin);

        Parcel parcel = new Parcel(tracking, sender.getUniqueId(), sender.getName(),
                recipientId, recipientName, contents, now);
        parcel.origin(origin);
        parcel.destination(dest);
        parcel.originName(postPoints.hasOffices() ? "почтовое отделение" : "пункт отправки");
        parcel.destName(postPoints.hasPostomats() ? "почтомат" : "пункт выдачи");

        boolean sorting = config.rollSorting();
        long baseMs = config.randomDeliverySeconds() * 1000L;
        long sortMs = sorting ? config.randomSortingExtraSeconds() * 1000L : 0L;
        long deliverAt = now + ACCEPTANCE_TICKS * 50L + baseMs + sortMs;

        parcel.status(ParcelStatus.ACCEPTED);
        parcel.deliverAt(deliverAt);
        parcel.updatedAt(now);
        store.put(parcel);

        notifications.announceIncoming(parcel);

        // Вылет после приёмки. Запускаем на потоке региона отделения.
        schedulers.atLocationLater(origin, () -> startFlight(parcel, sorting, sortMs), ACCEPTANCE_TICKS);
        return tracking;
    }

    private void startFlight(Parcel parcel, boolean sorting, long sortMs) {
        long now = System.currentTimeMillis();
        World world = parcel.origin() != null ? parcel.origin().getWorld() : null;
        if (world == null) {
            arriveLogical(parcel);
            return;
        }

        setStatus(parcel, ParcelStatus.IN_TRANSIT, "");
        notifications.announceDeparture(parcel);

        Location p0 = parcel.origin().clone().add(0, 2, 0);
        Location pdest = parcel.destination() != null
                ? parcel.destination().clone().add(0, 2, 0)
                : p0.clone();

        if (pdest.getWorld() != null && pdest.getWorld() != world) {
            // Точки в разных мирах — связного полёта нет, завершаем логически в срок.
            long delay = Math.max(1L, (parcel.deliverAt() - now) / 50L);
            schedulers.globalLater(() -> arriveLogical(parcel), delay);
            return;
        }

        double cruiseY = Math.max(p0.getY(), pdest.getY()) + config.cruiseHeight();
        Location pmid = new Location(world,
                (p0.getX() + pdest.getX()) / 2.0, cruiseY, (p0.getZ() + pdest.getZ()) / 2.0);

        DroneEntity drone = new DroneEntity();
        drone.spawn(p0, config.bodyMaterial(), config.rotorMaterial(), config.noseMaterial(),
                config.droneScale(), config.rotorCount());
        if (!drone.isSpawned() || drone.body() == null) {
            arriveLogical(parcel);
            return;
        }

        Flight flight = new Flight();
        flight.parcel = parcel;
        flight.drone = drone;
        flight.p0 = p0;
        flight.pmid = pmid;
        flight.pdest = pdest;
        flight.sorting = sorting;
        flight.departure = now;

        long remaining = Math.max(2000L, parcel.deliverAt() - now);
        long travel = Math.max(1000L, remaining - sortMs);
        flight.leg1End = now + travel / 2L;
        flight.hoverEnd = sorting ? flight.leg1End + sortMs : flight.leg1End;
        flight.arrival = parcel.deliverAt();
        flight.sortingAnnounced = false;
        flight.tickCounter = 0;

        active.put(parcel.tracking(), flight);

        safeParticle(p0, ParticleKind.DEPART, 20, 0.3, 0.3, 0.3, 0.02);

        MailTask task = schedulers.onEntityTimer(drone.body(),
                self -> tick(flight, self),
                () -> onDroneRetired(flight),
                1L, ANIM_PERIOD);
        flight.task = task;
    }

    private void tick(Flight flight, MailTask self) {
        DroneEntity drone = flight.drone;
        if (drone == null || !drone.isSpawned()) {
            self.cancel();
            arriveLogical(flight.parcel);
            active.remove(flight.parcel.tracking());
            return;
        }

        long now = System.currentTimeMillis();
        drone.spinStep((int) ANIM_PERIOD);

        Location pos;
        if (now < flight.leg1End) {
            double f = fraction(flight.departure, flight.leg1End, now);
            pos = lerp(flight.p0, flight.pmid, f);
        } else if (flight.sorting && now < flight.hoverEnd) {
            pos = flight.pmid;
            if (!flight.sortingAnnounced) {
                flight.sortingAnnounced = true;
                setStatus(flight.parcel, ParcelStatus.AT_SORTING, config.randomSortingNote());
            }
        } else if (now < flight.arrival) {
            if (flight.parcel.status() != ParcelStatus.IN_TRANSIT) {
                setStatus(flight.parcel, ParcelStatus.IN_TRANSIT, "");
            }
            long leg2Start = flight.sorting ? flight.hoverEnd : flight.leg1End;
            double f = fraction(leg2Start, flight.arrival, now);
            pos = lerp(flight.pmid, flight.pdest, f);
        } else {
            arrive(flight, self);
            return;
        }

        if (config.faceTravel() && flight.prevPos != null) {
            double dx = pos.getX() - flight.prevPos.getX();
            double dz = pos.getZ() - flight.prevPos.getZ();
            if (dx * dx + dz * dz > 1.0e-6) {
                flight.yaw = Math.atan2(dx, dz);
            }
        }
        flight.prevPos = pos;
        drone.moveTo(pos, config.faceTravel() ? flight.yaw : 0.0, (int) ANIM_PERIOD);

        flight.tickCounter++;
        if (flight.tickCounter % 3 == 0) {
            safeParticle(pos, ParticleKind.EXHAUST, 2, 0.05, 0.05, 0.05, 0.0);
            safeParticle(pos, ParticleKind.TRAIL, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private void arrive(Flight flight, MailTask self) {
        Parcel parcel = flight.parcel;
        Location pos = flight.drone.location();
        safeParticle(pos, ParticleKind.ARRIVE, 25, 0.4, 0.4, 0.4, 0.05);
        self.cancel();
        flight.drone.remove();
        active.remove(parcel.tracking());

        setStatus(parcel, ParcelStatus.ARRIVED, "");
        notifications.announceArrival(parcel);
    }

    private void onDroneRetired(Flight flight) {
        // Сущность была удалена (например, выгрузка/смерть на Folia) — завершаем логически.
        active.remove(flight.parcel.tracking());
        if (flight.parcel.status() != ParcelStatus.ARRIVED) {
            arriveLogical(flight.parcel);
        }
    }

    /** Логическое прибытие без визуала (резюме после рестарта / мир недоступен). */
    private void arriveLogical(Parcel parcel) {
        setStatus(parcel, ParcelStatus.ARRIVED, "");
        notifications.announceArrival(parcel);
    }

    /** Возобновляет доставки, прерванные рестартом. */
    public void resumeAll() {
        long now = System.currentTimeMillis();
        for (Parcel p : store.inDelivery()) {
            long left = p.deliverAt() - now;
            if (left <= 0) {
                arriveLogical(p);
            } else {
                schedulers.globalLater(() -> arriveLogical(p), Math.max(1L, left / 50L));
            }
        }
    }

    /** Снимает все летящие дроны (вызывать при выключении). */
    public void shutdown() {
        for (Flight f : active.values()) {
            try {
                if (f.task != null) {
                    f.task.cancel();
                }
                if (f.drone != null) {
                    // На выключении нельзя планировать новые задачи — снимаем дрон сразу.
                    f.drone.remove();
                }
            } catch (Throwable ignored) {
                // best-effort: setPersistent(false) уберёт остатки при перезапуске.
            }
        }
        active.clear();
    }

    /** Выдаёт содержимое посылки игроку (для почтомата). Возвращает true при успехе. */
    public boolean collect(Player player, Parcel parcel) {
        if (parcel.status() != ParcelStatus.ARRIVED || parcel.collected()) {
            return false;
        }
        List<ItemStack> contents = parcel.contents();
        boolean overflow = false;
        for (ItemStack it : contents) {
            if (it == null || it.getType().isAir()) {
                continue;
            }
            var leftover = player.getInventory().addItem(it.clone());
            if (!leftover.isEmpty()) {
                overflow = true;
                Location at = player.getLocation();
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(at, drop);
                }
            }
        }
        parcel.contents().clear();
        parcel.collected(true);
        setStatus(parcel, ParcelStatus.DELIVERED, "");
        notifications.announceDelivered(parcel);
        if (overflow) {
            player.sendMessage(messages.msg("inventory-full"));
        }
        return true;
    }

    private void setStatus(Parcel parcel, ParcelStatus status, String note) {
        parcel.status(status);
        parcel.note(note);
        parcel.updatedAt(System.currentTimeMillis());
        store.put(parcel);
    }

    private Location resolveOrigin(Location senderLoc) {
        if (postPoints.hasOffices()) {
            Location office = postPoints.nearestOffice(senderLoc);
            if (office != null) {
                return office;
            }
        }
        return senderLoc.clone();
    }

    private Location resolveDestination(Location origin) {
        // Не читаем локацию получателя из чужого региона (Folia-safe): маршрут —
        // к ближайшему к отделению почтомату; иначе финиш у точки отправки.
        // Получение всё равно идёт через /post box или любой почтомат.
        if (postPoints.hasPostomats()) {
            Location postomat = postPoints.nearestPostomat(origin);
            if (postomat != null) {
                return postomat;
            }
        }
        return origin.clone();
    }

    private void safeParticle(Location loc, ParticleKind kind, int count,
                              double offsetX, double offsetY, double offsetZ, double extra) {
        if (loc == null || !config.particles() || particlesBroken) {
            return;
        }
        try {
            adapter.spawnParticle(loc, kind, count, offsetX, offsetY, offsetZ, extra);
        } catch (Throwable t) {
            // Несовместимость частиц на платформе — отключаем на сессию, без спама и без срыва полёта.
            particlesBroken = true;
        }
    }

    private String uniqueTracking() {
        String t;
        do {
            t = TrackingNumbers.generate();
        } while (store.exists(t));
        return t;
    }

    private static double fraction(long start, long end, long now) {
        if (end <= start) {
            return 1.0;
        }
        double f = (double) (now - start) / (double) (end - start);
        return Math.max(0.0, Math.min(1.0, f));
    }

    private static Location lerp(Location a, Location b, double f) {
        if (a.getWorld() != b.getWorld()) {
            // Защита от интерполяции между мирами — возвращаем исходную точку.
            return a.clone();
        }
        return new Location(a.getWorld(),
                a.getX() + (b.getX() - a.getX()) * f,
                a.getY() + (b.getY() - a.getY()) * f,
                a.getZ() + (b.getZ() - a.getZ()) * f);
    }

    /** Внутреннее состояние одного полёта. */
    private static final class Flight {
        Parcel parcel;
        DroneEntity drone;
        MailTask task;
        Location p0;
        Location pmid;
        Location pdest;
        boolean sorting;
        boolean sortingAnnounced;
        long departure;
        long leg1End;
        long hoverEnd;
        long arrival;
        int tickCounter;
        double yaw;
        Location prevPos;
    }
}
