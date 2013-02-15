package com.tf.getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

public class GetterSetterFinder {
	static int classesTotal;
	static int methodsTotal;
	static int getsetterTotal;

	public static void main(String[] args) throws Exception {
		List<JavaClass> classList = new ArrayList<JavaClass>();
		GetterSetterFinder.findClasses(args[0], classList);
		for (JavaClass jc : classList) {
			if (jc.isAbstract() || jc.isInterface()) {
			} else
				printPureGetSetters(jc);
		}
		System.out.println("Total # of classes : " + classesTotal);
		System.out.println("Total # of methods : " + methodsTotal);
		System.out
				.println("Total # of pure getter/setters : " + getsetterTotal);
	}

	private static void printPureGetSetters(JavaClass jc) {
		List<Method> getsetters = new ArrayList<Method>();
		Method[] methods = jc.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			methodsTotal++;
			if (isPureGetter(method) || isPureSetter(method)) {
				getsetters.add(method);
			}
		}
		if (getsetters.size() > 0) {
			System.out.println("Class : " + jc.getClassName());
			for (Method m : getsetters) {
				getsetterTotal++;
				System.out.println("\t" + m);
			}
		}
	}

	private static void findClasses(String dirOrZipPath,
			List<JavaClass> methodList) throws Exception {
		File file = new File(dirOrZipPath);
		String fName = file.getName().toUpperCase();
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				File f = files[i];
				if (f.isDirectory()) {
					findClasses(f.getCanonicalPath(), methodList);
				} else if (f.isFile()) {
					if (f.getName().endsWith(".class")) {
						String filePath = f.getAbsolutePath();
						classesTotal++;
						parseClass(filePath, methodList);
					}
				}
			}
		} else if (file.isFile()) {
			if (fName.endsWith(".ZIP") || fName.endsWith(".JAR")) {
				readZip(file.getAbsolutePath(), methodList);
			} else {
				throw new IllegalStateException("Not correct form file");
			}
		}
	}

	private static void readZip(String path, List<JavaClass> resultList) {
		try {
			// FileInputStream으로 파일을 읽은 후 ZipInputStream으로 변환
			FileInputStream fis = new FileInputStream(path);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze;
			// ZipEntry가 있는 동안 반복
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.getName().endsWith(".class")) {
					classesTotal++;
					parseClass(path, ze.getName(), resultList);
				}
				zis.closeEntry();
			}
			zis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void parseClass(String zipFilePath, String classFilePath,
			List<JavaClass> resultList) {
		try {
			JavaClass classz = new ClassParser(zipFilePath, classFilePath)
					.parse();
			resultList.add(classz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void parseClass(String classFilePath,
			List<JavaClass> resultList) {
		try {
			JavaClass classz = new ClassParser(classFilePath).parse();
			resultList.add(classz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isPureGetter(Method m) {
		boolean result = false;
		if (!m.isNative() || !m.isProtected()) {
			Code code = m.getCode();
			int codeLength = code.getCode().length;
			int maxStack = code.getMaxStack();
			int maxLocal = code.getMaxLocals();
			Type returnType = m.getReturnType();
			if (returnType != Type.VOID) {
				if (returnType == Type.DOUBLE || returnType == Type.LONG) {
					if (codeLength == 5 && maxStack == 2 && maxLocal == 1
							&& m.getName().startsWith("get")) {
						result = true;
					}
				} else if (returnType == Type.BOOLEAN) {
					if (codeLength == 5 && maxStack == 1 && maxLocal == 1) {
						if (m.getName().startsWith("get")
								|| m.getName().startsWith("is")) {
							result = true;
						}
					}
				}
			}
		}
		return result;
	}

	private static boolean isPureSetter(Method m) {
		boolean result = false;
		if (!m.isNative() || !m.isProtected()) {
			Code code = m.getCode();
			int codeLength = code.getCode().length;
			int maxStack = code.getMaxStack();
			int maxLocal = code.getMaxLocals();
			Type returnType = m.getReturnType();
			Type[] argType = m.getArgumentTypes();
			if (argType.length == 1 && Type.VOID == returnType) {
				if (m.getName().startsWith("set")) {
					if (Type.DOUBLE == argType[0] || Type.LONG == argType[0]) {
						if (codeLength == 6 && maxStack == 3 && maxLocal == 3) {
							result = true;
						}
					}
					if (codeLength == 6 && maxStack == 2 && maxLocal == 2) {
						result = true;
					}
				}
			}
		}
		return result;
	}
}