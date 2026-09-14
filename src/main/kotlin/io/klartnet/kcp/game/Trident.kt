package io.klartnet.kcp.game

import net.minestom.server.collision.Aerodynamics
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import java.time.Duration

val tridentTag = Tag.Boolean("trident")

class Trident(
	val owner: Player
) : EntityProjectile(owner, EntityType.TRIDENT) {
	init {
		this.setTag(tridentTag, true)
		
		aerodynamics = Aerodynamics(0.00, 0.99, 0.99)
		setNoGravity(true)
		setBoundingBox(0.1, 0.1, 0.1)
		
		scheduleRemove(Duration.ofSeconds(5))
	}
}