package io.klartnet.kcp.instances.game

import net.minestom.server.coordinate.Point
import net.minestom.server.inventory.Inventory

class GameContainers {
	private val inventories = mutableMapOf<Point, Inventory>()
	
	fun getOrCreate(
		position: Point,
		factory: () -> Inventory
	): Inventory =
		inventories.computeIfAbsent(position) { factory() }
	
	fun clear() {
		inventories.clear()
	}
}