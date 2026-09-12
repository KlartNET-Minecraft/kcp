package io.klartnet.kcp.instances.game

import io.github.togar2.pvp.feature.CombatFeatures
import io.github.togar2.pvp.feature.FeatureType
import io.github.togar2.pvp.utils.CombatVersion
import io.klartnet.kcp.game.*
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
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerDeathEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.timer.TaskSchedule
import java.util.*
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
		.add(CombatFeatures.VANILLA_ATTACK)
		.add(CombatFeatures.VANILLA_KNOCKBACK)
		.build()
	val noTagTeam = MinecraftServer.getTeamManager()
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
	private val minimap = Minimap(id, map.spawnPos, map.phases.first().size, map.mmapBytes)
	private val zone = Zone(instance, map.spawnPos, map.phases) { center, size ->
		minimap.render(instance.players, center, size)
	}
	
	private val alivePlayers = mutableSetOf<UUID>()
	val playerCount: Int get() = instance.players.size

	val containers = mutableMapOf<String, Inventory>()
	
	init {
		bindEvents()
	}

	fun isAlive(player: Player): Boolean =
		alivePlayers.contains(player.uuid)
	
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
		
		eliminatePlayer(player, "게임을 떠났습니다.")
		
		return player.setInstance(
			LobbyInstance.instance,
			LobbyInstance.map.spawnPos
		)
	}
	
	fun start() {
		if (state != GameState.WAITING || playerCount == 0) return
		
		state = GameState.RUNNING
		alivePlayers.clear()
		alivePlayers.addAll(instance.players.map { it.uuid })
		
		for (player in instance.players) {
			player.gameMode = GameMode.SURVIVAL
			
			player.teleport(getRandomAirPos()).thenRun {
				player.team = noTagTeam

				minimap.give(player)

				instance.scheduler().scheduleNextTick {
					player.entityMeta.isFlyingWithElytra = true
				}
			}
		}
		
		zone.start()
	}
	
	private fun eliminatePlayer(player: Player, reason: String) {
		if (!alivePlayers.remove(player.uuid)) return

		spawnDeathChest(player)

		player.gameMode = GameMode.SPECTATOR
		instance.sendMessage(
			Component.text(
				"${player.username}님이 $reason (${alivePlayers.size}명 남음)"
			)
		)

		checkWinner()
	}
	
	private fun checkWinner() {
		if (state != GameState.RUNNING || alivePlayers.size > 1) return
		
		state = GameState.ENDING
		
		val winner = alivePlayers.firstOrNull()?.let {
			MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it)
		}
		
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
		
		instance.scheduler().scheduleTask({
			destroy()
		}, TaskSchedule.seconds(5), TaskSchedule.stop())
	}
	
	private fun getRandomAirPos(): Pos {
		val safeRadius = (zone.size / 2.0) - 20.0
		val randomX = map.spawnPos.x + Random.nextDouble(-safeRadius, safeRadius)
		val randomZ = map.spawnPos.z + Random.nextDouble(-safeRadius, safeRadius)
		
		return Pos(
			randomX,
			map.spawnPos.y + 90.0,
			randomZ,
			Random.nextFloat() * 360F,
			40F
		)
	}
	
	private fun destroy() {
		zone.stop()
		
		val transfers = instance.players.map { player ->
			leave(player)
		}.toTypedArray()
		
		CompletableFuture.allOf(*transfers).thenRun {
			MinecraftServer.getInstanceManager().unregisterInstance(instance)
			GameManager.remove(id)
		}
	}

	private fun spawnDeathChest(player: Player) {
		val items = player.inventory.itemStacks.filter { !it.isAir }
		if (items.isEmpty()) return

		val p = player.position
		val key = "${p.blockX()}_${p.blockY()}_${p.blockZ()}"
		val blockPos = Vec(
			p.blockX().toDouble(),
			p.blockY().toDouble(),
			p.blockZ().toDouble()
		)
		instance.setBlock(blockPos, Block.CHEST)

		val inv = Inventory(
			InventoryType.CHEST_3_ROW,
			Component.text(player.username)
		)
		items.take(inv.size).forEachIndexed { i, item ->
			inv.setItemStack(i, item)
		}
		containers[key] = inv
	}
	
	private fun bindEvents() {
		instance.eventNode().apply {
			addListener(PlayerSpawnEvent::class.java) { event ->
				val player = event.player
				
				player.respawnPoint = map.spawnPos
				
				player.gameMode = GameMode.SPECTATOR
				player.health = 20.0F
				player.inventory.clear()
			}
	
			addListener(ItemDropEvent::class.java) { event ->
				event.isCancelled = true
			}
			addListener(PlayerDeathEvent::class.java) { event ->
				event.chatMessage = null
	
				eliminatePlayer(event.player, "사망했습니다.")
			}
			addListener(PlayerDisconnectEvent::class.java) { event ->
				leave(event.player)
			}
	
			addFootstepListeners(this@GameInstance)
			addDoorListeners(this@GameInstance)
			addCombatListeners(this@GameInstance)
			addHealListeners(this@GameInstance)
			addLootListeners(this@GameInstance)
			addDeathboxListeners(this@GameInstance)
				
			addChild(featureSet.createNode())
		}
	}
}