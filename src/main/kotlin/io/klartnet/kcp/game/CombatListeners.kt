package io.klartnet.kcp.game

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.text.Component
import net.minestom.server.event.player.*
import net.minestom.server.item.Material
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket
import net.minestom.server.tag.Tag

private val LASER_TAG = Tag.Long("laser")

fun addCombatListeners(game: GameInstance) {
	val node = game.instance.eventNode()
	
	node.addListener(PlayerUseItemEvent::class.java) { event ->
		val player = event.player
		if (!game.isAlive(player)) return@addListener
		
		val weapon = Weapon.from(event.itemStack.material()) ?: return@addListener
		if (player.onCooldown(weapon.material)) {
			event.isCancelled = true
			return@addListener
		}
		weapon.onUse(player)
	}
	
	node.addListener(PlayerMoveEvent::class.java) { event ->
		val player = event.player
		val weapon = player.heldWeapon() ?: return@addListener
		if (!game.isAlive(player) || weapon.material != Material.SPYGLASS) return@addListener
		
		val now = System.currentTimeMillis()
		if (now - (player.getTag(LASER_TAG) ?: 0L) < 100) return@addListener
		player.setTag(LASER_TAG, now)
		
		player.drawWeaponLaser(
			range = weapon.maxRange
		)
	}

	node.addListener(PlayerPacketEvent::class.java) { event ->
		val packet = event.packet as? ClientPlayerActionPacket ?: return@addListener
		if (packet.status.ordinal == 5 && game.isAlive(event.player)) {
			val player = event.player
			
			player.heldWeapon()?.onRelease(player)
		}
	}
	
	node.addListener(PlayerHandAnimationEvent::class.java) { event ->
		val player = event.player
		
		if (game.isAlive(player))
			player.heldWeapon()?.onSwing(player)
	}

	node.addListener(PlayerSwapItemEvent::class.java) { event ->
		event.isCancelled = true
		val player = event.player
		if (game.isAlive(player))
			player.heldWeapon()?.onReload(player) ?: player.sendActionBar(Component.empty())
	}
	
	node.addListener(PlayerChangeHeldSlotEvent::class.java) { event ->
		val player = event.player
		game.instance.scheduler().scheduleNextTick {
			player.heldWeapon()?.onHold(player) ?: player.sendActionBar(Component.empty())
		}
	}
}