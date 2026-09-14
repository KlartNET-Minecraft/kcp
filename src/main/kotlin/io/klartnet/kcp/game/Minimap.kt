package io.klartnet.kcp.game

import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.MapDataPacket

class Minimap(
	val id: Int,
	private val center: Pos,
	private val mapSize: Double,
	val terrainBytes: ByteArray
) {
	companion object {
		fun bake(instance: Instance, center: Pos, initialZoneSize: Double): ByteArray {
			val pixels = ByteArray(128 * 128)
			val step = initialZoneSize / 128.0
			val startX = center.x - (initialZoneSize / 2.0)
			val startZ = center.z - (initialZoneSize / 2.0)

			var lastCx = Int.MAX_VALUE
			var lastCz = Int.MAX_VALUE

			for (pz in 0 until 128) {
				for (px in 0 until 128) {
					val wx = (startX + px * step).toInt()
					val wz = (startZ + pz * step).toInt()
					val cx = wx shr 4
					val cz = wz shr 4

					if (cx != lastCx || cz != lastCz) {
						lastCx = cx
						lastCz = cz
						if (instance.getChunk(cx, cz) == null) {
							instance.loadChunk(cx, cz).join()
						}
					}
					if (instance.getChunk(cx, cz) == null) continue

					for (y in 120 downTo -30) {
						val block = instance.getBlock(wx, y, wz)
						if (!block.air()) {
							pixels[pz * 128 + px] = (block.mapColorId() * 4 + 2).toByte()
							break
						}
					}
				}
			}
			return pixels
		}
	}

	fun give(player: Player) {
		player.itemInOffHand = ItemStack.of(Material.FILLED_MAP)
			.with(DataComponents.MAP_ID, id)
	}

	// 1초 스케줄러에서 1회 호출: 테두리 1회 복사 후 모든 플레이어 패킷 일괄 전송
	fun render(
		players: Collection<Player>,
		zoneCenter: Point,
		zoneSize: Double
	) {
		val buffer = terrainBytes.clone()
		val startX = center.x - (mapSize / 2.0)
		val startZ = center.z - (mapSize / 2.0)

		// 현재 자기장 빨간색 테두리 (2px)
		val half = zoneSize / 2.0
		val minX = (((zoneCenter.x() - half - startX) / mapSize) * 128).toInt().coerceIn(0, 127)
		val maxX = (((zoneCenter.x() + half - startX) / mapSize) * 128).toInt().coerceIn(0, 127)
		val minZ = (((zoneCenter.z() - half - startZ) / mapSize) * 128).toInt().coerceIn(0, 127)
		val maxZ = (((zoneCenter.z() + half - startZ) / mapSize) * 128).toInt().coerceIn(0, 127)
		val red: Byte = 18

		for (x in minX..maxX) {
			buffer[minZ * 128 + x] = red
			buffer[maxZ * 128 + x] = red
			buffer[((minZ + 1).coerceAtMost(127)) * 128 + x] = red
			buffer[((maxZ - 1).coerceAtLeast(0)) * 128 + x] = red
		}
		for (z in minZ..maxZ) {
			buffer[z * 128 + minX] = red
			buffer[z * 128 + maxX] = red
			buffer[z * 128 + (minX + 1).coerceAtMost(127)] = red
			buffer[z * 128 + (maxX - 1).coerceAtLeast(0)] = red
		}

		// 플레이어 커서 부착
		for (player in players) {
			val p = player.position
			val relX = ((p.x - center.x) / mapSize * 256.0).toInt().coerceIn(-128, 127).toByte()
			val relZ = ((p.z - center.z) / mapSize * 256.0).toInt().coerceIn(-128, 127).toByte()
			val rot = (((p.yaw % 360.0 + 360.0) % 360.0 / 22.5) + 0.5).toInt().rem(16).toByte()

			val cursor = MapDataPacket.Icon(0, relX, relZ, rot, null)
			player.sendPacket(
				MapDataPacket(
					id,
					0.toByte(),
					false,
					true,
					listOf(cursor),
					MapDataPacket.ColorContent(
						128.toByte(),
						128.toByte(),
						0.toByte(),
						0.toByte(),
						buffer
					)
				)
			)
		}
	}
}