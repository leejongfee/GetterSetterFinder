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
		List<InheritanceInfo> InheritanceList = new ArrayList<InheritanceInfo>();
		List<GetSetInfo> getSetList = new ArrayList<GetSetInfo>();
		List<String> packageNameList = new ArrayList<String>();
		List<String> classNameList = new ArrayList<String>();
		List<String> methodNameList = new ArrayList<String>();
		GetterSetterFinder.findClasses(args[0], classList);

		findSubClass(classList, InheritanceList);
		findPureGetterSetter(classList, getSetList);
		removeOverriddenMethod(InheritanceList, getSetList);
		for (GetSetInfo gi : getSetList) {
			packageNameList.add(gi.jc.getPackageName());
			classNameList.add(gi.jc.getClassName());
			methodNameList.add(gi.getter.getName());
		}
		sortClass(classNameList, getSetList);
		sortPackage(packageNameList);
	}

	private static void removeOverriddenMethod(
			List<InheritanceInfo> InheritanceList, List<GetSetInfo> getSetList) {
		for (InheritanceInfo il : InheritanceList) {
			String ilSubName = il.subClass.getClassName();
			String ilSuperName = il.superClass.getClassName();
			String ilMethodName = il.method.getName();
			String ilMethodSignature = il.method.getSignature();
			for (int i = 0; i < getSetList.size(); i++) {
				String className = getSetList.get(i).jc.getClassName();
				String getterName = getSetList.get(i).getter.getName();
				String setterName = getSetList.get(i).setter.getName();
				String getSignatur = getSetList.get(i).getter.getSignature();
				String setSignatur = getSetList.get(i).setter.getSignature();
				if (ilMethodName.equals(getterName)
						&& ilMethodSignature.equals(getSignatur)) {
					if (ilSubName.equals(className)
							|| ilSuperName.equals(className)) {
						getSetList.remove(i);
					}
				} else if (ilMethodName.equals(setterName)
						&& ilMethodSignature.equals(setSignatur)) {
					if (ilSubName.equals(className)
							|| ilSuperName.equals(className)) {
						getSetList.remove(i);
					}
				}
			}
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
			int count = Integer.valueOf(sortResult.get(j).substring(0, 10)) * 2;
			System.out.println(sortResult.get(j).substring(10) + " : " + count);
		}
	}

	private static void sortClass(List<String> classNames,
			List<GetSetInfo> methodNames) {
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
			int count = Integer.valueOf(sortResult.get(j).substring(0, 10)) * 2;
			System.out.println(sortResult.get(j).substring(10) + " : " + count);
			for (GetSetInfo mi : methodNames) {
				if (mi.jc.getClassName()
						.equals(sortResult.get(j).substring(10))) {
					System.out.println(mi.getter.getName());
					System.out.println(mi.setter.getName());
				}
			}

		}
	}

	private static void findPureGetterSetter(List<JavaClass> classList,
			List<GetSetInfo> getSetList) {
		for (JavaClass jc : classList) {
			Method[] methods = jc.getMethods();
			for (Method getter : methods) {
				if (isPureGetter(getter)) {
					Method setter = isOnlyGetter(methods, getter);
					if (setter != null && isPureSetter(setter)) {
						GetSetInfo gsInfo = new GetSetInfo();
						gsInfo.jc = jc;
						gsInfo.getter = getter;
						gsInfo.setter = setter;
						getSetList.add(gsInfo);
					}
				}
			}
		}
	}

	private static void findOverriddenMethod(JavaClass subClass,
			JavaClass superClass, List<InheritanceInfo> overriddenMethodList) {
		if (subClass != null && superClass != null) {
			Method[] subMethods = subClass.getMethods();
			Method[] superMethods = superClass.getMethods();
			if (subMethods.length == 0 || superMethods.length == 0) {
				for (Method subMethod : subMethods) {
					for (Method superMethod : superMethods) {
						if (subMethod.getName().equals(superMethod.getName())) {
							if (subMethod.getSignature().equals(
									superMethod.getSignature()))
								if (!subMethod.getName().contains("<init>")
										|| !superMethod.getName().contains(
												"<init>")) {
									InheritanceInfo iInfo = new InheritanceInfo();
									iInfo.superClass = superClass;
									iInfo.subClass = subClass;
									iInfo.method = subMethod;
									overriddenMethodList.add(iInfo);
								}
						}
					}
				}
			}
		}
	}

	private static void findSuperClass(List<JavaClass> classList, JavaClass jc,
			List<InheritanceInfo> InheritanceList) {
		String superName = jc.getSuperclassName();
		while (true) {
			JavaClass superClass = findSpecificClass(classList, superName);
			if (superClass != null) {
				superName = superClass.getSuperclassName();
				if (!superClass.getSuperclassName().equals("java.lang.Object")) {
					findOverriddenMethod(jc, superClass, InheritanceList);
				} else {
					findOverriddenMethod(jc, superClass, InheritanceList);
					break;
				}
			} else {
				break;
			}
		}
	}

	private static JavaClass findSpecificClass(List<JavaClass> classList,
			String className) {
		JavaClass specificClass = null;
		for (JavaClass jc : classList) {
			if (jc.getClassName().equals(className)) {
				specificClass = jc;
			}
		}
		return specificClass;
	}

	private static void findInterface(List<JavaClass> classList, JavaClass jc,
			List<InheritanceInfo> InheritanceList) {
		String[] interfaceNames = jc.getInterfaceNames();
		for (String interfaceName : interfaceNames) {
			JavaClass interfaceClass = findSpecificClass(classList,
					interfaceName);
			findOverriddenMethod(jc, interfaceClass, InheritanceList);
		}
	}

	private static void findSubClass(List<JavaClass> classList,
			List<InheritanceInfo> InheritanceList) {
		for (JavaClass jc : classList) {
			String[] interfaceNames = jc.getInterfaceNames();
			String superClassName = jc.getSuperclassName();
			if (interfaceNames.length != 0) {
				findInterface(classList, jc, InheritanceList);
			}
			if (!superClassName.equals("java.lang.Object")) {
				findSuperClass(classList, jc, InheritanceList);
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

	static class GetSetInfo {
		JavaClass jc;
		Method getter;
		Method setter;
	}

	static class InheritanceInfo {
		JavaClass superClass;
		JavaClass subClass;
		Method method;
	}
}