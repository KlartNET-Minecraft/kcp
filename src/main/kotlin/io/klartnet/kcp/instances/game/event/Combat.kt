package io.klartnet.kcp.instances.game.event

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.player.PlayerMoveEvent

fun addDropListeners(game: GameInstance) {
	val node = game.instance.eventNode()

	node.addListener(PlayerMoveEvent::class.java) { event ->
		val player = event.player
		if (event.newPosition.sameBlock(player.position))
			return@addListener
		if (!player.entityMeta.isFlyingWithElytra || !game.isAlive(player))
			return@addListener

		val altitude = (player.position.y - game.map.spawnPos.y).toInt().coerceAtLeast(0)
		player.sendActionBar(
			Component.text(
				"지상까지 ${altitude}m 남음",
				when {
					altitude > 40 -> NamedTextColor.AQUA
					altitude > 15 -> NamedTextColor.YELLOW
					else -> NamedTextColor.RED
				}
			)
		)
	}
}