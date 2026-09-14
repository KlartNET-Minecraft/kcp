package io.klartnet.kcp.instances.game

import io.klartnet.kcp.game.*
import net.minestom.server.coordinate.Vec
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerDeathEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle

class GameEvents(
	private val game: GameInstance
) {
	fun register(node: EventNode<InstanceEvent>) {
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

		node.addListener(EntityTickEvent::class.java) { event ->
			val trident = event.entity as? Trident
				?: return@addListener
			if (!trident.getTag(tridentTag))
				return@addListener

			trident.instance.sendGroupedPacket(
				ParticlePacket(
					Particle.SONIC_BOOM,
					trident.position,
					Vec.ZERO,
					0f, 1
				)
			)
		}
		node.addListener(ProjectileCollideWithBlockEvent::class.java) { event ->
			val trident = event.entity as? Trident
				?: return@addListener

			game.instance.explode(
				event.collisionPosition.x.toFloat(),
				event.collisionPosition.y.toFloat(),
				event.collisionPosition.z.toFloat(),
				5f
			)

			trident.remove()
		}
		node.addListener(ProjectileCollideWithEntityEvent::class.java) { event ->
			val trident = event.entity as? Trident
				?: return@addListener
			
			game.instance.explode(
				event.collisionPosition.x.toFloat(),
				event.collisionPosition.y.toFloat(),
				event.collisionPosition.z.toFloat(),
				10f
			)

			trident.remove()
		}
		
		addFootstepListeners(game)
		addDoorListeners(game)
		addCombatListeners(game)
		addHealListeners(game)
		addLootListeners(game)
		addDropListeners(game)
	}
}