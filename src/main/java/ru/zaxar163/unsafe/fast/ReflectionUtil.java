package ru.zaxar163.unsafe.fast;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import ru.zaxar163.core.ClassUtil;
import ru.zaxar163.core.LookupUtil;
import ru.zaxar163.unsafe.UnsafeUtil;
import ru.zaxar163.unsafe.fast.proxies.ProxyList;

public class ReflectionUtil {
	private static final class FastFieldAccessor implements AccessorField {
		private final Object base;
		private final long offset;

		private FastFieldAccessor(final Field f) {
			if ((f.getModifiers() & Modifier.STATIC) == 0) {
				offset = ProxyList.UNSAFE.objectFieldOffset(f);
				base = null;
			} else {
				offset = ProxyList.UNSAFE.staticFieldOffset(f);
				base = ProxyList.UNSAFE.staticFieldBase(f.getDeclaringClass());
			}
		}

		@Override
		public Object getAndSetObject(final Object inst, final Object to) {
			return base == null ? ProxyList.UNSAFE.getAndSetObject(inst, offset, to)
					: ProxyList.UNSAFE.getAndSetObject(base, offset, to);
		}

		@Override
		public byte getByte(final Object inst) throws IllegalArgumentException {
			return base == null ? ProxyList.UNSAFE.getByte(inst, offset) : ProxyList.UNSAFE.getByte(base, offset);
		}

		@Override
		public double getDouble(final Object inst) throws IllegalArgumentException {
			return base == null ? ProxyList.UNSAFE.getDouble(inst, offset) : ProxyList.UNSAFE.getDouble(base, offset);
		}

		@Override
		public float getFloat(final Object inst) throws IllegalArgumentException {
			return base == null ? ProxyList.UNSAFE.getFloat(inst, offset) : ProxyList.UNSAFE.getFloat(base, offset);
		}

		@Override
		public int getInt(final Object inst) throws IllegalArgumentException {
			return base == null ? ProxyList.UNSAFE.getInt(inst, offset) : ProxyList.UNSAFE.getInt(base, offset);
		}

		@Override
		public long getLong(final Object inst) throws IllegalArgumentException {
			return base == null ? ProxyList.UNSAFE.getLong(inst, offset) : ProxyList.UNSAFE.getLong(base, offset);
		}

		@Override
		public Object getObject(final Object inst) {
			return base == null ? ProxyList.UNSAFE.getObject(inst, offset) : ProxyList.UNSAFE.getObject(base, offset);
		}

		@Override
		public short getShort(final Object inst) throws IllegalArgumentException {
			return base == null ? ProxyList.UNSAFE.getShort(inst, offset) : ProxyList.UNSAFE.getShort(base, offset);
		}

		@Override
		public void setByte(final Object inst, final byte to) throws IllegalArgumentException {
			if (base == null)
				ProxyList.UNSAFE.putByte(inst, offset, to);
			else
				ProxyList.UNSAFE.putByte(base, offset, to);
		}

		@Override
		public void setDouble(final Object inst, final double to) throws IllegalArgumentException {
			if (base == null)
				ProxyList.UNSAFE.putDouble(inst, offset, to);
			else
				ProxyList.UNSAFE.putDouble(base, offset, to);
		}

		@Override
		public void setFloat(final Object inst, final float to) throws IllegalArgumentException {
			if (base == null)
				ProxyList.UNSAFE.putFloat(inst, offset, to);
			else
				ProxyList.UNSAFE.putFloat(base, offset, to);
		}

		@Override
		public void setInt(final Object inst, final int to) throws IllegalArgumentException {
			if (base == null)
				ProxyList.UNSAFE.putInt(inst, offset, to);
			else
				ProxyList.UNSAFE.putInt(base, offset, to);
		}

		@Override
		public void setLong(final Object inst, final long to) throws IllegalArgumentException {
			if (base == null)
				ProxyList.UNSAFE.putLong(inst, offset, to);
			else
				ProxyList.UNSAFE.putLong(base, offset, to);
		}

		@Override
		public void setObject(final Object inst, final Object to) {
			if (base == null)
				ProxyList.UNSAFE.putObject(inst, offset, to);
			else
				ProxyList.UNSAFE.putObject(base, offset, to);

		}

		@Override
		public void setShort(final Object inst, final short to) throws IllegalArgumentException {
			if (base == null)
				ProxyList.UNSAFE.putShort(inst, offset, to);
			else
				ProxyList.UNSAFE.putShort(base, offset, to);
		}
	}

