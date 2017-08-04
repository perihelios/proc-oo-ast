package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement

class MethodBuilder {
	private final VariableScope variableScope
	private final int modifiers

	private ClassNode returnType
	private String name
	private Parameter[] parameters
	private ClassNode[] exceptions
	private List<Statement> statements = []

	MethodBuilder(MethodNode oldMethod) {
		this.variableScope = oldMethod.variableScope
		this.modifiers =
			(oldMethod.modifiers & (MethodNode.ACC_PUBLIC | MethodNode.ACC_STATIC)) | MethodNode.ACC_SYNTHETIC

		useReturnType(oldMethod.returnType)
		useParameters(oldMethod.parameters)
		useName(oldMethod.name)
		useDeclaredExceptions(oldMethod.exceptions)
	}

	MethodBuilder useReturnType(ClassNode returnType) {
		// Return type is finicky; if it represents a generic class with a concrete type parameter
		//  (e.g., Foo<String>), you'll get an error: "A transform used a generics containing ClassNode..."
		//  Copying the ClassNode (discarding generics info in the process) fixes this. BUT, you must set
		//  the "redirect" (delegate) on the copy to point to the original so the new ClassNode will act
		//  as if it were the original (e.g., node.methods will return correct values).
		if (returnType.usingGenerics) {
			this.returnType = ClassHelper.make(returnType.name)
			this.returnType.setRedirect(returnType)
		} else {
			this.returnType = returnType
		}

		this
	}

	MethodBuilder useName(String name) {
		this.name = name
		this
	}

	MethodBuilder useParameters(Parameter... parameters) {
		// When using parameters from an existing method, they need to be copied while in a context where you
		//  have access to the original method, as the Groovy Verifier totally messes up the initialExpression
		//  at a later time
		this.parameters = parameters.collect { oldParam ->
			Parameter newParam = new Parameter(oldParam.type, oldParam.name, oldParam.initialExpression)
			newParam.addAnnotations(oldParam.annotations)
			newParam
		}
		this
	}

	MethodBuilder useDeclaredExceptions(ClassNode... exceptions) {
		this.exceptions = exceptions
		this
	}

	MethodBuilder leftShift(Statement statement) {
		statements << statement
		this
	}

	MethodBuilder leftShift(Iterable<Statement> statements) {
		for (Statement statement : statements) {
			leftShift(statement)
		}
		this
	}

	MethodNode build() {
		return new MethodNode(name, modifiers, returnType, parameters, exceptions,
			new BlockStatement(statements, variableScope))
	}
}
