package io.klartnet.kcp.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.WorldBorder
import net.minestom.server.network.packet.server.play.WorldBorderLerpSizePacket
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import kotlin.math.abs
import kotlin.random.Random

class Zone(
	private val instance: InstanceContainer,
	private val spawnPos: Pos,
	phases: List<ZonePhase>,
	private val onTick: ((center: Pos, size: Double) -> Unit)? = null
) {
	var center = Pos(
		spawnPos.x + Random.nextDouble(-50.0, 50.0),
		spawnPos.y,
		spawnPos.z + Random.nextDouble(-50.0, 50.0)
	)
	var size: Double = phases.first().size
		private set
	
	private val queue = ArrayDeque(phases.drop(1))
	/** 현재 Phase 인스턴스 */
	private var phase = queue.removeFirstOrNull()
	/** 현재 Phase 기준 축소까지 남은 시간(초) */
	private var timer = phase?.waitSec ?: 0L
	/** 자기장 축소 시작 직후인지 여부 */
	private var isShrinking = false
	/** 몰라 */
	private var startSize = size
	/** 몰라 */
	private var task: Task? = null
	
	fun start() {
		update()
		
		task = instance.scheduler().scheduleTask({
			tick()
			damage()
			onTick?.invoke(center, size)
		}, TaskSchedule.seconds(1), TaskSchedule.seconds(1))
	}
	
	fun stop() {
		task?.cancel()
		task = null
	}
	
	private fun tick() {
		val current = phase ?: return

		if (--timer > 0) {
			if (isShrinking) {
				val progress = 1.0 - (timer.toDouble() / current.shrinkSec)
				size = startSize + (current.size - startSize) * progress
			}
			return
		}
		
		if (isShrinking) {
			size = current.size
			isShrinking = false
			
			val next = queue.removeFirstOrNull()
			if (next == null) {
				timer = Long.MAX_VALUE
				instance.sendMessage(
					Component.text(
						"마지막 자기장에 도달했습니다!",
						NamedTextColor.DARK_RED
					)
				)
				return
			}
			
			phase = next
			timer = phase!!.waitSec
			instance.sendMessage(
				Component.text(
					"자기장 축소가 완료되었습니다. 다음 축소까지 ${timer}초 남았습니다.",
					NamedTextColor.GREEN
				)
			)
		} else {
			isShrinking = true
			timer = current.shrinkSec
			startSize = size
			instance.sendGroupedPacket(
				WorldBorderLerpSizePacket(
					size,
					current.size,
					current.shrinkSec * 20L
				)
			)
			
			instance.sendMessage(
				Component.text(
					"자기장 축소가 시작됩니다!",
					NamedTextColor.RED
				)
			)
		}
	}
	
	private fun update() {
		instance.worldBorder = WorldBorder(
			size,
			center.x, center.z,
			0, 0
		)
	}
	
	private fun damage() {
		val half = size / 2.0
		val dmg = Damage(
			DamageType.OUT_OF_WORLD,
			null, null, null,
			phase?.dps ?: 0.0f
		)
		val alert = Component.text(
			"자기장 밖입니다!",
			NamedTextColor.RED
		)
		
		for (player in instance.players) {
			if (player.gameMode == GameMode.SPECTATOR) continue
			
			val p = player.position
			val isOutside = abs(p.x - center.x) > half || abs(p.z - center.z) > half
			if (isOutside) {
				player.damage(dmg)
				player.sendActionBar(alert)
			}
		}
	}
}