package io.klartnet.kcp.instances.game.weapon

import io.klartnet.kcp.instances.game.loot.Item
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag

interface ConsumableWeapon

abstract class Weapon(
	val item: Item,
	val maxAmmo: Int = 0,
	val maxRange: Double = 0.0,
	val damage: Float = 0.0f,
	val reloadTicks: Int,
	val sound: WeaponSound
) {
	private val AMMO_TAG = Tag.Integer("ammo_${this.item.material.name()}")
	private val LASER_TAG = Tag.Long("laser")
	
	open fun onUse(player: Player) = fireGun(player, false)
	open fun onRelease(player: Player) {}
	open fun onSwing(player: Player) {}
	open fun onReload(player: Player) = reloadGun(player)
	open fun onHold(player: Player) = updateAmmoBar(player)
	open fun onMove(player: Player) {}
	
	open fun onShoot(player: Player) = shootRay(player)
	
	protected fun fireGun(player: Player, isCustomAction: Boolean = false) {
		if (player.instance == null)
			return
		if (player.onCooldown(this.item.material))
			return

		if (this is ConsumableWeapon) {
			player.itemInMainHand = player.itemInMainHand.consume(1)
			this.sound.playFireSound(player)
		} else {
			var ammo: Int = player.getTag(AMMO_TAG) ?: this.maxAmmo
			if (ammo <= 0) {
				reloadGun(player)
				return
			}

			ammo -= 1
			player.setTag(AMMO_TAG, ammo)
			
			this.sound.playFireSound(player)
			updateAmmoBar(player)
			onShoot(player)

			if (ammo <= 0)
				reloadGun(player)
		}
	}
	protected fun simulateLaser(
		player: Player
	) {
		val now = System.currentTimeMillis()
		if (now - (player.getTag(LASER_TAG) ?: 0L) < 100)
			return
		player.setTag(LASER_TAG, now)
		
		if (player.instance == null)
			return

		val dir = player.position.direction().normalize()
		val eye = player.position.add(
			0.0,
			if (player.isSneaking) 1.27 else player.eyeHeight,
			0.0
		)

		for (step in 1..(this.maxRange * 2).toInt()) {
			val point = eye.add(dir.mul(step * 0.5))
			val blockedBlock = player.instance.getBlock(point)
			if (blockedBlock.solid()) break

			player.instance.players.forEach {
				if (it != player)
					it.sendPacket(
						ParticlePacket(
							Particle.CRIT,
							point,
							Vec.ZERO,
							0f, 1
						)
					)
			}
		}
	}


	private fun getAllAmmo(player: Player): Int {
		return player.inventory.itemStacks
			.filter { it.material() == Item.AMMO.material }
			.sumOf { it.amount() }
	}
	private fun consumeAmmo(player: Player, count: Int): Int {
		var needed = count
		for ((slot, item) in player.inventory.itemStacks.withIndex()) {
			if (item.material() != Item.AMMO.material) continue
			val take = minOf(needed, item.amount())
			player.inventory.setItemStack(slot, item.consume(take))
			needed -= take
			if (needed <= 0) break
		}
		return count - needed
	}
	private fun shootRay(player: Player) {
		if (player.instance == null) return
		if (player.heldWeapon() == null) return

		simulateLaser(player)
		
		val target = player.getLineOfSightEntity(this.maxRange) {
			it is LivingEntity
		} as? LivingEntity
			?: return

		player.playSound(
			Sound.sound(
				SoundEvent.ENTITY_ARROW_HIT_PLAYER,
				Sound.Source.PLAYER,
				1.5f, 1.2f
			)
		)

		target.damage(
			Damage(
				DamageType.PLAYER_ATTACK,
				null,
				player,
				target.position,
				damage
			)
		)
	}
	private fun reloadGun(player: Player) {
		if (player.onCooldown(this.item.material))
			return
		
		val currentAmmo = (player.getTag(AMMO_TAG) ?: this.maxAmmo).coerceAtLeast(0)
		val needed = this.maxAmmo - currentAmmo
		if (needed <= 0)
			return
		
		val reloaded = consumeAmmo(player, needed)
		if (reloaded <= 0) {
			player.playSound(
				Sound.sound(
					SoundEvent.BLOCK_DISPENSER_FAIL,
					Sound.Source.PLAYER,
					1.0f, 1.2f
				)
			)
			return
		}

		this.sound.playReloadSound(player)

		player.setCooldown(this.item.material, this.reloadTicks)
		player.setTag(AMMO_TAG, currentAmmo + reloaded)
	}
	private fun updateAmmoBar(player: Player) {
		if (this.maxAmmo <= 0)
			return player.sendActionBar(Component.empty())

		val currentAmmo = player.getTag(AMMO_TAG) ?: this.maxAmmo
		val totalAmmo = getAllAmmo(player)
		player.sendActionBar(
			Component.text(
				"$currentAmmo / $totalAmmo",
				when {
					currentAmmo > 10 -> NamedTextColor.GREEN
					currentAmmo > 0 -> NamedTextColor.YELLOW
					else -> NamedTextColor.RED
				}
			)
		)
	}
}