package io.klartnet.kcp.instances.game.weapon.guns

import io.klartnet.kcp.instances.game.loot.Item
import io.klartnet.kcp.instances.game.weapon.Weapon
import io.klartnet.kcp.instances.game.weapon.WeaponSound
import net.kyori.adventure.key.Key

class Rifle : Weapon(
	Item.RIFLE,
	maxAmmo = 30,
	maxRange = 50.0,
	reloadTicks = 2 * 20,
	sound = WeaponSound(
		shootSound = Key.key("guns", "tacar_shoot"),
		reloadSound = Key.key("guns", "ak_reload")
	)
)