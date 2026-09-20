package io.klartnet.kcp.instances.game.weapon.guns

import io.klartnet.kcp.instances.game.loot.Item
import io.klartnet.kcp.instances.game.weapon.ConsumableWeapon
import io.klartnet.kcp.instances.game.weapon.Weapon
import io.klartnet.kcp.instances.game.weapon.WeaponSound
import net.kyori.adventure.key.Key
import net.minestom.server.collision.Aerodynamics
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import java.time.Duration

val RPG_TAG: Tag<Boolean> = Tag.Boolean("rpg")

class Rpg : Weapon(
	Item.RPG,
	reloadTicks = 1 * 20,
	sound = WeaponSound(
		shootSound = SoundEvent.ITEM_TRIDENT_THROW,
		reloadSound = Key.key("minecraft", "air")
	)
), ConsumableWeapon {
	override fun onUse(player: Player) {
		super.fireGun(player, true)

		val dir = player.position.direction()
		val eye = player.position.add(
			0.0,
			if (player.isSneaking) 1.27 else player.eyeHeight,
			0.0
		)

		val rpg = RpgProjectile(player)
			.apply {
				velocity = dir.mul(40.0)
			}

		rpg.setInstance(
			player.instance,
			eye.add(dir.mul(0.2))
		)
	}
}

class RpgProjectile(
	val owner: Player
) : EntityProjectile(owner, EntityType.TRIDENT) {
	init {
		this.setTag(RPG_TAG, true)
		
		this.aerodynamics = Aerodynamics(0.00, 0.99, 0.99)
		this.setNoGravity(true)
		this.setBoundingBox(0.1, 0.1, 0.1)
		
		this.scheduleRemove(Duration.ofSeconds(5))
		
		val node = this.eventNode()
		node.addListener(ProjectileCollideWithBlockEvent::class.java) { event ->
			val projectile = event.entity as? RpgProjectile
				?: return@addListener
			
			event.instance.explode(
				event.collisionPosition.x.toFloat(),
				event.collisionPosition.y.toFloat(),
				event.collisionPosition.z.toFloat(),
				15f
			)
			
			projectile.remove()
		}
		node.addListener(ProjectileCollideWithEntityEvent::class.java) { event ->
			val projectile = event.entity as? RpgProjectile
				?: return@addListener
			val player = event.target as? Player
				?: return@addListener

			event.instance.explode(
				event.collisionPosition.x.toFloat(),
				event.collisionPosition.y.toFloat(),
				event.collisionPosition.z.toFloat(),
				15f
			)
			
			player.damage(
				Damage(
					DamageType.EXPLOSION,
					null,
					owner,
					projectile.position,
					20.0f
				)
			)

			projectile.remove()
		}
	}
}