package com.tf.getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class GetterSetterFinder {

	public static void main(String[] args) throws Exception {
		List<JavaClass> classList = new ArrayList<JavaClass>();
		GetterSetterFinder.findClasses(args[0], classList);
		for (JavaClass jc : classList) {
			printPureGetSetters(jc);
		}
	}

	private static void printPureGetSetters(JavaClass jc) {
		List<Method> getsetters = new ArrayList<Method>();
		Method[] methods = jc.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (isPureGetter(method) || isPureSetter(method)) {
				getsetters.add(method);
			}
		}
		if (getsetters.size() > 0) {
			System.out.println("Class : " + jc.getClassName());
			for (Method m : getsetters) {
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
		int code_Length = m.getCode().getCode().length;
		int max_Stack = m.getCode().getMaxStack();
		int max_Local = m.getCode().getMaxLocals();
		String return_Type = m.getReturnType().toString();
		boolean result = false;
		if (return_Type != "void") {
			if (return_Type.equals("double") || (return_Type.equals("long"))) {
				if (code_Length == 5 && max_Stack == 2 && max_Local == 1
						&& m.getName().startsWith("get")) {
					result = true;
				}
			} else if (code_Length == 5 && max_Stack == 1 && max_Local == 1
					&& m.getName().startsWith("get")) {
				result = true;
			}
		}
		return result;
	}

	private static boolean isPureSetter(Method m) {
		int code_Length = m.getCode().getCode().length;
		int max_Stack = m.getCode().getMaxStack();
		int max_Local = m.getCode().getMaxLocals();
		String return_Type = m.getReturnType().toString();
		boolean result = false;
		if (return_Type != "void") {
			if (return_Type.equals("double") || (return_Type.equals("long"))) {
				if (code_Length == 6 && max_Stack == 3 && max_Local == 3
						&& m.getName().startsWith("set")) {
					result = true;
				}
			} else if (code_Length == 6 && max_Stack == 2 && max_Local == 2
					&& m.getName().startsWith("set")) {
				result = true;
			}
		}
		return result;
	}
}