package ru.maildrone.core.storage;

import ru.maildrone.core.parcel.Parcel;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Хранилище посылок: источник истины в памяти + персистентность на диск.
 * Реализация обязана быть потокобезопасной (Folia: мутации идут из разных
 * региональных потоков).
 */
public interface ParcelStore {

    /** Загружает посылки с диска (вызывать при включении, основной поток). */
    void load();

    /** Создаёт/обновляет посылку и планирует сохранение. */
    void put(Parcel parcel);

    /** Удаляет посылку. */
    void remove(String tracking);

    /** Возвращает посылку по трек-номеру или {@code null}. */
    Parcel get(String tracking);

    /** Все посылки (снимок). */
    Collection<Parcel> all();

    /** Посылки, отправленные игроком. */
    List<Parcel> bySender(UUID sender);

    /** Прибывшие, но не полученные посылки для игрока (для почтомата). */
    List<Parcel> incomingFor(UUID recipient);

    /** Активные посылки игрока (как отправленные, так и входящие, нетерминальные). */
    List<Parcel> activeFor(UUID player);

    /** Посылки, которые ещё в доставке (для возобновления после рестарта). */
    List<Parcel> inDelivery();

    /** Существует ли посылка с таким трек-номером. */
    boolean exists(String tracking);

    /** Принудительно сохраняет всё синхронно (вызывать при выключении). */
    void flush();
}
