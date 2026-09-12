package io.klartnet.kcp

import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

class TpCommand : Command("tp") {
	init {
		val targetArg = ArgumentType.Word("target")
		val posArg = ArgumentType.RelativeVec3("pos")

		// /tp <플레이어>
		addSyntax({ sender, ctx ->
			val player = sender as? Player ?: return@addSyntax
			val target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(ctx.get(targetArg)) ?: return@addSyntax

			val targetInst = target.instance ?: return@addSyntax
			if (targetInst != player.instance) {
				player.setInstance(targetInst, target.position)
			} else {
				player.teleport(target.position)
			}
		}, targetArg)

		// /tp <x y z> (~ ~ ~ 상대 좌표 지원)
		addSyntax({ sender, ctx ->
			val player = sender as? Player ?: return@addSyntax
			val vec = ctx.get(posArg).from(player)
			player.teleport(player.position.withCoord(vec.x(), vec.y(), vec.z()))
		}, posArg)
	}
}