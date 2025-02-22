package net.minecraft.server.v1_12_R0;

public class Manager {
    public String asd = "GFG";

    public Manager(String name, String version) {
        System.out.println("Name: " + name);
        System.out.println("Version: " + version);
        System.out.println("-----------------------");
    }

    public Manager(String name, int version) {

    }

    public void test(String prefix, String suffix) {
        System.out.println("!!!!!!!!!!!!!!!!!!!");
        System.out.println("prefix: " + prefix);
        System.out.println("suffix: " + suffix);
        System.out.println("!!!!!!!!!!!!!!!!!!!");
    }

    public TestEntity initEntity(TestEntity entity) {
        System.out.println(entity.getName());
        entity.setName("Test");
        return entity;
    }
}