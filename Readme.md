# SProxy

> Java 的代理反射工具，通过字节码实现抽象方法获取反射值
>
> 传统的 Proxy 只支持 Interface 类，局限性较大
>
> SProxy 支持 abstract class 和 interface，相对而言更方便，同时意味着可以通过定义动态变量访问反射值。
>
>
> 其实只是对比 NMSProxy，NMSProxy使用的是 Java Proxy，并且其机制较为僵硬，并且不支持跨版本的包 constructor

版本: `1.0.0`

环境: `JAVA - 8`



## 特点

- Abstract class
  - 动态变量访问反射值
  - 抽象方法获取反射值
  - 抽象方法设置反射对象变量值
- 同时支持 Abstract class 和 Interface
- 支持指定使用对象 constructor，并且实例化参数顺序可以随意修改
- 使用 Abstract class 意味着可以再创建子类并继承从而实现更自由的代码编写
- 全程使用注解，简单易上手



## 支持的操作

- 构造器反射
- 变量反射
- Getter / Setter 变量操作
- 方法反射



## 简单模板

> 推荐去 src/test 目录中看示范代码，示范代码是能正常运行的

### NMS 对象

```java
// 获取 minecraft v1.20.1 的 CraftPlayer

import me.xiaoying.sproxy.SProxy;
import me.xiaoying.sproxy.SproxyProvider;
import me.xiaoying.sproxy.annotation.SClass;;
import me.xiaoying.sproxy.annotation.SFieldMethod;

public class Main {
    public static void main(String[] args) {
        // 这个仓库考虑到不是所有人都写 Bukkit 插件，所以没有自动获取服务器版本
        // 如果想自动获取服务器版本可以参考我的 ServerBuild 仓库，里面有修改好的 SProxy
        SproxyProvider provider = new SproxyProvider("v1_20_R0");
        // 创建 SPlayer 对象，需要传递一个 Bukkit Player 对象转换成 CraftPlayer，否则就是空值
        SPlayer player = provider.constructorClass(SPlayer.class, Bukkit.getServer().getPlayer("玩家ID"));
        System.out.println(player.getHealth());
    }
}

// 指定 SPlayer 指向 CraftPlayer 类
@SClass(type = SClass.Type.NMS, className = "entity.CraftPlayer")
abstract class SPlayer implements SProxy {
    // 创建一个变量名为 playerHealth 的变量指向 CraftPlayer 中的 health 变量
    // 需要注意的是两个变量的类型必须相同，否则会报错
    @SField(fieldName = "health")
    public double playerHealth;
    
    // 创建一个名叫 getHealth 的方法并设置为 getter 的类型，其指向的是 CraftPlayer 的 health 变量
    // 方法可以写参数，参数 SProxy 不处理，如果存在子类则可以作为子类自行处理的参数
    @SFieldMethod(fielddName = "health", type = SFieldMethod.Type.Getter)
    public abstract double getHealth();
    
    // 创建一个名叫 getHealth 的方法并设置为 getter 的类型，其指向的是 CraftPlayer 的 health 变量
    // 可以写额外的参数，额外参数 SProxy 不处理，如果存在子类则可以作为子类自行处理的参数
    @SFieldMethod(fielddName = "health", type = SFieldMethod.Type.Setter)
    public abstract void setHealth(double health);
}
```

### 多版本数据包

