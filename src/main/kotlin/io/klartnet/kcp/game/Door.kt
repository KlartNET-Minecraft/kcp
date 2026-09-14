package io.klartnet.kcp.game

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.sound.SoundEvent
import kotlin.math.abs

fun addDoorListeners(game: GameInstance) {
	val node = game.instance.eventNode()

	node.addListener(PlayerBlockInteractEvent::class.java) { event ->
		val player = event.player
		if (event.hand != PlayerHand.MAIN || !game.isAlive(player)) return@addListener
		if (!event.block.name().endsWith("_door")) return@addListener
		
		val isUpper = event.block.getProperty("half") == "upper"
		val lowerPos =
			if (isUpper) event.blockPosition.sub(0.0, 1.0, 0.0)
			else event.blockPosition
		val upperPos = lowerPos.add(0.0, 1.0, 0.0)
		
		val lowerBlock = game.instance.getBlock(lowerPos)
		val upperBlock = game.instance.getBlock(upperPos)
		val isDoor = lowerBlock.name().endsWith("_door") && upperBlock.name().endsWith("_door")
		if (!isDoor) return@addListener
		
		val isOpen = lowerBlock.getProperty("open") == "true"
		
		if (!isOpen) {
			val cx = lowerPos.blockX() + 0.5
			val cz = lowerPos.blockZ() + 0.5
			val by = lowerPos.blockY().toDouble()

			val isBlocked = game.instance.players.any { p ->
				p != event.player && game.isAlive(p) &&
					abs(p.position.x - cx) < 0.7 &&
					abs(p.position.z - cz) < 0.7 &&
					p.position.y in (by - 0.5)..(by + 2.0)
			}

			if (isBlocked) {
				game.instance.playSound(
					Sound.sound(
						SoundEvent.BLOCK_WOODEN_DOOR_CLOSE,
						Sound.Source.BLOCK,
						1.0f, 1.8f
					),
					lowerPos.x(),
					lowerPos.y(),
					lowerPos.z()
				)
				return@addListener
			}
		}
		
		val nextState = if (isOpen) "false" else "true"
		game.instance.setBlock(
			lowerPos,
			lowerBlock.withProperty("open", nextState)
		)
		game.instance.setBlock(
			upperPos,
			upperBlock.withProperty("open", nextState)
		)
		
		val isIron = event.block.name().contains("iron")
		game.instance.playSoundExcept(
			player,
			Sound.sound(
				when {
					isOpen ->
						if (isIron) SoundEvent.BLOCK_IRON_DOOR_CLOSE
						else SoundEvent.BLOCK_WOODEN_DOOR_CLOSE
					else ->
						if (isIron) SoundEvent.BLOCK_IRON_DOOR_OPEN
						else SoundEvent.BLOCK_WOODEN_DOOR_OPEN
				},
				Sound.Source.BLOCK,
				1.0f, 1.0f
			),
			lowerPos
		)
	}
}