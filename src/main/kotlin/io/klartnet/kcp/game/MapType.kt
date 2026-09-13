package io.klartnet.kcp.game

import net.hollowcube.polar.*
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import java.nio.file.Files
import java.nio.file.Path

data class ZonePhase(
	val size: Double,
	val waitSec: Long = 0,
	val shrinkSec: Long = 0,
	val dps: Float = 0.0f
)

enum class MapType(
	val mapName: String,
	val spawnPos: Pos,
	val phases: List<ZonePhase>
) {
	LOBBY(
		mapName = "lobby",
		spawnPos = Pos(995.5, 162.0, -9660.5),
		phases = listOf()
	),
	SMALL(
		mapName = "small",
		spawnPos = Pos(439.5, 73.0, -174.5),
		phases = listOf(
			ZonePhase(
				size = 300.0,
			),
			ZonePhase(
				size = 200.0,
				waitSec = 60,
				shrinkSec = 60,
				dps = 1.0F
			),
			ZonePhase(
				size = 100.0,
				waitSec = 60,
				shrinkSec = 60,
				dps = 2.0F
			),
			ZonePhase(
				size = 50.0,
				waitSec = 30,
				shrinkSec = 60,
				dps = 3.0F
			),
			ZonePhase(
				size = 0.0,
				waitSec = 30,
				shrinkSec = 60,
				dps = 4.0F
			),
		)
	),
	BROVILLE(
		mapName = "broville",
		spawnPos = Pos(0.0, 72.0, 2048.0),
		phases = listOf(
			ZonePhase(
				size = 400.0,
			),
			ZonePhase(
				size = 200.0,
				waitSec = 60,
				shrinkSec = 60,
				dps = 1.0F
			),
			ZonePhase(
				size = 100.0,
				waitSec = 60,
				shrinkSec = 60,
				dps = 2.0F
			),
			ZonePhase(
				size = 50.0,
				waitSec = 30,
				shrinkSec = 30,
				dps = 3.0F
			),
			ZonePhase(
				size = 0.0,
				waitSec = 30,
				shrinkSec = 30,
				dps = 4.0F
			)
		)
	);
	
	private val polarFile = Path.of("maps/$mapName.polar")
	private val mmapFile = Path.of("maps/$mapName.mmap")
	private val worldDir = Path.of("worlds/$mapName")
	
	private fun checkChanges(): Boolean {
		if (Files.notExists(polarFile))
			return Files.exists(worldDir)
		
		val polarTime = Files.getLastModifiedTime(polarFile).toMillis()
		val region = worldDir.resolve("region")
		
		return Files.exists(region) && Files.list(region).use { stream ->
			stream.anyMatch {
				Files.getLastModifiedTime(it).toMillis() > polarTime
			}
		}
	}
	
	private val mapBytes: ByteArray by lazy {
		if (checkChanges()) {
			Files.createDirectories(polarFile.parent)
			val world = AnvilPolar.anvilToPolar(worldDir)
			
			Files.write(polarFile, PolarWriter.write(world))
		}
		Files.readAllBytes(polarFile)
	}
	
	val mmapBytes: ByteArray by lazy {
		if (phases.isEmpty()) return@lazy ByteArray(0)
		if (Files.exists(mmapFile) && !checkChanges()) {
			return@lazy Files.readAllBytes(mmapFile)
		}
		
		val temp = MinecraftServer.getInstanceManager().createInstanceContainer().apply {
			chunkLoader = PolarLoader(createWorld())
		}
		val pixels = Minimap.bake(temp, spawnPos, phases.first().size)
		MinecraftServer.getInstanceManager().unregisterInstance(temp)
		
		Files.write(mmapFile, pixels)
		
		pixels
	}
	fun createWorld(): PolarWorld = PolarReader.read(mapBytes)
}