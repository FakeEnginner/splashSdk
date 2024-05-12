package com.splashSdk.plugins

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.bson.Document
import org.bson.types.ObjectId


@Serializable
data class Splash(
    val id: String,
    val banner: String,
    val description: String
){
    fun splash(): Document = Document.parse(Json.encodeToString(this))

    companion object{
        private val json = Json { ignoreUnknownKeys = true}

        fun fromSplash(document: Document): Splash = json.decodeFromString(document.toJson())
    }
}

class SplashService(private val database: MongoDatabase){
    var collection : MongoCollection<Document>

    init {
        database.createCollection("splash")
        collection = database.getCollection("splash")
    }
    //create new splash
    suspend fun create(splash: Splash): String = withContext(Dispatchers.IO){
        val doc = splash.splash()
        collection.insertOne(doc)
        doc["id"].toString()
    }

    //Read a Splash
    suspend fun read(id: String): Splash? = withContext(Dispatchers.IO){
        collection.find(Filters.eq("id", ObjectId(id))).first()?.let(Splash::fromSplash)
    }

    //Update Splash
    suspend fun update(id: String, splash: Splash): Document? = withContext(Dispatchers.IO){
        collection.findOneAndReplace(Filters.eq("id",ObjectId(id)),splash.splash())
    }

    suspend fun  delete(id: String): Document? = withContext(Dispatchers.IO){
        collection.findOneAndDelete(Filters.eq("id",ObjectId(id)))
    }
}


