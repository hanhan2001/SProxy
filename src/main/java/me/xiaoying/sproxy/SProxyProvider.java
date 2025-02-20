package me.xiaoying.sproxy;

import me.xiaoying.sproxy.annotation.*;
import me.xiaoying.sproxy.utils.ClassUtils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class SProxyProvider {
    private final String version;

    public SProxyProvider() {
        this.version = "";
    }

    public SProxyProvider(String version) {
        this.version = version;
    }

    public <T extends SProxy> T constructorClass(Class<T> clazz, Object instance) throws Exception {

        if (!SProxy.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("class of " + clazz.getName() + " need to extended " + SProxy.class.getName());

        SClass classAnnotation = clazz.getAnnotation(SClass.class);
        if (classAnnotation == null)
            throw new IllegalArgumentException("class of " + clazz.getName() + " need @SClass annotation.");

        // get target class
        Class<?> target;
        if (classAnnotation.type().getClassName(classAnnotation.className(), this.version).isEmpty())
            target = null;
        else
            target = this.getClass().getClassLoader().loadClass(classAnnotation.type().getClassName(classAnnotation.className(), this.version));

        // byte buddy
        DynamicType.Builder<T> subclass = new ByteBuddy().subclass(clazz);

        subclass = subclass.defineField("temporary", Object.class, Modifier.PRIVATE);
        subclass = subclass.defineMethod("setTemporary", void.class, Modifier.PUBLIC)
                .withParameters(Object.class)
                .intercept(new Implementation.Simple((methodVisitor, context, methodDescription) -> {
                    methodVisitor.visitCode();

                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);

                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD,
                            context.getInstrumentedType().asErasure().getName().replace(".", "/"),
                            "temporary",
                            "Ljava/lang/Object;");

                    methodVisitor.visitInsn(Opcodes.RETURN);

                    methodVisitor.visitMaxs(2, 2);
                    methodVisitor.visitEnd();
                    return new ByteCodeAppender.Size(2, 2);
                }));

        // methods
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            if (declaredMethod.getAnnotation(SConstructor.class) != null)
                subclass = this.setConstructorMethod(subclass, declaredMethod);
            else if (declaredMethod.getAnnotation(SFieldMethod.class) != null)
                subclass = this.setFiledMethod(subclass, declaredMethod, target, instance);
        }

        DynamicType.Unloaded<T> make = subclass.make();

        // new instance class when finished method handle
        T t = make.load(clazz.getClassLoader()).getLoaded().newInstance();
        t.getClass().getDeclaredMethod("setTemporary", Object.class).invoke(t, instance);

        // filed handle
        for (Field declaredField : t.getClass().getSuperclass().getDeclaredFields())
            this.setFiled(t, declaredField, t, target);

        return t;
    }

    private <T> DynamicType.Builder<T>  setConstructorMethod(DynamicType.Builder<T> subclass, Method method) {
        SConstructor annotation = method.getAnnotation(SConstructor.class);

        if (annotation == null)
            return subclass;

        String target = annotation.target();

        Map<Integer, Integer> map = new HashMap<>();
        Class<?>[] classes = new Class<?>[method.getParameters().length];
        for (Parameter parameter : method.getParameters()) {
            SParameter anno = parameter.getAnnotation(SParameter.class);

            if (anno == null)
                continue;

            classes[anno.index()] = parameter.getType();
            map.put(map.size(), anno.index());
        }

        for (int i = 0; i < classes.length; i++) {
            if (classes[i] != null)
                continue;

            if (i + 1 >= classes.length)
                break;

            for (int j = i; j < classes.length; j++) {
                if (classes[j] == null)
                    continue;

                throw new RuntimeException("missing parameter " + i + " index in " + subclass.getClass().getSuperclass().getName() + method.getName());
            }
        }

        DynamicType.Builder.MethodDefinition.ImplementationDefinition<T> method1 = subclass.method(ElementMatchers.named(method.getName()));

        subclass = method1.intercept(new Implementation.Simple((methodVisitor, context, methodDescription) -> {
            methodVisitor.visitCode();

            methodVisitor.visitTypeInsn(Opcodes.NEW, target.replace('.', '/'));
            methodVisitor.visitInsn(Opcodes.DUP);

            StringBuilder stringClasses = new StringBuilder();
            map.keySet().forEach(index -> {
                stringClasses.append(ClassUtils.getClassByteCodeName(classes[index]));
                methodVisitor.visitVarInsn(Opcodes.ALOAD, map.get(index) + 1);
            });

            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    target.replace('.', '/'),
                    "<init>",
                    "(" + stringClasses + ")V",
                    false);
            // 返回实例化的 Manager 对象
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitMaxs(4, 3);
            methodVisitor.visitEnd();
            return new ByteCodeAppender.Size(4, 3);
        }));
        return subclass;
    }

    private <T> DynamicType.Builder<T> setFiledMethod(DynamicType.Builder<T> subclass, Method method, Class<?> target, Object instance) throws Exception {
        SFieldMethod filedAnnotation = method.getAnnotation(SFieldMethod.class);
        if (filedAnnotation == null)
            return subclass;

        String name = filedAnnotation.filedName();
        SFieldMethod.Type type = filedAnnotation.type();

        DynamicType.Builder.MethodDefinition.ImplementationDefinition<T> method1 = subclass.method(ElementMatchers.named(method.getName()));

        Field declaredField = target.getDeclaredField(name);
        declaredField.setAccessible(true);

        // getter
        if (type == SFieldMethod.Type.GETTER) {
            subclass = method1.intercept(new Implementation.Simple((methodVisitor, context, methodDescription) -> {
                methodVisitor.visitCode();

                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);

                methodVisitor.visitFieldInsn(Opcodes.GETFIELD,
                        context.getInstrumentedType().asErasure().getName().replace(".", "/"),
                        "temporary",
                        "Ljava/lang/Object;");

                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, instance.getClass().getName().replace('.', '/'));

                String descriptor = ClassUtils.getClassByteCodeName(declaredField.getType());

                methodVisitor.visitFieldInsn(Opcodes.GETFIELD,
                        instance.getClass().getName().replace('.', '/'),
                        name,
                        descriptor);

                Class<?> fieldType = declaredField.getType();
                int returnType;
                if (fieldType == void.class)
                    returnType = Opcodes.RETURN;
                else if (fieldType == int.class || fieldType == byte.class || fieldType == short.class || fieldType == char.class || fieldType == boolean.class)
                    returnType = Opcodes.IRETURN;
                else if (fieldType == long.class)
                    returnType = Opcodes.LRETURN;
                else if (fieldType == float.class)
                    returnType = Opcodes.FRETURN;
                else if (fieldType == double.class)
                    returnType = Opcodes.DRETURN;
                else
                    returnType = Opcodes.ARETURN;

                methodVisitor.visitInsn(returnType);
                methodVisitor.visitMaxs(2, 2);
                methodVisitor.visitEnd();
                return new ByteCodeAppender.Size(2, 2);
            }));

            return subclass;
        }

        // setter
        subclass = method1.intercept(new Implementation.Simple((methodVisitor, context, methodDescription) -> {
            methodVisitor.visitCode();

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);

            methodVisitor.visitFieldInsn(Opcodes.GETFIELD,
                    context.getInstrumentedType().asErasure().getName().replace(".", "/"),
                    "temporary",
                    "Ljava/lang/Object;");

            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, instance.getClass().getName().replace('.', '/'));
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);

            String descriptor = ClassUtils.getClassByteCodeName(declaredField.getType());

            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD,
                    instance.getClass().getName().replace('.', '/'),
                    name,
                    descriptor);

            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
            return new ByteCodeAppender.Size(2, 2);
        }));
        return subclass;
    }

    private <T> Object setFiled(T t, Field field, T newClass, Class<?> target) throws Exception {
        SField sfield = field.getAnnotation(SField.class);

        if (sfield == null)
            return t;

        field.setAccessible(true);
        Field targetField = target.getDeclaredField(sfield.fieldName());
        targetField.setAccessible(true);

        if (targetField.getType() != field.getType())
            throw new IllegalArgumentException("target field " + targetField.getType() + " not equals " + field.getType());

        Field declaredField = newClass.getClass().getDeclaredField("temporary");
        declaredField.setAccessible(true);
        Object object = declaredField.get(newClass);

        field.set(t, targetField.get(object));
        return t;
    }
}