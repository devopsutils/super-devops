/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.devops.tool.common.natives;

import static com.wl4g.devops.tool.common.lang.SystemUtils2.LOCAL_PROCESS_ID;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.SystemUtils.JAVA_IO_TMPDIR;
import static org.apache.commons.lang3.SystemUtils.OS_ARCH;
import static org.apache.commons.lang3.SystemUtils.OS_NAME;
import static org.apache.commons.lang3.SystemUtils.USER_NAME;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.wl4g.devops.tool.common.lang.StringUtils2.*;

import com.wl4g.devops.tool.common.lang.ClassUtils2;
import com.wl4g.devops.tool.common.resource.Resource;
import com.wl4g.devops.tool.common.resource.resolver.PathPatternResourceMatchingResolver;

import static com.wl4g.devops.tool.common.lang.Assert.*;

/**
 * The native class library auto-loader is based on the class path or file path,
 * jar file path, usually including x64/x86/amd64/ppc64/aarch64, etc (using JNI
 * - Java Native Interface).
 * 
 * @see <a href=
 *      "http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar">http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar</a>
 * @see <a href=
 *      "https://github.com/adamheinrich/native-utils">https://github.com/adamheinrich/native-utils</a>
 *
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2019年12月3日
 * @since
 */
public class PathPatternNativeLibraryLoader {
	final protected static Logger log = LoggerFactory.getLogger(PathPatternNativeLibraryLoader.class);

	/**
	 * Loaded state flag.
	 */
	final private AtomicBoolean loadedState = new AtomicBoolean(false);

	/**
	 * Native library classLoader.
	 */
	final private ClassLoader classLoader;

	/**
	 * Native library loader classPath location pattern.
	 */
	final private String libLocationPattern;

	/**
	 * OS arch share supports mapping.
	 */
	final private List<OSArchSupport> osArchDefineSupports;

	/**
	 * Matched load native library file resources.
	 */
	final private List<File> loadLibFiles = new ArrayList<>(4);

	public PathPatternNativeLibraryLoader(String libLocationPattern) {
		this(ClassUtils2.getDefaultClassLoader(), libLocationPattern);
	}

	public PathPatternNativeLibraryLoader(ClassLoader classLoader, String libLocationPattern) {
		this(classLoader, libLocationPattern, new ArrayList<OSArchSupport>(32) {
			private static final long serialVersionUID = 5148648284597551860L;
			{
				add(new OSArchSupport("AIX", "ppc"));
				add(new OSArchSupport("AIX", "ppc64", "amd64", "x64"));
				add(new OSArchSupport("FreeBSD", "x86_64", "amd64", "x64"));
				add(new OSArchSupport("Linux", "aarh64", "amd64", "x64"));
				add(new OSArchSupport("Linux", "android-arm"));
				add(new OSArchSupport("Linux", "arm"));
				add(new OSArchSupport("Linux", "armv6"));
				add(new OSArchSupport("Linux", "armv7"));
				add(new OSArchSupport("Linux", "ppc"));
				add(new OSArchSupport("Linux", "ppc64", "amd64", "x64"));
				add(new OSArchSupport("Linux", "ppc64le", "amd64", "x64"));
				add(new OSArchSupport("Linux", "s390x"));
				add(new OSArchSupport("Linux", "x86"));
				add(new OSArchSupport("Linux", "x86_64", "amd64", "x64"));
				add(new OSArchSupport("Mac", "x86"));
				add(new OSArchSupport("Mac", "x86_64", "amd64", "x64"));
				add(new OSArchSupport("SunOS", "sparc"));
				add(new OSArchSupport("SunOS", "x86"));
				add(new OSArchSupport("SunOS", "x86_64", "amd64", "x64"));
				add(new OSArchSupport("Windows", "x86"));
				add(new OSArchSupport("Windows", "x86_64", "amd64", "x64"));
			}
		});
	}

	/**
	 * For example:
	 * 
	 * <pre>
	 * new PathPatternNativeLibraryLoader("/org/xerial/snappy/native/××/×.×");
	 * </pre>
	 * 
	 * <font color=red>Note: Because of the Java multiline annotation problem,
	 * the "*" is replaced by "×"</font>
	 * 
	 * @param classLoader,
	 * @param libLocationPattern
	 * @param osArchShareSupports
	 */
	public PathPatternNativeLibraryLoader(ClassLoader classLoader, String libLocationPattern,
			List<OSArchSupport> osArchShareSupports) {
		notNull(classLoader, "Native library classLoader can't null.");
		hasText(libLocationPattern, "Native library location pattern can't empty.");
		notNull(osArchShareSupports, "OS name and arch support mapping can't null.");
		isTrue(libLocationPattern.startsWith("/"), "The path has to be absolute (start with '/').");
		// Check filename deep hierarchy is okay?
		int libPathDeep = libLocationPattern.split("/").length;
		if (libPathDeep < NATIVE_LIBS_PATH_LEN_MIN) {
			throw new IllegalArgumentException(
					"The filename has to be at least " + NATIVE_LIBS_PATH_LEN_MIN + " characters long.");
		}
		this.classLoader = classLoader;
		this.libLocationPattern = libLocationPattern;
		this.osArchDefineSupports = osArchShareSupports;
	}

