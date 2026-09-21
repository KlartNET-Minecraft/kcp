package io.klartnet.kcp.instances.game.weapon

import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.sound.SoundEvent


class WeaponSound(
	val shootSound: SoundEvent,
	val reloadSound: SoundEvent,
) {
	private fun Player.playWeaponSound(
		soundEvent: SoundEvent,
		volume: Float = 0.5f,
		pitch: Float = 1f
	) {
		if (this.instance == null) return

		this.playSound(
			Sound.sound(
				soundEvent,
				Sound.Source.PLAYER,
				volume, pitch
			),
			Sound.Emitter.self()
		)

		val eye = this.position.add(
			0.0,
			if (isSneaking) 1.27 else eyeHeight,
			0.0
		)
		this.instance.playSoundExcept(
			this,
			Sound.sound(
				soundEvent.key(),
				Sound.Source.PLAYER,
				volume + 0.5f, pitch
			),
			eye
		)
	}

	fun playFireSound(player: Player) {
		player.playWeaponSound(
			shootSound,
			0.8f, 1f
		)
	}
	fun playReloadSound(player: Player) {
		player.playWeaponSound(
			reloadSound,
			0.8f, 1f
		)
	}
}
