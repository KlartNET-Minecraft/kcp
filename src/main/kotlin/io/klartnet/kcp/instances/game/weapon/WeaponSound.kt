package io.klartnet.kcp.instances.game.weapon

import net.kyori.adventure.key.Keyed
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player


class WeaponSound(
	val shootSound: Keyed,
	val reloadSound: Keyed,
) {
	private fun Player.playWeaponSound(
		soundEvent: Keyed,
		volume: Float = 0.5f,
		pitch: Float = 1f
	) {
		if (this.instance == null) return

		val sound = Sound.sound(
			soundEvent.key(),
			Sound.Source.PLAYER,
			volume, pitch
		)

		this.playSound(
			sound,
			Sound.Emitter.self()
		)

		val eye = this.position.add(
			0.0,
			if (isSneaking) 1.27 else eyeHeight,
			0.0
		)
		this.instance.playSoundExcept(
			this,
			sound,
			eye
		)
	}

	fun playFireSound(player: Player) {
		player.playWeaponSound(
			shootSound.key(),
			0.8f, 1f
		)
	}
	fun playReloadSound(player: Player) {
		player.playWeaponSound(
			reloadSound.key(),
			0.8f, 1f
		)
	}
}
