package ru.zaxar163.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class CompoundEnumeration<E> implements Enumeration<E> {
	private final Enumeration<E>[] enums;
	private int index = 0;

	public CompoundEnumeration(final Enumeration<E>[] enums) {
		this.enums = enums;
	}

	@Override
	public boolean hasMoreElements() {
		return next();
	}

	private boolean next() {
		while (index < enums.length) {
			if (enums[index] != null && enums[index].hasMoreElements())
				return true;
			index++;
		}
		return false;
	}

	@Override
	public E nextElement() {
		if (!next())
			throw new NoSuchElementException();
		return enums[index].nextElement();
	}
}

public final class DelegateClassLoader extends ClassLoader {
	public static final DelegateClassLoader INSTANCE = new DelegateClassLoader();
	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final Set<ClassLoader> cls;

	private DelegateClassLoader() {
		super(null);
		cls = Collections.newSetFromMap(new ConcurrentHashMap<>());
		cls.add(ClassLoader.getSystemClassLoader());
		cls.add(this.getClass().getClassLoader());
	}

	public void append(final Class<?> c) {
		if (c != null)
			append(c.getClassLoader());
	}

	public void append(final ClassLoader c) {
		if (c != null)
			cls.add(c);
	}

	public void append(final Constructor<?> c) {
		if (c != null)
			append(c.getDeclaringClass().getClassLoader());
	}

	public void append(final Field c) {
		if (c != null)
			append(c.getDeclaringClass().getClassLoader());
	}

	public void append(final Method c) {
		if (c != null)
			append(c.getDeclaringClass().getClassLoader());
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		for (final ClassLoader e : cls)
			try {
				return e.loadClass(name);
			} catch (final Throwable t) {
			}
		throw new ClassNotFoundException(name);
	}

	public Class<?> findLoadedClassA(final String name) {
		return cls.stream().map(e -> ClassUtil.findLoadedClass(name)).filter(e -> e != null).findFirst().orElse(null);
	}

	@Override
	protected URL findResource(final String name) {
		return getResource(name);
	}

	@Override
	protected Enumeration<URL> findResources(final String name) throws IOException {
		return getResources(name);
	}

	@Override
	public URL getResource(final String name) {
		URL url;
		for (final ClassLoader l : cls) {
			url = l.getResource(name);
			if (url != null)
				return url;
		}
		return null;
	}

	@Override
	public InputStream getResourceAsStream(final String name) {
		InputStream in;
		for (final ClassLoader l : cls) {
			in = l.getResourceAsStream(name);
			if (in != null)
				return in;
		}
		return null;
	}

	@Override
	public Enumeration<URL> getResources(final String name) throws IOException {
		final ClassLoader[] current = cls.toArray(new ClassLoader[0]);
		@SuppressWarnings("unchecked")
		final Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[current.length];
		for (int i = 0; i < current.length; i++)
			tmp[i] = current[i].getResources(name);

		return new CompoundEnumeration<>(tmp);
	}

	@Override
	public Class<?> loadClass(final String name) throws ClassNotFoundException {
		for (final ClassLoader e : cls)
			try {
				return e.loadClass(name);
			} catch (final Throwable t) {
			}
		throw new ClassNotFoundException(name);
	}

	@Override
	protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
		for (final ClassLoader e : cls)
			try {
				final Class<?> clazz = e.loadClass(name);
				if (resolve)
					resolveClass(clazz);
				return clazz;
			} catch (final Throwable t) {
			}
		throw new ClassNotFoundException(name);
	}
}