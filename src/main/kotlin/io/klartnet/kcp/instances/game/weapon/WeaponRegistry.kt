package io.klartnet.kcp.instances.game.weapon

import io.klartnet.kcp.instances.game.weapon.guns.Rifle
import io.klartnet.kcp.instances.game.weapon.guns.Rpg
import io.klartnet.kcp.instances.game.weapon.guns.Sniper
import net.minestom.server.entity.Player
import net.minestom.server.item.Material

private object WeaponRegistry {
	private val weapons = listOf(
		Rifle(),
		Rpg(),
		Sniper()
	)
	
	private val registry = weapons.associateBy { it.item.material }
	
	fun from(material: Material): Weapon? = registry[material]
}

fun Player.heldWeapon(): Weapon? {
	return WeaponRegistry.from(this.itemInMainHand.material())
}