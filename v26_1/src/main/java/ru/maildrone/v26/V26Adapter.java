package ru.maildrone.v26;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import ru.maildrone.adapter.ParticleKind;
import ru.maildrone.adapter.PlatformAdapter;

/**
 * Адаптер для линейки Minecraft 26.x.
 *
 * <p>Перечисление {@code org.bukkit.Particle} в 26.x подкреплено реестром, но
 * его константы по-прежнему доступны, поэтому код общий с 1.21.x. Это
 * отдельная точка расширения: если в будущем 26.x потребует доступ через
 * реестр/иной API, изменения изолируются здесь, не затрагивая core.
 */
public final class V26Adapter implements PlatformAdapter {

    @Override
    public String name() {
        return "26.x";
    }

    @Override
    public void spawnParticle(Location location, ParticleKind kind, int count,
                              double offsetX, double offsetY, double offsetZ, double extra) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(map(kind), location, count, offsetX, offsetY, offsetZ, extra);
    }

    private static Particle map(ParticleKind kind) {
        return switch (kind) {
            case EXHAUST -> Particle.SMOKE;
            case DEPART -> Particle.CLOUD;
            case TRAIL -> Particle.END_ROD;
            case ARRIVE -> Particle.HAPPY_VILLAGER;
            case STUCK -> Particle.ANGRY_VILLAGER;
        };
    }
}
