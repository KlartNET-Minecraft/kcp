package io.klartnet.kcp.instances.game.event

import io.klartnet.kcp.instances.game.GameInstance
import io.klartnet.kcp.instances.game.weapon.heldWeapon
import net.kyori.adventure.text.Component
import net.minestom.server.event.player.*
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket

fun addCombatListeners(game: GameInstance) {
	val node = game.instance.eventNode()
	
	node.addListener(PlayerUseItemEvent::class.java) { event ->
		val player = event.player
		if (!game.isAlive(player)) return@addListener
		
		player.heldWeapon()?.onUse(player)
	}
	
	node.addListener(PlayerMoveEvent::class.java) { event ->
		val player = event.player
		if (!game.isAlive(player)) return@addListener

		player.heldWeapon()?.onMove(player)
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
		if (!game.isAlive(player)) return@addListener
		
		player.heldWeapon()?.onSwing(player)
	}

	node.addListener(PlayerSwapItemEvent::class.java) { event ->
		event.isCancelled = true
		
		val player = event.player
		if (!game.isAlive(player)) return@addListener
		
		player.heldWeapon()?.onReload(player) ?: player.sendActionBar(Component.empty())
	}
	
	node.addListener(PlayerChangeHeldSlotEvent::class.java) { event ->
		val player = event.player
		
		game.instance.scheduler().scheduleNextTick {
			player.heldWeapon()?.onHold(player) ?: player.sendActionBar(Component.empty())
		}
	}
}