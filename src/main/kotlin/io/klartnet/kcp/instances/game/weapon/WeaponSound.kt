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

		this.playSound(
			Sound.sound(
				soundEvent.key(),
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
				//TODO 본인 제외 모든 플레이어는 멀리서도 들릴 수 있도록 볼륨 수정
				volume + 5.0f, pitch
			),
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
