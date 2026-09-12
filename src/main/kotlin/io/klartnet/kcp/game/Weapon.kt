package io.klartnet.kcp.game

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.collision.Aerodynamics
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.*
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.network.packet.server.play.SetCooldownPacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import java.time.Duration
import kotlin.math.abs

enum class Weapon(
	val material: Material,
	val reloadTicks: Int = 20,
	val maxAmmo: Int = 0
) {
	RIFLE(
		Material.IRON_HOE,
		maxAmmo = 30,
		reloadTicks = 2 * 20
	) {
		override fun onUse(p: Player) {
			p.fireGun(
				this,
				damage = 1f,
				range = 50.0
			)
		}
	},
	SNIPER(
		Material.SPYGLASS,
		maxAmmo = 1,
		reloadTicks = 5 * 20
	) {
		override fun onRelease(p: Player) {
			p.fireGun(
				this,
				damage = 10f,
				range = 100.0
			)
		}
		override fun onSwing(p: Player) {
			p.fireGun(
				this,
				damage = 10f,
				range = 100.0
			)
		}
	},
	TRIDENT(
		Material.DIAMOND_SPEAR,
		maxAmmo = 0,
		reloadTicks = 1 * 20
	) {
		override fun onUse(p: Player) {
			p.shootTrident(this)
		}
	};
	
	open fun onUse(p: Player) {}
	open fun onRelease(p: Player) {}
	open fun onSwing(p: Player) {}
	fun onReload(p: Player) = p.reloadGun(this)
	fun onHold(p: Player) = p.updateAmmoBar(this)
	
	companion object {
		private val registry = entries.associateBy { it.material }
		fun from(material: Material): Weapon? = registry[material]
	}
}

fun Player.heldWeapon(): Weapon? = Weapon.from(this.itemInMainHand.material())
fun Player.fireGun(weapon: Weapon, damage: Float, range: Double) {
	if (onCooldown(weapon.material)) return
	val tag = Tag.Integer("ammo_${weapon.material.name()}")
	val ammo = getTag(tag) ?: weapon.maxAmmo
	
	if (ammo <= 0) {
		reloadGun(weapon)
		return
	}
	
	val nextAmmo = ammo - 1
	setTag(tag, nextAmmo)
	updateAmmoBar(weapon)
	shootRay(damage, range)
	
	if (nextAmmo == 0 || weapon.maxAmmo == 1)
		reloadGun(weapon)
}
fun Player.reloadGun(weapon: Weapon) {
	if (weapon.maxAmmo <= 0 || onCooldown(weapon.material)) return
	val tag = Tag.Integer("ammo_${weapon.material.name()}")
	if ((getTag(tag) ?:weapon.maxAmmo) >= weapon.maxAmmo) return
	
	setCooldown(weapon.material, weapon.reloadTicks)
	setTag(tag, weapon.maxAmmo)
	
	playSound(
		Sound.sound(
			SoundEvent.ITEM_ARMOR_EQUIP_IRON,
			Sound.Source.PLAYER,
			1f, 1f
		)
	)
}
fun Player.updateAmmoBar(weapon: Weapon) {
	if (weapon.maxAmmo <= 0) 
		return sendActionBar(Component.empty())
	
	val ammo = getTag(Tag.Integer("ammo_${weapon.material.name()}")) ?: weapon.maxAmmo
	sendActionBar(
		Component.text(
			"탄약: $ammo / ${weapon.maxAmmo}",
			when {
				ammo > 10 -> NamedTextColor.GREEN
				ammo > 0 -> NamedTextColor.YELLOW
				else -> NamedTextColor.RED
			}
		)
	)
}

