package ru.maildrone.core.scheduler;

/**
 * Унифицированный хэндл задачи планировщика. Скрывает за собой как
 * {@code BukkitTask} (обычный Paper/форки), так и Folia
 * {@code ScheduledTask}, чтобы остальной код не зависел от платформы.
 */
@FunctionalInterface
public interface MailTask {

    /** Отменяет задачу. Безопасно вызывать повторно. */
    void cancel();

    MailTask NONE = () -> { };
}
