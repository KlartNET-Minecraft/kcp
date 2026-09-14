package io.klartnet.kcp.instances.game

import net.minestom.server.entity.Player
import java.util.*

class GamePlayers {
	private val alive = mutableSetOf<UUID>()
	
	val aliveCount: Int
		get() = alive.size
	
	fun reset(players: Collection<Player>) {
		alive.clear()
		alive.addAll(players.map(Player::getUuid))
	}
	
	fun isAlive(player: Player): Boolean =
		player.uuid in alive
	
	fun eliminate(player: Player): Boolean =
		alive.remove(player.uuid)
	
	fun clear() {
		alive.clear()
	}
}