package io.klartnet.kcp.instances.game

import io.github.togar2.pvp.feature.CombatFeatures
import io.github.togar2.pvp.feature.FeatureType
import io.github.togar2.pvp.utils.CombatVersion
import io.klartnet.kcp.game.MapType
import io.klartnet.kcp.game.Minimap
import io.klartnet.kcp.game.Zone
import io.klartnet.kcp.instances.lobby.LobbyInstance
import net.hollowcube.polar.PolarLoader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerDeathEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

enum class GameState {
	WAITING,
	RUNNING,
	ENDING
}

class GameInstance(
	val id: Int,
	val map: MapType,
	val maxPlayers: Int = 3
) {
	private val featureSet = CombatFeatures.empty()
		.version(CombatVersion.MODERN)
		.add(CombatFeatures.VANILLA_EXPLOSION)
		.add(CombatFeatures.VANILLA_KNOCKBACK)
		.build()
	private val noTagTeam = MinecraftServer.getTeamManager()
		.let { tm ->
			tm.getTeam("no_tag") ?: tm.createBuilder("no_tag")
				.nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
				.build()
		}
	
	var state: GameState = GameState.WAITING
		private set
	var instance = MinecraftServer
		.getInstanceManager()
		.createInstanceContainer()
		.apply {
			chunkLoader = PolarLoader(map.createWorld())
			explosionSupplier = featureSet.get(FeatureType.EXPLOSION).explosionSupplier
		}
	
	private val minimap = Minimap(
		id,
		map.spawnPos,
		map.phases.first().size,
		map.mmapBytes
	)
	private val zone = Zone(instance, map.spawnPos, map.phases) { center, size ->
		minimap.render(instance.players, center, size)
	}
	
	private val players = GamePlayers()
	private val events = GameEvents(this)
	
	private var endTask: Task? = null
	
	val playerCount: Int
		get() = instance.players.size

	val containers = GameContainers()
	
	init {
		events.register(instance.eventNode())
	}

	fun isAlive(player: Player): Boolean =
		players.isAlive(player)
	
	fun join(player: Player): Boolean {
		if (state != GameState.WAITING || playerCount >= maxPlayers)
			return false

		player.setInstance(
			instance,
			map.spawnPos
		).thenRun {
			instance.sendMessage(
				Component.text(
					"${player.username}님이 입장했습니다. (${playerCount}/$maxPlayers)"
				)
			)
		}
		
		return true
	}
	fun leave(player: Player): CompletableFuture<Void?>? {
		player.team = null
		zone.hideBossBar(player)
		eliminatePlayer(player, "게임을 떠났습니다.")
		
		return player.setInstance(
			LobbyInstance.instance,
			LobbyInstance.map.spawnPos
		)
	}
	
	fun start() {
		if (state != GameState.WAITING || playerCount == 0) return
		
		state = GameState.RUNNING
		players.reset(instance.players)
		
		instance.players.forEach { player ->
			player.gameMode = GameMode.SURVIVAL
			
			player.teleport(getRandomAirPos()).thenRun {
				player.team = noTagTeam
				minimap.give(player)

				instance.scheduler().scheduleNextTick {
					if (state != GameState.RUNNING)
						return@scheduleNextTick
					
					player.entityMeta.isFlyingWithElytra = true
				}
			}
		}
		
		zone.start()
	}
	
	fun onPlayerSpawn(event: PlayerSpawnEvent) {
		val player = event.player
		
		player.respawnPoint = map.spawnPos	
		player.gameMode = GameMode.SPECTATOR
		player.health = 20.0f
		player.inventory.clear()
	}
	
	fun onPlayerDeath(event: PlayerDeathEvent) {
		eliminatePlayer(event.player, "사망했습니다.")
	}
	
	fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
		leave(event.player)
	}
	
	private fun eliminatePlayer(player: Player, reason: String) {
		if (!players.eliminate(player))
			return

		spawnDeathChest(player)

		player.gameMode = GameMode.SPECTATOR
		
		instance.sendMessage(
			Component.text(
				"${player.username}님이 $reason (${players.aliveCount}명 남음)"
			)
		)

		checkWinner()
	}
	
	private fun checkWinner() {
		if (state != GameState.RUNNING || players.aliveCount > 1)
			return
		
		state = GameState.ENDING
		
		val winner = instance.players.firstOrNull { players.isAlive(it) }
		if (winner != null) {
			instance.showTitle(
				Title.title(
					Component.text(
						"게임 종료!",
						NamedTextColor.GOLD
					),
					Component.text(
						"${winner.username} 승리!",
						NamedTextColor.YELLOW
					)
				)
			)
		} else {
			instance.sendMessage(
				Component.text(
					"생존자가 없어 무승부로 종료되었습니다!",
					NamedTextColor.RED
				)
			)
		}
		
		endTask = instance.scheduler().scheduleTask({
			endTask = null
			destroy()
		}, TaskSchedule.seconds(5), TaskSchedule.stop())
	}
	
	private fun getRandomAirPos(): Pos {
		val safeRadius = (zone.size / 2.0) - 20.0
		val randomX = map.spawnPos.x + Random.nextDouble(-safeRadius, safeRadius)
		val randomZ = map.spawnPos.z + Random.nextDouble(-safeRadius, safeRadius)
		
		return Pos(
			randomX,
			map.spawnPos.y + 150.0,
			randomZ,
			Random.nextFloat() * 360F,
			40F
		)
	}
	
	private fun destroy() {
		stop()
		
		val transfers = instance.players.map { player ->
			leave(player)
		}.toTypedArray()
		
		CompletableFuture.allOf(*transfers).thenRun {
			MinecraftServer.getInstanceManager().unregisterInstance(instance)
			GameManager.remove(id)
		}
	}
	
	private fun stop() {
		endTask?.cancel()
		endTask = null
		
		zone.stop()
		players.clear()
		containers.clear()
	}

	private fun spawnDeathChest(player: Player) {
		val items = player.inventory.itemStacks.filter { !it.isAir }
		if (items.isEmpty())
			return

		val pp = player.position
		val blockPos = Vec(
			pp.blockX().toDouble(),
			pp.blockY().toDouble(),
			pp.blockZ().toDouble()
		)
		instance.setBlock(blockPos, Block.CHEST)

		containers.getOrCreate(blockPos) {
			Inventory(
				InventoryType.CHEST_1_ROW,
				Component.text(player.username)
			)
		}.also { inventory ->
			items.take(inventory.size).forEachIndexed { i, item ->
				inventory.setItemStack(i, item)
			}
		}
	}
}