```java
import me.xiaoying.sproxy.SProxy;
import me.xiaoying.sproxy.SproxyProvider;
import me.xiaoying.sproxy.annotation.SClass;
import me.xiaoying.sproxy.annotation.SConstructor;

public class Main() {
    public static void main(String[] agrs) {
        // 这个仓库考虑到不是所有人都写 Bukkit 插件，所以没有自动获取服务器版本
        // 如果想自动获取服务器版本可以参考我的 ServerBuild 仓库，里面有修改好的 SProxy
        SproxyProvider provider = new SproxyProvider("v1_20_R0");
        
        // 创建 Packet 对象
        // 需要注意的是这里传 null 是因为不需要参考对象
        // CraftPlayer 需要参考数据是因为 CraftPlayer 内含本身含有数据并且 implements Player
        // Bukkit 只能获取 Player 对象，所以要获取 CraftPlayer 就需要强转 Player 来得到
        // 做 java 的其他开发时也是相同的道理，如果没有则不需要传递一个参考数据
        Packet entity = provider.constructorClass(Packet.class, null);
        
        // 在编写多版本的数据包时常常遇到不同版本数据包的构造方法不同
        // SProxy 可以很好的解决这个问题
        // 这里假设已经获取到服务器的版本
        String version = ....;
        PacketPlayOutEntityMetadata packet = null;
        switch (version) {
			// 我就只写两个版本了，仅作为参考，代码不一定对
            case "v1_20_R1":
                // 假设参数已经设置好了
                packet = entity.get1_20_R1(second, first);
                break;
            case "v1_8_R1":
                // 假设参数已经设置好了
                packet = entity.get1_8_R1(second, first);
                break;
        }
        
        // 处理玩家发包
    }
}

// 不需要指向任何包可以直接留空
@SClass(type = SClass.Type.OTHER, className = "")
class Packet implements SProxy {
    // 这里参数我都是乱写的，和 nms 中的数据包实例化参数不一样，只能做参考用
    // @SConstructor 中 target 指的是实例化对象的 class 路径
    // @SParameter中 index 指的是当前修饰的传参参数是 PacketPlayOutEntityMetadata 实例化的第几个参数，这意味着通过 SProxy 实例化的对象 需要的参数顺序可以随意更改
    // 假设 PacketPlayOutEntityMetadata 有两个参数，此处的代码就是交换了两个参数的顺序
    // 在调用 Packet#get1_20_R1 方法时传参就是 String, int
    // 对应的会自动调用 PacketPlayOutEntityMetadata(int, String)
    // 同样的可以添加额外的参数，前提是不要使用注解，否则会当作 PacketPlayOutEntityMetadata 的实例化参数进行处理
    @SConstructor(target = "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata")
    public Object get1_20_R1(@SParameter(index = 1) String second, @SParameter(index = 0) int first);
        
    // 假设 1.8.1 版本的 PacketPlayOutEntityMetadata 只有一个参数，此处代码只能做参考
    @SConstructor(target = "net.minecraft.PacketPlayOutEntityMetadata")
    public Object get1_8_R1(@SConstructorParameter(index = 0) int first);
}
```

### 调用方法

```java
import me.xiaoying.sproxy.SProxy;
import me.xiaoying.sproxy.SproxyProvider;
import me.xiaoying.sproxy.annotation.SClass;
import me.xiaoying.sproxy.annotation.SMethod;

public class Main() {
    public static void main(String[] agrs) {
        // 假设有一个实例化 manager 对象
        Manager manager....
        
        // 这个仓库考虑到不是所有人都写 Bukkit 插件，所以没有自动获取服务器版本
        // 如果想自动获取服务器版本可以参考我的 ServerBuild 仓库，里面有修改好的 SProxy
        SproxyProvider provider = new SproxyProvider("v1_20_R0");
        
        // 需要注意的是这里传 manager 是因为需要一个参考对象
        // CraftPlayer 需要参考数据是因为 CraftPlayer 内含本身含有数据并且 implements Player
        // Bukkit 只能获取 Player 对象，所以要获取 CraftPlayer 就需要强转 Player 来得到
        // 做 java 的其他开发时也是相同的道理，如果没有则不需要传递一个参考数据
        Entity entity = provider.constructorClass(Entity.class, manager);
        entity.runTest("Suffix", "Prefix");
    }
}

@SClass(type = SClass.Type.NMS, className = "Manager")
class Entity implements SProxy {
    // 指定调用 net.minecraft.v1_20_R0.Manager 中的 test 方法
    
    // 同样的可以添加额外的参数，前提是不要使用注解，否则会当作 test 的传递参数进行处理
    @SMethod(methodName = "test")
    public abstract void runTest(@SParameter(index = 1) String suffix, @SParameter(index = 0) String prefix);
}
```

