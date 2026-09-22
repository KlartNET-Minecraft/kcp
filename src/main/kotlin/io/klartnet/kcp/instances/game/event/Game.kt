package io.klartnet.kcp.instances.game.event

import io.klartnet.kcp.instances.game.GameInstance
import io.klartnet.kcp.instances.game.loot.addLootListeners
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityPose
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerDeathEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerInputEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import kotlin.math.cos
import kotlin.math.sin

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
		node.addListener(PlayerInputEvent::class.java) { event ->
			val player = event.player
			
			if (!player.isOnGround)
				return@addListener

			if (event.isHoldingShiftKey && event.isHoldingSprintKey) {
				val yawRad = Math.toRadians(player.position.yaw.toDouble())
				val dir = Vec(-sin(yawRad), 0.0, cos(yawRad)).normalize()
				player.velocity = dir.mul(8.0)
				player.pose = EntityPose.SWIMMING
			} else if (player.isSneaking) {
				player.pose = EntityPose.STANDING
			}
		}

		addFootstepListeners(game)
		addDoorListeners(game)
		addWeaponListeners(game)
		addHealListeners(game)
		addLootListeners(game)
		addDropListeners(game)
	}
}