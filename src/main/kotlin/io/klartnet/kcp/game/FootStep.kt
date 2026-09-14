package io.klartnet.kcp.game

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceUnregisterEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import java.util.*
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

private val stepDistTag = Tag.Double("step_dist")

fun EventNode<InstanceEvent>.addFootstepListeners(game: GameInstance) {
	val diggingTasks = mutableMapOf<UUID, Task>()

	fun Player.stopDigging() {
		diggingTasks.remove(this.uuid)?.cancel()
	}
	
	addListener(PlayerMoveEvent::class.java) { event ->
		val player = event.player
		if (!game.isAlive(player)) return@addListener
		if (!player.isOnGround) {
			player.setTag(stepDistTag, 0.0)
			return@addListener
		}
		
		val dx = event.newPosition.x - player.position.x
		val dz = event.newPosition.z - player.position.z
		val dist = hypot(dx, dz)
		if (dist <= 0.001) return@addListener

		val total = (player.getTag(stepDistTag) ?: 0.0) + dist
		val stepInterval = if (player.isSprinting) 1.3 else 1.9

		if (total < stepInterval) {
			player.setTag(stepDistTag, total)
			return@addListener
		}
		player.setTag(stepDistTag, total - stepInterval)

		val block = game.instance.getBlock(
			event.newPosition.blockX(),
			floor(event.newPosition.y - 0.1).toInt(),
			event.newPosition.blockZ()
		)
		if (block.air()) return@addListener
		
		val blockSound = block.blockSoundType()?.stepSound() ?: SoundEvent.BLOCK_ANVIL_BREAK
		val basePitch = 0.9f + Random.nextFloat() * 0.2f
		game.instance.players.forEach { listener ->
			if (listener != player) return@addListener
			if (listener == player) return@forEach
			
			val dy = event.newPosition.y - listener.position.y
			val pitch = when {
				dy > 1.5 -> basePitch * 0.7f
				dy < -1.5 -> basePitch * 1.3f
				else -> basePitch
			}
			
			listener.playSound(
				Sound.sound(
					blockSound,
					Sound.Source.PLAYER,
					1.0f, pitch
				),
				event.newPosition
			)
		}
	}

	addListener(PlayerStartDiggingEvent::class.java) { event ->
		val player = event.player
		if (!game.isAlive(player)) return@addListener
		
		player.stopDigging()
		
		val blockSound = event.block.blockSoundType() ?: return@addListener
		val position = event.blockPosition.asPos().add(0.5, 0.5, 0.5)
		val sound = Sound.sound(
			blockSound.hitSound(),
			Sound.Source.BLOCK,
			blockSound.volume(), blockSound.pitch()
		)
		
		diggingTasks[player.uuid] = game.instance.scheduler().scheduleTask({
			if (!game.isAlive(player) || player.instance != game.instance) {
				player.stopDigging()
				return@scheduleTask
			}
			
			game.instance.players
				.filter { it != player }
				.forEach { it.playSound(sound, position) }
		}, TaskSchedule.immediate(), TaskSchedule.tick(4))
	}
	addListener(PlayerCancelDiggingEvent::class.java) { event ->
		val player = event.player
		player.stopDigging()
	}
	addListener(PlayerFinishDiggingEvent::class.java) { event ->
		val player = event.player
		player.stopDigging()
	}
	addListener(PlayerDisconnectEvent::class.java) { event ->
		val player = event.player
		player.stopDigging()
		player.setTag(stepDistTag, 0.0)
	}
	addListener(InstanceUnregisterEvent::class.java) {
		diggingTasks.values.forEach(Task::cancel)
		diggingTasks.clear()
	}
}