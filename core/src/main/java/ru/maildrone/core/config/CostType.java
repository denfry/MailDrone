package ru.maildrone.core.config;

/** Способ оплаты отправки посылки. */
public enum CostType {
    /** Бесплатно. */
    NONE,
    /** Списание уровней опыта. */
    XP_LEVELS,
    /** Списание предметов (например, изумрудов). */
    ITEM,
    /** Списание денег через Vault. */
    VAULT
}