	/**
	 * The file from JAR(CLASSPATH) is copied into system temporary directory
	 * and then loaded. The temporary file is deleted after exiting. Method uses
	 * String as filename because the pathname is "abstract", not
	 * system-dependent.
	 * 
	 * @param classLoader
	 *            {@link ClassLoader} for loading native class library
	 * @throws IOException
	 *             Dynamic library read write error
	 * @throws FileNotFoundException
	 *             The specified file was not found in the jar package.
	 */
	public final synchronized void loadLibrarys() throws IOException {
		if (!loadedState.compareAndSet(false, true)) { // Loaded?
			return;
		}

		// Copy files from jar package to system temporary folder.
		PathPatternResourceMatchingResolver resolver = new PathPatternResourceMatchingResolver(classLoader);
		Resource[] resoruces = resolver.getResources(libLocationPattern);
		for (Resource r : resoruces) {
			if (!r.exists() || r.isOpen() || !r.isReadable()) {
				log.warn("Unable to load native class library file: {}", r.getURL().toString());
				continue;
			}
			if (!meetOSTypeAndArch(r.getURL())) {
				continue;
			}
			if (log.isInfoEnabled()) {
				log.info("Load native class library of: {}", r.getURL().toString());
			}

			File tmpLibFile = null; // Native library temporary file.
			try (InputStream in = r.getInputStream()) {
				tmpLibFile = new File(libNativeTmpDir, r.getFilename());
				this.loadLibFiles.add(tmpLibFile);

				// Copy to temporary directory.
				Files.copy(in, tmpLibFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

				// Load to JVM.
				System.load(tmpLibFile.getAbsolutePath());
			} catch (IOException e) {
				if (nonNull(tmpLibFile))
					tmpLibFile.delete();
				throw e;
			} catch (NullPointerException e) {
				if (nonNull(tmpLibFile))
					tmpLibFile.delete();
				throw new FileNotFoundException("Library file [" + r.getURL() + "] was not found inside JAR.");
			}
		}

		// Check any loaded library?
		state(!loadLibFiles.isEmpty(), String.format("Failed to load native library, of os: %s arch: %s, was found resources: %s",
				OS_NAME, OS_ARCH, Arrays.asList(loadLibFiles)));

		// Cleanup temporary lib files.
		/*
		 * It has been proved that when the tmpFile.deleteOnExit() method is
		 * called, the dynamic library file cannot be deleted after the system
		 * exits, because the program is occupied, so if you want to unload the
		 * dynamic library file when the program exits, you can only use hook
		 * (calling private properties and private methods through reflection)
		 */
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				for (File loadFile : loadLibFiles) {
					try {
						unloadNativeLib(loadFile);
					} catch (Throwable th) {
						log.warn(String.format("Failed to unload native library tmp libfile: %", loadFile), th);
					}
				}
			}
		});

	}

	/**
	 * Automatically match the current operating system type and architecture.
	 * 
	 * @return
	 */
	protected boolean meetOSTypeAndArch(URL path) {
		String[] parts = split(path.toString(), "/");
		String osName = parts[parts.length - 3];
		String osArch = parts[parts.length - 2];

		// Skip non current OS platform.
		// e.g. Windows/Windows 7/Windows XP, Mac/Mac OS X/Mac OS X 10.0
		if (!startsWithIgnoreCase(OS_NAME, osName)) {
			return false;
		}

		// Shared fuzzy matching OS architecture.
		List<String> archs = null;
		ok: for (OSArchSupport support : osArchDefineSupports) {
			// e.g. Windows/Windows 7/Windows XP, Mac/Mac OS X/Mac OS X 10.0
			if (startsWithIgnoreCase(OS_NAME, support.getOsName())) {
				for (String arch : support.getOsArchs()) {
					if (equalsIgnoreCase(arch, OS_ARCH)) {
						archs = support.getOsArchs();
						break ok;
					}
				}
			}
		}
		if (Objects.isNull(archs)) {
			throw new IllegalArgumentException(
					String.format("Not found current OS: %s arch support. Supported archs:\n%s", OS_NAME, archs));
		}

		return archs.stream().filter(arch -> equalsIgnoreCase(arch, osArch)).count() > 0;
	}

	/**
	 * Unload native library tmpFile.
	 * 
	 * @param tmpLibFile
	 * @throws Exception
	 * @see <a href="http://peiyuxin.blog.sohu.com/300313528.html">JVM takes up
	 *      local library file reference</a>
	 */
	@SuppressWarnings("unchecked")
	private final synchronized void unloadNativeLib(File tmpLibFile) throws Exception {
		Field field = ClassLoader.class.getDeclaredField("nativeLibraries");
		field.setAccessible(true);
		Vector<Object> libs = (Vector<Object>) field.get(classLoader);
		Iterator<Object> it = libs.iterator();
		while (it.hasNext()) {
			Object object = it.next();
			Field[] fs = object.getClass().getDeclaredFields();
			for (int k = 0; k < fs.length; k++) {
				if (fs[k].getName().equals("name")) {
					fs[k].setAccessible(true);
					String libPath = fs[k].get(object).toString();
					/**
					 * Use the canonical path, otherwise:
					 * 
					 * <pre>
					 * C:\Users\ADMINI~1\AppData\Local\Temp\javanativelibs_Administrator\6480-1577085319527\snappyjava.dll
					 * </pre>
					 */
					if (new File(libPath).getCanonicalPath().equals(tmpLibFile.getCanonicalPath())) {
						Method finalize = object.getClass().getDeclaredMethod("finalize");
						finalize.setAccessible(true);
						finalize.invoke(object);
						if (!tmpLibFile.delete()) {
							log.warn("Failed to cleanup tmp library file of: {}", tmpLibFile);
						}
					}
				}
			}
		}
	}

	/**
	 * Get or create a temporary folder under the system temporary folder
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	private final synchronized static File libsTmpDirectory0(String path) {
		File libsTmpDir = new File(JAVA_IO_TMPDIR, path);
		if (!libsTmpDir.exists()) {
			state(libsTmpDir.mkdirs(), "Failed to create temp directory [" + libsTmpDir.getName() + "]");
		}
		return libsTmpDir;
	}

	/**
	 * The minimum length a prefix for a file has to have according to
	 * {@link File#createTempFile(String, String)}}.
	 */
	final public static int NATIVE_LIBS_PATH_LEN_MIN = 3;

	/**
	 * Java dynamic link native libraries temporary base directory path.
	 */
	final public static File libNativeTmpDir = libsTmpDirectory0(File.separator + "javanativelibs_" + USER_NAME + File.separator
			+ LOCAL_PROCESS_ID + "-" + System.currentTimeMillis());

	/**
	 * Defined operating system architecture share mapping, for example:
	 * (<b>Linux</b> = > <b>amd64</b>, <b>x86_64</b>) means that if the current
	 * system is <b>amd64</b>, However, when there is no native library file in
	 * the <b>amd64</b> directory, it will match in order, and finally
	 * <b>x86_64</b> will be matched successful. </br>
	 * 
	 * For example definitions reference:
	 * 
	 * <pre>
	 * /org/xerial/snappy/native/AIX/ppc/libsnappyjava.a
	 * /org/xerial/snappy/native/AIX/ppc64/libsnappyjava.a
	 * /org/xerial/snappy/native/FreeBSD/x86_64/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/aarh64/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/android-arm/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/arm/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/armv6/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/armv7/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/ppc/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/ppc64/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/ppc64le/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/s390x/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/x86/libsnappyjava.so
	 * /org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so
	 * /org/xerial/snappy/native/Mac/x86/libsnappyjava.jnilib
	 * /org/xerial/snappy/native/Mac/x86_64/libsnappyjava.jnilib
	 * /org/xerial/snappy/native/SunOS/sparc/libsnappyjava.jnilib
	 * /org/xerial/snappy/native/SunOS/x86/libsnappyjava.jnilib
	 * /org/xerial/snappy/native/SunOS/x86_64/libsnappyjava.jnilib
	 * /org/xerial/snappy/native/Windows/x86/libsnappyjava.dll
	 * /org/xerial/snappy/native/Windows/x86_64/libsnappyjava.dll
	 * </pre>
	 * 
	 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
	 * @version v1.0 2019年12月23日
	 * @since
	 */
	final public static class OSArchSupport implements Serializable {
		private static final long serialVersionUID = -5552933071556992452L;

		final private String osName;
		final private List<String> osArchs;

		public OSArchSupport(String osName, String... osArchs) {
			hasText(osName, "Support OS name can't empty.");
			isTrue(nonNull(osArchs), String.format("Support osArchs can't empty."));
			this.osName = osName;
			this.osArchs = Arrays.asList(osArchs);
		}

		public String getOsName() {
			return osName;
		}

		public List<String> getOsArchs() {
			return osArchs;
		}

	}

}