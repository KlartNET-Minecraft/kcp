package io.klartnet.kcp.instances.game.weapon.guns

import io.klartnet.kcp.instances.game.loot.Item
import io.klartnet.kcp.instances.game.weapon.Weapon
import io.klartnet.kcp.instances.game.weapon.WeaponSound
import net.kyori.adventure.key.Key
import net.minestom.server.entity.Player
import net.minestom.server.sound.SoundEvent

class Sniper : Weapon(
	Item.SNIPER,
	maxAmmo = 1,
	maxRange = 50.0,
	damage = 8.0f,
	reloadTicks = 5 * 20,
	sound = WeaponSound(
		shootSound = SoundEvent.of(
			Key.key("guns", "awp_shoot"),
			16f
		),
		reloadSound = SoundEvent.of(
			Key.key("guns", "awp_reload"),
			16f
		)
	)
) {
	override fun onUse(player: Player) {}
	override fun onRelease(player: Player) = super.fireGun(player, false)
	override fun onMove(player: Player) = super.simulateLaser(player)
}