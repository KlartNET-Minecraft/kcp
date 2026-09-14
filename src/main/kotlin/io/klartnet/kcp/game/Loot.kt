package io.klartnet.kcp.game

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerMoveEvent
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
	ItemStack.of(Material.PAPER) to 70,
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

fun addLootListeners(game: GameInstance) {
	val node = game.instance.eventNode()
	
	node.addListener(PlayerBlockInteractEvent::class.java) { event ->
		if (event.hand != PlayerHand.MAIN || !game.isAlive(event.player)) return@addListener
		if (!event.block.compare(Block.CHEST) && !event.block.compare(Block.TRAPPED_CHEST)) return@addListener
		
		val blockPos = event.blockPosition
		val inventory = game.containers.getOrCreate(blockPos) {
			Inventory(
				InventoryType.CHEST_1_ROW,
				Component.text("아이템 상자")
			).apply {
				val itemCount = (3..4).random()
				(0 until this.size)
					.shuffled()
					.take(itemCount)
					.forEach { slot ->
						this.setItemStack(slot, rollLoot())
					}
			}
		}
		
		val player = event.player
		player.openInventory(inventory)
		player.playSound(
			Sound.sound(
				SoundEvent.BLOCK_CHEST_OPEN,
				Sound.Source.BLOCK,
				1f, 1f
			),
			blockPos.x(),
			blockPos.y(),
			blockPos.z()
		)
	}
}

fun addDropListeners(game: GameInstance) {
	val node = game.instance.eventNode()
	
	node.addListener(PlayerMoveEvent::class.java) { event ->
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