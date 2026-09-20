package io.klartnet.kcp.instances.game.event

import io.klartnet.kcp.instances.game.GameInstance
import io.klartnet.kcp.instances.game.loot.Item
import io.klartnet.kcp.instances.game.weapon.onCooldown
import io.klartnet.kcp.instances.game.weapon.setCooldown
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

enum class Heal(
	val item: Item,
	val healAmount: Float,
	val cooldownTicks: Int
) {
	BANDAGE(
		Item.BANDAGE,
		healAmount = 4.0f,
		cooldownTicks = 4 * 20
	);
	
	companion object {
		private val registry = entries.associateBy { it.item.material }
		fun from(material: Material): Heal? = registry[material]
	}
}

fun addHealListeners(game: GameInstance) {
	val node = game.instance.eventNode()
	
	node.addListener(PlayerUseItemEvent::class.java) { event ->
		val player = event.player
		if (!game.isAlive(player)) return@addListener
		
		val heal = Heal.from(event.itemStack.material()) ?: return@addListener
		if (player.health >= 20.0f || player.onCooldown(heal.item.material)) return@addListener
		
		player.sendActionBar(
			Component.text(
				"치료 중...",
				NamedTextColor.YELLOW
			)
		)
		
		player.setCooldown(
			heal.item.material,
			heal.cooldownTicks
		)
		player.setItemInHand(
			event.hand,
			player.getItemInHand(event.hand).consume(1)
		)
		player.health = (player.health + heal.healAmount).coerceAtMost(20.0f)

		game.instance.playSound(
			Sound.sound(
				SoundEvent.ENTITY_PLAYER_BURP,
				Sound.Source.PLAYER,
				0.5f, 1.2f
			),
			player.position
		)
	}
}