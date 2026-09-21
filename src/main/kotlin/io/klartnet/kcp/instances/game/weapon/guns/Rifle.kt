package io.klartnet.kcp.instances.game.weapon.guns

import io.klartnet.kcp.instances.game.loot.Item
import io.klartnet.kcp.instances.game.weapon.Weapon
import io.klartnet.kcp.instances.game.weapon.WeaponSound
import net.kyori.adventure.key.Key
import net.minestom.server.sound.SoundEvent

class Rifle : Weapon(
	Item.RIFLE,
	maxAmmo = 30,
	maxRange = 50.0,
	damage = 1.0f,
	reloadTicks = 2 * 20,
	sound = WeaponSound(
		shootSound = SoundEvent.of(
			Key.key("guns", "machinepistol_shoot"),
			16f
		),
		reloadSound = SoundEvent.of(
			Key.key("guns", "machinepistol_reload"),
			16f
		)
	)
)