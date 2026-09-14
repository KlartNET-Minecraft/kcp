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
	val maxAmmo: Int = 0,
	val maxRange: Double = 0.0,
	val reloadTicks: Int = 20,
) {
	RIFLE(
		Material.IRON_HOE,
		maxAmmo = 30,
		maxRange = 50.0,
		reloadTicks = 2 * 20
	) {
		override fun onUse(p: Player) {
			p.fireGun(
				this,
				damage = 1f,
				range = this.maxRange
			)
		}
	},
	SNIPER(
		Material.SPYGLASS,
		maxAmmo = 1,
		maxRange = 100.0,
		reloadTicks = 5 * 20
	) {
		override fun onRelease(p: Player) {
			p.fireGun(
				this,
				damage = 10f,
				range = this.maxRange
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
private fun Player.fireGun(weapon: Weapon, damage: Float, range: Double) {
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
private fun Player.reloadGun(weapon: Weapon) {
	if (weapon.maxAmmo <= 0 || this.onCooldown(weapon.material)) return
	val tag = Tag.Integer("ammo_${weapon.material.name()}")
	if ((this.getTag(tag) ?: weapon.maxAmmo) >= weapon.maxAmmo) return
	
	this.setCooldown(weapon.material, weapon.reloadTicks)
	this.setTag(tag, weapon.maxAmmo)
	
	this.playSound(
		Sound.sound(
			SoundEvent.ITEM_ARMOR_EQUIP_IRON,
			Sound.Source.PLAYER,
			1f, 1f
		)
	)
}
private fun Player.updateAmmoBar(weapon: Weapon) {
	if (weapon.maxAmmo <= 0) 
		return sendActionBar(Component.empty())
	
	val ammo = getTag(Tag.Integer("ammo_${weapon.material.name()}")) ?: weapon.maxAmmo
	sendActionBar(
		Component.text(
			"$ammo / ${weapon.maxAmmo}",
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
	val dir = this.position.direction().normalize()
	val eye = this.position.add(
		0.0,
		if (isSneaking) 1.27 else this.eyeHeight,
		0.0
	)
	
	this.playWeaponSound(
		SoundEvent.ENTITY_FIREWORK_ROCKET_BLAST,
		1.8f
	)
	
	for (step in 1..(range * 2).toInt()) {
		val point = eye.add(dir.mul(step * 0.5))
		val blockedBlock = this.instance.getBlock(point)
		
		if (blockedBlock.solid()) {
			val blockSound = blockedBlock.blockSoundType() ?: continue
			
			this.instance.playSound(
				Sound.sound(
					blockSound.breakSound(),
					Sound.Source.BLOCK,
					0.5f, 1.0f
				),
				point
			)
			break
		}

		this.instance.players.forEach { player ->
			if (player != this)
				player.sendPacket(
					ParticlePacket(
						Particle.CRIT,
						point,
						Vec.ZERO,
						0f, 1
					)
				)
		}

		val target = this.instance.getNearbyEntities(point, 2.0)
			.filterIsInstance<LivingEntity>()
			.firstOrNull { it != this && it.contains(point) } ?: continue

		target.damage(
			Damage(
				DamageType.PLAYER_ATTACK,
				null,
				this,
				point,
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

	this.playWeaponSound(
		SoundEvent.ITEM_TRIDENT_THROW,
		1.0f
	)

	val dir = this.position.direction()
	val eye = this.position.add(
		0.0,
		if (isSneaking) 1.27 else eyeHeight,
		0.0
	)
	val spawnPos = eye.add(dir.mul(0.2))

	val aerodyn = Aerodynamics(0.00, 0.99, 0.99)
	val vel = dir.mul(40.0)

	val trident = EntityProjectile(
		this,
		EntityType.TRIDENT
	).apply {
		aerodynamics = aerodyn
		velocity = vel
		setNoGravity(true)
		
		setBoundingBox(0.1, 0.1, 0.1)

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
private fun Player.playWeaponSound(soundEvent: SoundEvent, pitch: Float = 1f) {
	if (instance == null) return
	
	val sound = Sound.sound(
		soundEvent,
		Sound.Source.PLAYER,
		1f,
		pitch
	)

	playSound(sound)

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
fun Player.drawWeaponLaser(range: Double) {
	if (this.instance == null) return
	val dir = this.position.direction().normalize()
	val eye = this.position.add(
		0.0,
		if (isSneaking) 1.27 else this.eyeHeight,
		0.0
	)

	for (step in 1..(range * 2).toInt()) {
		val point = eye.add(dir.mul(step * 0.5))
		val blockedBlock = this.instance.getBlock(point)
		if (blockedBlock.solid()) break
		
		this.instance.players.forEach {
			if (it != this)
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

private fun Entity.contains(point: Point): Boolean {
	val rel = point.sub(position)
	return abs(rel.x()) <= boundingBox.width() / 2.0 &&
		abs(rel.z()) <= boundingBox.depth() / 2.0 &&
		rel.y() in 0.0..boundingBox.height()
}
fun Player.onCooldown(material: Material): Boolean {
	val cooldown = getTag(
		Tag.Long("cd_${material.name()}")
	) ?: 0L

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