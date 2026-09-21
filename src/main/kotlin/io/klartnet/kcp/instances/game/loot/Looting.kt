package io.klartnet.kcp.instances.game.loot

import io.klartnet.kcp.instances.game.GameInstance
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.sound.SoundEvent
import kotlin.random.Random

private class LootEntry(
	val item: Item,
	val amount: IntRange = 1..1,
	val weight: Int = 1
)

private val lootTable = listOf(
	LootEntry(
		Item.RIFLE,
		amount = 1..1,
		weight = 15,
	),
	LootEntry(
		Item.SNIPER,
		amount = 1..1,
		weight = 10,
	),
	LootEntry(
		Item.RPG,
		amount = 1..1,
		weight = 5
	),
	LootEntry(
		Item.AMMO,
		amount = 15..20,
		weight = 70
	),
	LootEntry(
		Item.BANDAGE,
		amount = 3..3,
		weight = 40
	),
)

private fun rollLoot(): ItemStack {
	val total = lootTable.sumOf { it.weight }
	var roll = Random.nextInt(total)
	for (entry in lootTable) {
		roll -= entry.weight
		if (roll < 0)
			return entry.item.getItemStack().withAmount(entry.amount.random())
	}
	
	val first = lootTable.first()
	return first.item.getItemStack().withAmount(first.amount.random())
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
		player.instance.playSound(
			Sound.sound(
				SoundEvent.BLOCK_CHEST_OPEN,
				Sound.Source.BLOCK,
				0.5f, 1.0f
			),
			blockPos.x(),
			blockPos.y(),
			blockPos.z()
		)
	}
}