	private static final Class<?> accGenerator;
	private static final MethodHandle constuctorGenerator;
	private static final MethodHandle copyMethod;
	private static final MethodHandle generatorConstructor;
	private static final MethodHandle generatorMethod;
	private static final Class<?> ifaceAccessor;
	private static final Class<?> ifaceConsAccessor;
	private static final MethodHandle invokerConsMethod;
	private static final MethodHandle invokerMethod;
	private static final long methodClazzO;
	private static final long methodExceptionsO;
	private static final long methodModifiersO;
	private static final long methodNameO;
	private static final long methodParamsO;
	private static final String nameInit;
	private static final Set<Field> objectFields = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList(Object.class.getDeclaredFields())));
	private static final Map<Class<?>, Object> objs;
	private static final long overrideAccessibleObjectOffset;

	private static final long rootOffsetMethod;

	private static final long slotOffsetConstructor;

	private static final long slotOffsetMethod;

	static {
		try {
			slotOffsetConstructor = ProxyList.UNSAFE.objectFieldOffset(LookupUtil.getField(Constructor.class, "slot"));
			slotOffsetMethod = ProxyList.UNSAFE.objectFieldOffset(LookupUtil.getField(Method.class, "slot"));
			rootOffsetMethod = ProxyList.UNSAFE.objectFieldOffset(LookupUtil.getField(Method.class, "root"));
			overrideAccessibleObjectOffset = ProxyList.UNSAFE
					.objectFieldOffset(LookupUtil.getField(AccessibleObject.class, "override"));
			copyMethod = LookupUtil.ALL_LOOKUP.findVirtual(Method.class, "copy", MethodType.methodType(Method.class));
			ifaceAccessor = ClassUtil.nonThrowingFirstClass("jdk.internal.reflect.MethodAccessor",
					"sun.reflect.MethodAccessor");
			ifaceConsAccessor = ClassUtil.nonThrowingFirstClass("jdk.internal.reflect.ConstructorAccessor",
					"sun.reflect.ConstructorAccessor");
			invokerMethod = LookupUtil.ALL_LOOKUP.findVirtual(ifaceAccessor, "invoke",
					MethodType.methodType(Object.class, Object.class, Object[].class));
			accGenerator = ClassUtil.nonThrowingFirstClass("jdk.internal.reflect.MethodAccessorGenerator",
					"sun.reflect.MethodAccessorGenerator");
			constuctorGenerator = LookupUtil.ALL_LOOKUP.findConstructor(accGenerator,
					MethodType.methodType(void.class));
			generatorMethod = LookupUtil.ALL_LOOKUP.findVirtual(accGenerator, "generateMethod", MethodType.methodType(
					ifaceAccessor, Class.class, String.class, Class[].class, Class.class, Class[].class, int.class));
			generatorConstructor = LookupUtil.ALL_LOOKUP.findVirtual(accGenerator, "generateConstructor",
					MethodType.methodType(ifaceConsAccessor, Class.class, Class[].class, Class[].class, int.class));
			invokerConsMethod = LookupUtil.ALL_LOOKUP.findVirtual(ifaceConsAccessor, "newInstance",
					MethodType.methodType(Object.class, Object[].class));

			methodNameO = ProxyList.UNSAFE.objectFieldOffset(LookupUtil.getField(Method.class, "name"));
			methodParamsO = ProxyList.UNSAFE.objectFieldOffset(LookupUtil.getField(Method.class, "parameterTypes"));
			methodExceptionsO = ProxyList.UNSAFE.objectFieldOffset(LookupUtil.getField(Method.class, "exceptionTypes"));
			methodModifiersO = ProxyList.UNSAFE.objectFieldOffset(LookupUtil.getField(Method.class, "modifiers"));
			methodClazzO = ProxyList.UNSAFE.objectFieldOffset(LookupUtil.getField(Method.class, "clazz"));
			nameInit = "<init>";
			objs = new ConcurrentHashMap<>();
			objs.put(Class.class, Object.class);
		} catch (final Throwable t) {
			throw new Error(t);
		}
	}

	public static Object changeObjFullUnsafe(final Object required, final Object o) {
		ProxyList.UNSAFE.putInt(o, 8L, ProxyList.UNSAFE.getInt(required, 8L));
		return o;
	}

	@SuppressWarnings("unchecked")
	public static <T> T changeObjUnsafe(final Class<T> required, final Object o) {
		return (T) changeObjFullUnsafe(objs.computeIfAbsent(required, c -> ProxyList.UNSAFE.allocateInstance(c)), o);
	}

	public static MethodHandle handleD(final Constructor<?> m) {
		try {
			return invokerConsMethod.bindTo(generatorConstructor.invoke(accGenerator.cast(constuctorGenerator.invoke()),
					m.getDeclaringClass(), m.getParameterTypes(), m.getExceptionTypes(), m.getModifiers()));
		} catch (final Throwable t) {
			throw new Error(t);
		}
	}

	public static MethodHandle handleD(final Method m) {
		try {
			return invokerMethod.bindTo(generatorMethod.invoke(accGenerator.cast(constuctorGenerator.invoke()),
					m.getDeclaringClass(), m.getName(), m.getParameterTypes(), m.getReturnType(), m.getExceptionTypes(),
					m.getModifiers()));
		} catch (final Throwable t) {
			throw new Error(t);
		}
	}

	public static AccessorField handleF(final Field f) {
		return new FastFieldAccessor(f);
	}

	public static InvokerConstructor handleHC(final MethodHandle mn) {
		try {
			return LookupUtil.wrap(InvokerConstructor.class, mn);
		} catch (final Throwable t) {
			throw new Error(t);
		}
	}

	public static InvokerMethod handleHM(final MethodHandle mn) {
		try {
			return LookupUtil.wrap(InvokerMethod.class, mn);
		} catch (final Throwable t) {
			throw new Error(t);
		}
	}

	/**
	 * Call init, but non instance it (requires to be instanced before)...
	 */
	public static Method methodify(final Constructor<?> cn) {
		try {
			final Method s = cn.getDeclaringClass().getDeclaredMethods()[0];
			final Object tmp = ProxyList.UNSAFE.getAndSetObject(s, rootOffsetMethod, null);
			final Method ret = (Method) copyMethod.invokeExact(s);
			ProxyList.UNSAFE.putObject(s, rootOffsetMethod, tmp);
			ProxyList.UNSAFE.putInt(ret, slotOffsetMethod, ProxyList.UNSAFE.getInt(cn, slotOffsetConstructor));
			ProxyList.UNSAFE.putObject(ret, rootOffsetMethod, null);
			ProxyList.UNSAFE.putObject(ret, methodNameO, nameInit);
			ProxyList.UNSAFE.putObject(ret, methodParamsO, cn.getParameterTypes());
			ProxyList.UNSAFE.putObject(ret, methodExceptionsO, cn.getExceptionTypes());
			ProxyList.UNSAFE.putObject(ret, methodModifiersO, 0 & Modifier.STRICT); // why it works with it?
			ProxyList.UNSAFE.putObject(ret, methodClazzO, cn.getDeclaringClass());
			return ret;
		} catch (final Throwable e) {
			throw new Error(e);
		}
	}

	private static Class<?> sameSizeClass(final ClassLoader loader, final Class<?> klass,
			final Collection<String> excluded) {
		final String className = ProxyData.nextName(true);
		final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, ProxyData.MAGIC_SUPER, null);
		int i = 0;
		for (final Field field : klass.getFields()) {
			if (objectFields.contains(field) || excluded.contains(field.getName()))
				continue;
			cw.visitField(Opcodes.ACC_PRIVATE, "field_generated" + ProxyData.r.nextInt(Integer.MAX_VALUE) + "m" + i,
					Type.getDescriptor(field.getType()), null, null);
			i++;
		}
		final byte[] code = cw.toByteArray();
		try {
			return UnsafeUtil.defineClass(className, code, 0, code.length, loader, null);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Supplier<Object> sameSizeObject(final ClassLoader loader, final Class<?> klass,
			final Collection<String> excluded) {
		final Class<?> proxy = sameSizeClass(loader, klass, excluded);
		return () -> ProxyList.UNSAFE.allocateInstance(proxy);
	}

	public static Method setAccessible(final Method m) {
		ProxyList.UNSAFE.putBoolean(m, overrideAccessibleObjectOffset, true);
		return m;
	}

	public static InvokerConstructor wrapConstructor(final Constructor<?> t) {
		return handleHC(handleD(t));
	}

	public static InvokerMethod wrapConstructorNonInstance(final Constructor<?> t) {
		return handleHM(handleD(methodify(t)));
	}
}
