package ru.zaxar163.demonstration;

import ru.zaxar163.unsafe.xlevel.ThreadList;

public final class InternalPlayGround {
	public static void main(final String... args) throws Throwable {
		ThreadList.getThreads().forEach((n, t) -> {
			System.out.println("Thread # " + n + " Data: " + t + " Classloader: " + t.getContextClassLoader());
		});
		JVMPlayGround.main(args);
	}

	private InternalPlayGround() {
	}
}
