package io.klartnet.kcp.game

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.TaskSchedule

enum class Heal(
	val material: Material,
	val healAmount: Float,
	val useSec: Long,
	val cooldownTicks: Int
) {
	BANDAGE(
		Material.PAPER,
		6.0f,
		2L,
		2 * 20
	);
	
	companion object {
		private val registry = entries.associateBy { it.material }
		fun from(material: Material): Heal? = registry[material]
	}
}

private val HEALING_TAG = Tag.Boolean("is_healing")

fun addHealListeners(game: GameInstance) {
	val node = game.instance.eventNode()
	
	node.addListener(PlayerUseItemEvent::class.java) { event ->
		val player = event.player
		if (!game.isAlive(player)) return@addListener
		
		val heal = Heal.from(event.itemStack.material()) ?: return@addListener
		if (player.health >= 20.0f || player.onCooldown(heal.material) || player.hasTag(HEALING_TAG)) return@addListener
		
		player.setTag(HEALING_TAG, true)
		player.sendActionBar(
			Component.text(
				"치료 중... (${heal.useSec}초)",
				NamedTextColor.YELLOW
			)
		)
		
		game.instance.scheduler().scheduleTask({
			player.removeTag(HEALING_TAG)
			if (!game.isAlive(player) || player.getItemInHand(event.hand).material() != heal.material)
				return@scheduleTask
			
			player.setCooldown(
				heal.material,
				heal.cooldownTicks
			)
			player.setItemInHand(
				event.hand, player.getItemInHand(event.hand).consume(1)
			)
			player.health = (player.health + heal.healAmount).coerceAtMost(20.0f)

			player.playSound(
				Sound.sound(
					SoundEvent.ENTITY_PLAYER_BURP,
					Sound.Source.PLAYER,
					1.0f, 1.2f
				)
			)
		}, TaskSchedule.seconds(heal.useSec), TaskSchedule.stop())
	}
}