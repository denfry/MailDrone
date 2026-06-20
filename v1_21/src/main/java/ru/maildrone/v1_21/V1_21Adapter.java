package ru.maildrone.v1_21;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import ru.maildrone.adapter.ParticleKind;
import ru.maildrone.adapter.PlatformAdapter;

/**
 * Адаптер для линейки Minecraft 1.21.x. Частицы — через перечисление
 * {@code org.bukkit.Particle}.
 */
public final class V1_21Adapter implements PlatformAdapter {

    @Override
    public String name() {
        return "1.21.x";
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
