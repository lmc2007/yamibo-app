package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.UserId

/**
 * Profile page data model.
 *
 * @property uid User ID.
 * @property username Username(e.g. "thenano").
 * @property userGroup User group(e.g. "百合花蕾").
 * @property points Points(積分).
 * @property partner Objects(對象).
 * @property totalPoints Total points(總積分).
 * @property avatarUrl Avatar URL(e.g. "uc_server/data/avatar/000/65/66/26_avatar_big.jpg").
 * @property gender Gender(性別, e.g. "保密").
 * @property birthday Birthday(生日, e.g. "2000-1-1" or "-" if not set).
 * @property onlineHours Online hours(在線時間, e.g. 172).
 * @property registerTime Registration time(注冊時間, e.g. "2024-8-14 20:23").
 * @property lastVisit Last visit time(最後訪問, e.g. "2026-2-24 00:49").
 * @property formHash Form hash(Important Info, use for all post request.).
 */
data class ProfilePage(
    val uid: UserId,
    val username: String,
    val userGroup: String,
    val points: Int,
    val partner: Int,
    val totalPoints: Int,
    val avatarUrl: String?,
    val gender: String?,
    val birthday: String?,
    val onlineHours: Int,
    val registerTime: String?,
    val lastVisit: String?,
    val formHash: FormHash?
)
