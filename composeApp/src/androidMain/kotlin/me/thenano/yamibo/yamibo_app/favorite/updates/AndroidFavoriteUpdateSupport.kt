package me.thenano.yamibo.yamibo_app.favorite.updates

import android.content.Context
import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.AndroidLocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidAuthRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidForumRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidTagRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidThreadRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.favorite.FavoriteUpdateRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.rss.RssSearchSubscriptionRepositoryImpl
import me.thenano.yamibo.yamibo_app.store.AndroidCookieStore
import me.thenano.yamibo.yamibo_app.store.AndroidUserStore

internal object AndroidFavoriteUpdateSupport {
    fun createRepository(context: Context): FavoriteUpdateRepository {
        val appContext = context.applicationContext
        val dbFactory = DatabaseFactory(appContext)
        val cookieStore = AndroidCookieStore(appContext)
        val userStore = AndroidUserStore(appContext)
        val yamiboClient = YamiboClient(timeoutMillis = 60_000L)
        val diskCacheFactory = DiskCacheFactory(
            dbFactory = dbFactory,
            cacheDirPath = appContext.cacheDir.absolutePath,
        )
        val db = Database(dbFactory.createDriver())
        val authRepository = AndroidAuthRepository(cookieStore, userStore, yamiboClient)
        val forumRepository = AndroidForumRepository(cookieStore, yamiboClient, diskCacheFactory)
        return FavoriteUpdateRepositoryImpl(
            db = db,
            localFavoriteRepository = AndroidLocalFavoriteRepository(dbFactory),
            threadRepository = AndroidThreadRepository(cookieStore, yamiboClient, diskCacheFactory),
            tagRepository = AndroidTagRepository(cookieStore, yamiboClient, diskCacheFactory),
            rssSearchSubscriptionRepository = RssSearchSubscriptionRepositoryImpl(
                db = db,
                authRepository = authRepository,
                forumRepository = forumRepository,
            ),
        )
    }
}
