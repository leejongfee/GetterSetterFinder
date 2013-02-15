package com.tf.getter;

import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.classfile.JavaClass;

public class Main {

	public static void main(String[] args) throws Exception {
		String methodName =args[1];
		List<JavaClass> classList = new ArrayList<JavaClass>();
		GetterSetterFinder.findClasses(args[0], classList);
		for (JavaClass jc : classList) {
			if (jc.isAbstract() || jc.isInterface()) {
			} else {
				GetterSetterFinder.printSpecificMethod(jc,methodName);
			}		}
		System.out.println("Total # of classes : "
				+ GetterSetterFinder.classesTotal);
		System.out.println("Total # of "+methodName+" : " 
				+ GetterSetterFinder.getsetterTotal);
	}
}
