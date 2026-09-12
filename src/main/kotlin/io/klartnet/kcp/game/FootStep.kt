package io.klartnet.kcp.game

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.sound.Sound
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

private val stepDistTag = Tag.Double("step_dist")

fun EventNode<InstanceEvent>.addFootstepListeners(game: GameInstance) {
	addListener(PlayerMoveEvent::class.java) { event ->
		val player = event.player
		if (!game.isAlive(player)) return@addListener
		if (player.isSneaking && !player.isOnGround) return@addListener

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
		player.setTag(stepDistTag, 0.0)

		val block = game.instance.getBlock(
			event.newPosition.blockX(),
			floor(event.newPosition.y - 0.1).toInt(),
			event.newPosition.blockZ()
		)
		val sound = when {
			block.name().contains("grass") || block.name().contains("dirt") ->
				SoundEvent.BLOCK_GRASS_STEP
			block.name().contains("wood") || block.name().contains("planks") ->
				SoundEvent.BLOCK_WOOD_STEP
			block.name().contains("sand") ->
				SoundEvent.BLOCK_SAND_STEP
			block.name().contains("gravel") ->
				SoundEvent.BLOCK_GRAVEL_STEP
			else ->
				SoundEvent.BLOCK_STONE_STEP
		}

		val pitch = 0.9f + Random.nextFloat() * 0.2f
		game.instance.players.forEach {
			if (it != player)
				it.playSound(
					Sound.sound(
						sound,
						Sound.Source.PLAYER, 2.5f, pitch
					),
					event.newPosition
				)
		}
	}
}