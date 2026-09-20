package io.klartnet.kcp

import io.klartnet.kcp.instances.lobby.LobbyInstance
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerGameModeChangeEvent
import java.net.URI
import java.nio.file.Path

fun main() {
	val resourcePackServer = ResourcePackServer(
		Path.of("resources/kcp.zip"),
		25555
	)
	resourcePackServer.start()
	val resourcePack = ResourcePackRequest.resourcePackRequest()
		.packs(
			ResourcePackInfo.resourcePackInfo(
				resourcePackServer.uuid,
				URI.create("http://my.klartnet.io:25555/resources"),
				resourcePackServer.hash
			)
		)
		.build()
	
	
	val server = MinecraftServer.init(Auth.Online())
	
	val globalEventHandler = MinecraftServer.getGlobalEventHandler()
	globalEventHandler.apply {
		addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
			val player = event.player
			
			player.sendResourcePacks(resourcePack)
			
			event.spawningInstance = LobbyInstance.instance
			
			player.respawnPoint = LobbyInstance.map.spawnPos
			player.getAttribute(Attribute.BLOCK_BREAK_SPEED).baseValue = 2.5
		}
		addListener(PlayerGameModeChangeEvent::class.java) { event ->
			val player = event.player
			val isSpectator = event.newGameMode == GameMode.SPECTATOR
			
			player.isInvulnerable = isSpectator
//			player.isAutoViewable = !isSpectator
		}
	}

	registerCommands()
	
	watchDebugMode()
	
	server.start("0.0.0.0", 25575)
}