package io.klartnet.kcp

import io.klartnet.kcp.instances.game.GameManager
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player

private fun playerCommand(
	name: String,
	block: (Player) -> Unit
) = Command(name).apply {
	setDefaultExecutor { sender, _ ->
		(sender as? Player)?.let(block)
	}
}

fun registerCommands() {
	val manager = MinecraftServer.getCommandManager()

	manager.register(TpCommand())
	manager.register(playerCommand("match") { player ->
		if (!GameManager.match(player)) {
			player.sendMessage(
				Component.text(
					"참가 가능한 방이 없습니다."
				)
			)
		}
	})
	manager.register(playerCommand("join") { player ->
		player.sendMessage(
			Component.text(
				"아직 구현되지 않은 기능입니다 ㅗㅗ"
			)
		)
	})
	manager.register(playerCommand("leave") { player ->
		GameManager.getGame(player)?.leave(player)
	})
	manager.register(playerCommand("start") { player ->
		val game = GameManager.getGame(player)
		if (game == null) {
			player.sendMessage(
				Component.text(
					"현재 참가 중인 방이 없습니다."
				)
			)
			return@playerCommand
		}
		
		game.start()
	})
	manager.register(playerCommand("debug") { player ->
		player.sendMessage("§e===== [DEBUG] =====")
		
		val rt = Runtime.getRuntime()
		val used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
		val max = rt.maxMemory() / (1024 * 1024)
		
		player.sendMessage("§f메모리: §a${used}MB §7/ §c${max}MB")
		
		player.sendMessage("§e===== [DEBUG] =====")
	})
	manager.register(playerCommand("gc") { player ->
		val rt = Runtime.getRuntime()
		val before = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
		System.gc()
		val after = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
		
		player.sendMessage("GC: ${before}MB -> ${after}MB (Released: ${before - after}MB)")
	})
}