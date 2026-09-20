package io.klartnet.kcp

import io.klartnet.kcp.instances.game.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

internal fun registerCommands() {
	val manager = MinecraftServer.getCommandManager()

	fun playerCommand(
		name: String,
		block: (Player) -> Unit
	) = manager.register(
		Command(name).apply {
			setDefaultExecutor { sender, _ ->
				(sender as? Player)?.let(block)
			}
		}
	)

	playerCommand("match") { player ->
		if (!GameManager.match(player)) {
			player.sendMessage(
				Component.text(
					"참가 가능한 방이 없습니다."
				)
			)
		}
	}
	playerCommand("join") { player ->
		player.sendMessage(
			Component.text(
				"아직 구현되지 않은 기능입니다 ㅗㅗ"
			)
		)
	}
	playerCommand("leave") { player ->
		GameManager.getGame(player)?.leave(player)
	}
	playerCommand("start") { player ->
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
	}
	playerCommand("debug") { player ->
		if (player.hasTag(DEBUGGER_TAG)) {
			player.removeTag(DEBUGGER_TAG)
			player.sendMessage(
				Component.text(
					"디버그 모니터 꺼짐",
					NamedTextColor.GREEN
				)
			)
		} else {
			player.setTag(DEBUGGER_TAG, true)
			player.sendMessage(
				Component.text(
					"디버그 모니터 켜짐",
					NamedTextColor.GREEN
				)
			)
		}
	}
	playerCommand("gc") { player ->
		val rt = Runtime.getRuntime()
		val before = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
		System.gc()
		val after = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)

		player.sendMessage("GC: ${before}MB -> ${after}MB (Released: ${before - after}MB)")
	}

	manager.register(Command("tp").apply {
		val targetArg = ArgumentType.Entity("target")
			.onlyPlayers(true)
			.singleEntity(true)
		val posArg = ArgumentType.RelativeVec3("location")

		this.addSyntax({ sender, ctx ->
			val player = sender as? Player ?: return@addSyntax
			val target = ctx.get(targetArg)
				.findFirstPlayer(player) ?: return@addSyntax

			if (target.instance != player.instance) {
				player.sendMessage(
					Component.text(
						"대상 플레이어가 다른 인스턴스에 있습니다.",
						NamedTextColor.RED
					)
				)
				return@addSyntax
			}

			player.teleport(target.position)
		}, targetArg)
		this.addSyntax({ sender, ctx ->
			val player = sender as? Player ?: return@addSyntax
			val vec = ctx.get(posArg).from(player)

			player.teleport(
				player.position.withCoord(
					vec.x(),
					vec.y(),
					vec.z()
				)
			)
		}, posArg)
	})
}