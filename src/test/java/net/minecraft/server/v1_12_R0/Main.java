package net.minecraft.server.v1_12_R0;

import me.xiaoying.sproxy.SProxyProvider;

public class Main {
    public static void main(String[] args) {
        Manager manager = new Manager("Name", "1.0.0");

        SProxyProvider sProxyProvider = new SProxyProvider("v1_12_R0");
        try {
            SEntity sEntity = sProxyProvider.constructorClass(SEntity.class, manager);
            System.out.println(sEntity.getName());
            sEntity.setName("ninin");
            System.out.println(sEntity.getName());
            System.out.println(sEntity.name);
            sEntity.getManager("15.15.1", "测试");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}