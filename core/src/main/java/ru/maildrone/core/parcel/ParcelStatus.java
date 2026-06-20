package ru.maildrone.core.parcel;

/**
 * Статусы посылки в духе трекинга Почты России.
 */
public enum ParcelStatus {

    /** Создана отправителем, ещё не принята. */
    CREATED("Создана", "<gray>"),
    /** Принята в отделении, ждёт отправки. */
    ACCEPTED("Принята в отделении", "<yellow>"),
    /** Дрон в пути к получателю. */
    IN_TRANSIT("В пути", "<aqua>"),
    /** Застряла в сортировочном центре (мем-задержка). */
    AT_SORTING("Обрабатывается в сортировочном центре", "<gold>"),
    /** Прибыла в почтомат, ожидает получения. */
    ARRIVED("Прибыла, ожидает получения", "<green>"),
    /** Вручена получателю. */
    DELIVERED("Вручена", "<dark_green>"),
    /** Возвращена отправителю. */
    RETURNED("Возврат отправителю", "<red>");

    private final String label;
    private final String colorTag;

    ParcelStatus(String label, String colorTag) {
        this.label = label;
        this.colorTag = colorTag;
    }

    /** Человекочитаемое название статуса (RU). */
    public String label() {
        return label;
    }

    /** MiniMessage-тег цвета для отображения статуса. */
    public String colorTag() {
        return colorTag;
    }

    /** Статус, в котором посылку ещё везут (дрон в воздухе / на сортировке). */
    public boolean inDelivery() {
        return this == ACCEPTED || this == IN_TRANSIT || this == AT_SORTING;
    }

    /** Терминальный статус — дальше посылка не меняется автоматически. */
    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED;
    }
}
