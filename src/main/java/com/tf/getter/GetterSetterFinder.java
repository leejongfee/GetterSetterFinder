package com.tf.getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
		List<MethodInfo> overriddenMethodList = new ArrayList<MethodInfo>();
		List<MethodInfo[]> getSetList = new ArrayList<MethodInfo[]>();
		List<String> packageNameList = new ArrayList<String>();
		List<String> classNameList = new ArrayList<String>();
		List<String> methodNameList = new ArrayList<String>();
		GetterSetterFinder.findClasses(args[0], classList);
		findSubClass(classList, overriddenMethodList);
		findPureGetterSetter(classList, getSetList);

		for (int i = 0; i < getSetList.size(); i++) {
			// System.out.println(getSetList.get(i).jc.getClassName());
			// System.out.println(getSetList.get(i).m.getName());
		}

		for (int i = 0; i < overriddenMethodList.size(); i++) {
			// System.out.println(overriddenMethodList.get(i).jc.getClassName());
			// System.out.println(overriddenMethodList.get(i).m.getName());
		}
	}

	private static void sortPackage(List<String> packageNames) {
		List<String> listTemp = new ArrayList<String>();
		System.out
				.println("--------------------Package 별로 분류--------------------");
		for (int i = 0; i < packageNames.size(); i++) {
			String pkgName = packageNames.get(i);
			String count = String.format("%010d",
					Collections.frequency(packageNames, pkgName));
			listTemp.add(count + packageNames.get(i));
		}
		ArrayList<String> sortResult = new ArrayList<String>(
				new HashSet<String>(listTemp));
		Collections.sort(sortResult);
		Collections.reverse(sortResult);
		for (int j = 0; j < sortResult.size(); j++) {
			int count = Integer.valueOf(sortResult.get(j).substring(0, 10));
			System.out.println(sortResult.get(j).substring(10) + " : " + count);
		}
	}

	private static void sortClass(List<String> classNames,
			List<MethodInfo> methodNames) {
		List<String> listTemp = new ArrayList<String>();
		System.out
				.println("--------------------Class 별로 분류--------------------");
		for (int i = 0; i < classNames.size(); i++) {
			String pkgName = classNames.get(i);
			String count = String.format("%010d",
					Collections.frequency(classNames, pkgName));
			listTemp.add(count + classNames.get(i));
		}
		ArrayList<String> sortResult = new ArrayList<String>(
				new HashSet<String>(listTemp));
		Collections.sort(sortResult);
		Collections.reverse(sortResult);
		for (int j = 0; j < sortResult.size(); j++) {
			int count = Integer.valueOf(sortResult.get(j).substring(0, 10));
			System.out.println(sortResult.get(j).substring(10) + " : " + count);
			for (MethodInfo mi : methodNames) {
				if (mi.jc.getClassName()
						.equals(sortResult.get(j).substring(10))) {
					System.out.println("\t" + mi.m.getName());
				}
			}

		}
	}

	private static void findPureGetterSetter(List<JavaClass> classList,
			List<MethodInfo[]> getSetList) {
		for (JavaClass jc : classList) {
			Method[] methods = jc.getMethods();
			for (Method method : methods) {
				if (isPureSetter(method) || isPureGetter(method)) {
					Method method2 = isOnlyGetter(methods, method);
					if (method2 != null) {
						System.out.println(method2);
						System.out.println(method);
						MethodInfo mi = new MethodInfo();
						mi.jc = jc;
						mi.m = method;
					}
				}
			}
		}
	}

	private static void findOverriddenMethod(JavaClass subClass,
			JavaClass superClass, List<MethodInfo> overriddenMethodList) {
		Method[] subMethods = subClass.getMethods();
		Method[] superMethods = superClass.getMethods();
		for (Method subMethod : subMethods) {
			for (Method superMethod : superMethods) {
				if (subMethod.getName().equals(superMethod.getName())) {
					if (subMethod.getSignature().equals(
							superMethod.getSignature()))
						if (!subMethod.getName().contains("<init>")
								|| !superMethod.getName().contains("<init>")) {
							MethodInfo miSuper = new MethodInfo();
							MethodInfo miSub = new MethodInfo();
							miSuper.jc = superClass;
							miSuper.m = superMethod;
							miSub.jc = subClass;
							miSub.m = subMethod;
							overriddenMethodList.add(miSuper);
							overriddenMethodList.add(miSub);
						}
				}
			}
		}
	}

	private static void findSuperClass(List<JavaClass> classList,
			JavaClass subClass, List<MethodInfo> overriddenMethodList) {
		String superClassName = subClass.getSuperclassName();
		for (JavaClass superClass : classList) {
			if (superClassName.equals(superClass.getClassName())) {
				findOverriddenMethod(subClass, superClass, overriddenMethodList);
			}
		}
	}

	private static void findSubClass(List<JavaClass> classList,
			List<MethodInfo> overriddenMethodList) {
		for (JavaClass jc : classList) {
			String superClassName = jc.getSuperclassName();
			if (!superClassName.equals("java.lang.Object")) {
				findSuperClass(classList, jc, overriddenMethodList);
			}
		}
	}

	static void findClasses(String dirOrZipPath, List<JavaClass> methodList)
			throws Exception {
		File file = new File(dirOrZipPath);
		String fName = file.getName().toUpperCase();
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
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
			}
		} else if (file.isFile()) {
			if (fName.endsWith(".ZIP") || fName.endsWith(".JAR")) {
				readZip(file.getAbsolutePath(), methodList);
			} else {
				throw new IllegalStateException("Not correct form file");
			}
		}
	}

	private static void readZip(String path, List<JavaClass> methodList) {
		try {
			FileInputStream fis = new FileInputStream(path);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.getName().endsWith(".class")) {
					classesTotal++;
					parseClass(path, ze.getName(), methodList);
				}
				zis.closeEntry();
			}
			zis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void parseClass(String zipFilePath, String classFilePath,
			List<JavaClass> methodList) {
		try {
			JavaClass classz = new ClassParser(zipFilePath, classFilePath)
					.parse();
			methodList.add(classz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void parseClass(String classFilePath,
			List<JavaClass> methodList) {
		try {
			JavaClass classz = new ClassParser(classFilePath).parse();
			methodList.add(classz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isPureGetter(Method m) {
		boolean result = false;
		if (!m.isNative() && !m.isVolatile() && !m.isAbstract()) {
			Code code = m.getCode();
			int codeLength = code.getCode().length;
			int maxStack = code.getMaxStack();
			int maxLocal = code.getMaxLocals();
			Type returnType = m.getReturnType();
			if (returnType != Type.VOID) {
				if (returnType == Type.DOUBLE || returnType == Type.LONG) {
					boolean isPureGetterBytecodePatternBig = codeLength == 5
							&& maxStack == 2 && maxLocal == 1;
					if (isPureGetterBytecodePatternBig
							&& m.getName().startsWith("get")) {
						result = true;
					}
				} else {
					boolean isPureGetterBytecodePattern = codeLength == 5
							&& maxStack == 1 && maxLocal == 1;
					if (returnType == Type.BOOLEAN) {
						if (isPureGetterBytecodePattern) {
							if (m.getName().startsWith("is")) {
								result = true;
							}
						}
					} else if (isPureGetterBytecodePattern) {
						if (m.getName().startsWith("get")) {
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
		Code code2 = m.getCode();
		if (code2 != null) {
			Code code = m.getCode();
			int codeLength = code.getCode().length;
			int maxStack = code.getMaxStack();
			int maxLocal = code.getMaxLocals();
			Type returnType = m.getReturnType();
			Type[] argType = m.getArgumentTypes();
			if (argType.length == 1 && Type.VOID == returnType) {
				if (m.getName().startsWith("set")) {
					if (Type.DOUBLE == argType[0] || Type.LONG == argType[0]) {
						boolean isPureSetterBytecodePatternBig = codeLength == 6
								&& maxStack == 3 && maxLocal == 3;
						if (isPureSetterBytecodePatternBig) {
							result = true;
						}
					}
					boolean isPureSetterBytecodePattern = codeLength == 6
							&& maxStack == 2 && maxLocal == 2;
					if (isPureSetterBytecodePattern) {
						result = true;
					}
				}
			}
		}
		return result;
	}

	private static Method isOnlyGetter(Method[] methods, Method method) {
		Method resultMethod = null;
		String methodName = method.getName();
		for (int i = 0; i < methods.length; i++) {
			String compareName = methods[i].getName();
			if (methodName.startsWith("is")) {
				if (compareName.startsWith("get")
						|| compareName.startsWith("set")) {
					if (methodName.substring(2)
							.equals(compareName.substring(3))) {
						resultMethod = methods[i];
					}
				} else if (compareName.startsWith("is")) {
					if (methodName.substring(3)
							.equals(compareName.substring(2))) {
						resultMethod = methods[i];
					}
				}
			}
			if (!methodName.equals(compareName)) {
				if (compareName.startsWith("get")
						|| compareName.startsWith("set")) {
					if (methodName.substring(3)
							.equals(compareName.substring(3))) {
						resultMethod = methods[i];
					}
				} else if (compareName.startsWith("is")) {
					if (methodName.substring(3)
							.equals(compareName.substring(2))) {
						resultMethod = methods[i];
					}
				}
			}
		}
		return resultMethod;
	}

	static class MethodInfo {
		JavaClass jc;
		Method m;
	}
}