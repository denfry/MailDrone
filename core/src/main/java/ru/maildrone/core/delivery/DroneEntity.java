package ru.maildrone.core.delivery;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Визуальная модель дрона: корпус (BlockDisplay) + 4 винта (ItemDisplay).
 *
 * <p>Части — независимые сущности, перемещаются обычным {@code teleportAsync}
 * с интерполяцией (без пассажиров и без флагов телепортации, которые
 * различаются/устаревают между 1.21.x и 26.x). Винты крутятся через
 * трансформацию. Всё на публичном API — работает на Paper и Folia.
 */
public final class DroneEntity {

    private BlockDisplay body;
    private final List<ItemDisplay> rotors = new ArrayList<>();
    private final List<double[]> rotorOffsets = new ArrayList<>();
    private double scale = 0.6;
    private double spin = 0.0;

    public boolean isSpawned() {
        return body != null && body.isValid();
    }

    public BlockDisplay body() {
        return body;
    }

    public void spawn(Location at, Material bodyMaterial, Material rotorMaterial, double scale) {
        World world = at.getWorld();
        if (world == null) {
            return;
        }
        this.scale = scale;

        body = world.spawn(at, BlockDisplay.class, d -> {
            d.setBlock(bodyMaterial.createBlockData());
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTransformation(bodyTransform(scale));
            d.setPersistent(false);
            d.setTeleportDuration(2);
        });

        double off = scale * 0.6;
        double up = scale * 0.45;
        double[][] corners = {
                {off, up, off},
                {off, up, -off},
                {-off, up, off},
                {-off, up, -off},
        };
        for (double[] corner : corners) {
            Location rotorLoc = at.clone().add(corner[0], corner[1], corner[2]);
            ItemDisplay rotor = world.spawn(rotorLoc, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(rotorMaterial));
                d.setBrightness(new Display.Brightness(15, 15));
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setTransformation(rotorTransform(scale, 0.0));
                d.setPersistent(false);
                d.setTeleportDuration(2);
            });
            rotors.add(rotor);
            rotorOffsets.add(corner);
        }
    }

    /** Плавно перемещает дрон так, чтобы корпус оказался в {@code center}. */
    public void moveTo(Location center, int teleportDurationTicks) {
        if (!isSpawned()) {
            return;
        }
        int dur = Math.max(1, Math.min(59, teleportDurationTicks));
        body.setTeleportDuration(dur);
        body.teleportAsync(center);
        for (int i = 0; i < rotors.size(); i++) {
            ItemDisplay rotor = rotors.get(i);
            if (rotor == null || !rotor.isValid()) {
                continue;
            }
            double[] o = rotorOffsets.get(i);
            rotor.setTeleportDuration(dur);
            rotor.teleportAsync(center.clone().add(o[0], o[1], o[2]));
        }
    }

    /** Один шаг анимации винтов. Вызывать на потоке владельца сущности. */
    public void spinStep(int periodTicks) {
        spin += 0.7;
        int dur = Math.max(1, periodTicks);
        for (ItemDisplay rotor : rotors) {
            if (rotor == null || !rotor.isValid()) {
                continue;
            }
            rotor.setInterpolationDelay(0);
            rotor.setInterpolationDuration(dur);
            rotor.setTransformation(rotorTransform(scale, spin));
        }
    }

    /** Текущая позиция дрона или {@code null}. */
    public Location location() {
        return isSpawned() ? body.getLocation() : null;
    }

    public void remove() {
        for (ItemDisplay rotor : rotors) {
            if (rotor != null && rotor.isValid()) {
                rotor.remove();
            }
        }
        rotors.clear();
        rotorOffsets.clear();
        if (body != null && body.isValid()) {
            body.remove();
        }
        body = null;
    }

    private static Transformation bodyTransform(double scale) {
        return new Transformation(
                new Vector3f((float) (-scale / 2.0), 0f, (float) (-scale / 2.0)),
                new Quaternionf(),
                new Vector3f((float) scale, (float) (scale * 0.35), (float) scale),
                new Quaternionf());
    }

    private static Transformation rotorTransform(double scale, double spin) {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf().rotateY((float) spin),
                new Vector3f((float) (scale * 0.5), (float) (scale * 0.08), (float) (scale * 0.5)),
                new Quaternionf());
    }
}
