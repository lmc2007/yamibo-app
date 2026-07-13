package me.thenano.yamibo.yamibo_app.i18n

import me.thenano.yamibo.yamibo_app.repository.settings.BackupInterval

fun BackupInterval.localizedLabel(): String = when (this) {
    BackupInterval.HOURS_6 -> i18n("6 小時")
    BackupInterval.HOURS_12 -> i18n("12 小時")
    BackupInterval.DAYS_1 -> i18n("1 天")
    BackupInterval.DAYS_3 -> i18n("3 天")
    BackupInterval.WEEK_1 -> i18n("1 週")
    BackupInterval.NEVER -> i18n("永不")
}
