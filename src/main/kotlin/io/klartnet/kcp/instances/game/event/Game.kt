package io.klartnet.kcp.instances.game.event

import io.klartnet.kcp.instances.game.GameInstance
import io.klartnet.kcp.instances.game.loot.addDropListeners
import io.klartnet.kcp.instances.game.loot.addLootListeners
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerDeathEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent

object Game {
	fun register(game: GameInstance) {
		val node = game.instance.eventNode()

		node.addListener(PlayerSpawnEvent::class.java) { event ->
			game.onPlayerSpawn(event)
		}
		node.addListener(ItemDropEvent::class.java) { event ->
			event.isCancelled = true
		}
		node.addListener(PlayerDeathEvent::class.java) { event ->
			event.chatMessage = null
			game.onPlayerDeath(event)
		}
		node.addListener(PlayerDisconnectEvent::class.java) { event ->
			game.onPlayerDisconnect(event)
		}

		addFootstepListeners(game)
		addDoorListeners(game)
		addWeaponListeners(game)
		addHealListeners(game)
		addLootListeners(game)
		addDropListeners(game)
	}
}