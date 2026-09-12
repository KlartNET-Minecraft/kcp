package io.klartnet.kcp

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.timer.TaskSchedule

fun startDebugMonitor() {
	val runtime = Runtime.getRuntime()
	MinecraftServer.getSchedulerManager().scheduleTask({
		val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
		val maxMem = runtime.maxMemory() / (1024 * 1024)
		val instances = MinecraftServer.getInstanceManager().instances
		val chunks = instances.sumOf { it.chunks.size }
		val entities = instances.sumOf { it.entities.size }

		val bar = Component.text(
			"RAM: ${usedMem}/${maxMem}MB | Inst: ${instances.size} | Chunk: $chunks | Ent: $entities",
			NamedTextColor.GREEN
		)
		MinecraftServer.getConnectionManager().onlinePlayers.forEach { it.sendActionBar(bar) }
	}, TaskSchedule.immediate(), TaskSchedule.tick(10))
}