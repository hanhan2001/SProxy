package net.minecraft.server;

import me.xiaoying.sproxy.SProxy;
import me.xiaoying.sproxy.annotation.SClass;
import me.xiaoying.sproxy.annotation.SConstructor;
import me.xiaoying.sproxy.annotation.SParameter;

@SClass(type = SClass.Type.OTHER, className = "")
public abstract class MultiPacketEntity implements SProxy {
    @SConstructor(target = "net.minecraft.server.v1_8_R0.Manager")
    public abstract Object getManager1_8_R0(@SParameter(index = 0) String name);

    @SConstructor(target = "net.minecraft.server.v1_12_R0.Manager")
    public abstract Object getManager1_12_R0(@SParameter(index = 1) String version, @SParameter(index = 0) String name);
}