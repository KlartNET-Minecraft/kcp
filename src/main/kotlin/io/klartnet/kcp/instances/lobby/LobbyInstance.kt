package io.klartnet.kcp.instances.lobby

import io.klartnet.kcp.instances.game.event.MapType
import net.hollowcube.polar.PolarLoader
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.PlayerSpawnEvent

object LobbyInstance {
	val map = MapType.LOBBY
	val instance = MinecraftServer
		.getInstanceManager()
		.createInstanceContainer()
		.apply {
			chunkLoader = PolarLoader(map.createWorld())
		}
	
	init {
		registerEvents()
	}
	
	private fun registerEvents() {
		val node = instance.eventNode()
		
		node.addListener(PlayerSpawnEvent::class.java) { event ->
			val player = event.player
			
			player.respawnPoint = map.spawnPos
			
			player.gameMode = GameMode.ADVENTURE
			player.health = 20.0F
			player.inventory.clear()

			player.showTitle(
				Title.title(
					Component.text("환영합니다!"),
					Component.text("본 미니게임 서버는 챔피언입니다입니다")
				)
			)
		}
	}
}