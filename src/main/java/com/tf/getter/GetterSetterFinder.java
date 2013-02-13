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

		for (JavaClass j : classList) {
			System.out.println("----------------------" + j.getClassName()
					+ "----------------------");
			Method[] methods = readMethod(j);
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				System.out.println(method);
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
		}  catch (IOException e) {
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

	private static Method[] readMethod(JavaClass resultList) {
		Method[] methods = resultList.getMethods();
		return methods;
	}
}
