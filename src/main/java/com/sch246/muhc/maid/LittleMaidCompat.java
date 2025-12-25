package com.sch246.muhc.maid;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.sch246.muhc.event.ClothConfigEvent;
import com.sch246.muhc.maid.task.HandCrankTask;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;


@LittleMaidExtension
public class LittleMaidCompat implements ILittleMaid {
    // 默认构造函数，女仆模组会在合适的时间调用这个构造函数。可以在这里注册女仆专属的事件
    public LittleMaidCompat() {
        if (ModList.get().isLoaded("cloth_config")) {
            MinecraftForge.EVENT_BUS.register(new ClothConfigEvent());
        }
    }

    /**
     * 注册女仆工作任务的方法
     * <p>
     * Method to add maid work tasks
     */
    @Override
    public void addMaidTask(TaskManager manager) {
        // 添加自定义任务
        manager.add(new HandCrankTask());
    }
}
