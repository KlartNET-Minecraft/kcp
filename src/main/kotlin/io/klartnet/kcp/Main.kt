package io.klartnet.kcp

import io.klartnet.kcp.instances.lobby.LobbyInstance
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerGameModeChangeEvent

fun main() {
	val server = MinecraftServer.init(Auth.Online())

	registerCommands()
	
	val globalEventHandler = MinecraftServer.getGlobalEventHandler()
	globalEventHandler.apply {
		addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
			event.spawningInstance = LobbyInstance.instance
			event.player.respawnPoint = LobbyInstance.map.spawnPos
		}
		addListener(PlayerGameModeChangeEvent::class.java) { event ->
			val player = event.player
			val isSpectator = event.newGameMode == GameMode.SPECTATOR
			player.isInvulnerable = isSpectator
//			player.isAutoViewable = !isSpectator
		}
	}
	
	startDebugMonitor()
	
	server.start("0.0.0.0", 25575)
}