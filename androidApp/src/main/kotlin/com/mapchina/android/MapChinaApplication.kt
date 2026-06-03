package com.mapchina.android

import android.app.Application
import java.io.File
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import okio.Path.Companion.toOkioPath
import com.amap.api.maps.MapsInitializer
import okhttp3.OkHttpClient
import com.mapchina.data.remote.BoundaryLoader
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.di.appModule
import com.mapchina.di.platformModule
import com.mapchina.di.seedDataAsync
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import kotlin.concurrent.thread

class MapChinaApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: android.content.Context): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
                    .header("Referer", "https://amap.com")
                    .build()
                chain.proceed(request)
            }
            .build()
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(client))
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(this)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)

        startKoin {
            androidContext(this@MapChinaApplication)
            modules(appModule, platformModule)
        }

        thread(name = "data-seed") {
            val koin = KoinPlatform.getKoin()
            val regionRepo = koin.get<RegionRepository>()
            val attractionRepo = koin.get<AttractionRepository>()
            val boundaryLoader = koin.get<BoundaryLoader>()
            val achievementRepo = koin.get<AchievementRepository>()
            val atlasRepo = koin.get<AtlasRepository>()
            seedDataAsync(regionRepo, attractionRepo, boundaryLoader, achievementRepo, atlasRepo)
        }
    }
}
