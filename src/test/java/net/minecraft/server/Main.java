package net.minecraft.server;

import me.xiaoying.sproxy.SProxyProvider;
import net.minecraft.server.v1_12_R0.Manager;
import net.minecraft.server.v1_12_R0.TestEntity;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        Manager manager = new Manager("Name", "1.0.0");

        SProxyProvider provider = new SProxyProvider("v1_12_R0");
        provider.debug(true);
        try {
            SEntity sEntity = provider.constructorClass(SEntity.class, manager);
            System.out.println(sEntity.getName());
            sEntity.setName("ninin");
            System.out.println(sEntity.getName());
            System.out.println(sEntity.name);
            sEntity.getManager("15.15.1", "测试");

            System.out.println("\n\n\n");

            sEntity.test("Suffix", 20);

            System.out.println("\n\n\n");

            // 假设通过特殊途径获取 TestEntity
            TestEntity testEntity = new TestEntity("Hello World");
            testEntity = (TestEntity) sEntity.init(testEntity);
            System.out.println(testEntity.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("\n\n\n");

        // multi packet
        try {
            MultiPacketEntity multiPacketEntity = provider.constructorClass(MultiPacketEntity.class, null);
            multiPacketEntity.getManager1_8_R0("测试");
            multiPacketEntity.getManager1_12_R0("1.0.0", "假测试");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}