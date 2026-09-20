package io.klartnet.kcp.instances.game.event

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.event.instance.InstanceUnregisterEvent
import net.minestom.server.event.player.*
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import java.util.*
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

private val STEPDIST_TAG = Tag.Double("step_dist")

fun addFootstepListeners(game: GameInstance) {
	val node = game.instance.eventNode()
	
	val diggingTasks = mutableMapOf<UUID, Task>()

	fun Player.stopDigging() {
		diggingTasks.remove(this.uuid)?.cancel()
	}

	node.addListener(PlayerMoveEvent::class.java) { event ->
		val player = event.player
		
		if (!game.isAlive(player) || !player.isOnGround) {
			player.setTag(STEPDIST_TAG, 0.0)
			return@addListener
		}
		
		val dx = event.newPosition.x - player.position.x
		val dz = event.newPosition.z - player.position.z
		val distance = hypot(dx, dz)
		
		if (distance <= 0.001)
			return@addListener

		val stepInterval = if (player.isSprinting) 1.3 else 1.9
		val travelled = (player.getTag(STEPDIST_TAG) ?: 0.0) + distance

		if (travelled < stepInterval) {
			player.setTag(STEPDIST_TAG, travelled)
			return@addListener
		}
		
		player.setTag(STEPDIST_TAG, travelled - stepInterval)

		val block = game.instance.getBlock(
			event.newPosition.blockX(),
			floor(event.newPosition.y - 0.1).toInt(),
			event.newPosition.blockZ()
		)
		if (block.air())
			return@addListener
		
		val blockSound = block.blockSoundType()?.stepSound() ?: SoundEvent.BLOCK_ANVIL_BREAK
		val basePitch = 0.9f + Random.nextFloat() * 0.2f
		game.instance.players.forEach { listener ->
			if (listener == player)
				return@forEach
			
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

	node.addListener(PlayerStartDiggingEvent::class.java) { event ->
		val player = event.player
		
		if (!game.isAlive(player))
			return@addListener
		
		player.stopDigging()
		
		val blockSound = event.block.blockSoundType()
			?: return@addListener
		val position = event.blockPosition.asPos().add(0.5, 0.5, 0.5)
		val sound = Sound.sound(
			blockSound.hitSound(),
			Sound.Source.BLOCK,
			1.0f, blockSound.pitch()
		)
		
		diggingTasks[player.uuid] = game.instance.scheduler().scheduleTask({
			if (!game.isAlive(player) || player.instance != game.instance) {
				player.stopDigging()
				return@scheduleTask
			}
			
			game.instance.playSoundExcept(
				player,
				sound,
				position
			)
		}, TaskSchedule.immediate(), TaskSchedule.tick(4))
	}
	node.addListener(PlayerCancelDiggingEvent::class.java) { event ->
		val player = event.player
		player.stopDigging()
	}
	node.addListener(PlayerFinishDiggingEvent::class.java) { event ->
		val player = event.player
		player.stopDigging()
	}
	node.addListener(PlayerDisconnectEvent::class.java) { event ->
		val player = event.player
		player.stopDigging()
		player.setTag(STEPDIST_TAG, 0.0)
	}
	node.addListener(InstanceUnregisterEvent::class.java) {
		diggingTasks.values.forEach(Task::cancel)
		diggingTasks.clear()
	}
}