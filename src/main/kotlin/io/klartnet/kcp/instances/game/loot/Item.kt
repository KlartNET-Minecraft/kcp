package io.klartnet.kcp.instances.game.loot

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

private fun cleanText(text: String): TextComponent {
	return Component.text(text)
		.decoration(TextDecoration.ITALIC, false)
}
private fun cleanText(text: String, color: TextColor): TextComponent {
	return Component.text(text, color)
		.decoration(TextDecoration.ITALIC, false)
}

enum class ItemCategory(val textComponent: TextComponent) {
	NONE(
		cleanText(
			"분류 없음",
			NamedTextColor.DARK_GRAY
		)
	),
	WEAPON(
		cleanText(
			"일반 무기",
			NamedTextColor.DARK_GRAY
		)
	),
	HEAL(
		cleanText(
			"체력 아이템",
			NamedTextColor.RED
		)
	),
	AMMO(
		cleanText(
			"탄약",
			NamedTextColor.DARK_GRAY
		)
	),
}

enum class Item(
	val category: ItemCategory = ItemCategory.NONE,
	val material: Material,
	val itemName: Component = cleanText("이름 없음"),
	val lore: Component = cleanText(""),
	val modelKey: String = "",
) {
	AMMO(
		ItemCategory.AMMO,
		Material.IRON_NUGGET,
		itemName = cleanText(
			"탄약"
		),
		lore = cleanText(
			"모든 일반 무기와 호환되는 탄약입니다."
		),
		modelKey = "minecraft:ammo/bullets"
	),
	RIFLE(
		ItemCategory.WEAPON,
		Material.IRON_HOE,
		itemName = cleanText(
			"자동 소총"
		),
		modelKey = "minecraft:guns/ak_47"
	),
	SNIPER(
		ItemCategory.WEAPON,
		Material.SPYGLASS,
		itemName = cleanText(
			"저격 소총"
		),
		modelKey = "minecraft:guns/autosniper"
	),
	SHOTGUN(
		ItemCategory.WEAPON,
		Material.IRON_AXE,
		itemName = cleanText(
			"샷건",
		),
		modelKey = "minecraft:guns/pump_shotgun"
	),
	RPG(
		ItemCategory.WEAPON,
		Material.DIAMOND_SPEAR,
		itemName = cleanText(
			"로켓 발사기",
			NamedTextColor.AQUA
		),
		lore = cleanText(
			"격발 시 정면으로 로켓을 발사합니다."
		)
	),
	BANDAGE(
		ItemCategory.HEAL,
		Material.PAPER,
		itemName = cleanText(
			"붕대"
		),
		lore = cleanText(
			"사용 시 체력을 2칸 회복합니다."
		)
	);
	
	fun getItemStack(): ItemStack {
		val modelKey = this.modelKey
			.ifEmpty { this.material.key().asString() }
		
		return ItemStack.builder(this.material)
			.customName(this.itemName)
			.lore(listOf(category.textComponent, lore))
			.itemModel(modelKey)
			.build()
	}
}