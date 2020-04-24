package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.internalName
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.IllegalStateException

/**
 * @author cookiedragon234 23/Apr/2020
 */
object EntityInsentientTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "doTick" && method.desc == "()V") {
				for (insn in method.instructions) {
					if (insn is LdcInsnNode) {
						if (insn.cst == "sensing") {
							val load = insn.previous?.previous?.previous
							
							if (load == null) {
								IllegalStateException("Null Load").printStackTrace()
								return
							}
							
							val list = InsnList().apply {
								val out = LabelNode()
								add(VarInsnNode(ALOAD, 0))
								add(MethodInsnNode(INVOKESTATIC, EntityInsentientTransformer::class.internalName, "shouldDoAi", "(Ljava/lang/Object;)Z", false))
								add(JumpInsnNode(IFNE, out))
								add(InsnNode(RETURN))
								add(out)
							}
							
							method.instructions.insertBefore(load, list)
							return
						}
					}
				}
			}
		}
		error("Couldnt find target")
	}
	
	@JvmStatic
	fun shouldDoAi(entity: Any): Boolean {
		entity as EntityInsentient
		
		return when (entity) {
			is EntityBat -> false
			is EntitySnowman -> false
			is EntityRabbit -> false
			is EntityPolarBear -> false
			is EntityEndermite -> false
			is EntityArmorStand -> false
			is EntityParrot -> false
			is EntityPigZombie -> PaperBinInfo.ticks % 25 == 0 // every 60 ticks
			is EntityVillager -> PaperBinInfo.ticks % 15 == 0 // every 40 ticks
			is EntityMonster -> PaperBinInfo.ticks % 15 == 0 // every 40 ticks
			else -> PaperBinInfo.ticks % 5 == 0 // Only every 10 ticks
		} && entity.world.findNearbyPlayer(entity, 40.0) == null // If there are no players within a 40 block radius then dont calculate AI
	}
}
