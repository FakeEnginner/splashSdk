package com.splashSdk.plugins

import com.mongodb.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.*
import kotlinx.coroutines.*

fun Application.configureDatabases() {
    val mongoDatabase = connectToMongoDB()
    val splashService = SplashService(mongoDatabase)
    routing {

        // Create a Splash
//        post("/splash"){
//            var splash = call.receive<Splash>()
//            var id = splashService.create(splash)
//            call.respond(HttpStatusCode.Created, id)
//        }
        post("/splash") {
            try {
                val splash = call.receive<Splash>()
                val id = splashService.create(splash)
                call.respond(HttpStatusCode.Created, id)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error creating splash: ${e.message}")
                // Log the exception for debugging
                application.log.error("Error creating splash", e)
            }
        }
        // Read car
        get("/splash/{id}"){
            val id = call.parameters["id"] ?: throw  IllegalArgumentException("No ID found")
            splashService.read(id)?.let{ splash ->
                call.respond(splash)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        // Update car
        put("/splash/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            val splash = call.receive<Splash>()
            splashService.update(id, splash)?.let {
                call.respond(HttpStatusCode.OK)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        // Delete car
        delete("/splash/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            splashService.delete(id)?.let {
                call.respond(HttpStatusCode.OK)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}

/**
 * Establishes connection with a MongoDB database.
 *
 * The following configuration properties (in application.yaml/application.conf) can be specified:
 * * `db.mongo.user` username for your database
 * * `db.mongo.password` password for the user
 * * `db.mongo.host` host that will be used for the database connection
 * * `db.mongo.port` port that will be used for the database connection
 * * `db.mongo.maxPoolSize` maximum number of connections to a MongoDB server
 * * `db.mongo.database.name` name of the database
 *
 * IMPORTANT NOTE: in order to make MongoDB connection working, you have to start a MongoDB server first.
 * See the instructions here: https://www.mongodb.com/docs/manual/administration/install-community/
 * all the paramaters above
 *
 * @returns [MongoDatabase] instance
 * */
fun Application.connectToMongoDB(): MongoDatabase {
    val user = environment.config.tryGetString("db.mongo.user")
    val password = environment.config.tryGetString("db.mongo.password")
    val host = environment.config.tryGetString("db.mongo.host") ?: "localhost"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27017"
    val maxPoolSize = environment.config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "myDatabase"

    val credentials = user?.let { userVal -> password?.let { passwordVal -> "$userVal:$passwordVal@" } }.orEmpty()
    val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"

    val mongoClient = MongoClients.create(uri)
    val database = mongoClient.getDatabase(databaseName)

    environment.monitor.subscribe(ApplicationStopped) {
        mongoClient.close()
    }

    return database
}
