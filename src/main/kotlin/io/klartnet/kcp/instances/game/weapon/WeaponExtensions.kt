package io.klartnet.kcp.instances.game.weapon

import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.SetCooldownPacket
import net.minestom.server.tag.Tag

internal fun Player.onCooldown(material: Material): Boolean {
	val cooldown = this.getTag(
		Tag.Long("cd_${material.name()}")
	) ?: 0L

	return cooldown > System.currentTimeMillis()
}
internal fun Player.setCooldown(material: Material, ticks: Int) {
	this.setTag(
		Tag.Long("cd_${material.name()}"),
		System.currentTimeMillis() + (ticks * 50L)
	)

	this.sendPacket(
		SetCooldownPacket(
			material.name(),
			ticks
		)
	)
}