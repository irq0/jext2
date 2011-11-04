package jext2.annotations;

/**
 * Marks classes and methods that are not thread safe
 */
public @interface NotThreadSafe {
	boolean useLock();
}
