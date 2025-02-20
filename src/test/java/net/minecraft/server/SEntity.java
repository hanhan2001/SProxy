package net.minecraft.server;

import me.xiaoying.sproxy.SProxy;
import me.xiaoying.sproxy.annotation.*;

@SClass(type = SClass.Type.NMS, className = "Manager")
public abstract class SEntity implements SProxy {
//    @SConstructor(saveVariable = "packet")
//    public SEntity(@SConstructorParameter(index = 1) String version, @SConstructorParameter(index = 0) String name) {
//
//    }
    @SConstructor(target = "net.minecraft.server.v1_12_R0.Manager")
    public abstract Object getManager(@SConstructorParameter(index = 1) String version, @SConstructorParameter(index = 0) String name);

    @SField(fieldName = "asd")
    public String name;

    @SFieldMethod(filedName = "asd", type = SFieldMethod.Type.GETTER)
    public abstract String getName();

    @SFieldMethod(filedName = "asd", type = SFieldMethod.Type.SETTER)
    public abstract void setName(String name);
}