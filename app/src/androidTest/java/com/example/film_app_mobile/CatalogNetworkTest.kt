package com.example.film_app_mobile

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.util.Base64
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.film_app_mobile.core.network.*
import com.example.film_app_mobile.core.network.api.*
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.FilmDatabase
import com.example.film_app_mobile.data.image.CoverFiles
import com.example.film_app_mobile.data.image.digest
import com.example.film_app_mobile.data.repository.*
import com.example.film_app_mobile.data.session.AccountSession
import com.example.film_app_mobile.data.sync.DecisionSync
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class CatalogNetworkTest {
    @Test fun catalogAndPagesOnDevice() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val name = UUID.randomUUID().toString()
        val root = File(app.cacheDir,name).apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir() = root
            override fun getSharedPreferences(key: String, mode: Int) = app.getSharedPreferences(name,mode)
        }
        fun open() = Room.databaseBuilder(app,FilmDatabase::class.java,name).build()
        var db = open()
        try { MockWebServer().use { server ->
            server.start()
            val retrofit = NetworkStack(NetworkConfig(server.url("/api/v1/").toString(),true)).retrofit
            val api = retrofit.create(PacksApi::class.java)
            val library = retrofit.create(LibraryApi::class.java)
            fun session() = AccountSession(context,retrofit.create(ProfileApi::class.java),db.profile())
            var account = session()
            fun sync() = DecisionSync(account,library,db.library()) {}
            fun repo() = PacksRepository(account,api,db.pack(),db.deck(),sync(),CoverFiles(context))
            var packs = repo()
            suspend fun login(id: Long) = account.start(SessionDto("user-"+id,ProfileDto(id,"tester",false,true)))
            fun image(color: Int): ByteArray {
                val bitmap = Bitmap.createBitmap(3,3,Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(color)
                return ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it);bitmap.recycle();it.toByteArray() }
            }
            val url = "/api/v1/pack-covers/test.png"
            fun catalog(bytes: ByteArray, hash: String = digest(bytes)): String {
                val data = Base64.encodeToString(bytes,Base64.NO_WRAP)
                return """{"packs":[{"id":1,"title":"Pack","description":"","locked":false,"premium":false,"coverUrl":"$url","isCompleted":false}],"covers":{"$url":{"mimeType":"image/png","sha256":"$hash","base64":"$data"}}}"""
            }
            fun reply(code: Int, body: String = "", tag: String? = null) {
                val response = MockResponse.Builder().code(code)
                if(body.isNotEmpty()) response.body(body)
                tag?.let { response.addHeader("ETag",it) }
                server.enqueue(response.build())
            }
            fun request(path: String, user: Int = 1, tag: String? = null) {
                val req = server.takeRequest()
                assertEquals(path,req.url.encodedPath.removePrefix("/api/v1/") + (req.url.encodedQuery?.let {"?"+it} ?: ""))
                assertEquals("Bearer user-"+user,req.headers["Authorization"])
                assertEquals(tag,req.headers["If-None-Match"])
            }
            login(1)
            val first = image(android.graphics.Color.RED)
            val second = image(android.graphics.Color.BLUE)
            val a = "\"v1\""
            val b = "\"v2\""
            reply(200,catalog(first),a)
            coroutineScope { List(3){ async { packs.packs() } }.awaitAll() }
            request("packs/catalog")
            val file = File(root,"covers/"+digest(first))
            assertArrayEquals(first,file.readBytes())
            db.close(); db=open();account=session();packs=repo()
            assertEquals(digest(first),packs.cachedPacks().single().coverHash)
            packs.packs()
            assertEquals(1,server.requestCount)
            reply(304)
            packs.packs(true);request("packs/catalog",tag=a)
            reply(200,catalog(second,digest(first)),b)
            assertTrue(runCatching { packs.packs(true) }.isFailure);request("packs/catalog",tag=a)
            assertEquals(a,db.pack().stamp(1)?.etag)
            file.delete()
            reply(200,catalog(second),b)
            packs.packs();request("packs/catalog")
            assertArrayEquals(second,File(root,"covers/"+digest(second)).readBytes())
            fun page(ids: List<Int>,cursor: String?): String {
                val items = ids.joinToString { """{"id":$it,"title":"Film $it","description":"","posterUrl":null,"year":null,"director":null,"actors":[],"durationMinutes":null,"genres":[]}""" }
                val next = cursor?.let { "\"" + it + "\"" } ?: "null"
                return """{"items":[$items],"nextCursor":$next}"""
            }
            reply(200,page(listOf(2,3),"next+"))
            val firstPage = packs.page(1,limit=2)
            request("packs/1/films/page?limit=2")
            val decisions = LibraryRepository(account, library, db.library(),sync())
            reply(204); decisions.decide(firstPage.cards.first(),"WILL_WATCH");request("library/2")
            reply(503)
            assertTrue(runCatching { packs.page(1,firstPage.next,limit=2) }.isFailure)
            request("packs/1/films/page?limit=2&cursor=next%2B")
            assertEquals("next+",packs.localDeck(1)?.next)
            reply(200,page(listOf(3,4),null))
            val last=packs.page(1,firstPage.next,limit=2)
            request("packs/1/films/page?limit=2&cursor=next%2B")
            assertNull(last.next)
            assertEquals(listOf(3L,4L),last.cards.map { it.id })
            reply(204);decisions.reset(2);request("library/2")
            assertNull(packs.localDeck(1))
            login(2);assertTrue(packs.cachedPacks().isEmpty())
            reply(200,catalog(first),a);packs.packs();request("packs/catalog",2)
            login(1);assertEquals(digest(second),packs.cachedPacks().single().coverHash)
        } } finally {
            db.close();app.deleteDatabase(name);context.getSharedPreferences(name,0).edit().clear().commit()
            root.deleteRecursively()
        }
    }
}
