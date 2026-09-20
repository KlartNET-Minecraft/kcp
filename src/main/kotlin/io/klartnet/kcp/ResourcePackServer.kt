package io.klartnet.kcp

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Executors

class ResourcePackServer(
	private val zip: Path,
	port: Int = 25555
) {
	val hash: String = Files.newInputStream(zip).use { input ->
		val digest = MessageDigest.getInstance("SHA-1")
		val buffer = ByteArray(64 * 1024)

		while (true) {
			val read = input.read(buffer)
			if (read == -1) break
			digest.update(buffer, 0, read)
		}

		digest.digest().joinToString("") { "%02x".format(it) }
	}
	val uuid: UUID = UUID.nameUUIDFromBytes(
		hash.toByteArray(Charsets.UTF_8)
	)

	private val server = HttpServer.create(
		InetSocketAddress(port),
		0
	).apply {
		executor = Executors.newFixedThreadPool(2)
		
		createContext("/resources") { exchange ->
			if (exchange.requestMethod != "GET" || !Files.isRegularFile(zip)) {
				exchange.sendResponseHeaders(404, -1)
				exchange.close()
				return@createContext
			}

			exchange.responseHeaders.set("Content-Type", "application/zip")

			exchange.sendResponseHeaders(200, Files.size(zip))
			exchange.responseBody.use { output ->
				Files.newInputStream(zip).use { input ->
					input.copyTo(output, 64 * 1024)
				}
			}
		}
	}

	fun start() = server.start()

	fun stop() = server.stop(0)
}