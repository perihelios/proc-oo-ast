package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit

import java.lang.annotation.Annotation

class AstCategory {
	static boolean hasAnnotation(AnnotatedNode node, Class<? extends Annotation> annotationType) {
		node.annotations.any { it.classNode.name == annotationType.name }
	}

	static AnnotationNode annotation(AnnotatedNode node, Class<? extends Annotation> annotationType) {
		node.annotations.find { it.classNode.name == annotationType.name }
	}

	static boolean isProcedural(MethodNode method) {
		method.static && method.public && method.parameters.length >= 1 &&
			method.parameters[0].type.name == method.declaringClass.name
	}

	static List<MethodNode> getProceduralMethods(ClassNode type) {
		type.methods.findAll(this.&isProcedural)
	}

	static Parameter getProceduralParameter(MethodNode method) {
		method.parameters[0]
	}

	static boolean hasCopyConstructor(ClassNode classNode) {
		classNode.declaredConstructors.any { constructor ->
			constructor.parameters.length == 1 &&
				constructor.parameters[0].type.name == classNode.name
		}
	}

	static ClassNode leftShift(ClassNode type, MethodBuilder builder) {
		type.addMethod(builder.build())
		type
	}

	static ClassNode leftShift(ClassNode type, Iterable<? extends MethodBuilder> builders) {
		for (MethodBuilder builder : builders) {
			leftShift(type, builder)
		}
		type
	}

	static MethodNode leftShift(MethodNode method, Iterable<Statement> statements) {
		BlockStatement blockStatement = method.code as BlockStatement
		for (Statement statement : statements) {
			blockStatement.addStatement(statement)
		}
		method
	}

	static MethodNode leftShift(MethodNode method, Statement statement) {
		(method.code as BlockStatement).addStatement(statement)
		method
	}

	static Statement callStatic(ClassNode type, String methodName, List<String> arguments) {
		List<VariableExpression> argumentVariables = arguments.collect { new VariableExpression(it) }
		ArgumentListExpression argumentList = new ArgumentListExpression(argumentVariables)

		new ExpressionStatement(
			new StaticMethodCallExpression(type, methodName, argumentList)
		)
	}

	static MethodBuilder buildCopyNamed(MethodNode oldMethod, String newName) {
		new MethodBuilder(oldMethod).useName(newName)
	}

	static ReportError reportError(SourceUnit source, String message) {
		return new ReportError(source, message)
	}
}
