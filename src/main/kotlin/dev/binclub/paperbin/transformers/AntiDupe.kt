package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeature
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * @author cookiedragon234 11/May/2020
 */
object AntiDupe: PaperFeature {
	override fun registerTransformers() {
		// If an item is dropped then we duplicate it and empty the original stack
		// This should prevent some duplication glitches, e.g. 11/11
		register("org.bukkit.craftbukkit.v1_12_R1.event.CraftEventFactory") { classNode ->
			var i = 0
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (
						insn is MethodInsnNode
						&&
						insn.owner == "org/bukkit/craftbukkit/v1_12_R1/CraftWorld"
						&&
						insn.name == "dropItemNaturally"
						&&
						insn.desc == "(Lorg/bukkit/Location;Lorg/bukkit/inventory/ItemStack;)Lorg/bukkit/entity/Item;"
					) {
						val before = InsnNode(DUP_X2)
						val after = InsnList().apply {
							add(InsnNode(ICONST_0))
							add(MethodInsnNode(
								INVOKEVIRTUAL,
								"org/bukkit/inventory/ItemStack",
								"setAmount",
								"(I)V",
								false
							))
						}
						method.instructions.insertBefore(insn, before)
						method.instructions.insert(insn, after)
						
						i += 1
					}
				}
			}
			
			if (i < 2) error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.Entity") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/ItemStack;F)Lnet/minecraft/server/v1_12_R1/EntityItem;") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.name == "asBukkitCopy") {
							insn.name = "asCraftMirror"
						} else if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/EntityItem" && insn.name == "<init>") {
							val prev = insn.previous
							if (prev is VarInsnNode) {
								val before = MethodInsnNode(
									INVOKEVIRTUAL,
									"net/minecraft/server/v1_12_R1/ItemStack",
									"cloneItemStack",
									"()Lnet/minecraft/server/v1_12_R1/ItemStack;",
									false
								)
								val after = InsnList().apply {
									add(VarInsnNode(prev.opcode, prev.`var`))
									add(InsnNode(ICONST_0))
									add(MethodInsnNode(
										INVOKEVIRTUAL,
										"net/minecraft/server/v1_12_R1/ItemStack",
										"setCount",
										"(I)V",
										false
									))
								}
								
								method.instructions.insertBefore(insn, before)
								method.instructions.insert(insn, after)
							}
						}
					}
				}
			}
		}
	}
}