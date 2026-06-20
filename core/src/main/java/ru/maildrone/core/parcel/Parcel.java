package ru.maildrone.core.parcel;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Посылка: содержимое, отправитель/получатель, статус и временные метки.
 *
 * <p>Изменяемый объект. Источник истины хранится в {@code ParcelStore}; все
 * мутации статуса должны идти через {@code DeliveryManager}, который их
 * персистит.
 */
public final class Parcel {

    private final String tracking;
    private final UUID sender;
    private final String senderName;
    private final UUID recipient;
    private final String recipientName;
    private final List<ItemStack> contents;

    private ParcelStatus status;
    private final long createdAt;
    private long updatedAt;
    private long deliverAt;

    private String note;
    private Location origin;
    private Location destination;
    private String originName;
    private String destName;
    private boolean collected;

    public Parcel(String tracking, UUID sender, String senderName,
                  UUID recipient, String recipientName, List<ItemStack> contents,
                  long createdAt) {
        this.tracking = tracking;
        this.sender = sender;
        this.senderName = senderName;
        this.recipient = recipient;
        this.recipientName = recipientName;
        this.contents = contents == null ? new ArrayList<>() : new ArrayList<>(contents);
        this.status = ParcelStatus.CREATED;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.note = "";
        this.originName = "";
        this.destName = "";
    }

    public String tracking() {
        return tracking;
    }

    public UUID sender() {
        return sender;
    }

    public String senderName() {
        return senderName;
    }

    public UUID recipient() {
        return recipient;
    }

    public String recipientName() {
        return recipientName;
    }

    /** Содержимое посылки (изменяемый список — копия снаружи не делается). */
    public List<ItemStack> contents() {
        return contents;
    }

    public ParcelStatus status() {
        return status;
    }

    public void status(ParcelStatus status) {
        this.status = status;
    }

    public long createdAt() {
        return createdAt;
    }

    public long updatedAt() {
        return updatedAt;
    }

    public void updatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Момент (epoch millis), когда посылка должна прибыть. */
    public long deliverAt() {
        return deliverAt;
    }

    public void deliverAt(long deliverAt) {
        this.deliverAt = deliverAt;
    }

    public String note() {
        return note;
    }

    public void note(String note) {
        this.note = note == null ? "" : note;
    }

    public Location origin() {
        return origin;
    }

    public void origin(Location origin) {
        this.origin = origin;
    }

    public Location destination() {
        return destination;
    }

    public void destination(Location destination) {
        this.destination = destination;
    }

    public String originName() {
        return originName;
    }

    public void originName(String originName) {
        this.originName = originName == null ? "" : originName;
    }

    public String destName() {
        return destName;
    }

    public void destName(String destName) {
        this.destName = destName == null ? "" : destName;
    }

    public boolean collected() {
        return collected;
    }

    public void collected(boolean collected) {
        this.collected = collected;
    }

    /** Суммарное число предметов в посылке (для отображения). */
    public int itemCount() {
        int n = 0;
        for (ItemStack it : contents) {
            if (it != null) {
                n += it.getAmount();
            }
        }
        return n;
    }
}
