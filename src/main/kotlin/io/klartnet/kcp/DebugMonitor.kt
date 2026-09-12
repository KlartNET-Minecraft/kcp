package io.klartnet.kcp

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.tag.Tag
import net.minestom.server.timer.TaskSchedule

val DEBUGGER_TAG = Tag.Boolean("DEBUGGER")

fun startDebugMonitor() {
	val runtime = Runtime.getRuntime()
	MinecraftServer.getSchedulerManager().scheduleTask({
		val debuggers = MinecraftServer
			.getConnectionManager()
			.onlinePlayers
			.filter { it.hasTag(DEBUGGER_TAG) }
		
		val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
		val maxMem = runtime.maxMemory() / (1024 * 1024)
		val instances = MinecraftServer.getInstanceManager().instances
		val chunks = instances.sumOf { it.chunks.size }
		val entities = instances.sumOf { it.entities.size }

		debuggers.forEach {
			it.sendActionBar(
				Component.text(
					"RAM: ${usedMem}/${maxMem}MB | Inst: ${instances.size} | Chunk: $chunks | Ent: $entities",
					NamedTextColor.GREEN
				)
			)
		}
	}, TaskSchedule.immediate(), TaskSchedule.tick(10))
}