private fun Player.shootRay(damage: Float, range: Double) {
	if (this.instance == null) return
	val eye = this.position.add(0.0, this.eyeHeight, 0.0)
	val dir = this.position.direction().normalize()
	
	instance.playSound(
		Sound.sound(
			SoundEvent.ENTITY_FIREWORK_ROCKET_BLAST,
			Sound.Source.PLAYER,
			1f, 1.8f
		),
		eye
	)
	
	for (step in 1..(range * 2).toInt()) {
		val pt = eye.add(dir.mul(step * 0.5))
		if (!instance.getBlock(pt).air()) break

		instance.sendGroupedPacket(
			ParticlePacket(
				Particle.CRIT,
				pt,
				Vec.ZERO,
				0f, 1
			)
		)

		val target = instance.getNearbyEntities(pt, 2.0)
			.filterIsInstance<LivingEntity>()
			.firstOrNull { it != this && it.contains(pt) } ?: continue

		target.damage(
			Damage(
				DamageType.PLAYER_ATTACK,
				null,
				this,
				pt,
				damage
			)
		)

		this.playSound(
			Sound.sound(
				SoundEvent.ENTITY_ARROW_HIT_PLAYER,
				Sound.Source.PLAYER,
				1.5f, 1.2f
			)
		)
		break
	}
}
private fun Player.shootTrident(weapon: Weapon) {
	if (this.instance == null) return
	if (this.onCooldown(weapon.material)) return
	if (this.itemInMainHand.material() == weapon.material)
		this.itemInMainHand = itemInMainHand.consume(1)

	this.instance.playSound(
		Sound.sound(
			SoundEvent.ITEM_TRIDENT_THROW,
			Sound.Source.PLAYER,
			1.0F, 1.0F
		),
		this.position
	)

	val dir = this.position.direction()
	val spawnPos = this.position.add(0.0, eyeHeight, 0.0).add(dir.mul(0.8))

	val aerodyn = Aerodynamics(0.00, 0.99, 0.99)
	val vel = dir.mul(40.0)

	val trident = EntityProjectile(
		this,
		EntityType.TRIDENT
	).apply {
		aerodynamics = aerodyn
		velocity = vel
		setNoGravity(true)

		scheduleRemove(Duration.ofSeconds(3))
	}

	val node = trident.eventNode()
	node.addListener(EntityTickEvent::class.java) {
		trident.instance.sendGroupedPacket(
			ParticlePacket(
				Particle.SONIC_BOOM,
				trident.position,
				Vec.ZERO,
				0f,
				1
			)
		)
	}
	node.addListener(ProjectileCollideWithBlockEvent::class.java) {
		trident.instance.explode(
			it.collisionPosition.x.toFloat(),
			it.collisionPosition.y.toFloat(),
			it.collisionPosition.z.toFloat(),
			5f
		)

		trident.remove()
	}
	node.addListener(ProjectileCollideWithEntityEvent::class.java) { event ->
		val target = event.target as? Player ?: return@addListener
		target.damage(
			Damage(
				DamageType.TRIDENT,
				null,
				this,
				null,
				8.0f
			)
		)

		this.playSound(
			Sound.sound(
				SoundEvent.ENTITY_ARROW_HIT_PLAYER,
				Sound.Source.PLAYER,
				2.5f,
				1.0f
			)
		)

		trident.remove()
	}
	trident.setInstance(this.instance, spawnPos)
}

private fun Entity.contains(point: Point): Boolean {
	val rel = point.sub(position)
	return abs(rel.x()) <= boundingBox.width() / 2.0 &&
		abs(rel.z()) <= boundingBox.depth() / 2.0 &&
		rel.y() in 0.0..boundingBox.height()
}
fun Player.onCooldown(material: Material): Boolean {
	val cooldown = getTag(Tag.Long("cd_${material.name()}")) ?: 0L

	return cooldown > System.currentTimeMillis()
}
fun Player.setCooldown(material: Material, ticks: Int) {
	setTag(
		Tag.Long("cd_${material.name()}"),
		System.currentTimeMillis() + (ticks * 50L)
	)

	sendPacket(
		SetCooldownPacket(
			material.name(),
			ticks
		)
	)
}