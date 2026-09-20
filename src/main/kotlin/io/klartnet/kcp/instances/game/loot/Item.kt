package io.klartnet.kcp.instances.game.loot

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

enum class Item(
	val material: Material,
	val itemName: Component = Component.text("이름 없음"),
	val lore: Component = Component.text(""),
	val modelKey: String = "",
) {
	RIFLE(
		material = Material.IRON_HOE,
		itemName = Component.text(
			"자동 소총",
			NamedTextColor.GRAY
		),
		modelKey = "minecraft:guns/ak_47"
	),
	SNIPER(
		material = Material.SPYGLASS,
		itemName = Component.text(
			"저격 소총",
			NamedTextColor.GRAY
		),
		modelKey = "minecraft:guns/autosniper"
	),
	RPG(
		material = Material.DIAMOND_SPEAR,
		itemName = Component.text(
			"로켓 발사기"
		),
		lore = Component.text(
			"격발 시 정면으로 로켓을 발사합니다."
		)
	),
	BANDAGE(
		material = Material.PAPER,
		itemName = Component.text(
			"붕대"
		),
		lore = Component.text(
			"사용 시 체력을 회복합니다."
		)
	);
	
	fun getItemStack(): ItemStack {
		val modelKey =
			if (this.modelKey.isEmpty())
				this.material.key().asString()
			else
				this.modelKey
		
		return ItemStack.builder(this.material)
			.customName(this.itemName)
			.lore(this.lore)
			.itemModel(modelKey)
			.build()
	}
}