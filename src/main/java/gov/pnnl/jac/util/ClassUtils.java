package gov.pnnl.jac.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

/**
 * <p>
 * Contains static utility methods for convenient access to the context class
 * loader and resources.
 * </p>
 * 
 * @author R. Scarberry
 * 
 */
public class ClassUtils<T> {

	/**
	 * A convenience method for casting an object without generating an 
	 * unchecked cast warning. 
	 * 
	 * @param o  - a reference to be cast to type T.
	 * @param cu - a reference declared to be of type ClassUtils<T> in the code
	 *   from which you are calling this method.  This reference is typically null.  It
	 *   only exists to provide the proper casting context.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T uncheckedCast(Object o, ClassUtils<T> cu) {
		return (T) o;
	}
	
	/**
	 * Same as calling
	 * <code>Thread.currentThread().getContextClassLoader()</code>, but under
	 * the control of an AccessController class. The result is that code running
	 * under a security manager that forbids access to ClassLoaders will still
	 * work if the code's class has been given sufficient privileges, even when
	 * the caller doesn't have such privileges. Otherwise, the entire call stack
	 * must have sufficient privileges.
	 * 
	 * @return the context class loader associated with the current thread, or
	 *         null if security settings disallow it.
	 */
	public static ClassLoader getContextClassLoader() {
		PrivilegedAction<ClassLoader> action = new PrivilegedAction<ClassLoader>() {
			public ClassLoader run() {
				try {
					return Thread.currentThread().getContextClassLoader();
				} catch (AccessControlException ace) {
				}
				return null;
			}
		};
		return AccessController.doPrivileged(action);
	}

	/**
	 * Finds all resources with the given name by calling
	 * <tt>getResources(name)</tt> on the context class loader for the current
	 * thread. If security settings forbid access to the context class loader,
	 * this method returns the results from
	 * <tt>ClassLoader.getSystemResources(name)</tt>. All of this is done under
	 * the control of an <tt>AccessController</tt>, so that code running under a
	 * security manager that forbids access to class loaders will still work as
	 * long as the code's class has sufficient privileges.
	 * 
	 * A resource is some data (images, audio, text, etc) that can be accessed
	 * by class code in a way that is independent of the location of the code.
	 * 
	 * <p>
	 * The name of a resource is a <tt>/</tt>-separated path name that
	 * identifies the resource.
	 * </p>
	 * 
	 * @param name
	 *            the name of the resource.
	 * 
	 * @return An enumeration of <tt>java.net.URL</tt> objects for the resource.
	 *         If no resources could be found, the enumeration will be empty.
	 *         Resources that the class loader doesn't have access to will not
	 *         be in the enumeration. If security settings disallow access, null
	 *         is returned.
	 */
	public static Enumeration<URL> getResources(final String name) {
		final ClassLoader classLoader = getContextClassLoader();
		PrivilegedAction<Enumeration<URL>> action = new PrivilegedAction<Enumeration<URL>>() {
			public Enumeration<URL> run() {
				try {
					if (classLoader != null) {
						return classLoader.getResources(name);
					} else {
						return ClassLoader.getSystemResources(name);
					}
				} catch (IOException ioe) {
					return null;
				}
			}
		};
		return AccessController.doPrivileged(action);
	}

	/**
	 * <p>
	 * Finds the resource with the given name by calling
	 * <tt>getResource(name)</tt> on the context class loader for the current
	 * thread. If security settings forbid access to the context class loader,
	 * this method returns the results from
	 * <tt>ClassLoader.getSystemResource(name)</tt>. All of this is done under
	 * the control of an <tt>AccessController</tt>, so that code running under a
	 * security manager that forbids access to class loaders will still work as
	 * long as the code's class has sufficient privileges.
	 * </p>
	 * 
	 * <p>
	 * A resource is some data (images, audio, text, etc) that can be accessed
	 * by class code in a way that is independent of the location of the code.
	 * </p>
	 * 
	 * <p>
	 * The name of a resource is a '<tt>/</tt>'-separated path name that
	 * identifies the resource.
	 * </p>
	 * 
	 * @param name
	 *            the name of the resource.
	 * 
	 * @return A <tt>java.net.URL</tt> object for reading the resource, or
	 *         <tt>null</tt> if the resource could not be found or security
	 *         settings forbid access.
	 */
	public static URL getResource(final String name) {
		final ClassLoader classLoader = getContextClassLoader();
		PrivilegedAction<URL> action = new PrivilegedAction<URL>() {
			public URL run() {
				if (classLoader != null) {
					return classLoader.getResource(name);
				} else {
					return ClassLoader.getSystemResource(name);
				}
			}
		};
		return AccessController.doPrivileged(action);
	}

	/**
	 * Returns an input stream for reading the specified resource. This method
	 * obtains the stream by calling <tt>getResourceAsStream(name)</tt> on the
	 * context class loader for the current thread. If security settings forbid
	 * access to the context class loader, this method returns the results from
	 * <tt>ClassLoader.getSystemResourceAsStream(name)</tt>. All of this is done
	 * under the control of an <tt>AccessController</tt>, so that code running
	 * under a security manager that forbids access to class loaders will still
	 * work as long as the code's class has sufficient privileges.</p>
	 * 
	 * @param name
	 *            the name of the resource.
	 * 
	 * @return An input stream for reading the resource, or <tt>null</tt> if the
	 *         resource could not be found or security settings forbid access.
	 */
	public static InputStream getResourceAsStream(final String name) {
		final ClassLoader classLoader = getContextClassLoader();
		PrivilegedAction<InputStream> action = new PrivilegedAction<InputStream>() {
			public InputStream run() {
				if (classLoader != null) {
					return classLoader.getResourceAsStream(name);
				} else {
					return ClassLoader.getSystemResourceAsStream(name);
				}
			}
		};
		return AccessController.doPrivileged(action);
	}

}
