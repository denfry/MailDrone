package ru.maildrone.adapter;

import org.bukkit.Location;

/**
 * Платформенный адаптер: инкапсулирует немногочисленные различия API между
 * линейками Minecraft 1.21.x и 26.x. Конкретная реализация выбирается во время
 * запуска плагина по версии сервера ({@code Bukkit.getMinecraftVersion()}).
 *
 * <p>Большая часть используемого нами API (Display-сущности, планировщики Folia,
 * инвентари, PDC, Adventure) идентична между линейками, поэтому интерфейс
 * намеренно небольшой — здесь только то, что реально расходится.
 */
public interface PlatformAdapter {

    /** Короткое имя адаптера для логов, например {@code "1.21.x"} или {@code "26.x"}. */
    String name();

    /**
     * Спавнит частицы в точке мира. Вид частиц абстрагирован через
     * {@link ParticleKind}, потому что в 26.x частицы переехали на реестр и
     * перечисление {@code org.bukkit.Particle} может вести себя иначе.
     *
     * <p>Должно вызываться на потоке региона, владеющего {@code location}
     * (см. RegionScheduler), иначе на Folia будет ошибка доступа из чужого потока.
     */
    void spawnParticle(Location location, ParticleKind kind, int count,
                       double offsetX, double offsetY, double offsetZ, double extra);
}
