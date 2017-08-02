package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OoInjectedAstTransformation implements ASTTransformation {
	@Override
	void visit(ASTNode[] nodes, SourceUnit source) {
		source.AST.classes.each { ClassNode clazz ->
			visitClass(clazz, source)
		}
	}

	private static void visitClass(ClassNode clazz, SourceUnit source) {
		if (!clazz.annotations.any { it.classNode.name == ObjectOriented.name }) {
			return
		}

		List<MethodNode> ooMethods = []

		clazz.methods.findAll { MethodNode method ->
			method.static &&
				method.public &&
				method.parameters.length >= 1 &&
				method.parameters[0].type.name == clazz.name
		}.each { MethodNode method ->
			// TODO: Test variable scope set correctly (?)
			BlockStatement blockStatement = new BlockStatement([], method.variableScope)
			ClassNode returnType = method.returnType
			AnnotationNode ooCopyNode = method.parameters[0].annotations.find { it.classNode.name == OoCopy.name }
			VariableExpression targetVariable = new VariableExpression(method.parameters[0].name, clazz)

			if (ooCopyNode) {
				if (!clazz.declaredConstructors.any {
					it.parameters.length == 1 &&
						it.parameters[0].type.name == clazz.name
				}) {
					source.errorCollector.addError(new SyntaxErrorMessage(new SyntaxException(
						"Found @$OoCopy.simpleName on procedure parameter in class $clazz.name, " +
							"which has no copy constructor",
						ooCopyNode.lineNumber,
						ooCopyNode.columnNumber
					), source))
				}

				targetVariable = new VariableExpression("__oo_injection", clazz)

				blockStatement.addStatement(
					new ExpressionStatement(
						new DeclarationExpression(
							targetVariable,
							Token.newSymbol("=", -1, -1),
							new ConstructorCallExpression(clazz, new ArgumentListExpression(
								new VariableExpression(method.parameters[0].name, clazz)
							))
						)
					)
				)

				if (method.voidMethod) {
					// Copy of ClassNode is necessary to avoid error:
					//  "A transform used a generics containing ClassNode..."
					returnType = ClassHelper.make(clazz.name)
				}
			}

			blockStatement.addStatement(new ExpressionStatement(
				new StaticMethodCallExpression(
					clazz,
					method.name,
					new ArgumentListExpression(
						[targetVariable] +
							(method.parameters as List<Parameter>)
								.subList(1, method.parameters.length).collect { Parameter param ->
								new VariableExpression(param.name, param.type)
							} as List<Expression>
					)
				)
			)
			)

			if (ooCopyNode && method.isVoidMethod()) {
				blockStatement.addStatement(new ReturnStatement(targetVariable))
			}

			ooMethods << new MethodNode(
				"__oo_injection__" + method.name,
				MethodNode.ACC_PUBLIC | MethodNode.ACC_STATIC | MethodNode.ACC_SYNTHETIC,
				returnType,
				method.parameters.collect {
					Parameter param = new Parameter(
						it.type,
						it.name,
						it.hasInitialExpression() ? it.initialExpression : null
					)
					param.addAnnotations(it.annotations)

					return param
				} as Parameter[],
				method.exceptions,
				blockStatement
			)
		}

		ooMethods.each { clazz.addMethod(it) }
	}
}
