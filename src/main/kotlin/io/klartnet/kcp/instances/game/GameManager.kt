package io.klartnet.kcp.instances.game

import io.klartnet.kcp.instances.game.event.MapType
import net.minestom.server.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object GameManager {
	private val idCounter = AtomicInteger(1)
	private val games = ConcurrentHashMap<Int, GameInstance>()
	
	private fun findAvailableGame(): GameInstance? =
		games.values.firstOrNull {
			it.state == GameState.WAITING && it.playerCount < it.maxPlayers
		}
	
	fun getGame(player: Player): GameInstance? =
		games.values.firstOrNull { it.instance == player.instance }
	
	fun getGame(id: Int): GameInstance? =
		games[id]
	
	fun match(player: Player): Boolean {
		if (getGame(player) != null) return false

		val target = findAvailableGame() ?: GameInstance(
			idCounter.getAndIncrement(),
			MapType.MINESCHOOL
		)
		games[target.id] = target
		
		return target.join(player)
	}
	
	fun remove(id: Int) {
		games.remove(id)
	}
}