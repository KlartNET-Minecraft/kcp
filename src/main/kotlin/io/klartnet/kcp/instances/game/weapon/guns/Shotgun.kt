package io.klartnet.kcp.instances.game.weapon.guns

import io.klartnet.kcp.instances.game.loot.Item
import io.klartnet.kcp.instances.game.weapon.Weapon
import io.klartnet.kcp.instances.game.weapon.WeaponSound
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.sound.SoundEvent
import java.util.concurrent.ThreadLocalRandom

class Shotgun : Weapon(
	Item.SHOTGUN,
	maxAmmo = 1,
	maxRange = 20.0,
	damage = 1.0f,
	reloadTicks = 1 * 20,
	sound = WeaponSound(
		shootSound = SoundEvent.of(
			Key.key("guns:pump_shoot"),
			16f,
		),
		reloadSound = SoundEvent.of(
			Key.key("guns:pump_rack"),
			16f
		)
	)
) {
	override fun onShoot(player: Player) {
		if (player.instance == null) return

		val eyePos = player.position.add(0.0, player.eyeHeight, 0.0)
		val dir = player.position.direction()

		repeat(8) {
			val random = ThreadLocalRandom.current()
			val spread = Vec(
				(random.nextDouble() - 0.5) * 0.4,
				(random.nextDouble() - 0.5) * 0.4,
				(random.nextDouble() - 0.5) * 0.4
			)
			val vel = dir.add(spread).normalize().mul(50.0)

			val spit = Entity(EntityType.LLAMA_SPIT).apply {
				velocity = vel
			}
			spit.eventNode().addListener(EntityTickEvent::class.java) { event ->
				if (spit.isRemoved) return@addListener

				if (
					spit.position.distance(eyePos) > maxRange ||
					spit.velocity.lengthSquared() < 0.01
				) {
					spit.remove()
					return@addListener
				}

				val target = player.instance.getNearbyEntities(spit.position, 1.2)
					.firstOrNull { it is LivingEntity && it != player && it != this }
					as? LivingEntity
					?: return@addListener

				target.damage(
					Damage(
						DamageType.PLAYER_ATTACK,
						null,
						player,
						target.position,
						damage
					)
				)
				player.playSound(
					Sound.sound(
						SoundEvent.ENTITY_ARROW_HIT_PLAYER,
						Sound.Source.PLAYER,
						1.5f, 1.2f
					)
				)
				spit.remove()
			}
			spit.setInstance(player.instance, eyePos)
		}
	}
}