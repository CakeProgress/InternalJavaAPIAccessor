package ru.zaxar163.util.proxies;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ru.zaxar163.util.ClassUtil;
import ru.zaxar163.util.DelegateClassLoader;
import ru.zaxar163.util.LookupUtil;
import ru.zaxar163.util.dynamicgen.FastDynamicProxy;
import ru.zaxar163.util.dynamicgen.FastStaticProxy;

public final class ProxyList {
	public static final CleanerProxy CLEANER;
	public static final UnsafeProxy UNSAFE;
	public static final Map<String, Object> UNSAFE_FIELDS;

	static {
		try {
			final Class<?> unsafe = ClassUtil.nonThrowingFirstClass("jdk.internal.misc.Unsafe", "sun.misc.Unsafe");
			final Field[] fieldsUnsafe = LookupUtil.getDeclaredFields(unsafe);
			final Field unsafeInst = Arrays.stream(fieldsUnsafe)
					.filter(e -> e.getType().equals(unsafe) && e.getName().toLowerCase(Locale.US).contains("unsafe"))
					.findFirst().get();
			unsafeInst.setAccessible(true);
			final Object theUnsafe = unsafeInst.get(null);
			UNSAFE = new FastStaticProxy<>(DelegateClassLoader.INSTANCE, unsafe, UnsafeProxy.class).instance(theUnsafe);
			final Map<String, Object> toFillF = new HashMap<>();
			for (final Field f : fieldsUnsafe) {
				if (f.equals(theUnsafe))
					continue;
				f.setAccessible(true);
				toFillF.put(f.getName(), f.get(theUnsafe));
			}
			UNSAFE_FIELDS = Collections.unmodifiableMap(toFillF);
			CLEANER = new FastDynamicProxy<>(DelegateClassLoader.INSTANCE,
					ClassUtil.nonThrowingFirstClass("jdk.internal.ref.Cleaner", "sun.misc.Cleaner"), CleanerProxy.class)
							.instance();
		} catch (final Throwable e) {
			throw new Error(e);
		}
	}

	private ProxyList() {
	}
}
