package io.klartnet.kcp.game

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import kotlin.random.Random

private val lootTable = listOf(
	ItemStack.of(Material.IRON_HOE) to 10,
	ItemStack.of(Material.SPYGLASS) to 10,
	ItemStack.of(Material.PAPER) to 80,
	ItemStack.of(Material.DIAMOND_SPEAR) to 1
)

private fun rollLoot(): ItemStack {
	val total = lootTable.sumOf { it.second }
	var roll = Random.nextInt(total)
	for ((item, weight) in lootTable) {
		roll -= weight
		if (roll < 0) return item
	}
	
	return lootTable.first().first
}

fun EventNode<InstanceEvent>.addLootListeners(game: GameInstance) {
	addListener(PlayerBlockInteractEvent::class.java) { event ->
		if (event.hand != PlayerHand.MAIN || !game.isAlive(event.player)) return@addListener
		if (!event.block.compare(Block.CHEST) && !event.block.compare(Block.TRAPPED_CHEST)) return@addListener
		
		val p = event.blockPosition
		val key = "${p.blockX()}_${p.blockY()}_${p.blockZ()}"
		val inv = game.containers.computeIfAbsent(key) {
			val chest = Inventory(
				InventoryType.CHEST_1_ROW,
				Component.text("아이템 상자")
			)
			val itemCount = (3..5).random()
			(0 until chest.size).shuffled().take(itemCount).forEach { slot ->
				chest.setItemStack(slot, rollLoot())
			}
			chest
		}

		val player = event.player
		player.openInventory(inv)
		player.playSound(
			Sound.sound(
				SoundEvent.BLOCK_CHEST_OPEN,
				Sound.Source.BLOCK,
				1f, 1f
			),
			p.x(),
			p.y(),
			p.z()
		)
	}
}
fun EventNode<InstanceEvent>.addDropListeners(game: GameInstance) {
	addListener(PlayerMoveEvent::class.java) { event ->
		val player = event.player
		if (event.newPosition.sameBlock(player.position))
			return@addListener
		if (!player.entityMeta.isFlyingWithElytra || !game.isAlive(player))
			return@addListener

		val altitude = (player.position.y - game.map.spawnPos.y).toInt().coerceAtLeast(0)
		player.sendActionBar(
			Component.text(
				"지상까지 ${altitude}m 남음",
				when {
					altitude > 40 -> NamedTextColor.AQUA
					altitude > 15 -> NamedTextColor.YELLOW
					else -> NamedTextColor.RED
				}
			)
		)
	}
}