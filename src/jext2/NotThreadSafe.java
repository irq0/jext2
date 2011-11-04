package jext2;

/**
 * Marks classes and methods that are not thread safe
 */
public @interface NotThreadSafe {
	boolean useLock();
}
