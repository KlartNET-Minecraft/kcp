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

val Rifle = ItemStack.builder(Material.IRON_HOE)
	.itemModel("minecraft:guns/ak_47")
	.customName(
		Component.text(
			"자동 소총",
			NamedTextColor.GRAY
		)
	)
	.build()
val Sniper = ItemStack.builder(Material.SPYGLASS)
	.itemModel("minecraft:guns/autosniper")
	.customName(
		Component.text(
			"저격 소총",
			NamedTextColor.GRAY
		)
	)
	.build()
val Rpg = ItemStack.builder(Material.DIAMOND_SPEAR)
	.itemModel("minecraft:guns/trident")
	.customName(
		Component.text(
			"로켓 발사기",
			NamedTextColor.RED
		)
	)
	.lore(
		Component.text(
			"격발 시 정면으로 로켓을 발사합니다.",
			NamedTextColor.GRAY
		)
	)
	.glowing()
	.build()

val Syrnge = ItemStack.builder(Material.PAPER)
	.customName(Component.text("붕대", NamedTextColor.RED))
	.lore(
		Component.text(
			"사용 시 체력을 회복합니다.",
			NamedTextColor.GRAY
		)
	)
	.build()

private val lootTable = listOf(
	Rifle to 10,
	Sniper to 10,
	Syrnge to 70,
	Rpg to 1
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