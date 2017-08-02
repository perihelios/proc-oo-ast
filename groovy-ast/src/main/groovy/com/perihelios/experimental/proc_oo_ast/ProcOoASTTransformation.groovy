package com.perihelios.experimental.proc_oo_ast

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.SynchronizedStatement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@CompileStatic
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class ProcOoASTTransformation implements ASTTransformation {
	@Override
	void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
		List<ClassNode> classes = sourceUnit.AST.classes

		for (ClassNode clazz : classes) {
			clazz.methods.each { MethodNode method ->
				for (ListIterator<Statement> statements = ((BlockStatement) method.code).statements.listIterator();
				     statements; ) {
					statements.set(tweakStatement(statements.next()))
				}
			}

			if (clazz.annotations.any { it.classNode.name == ObjectOriented.name }) {
				List<MethodNode> ooMethods = []

				clazz.methods.findAll { MethodNode method ->
					method.static &&
					method.public &&
					method.parameters.length >= 1 &&
					method.parameters[0].type.name == clazz.name
				}.each { MethodNode method ->
					BlockStatement blockStatement = new BlockStatement([], method.variableScope)
					ClassNode returnType

					if (method.parameters[0].annotations.any { it.classNode.name == OoCopy.name } &&
							method.voidMethod) {
						blockStatement.statements << new ExpressionStatement(
							new DeclarationExpression(
								new VariableExpression("__oo_injection", clazz),
								Token.newSymbol("=", -1, -1),
								new ConstructorCallExpression(clazz, new ArgumentListExpression(
									new VariableExpression(method.parameters[0].name, clazz)
								))
							)
						)

						blockStatement.statements << new ExpressionStatement(
							new StaticMethodCallExpression(clazz, method.name, new ArgumentListExpression(
								[new VariableExpression("__oo_injection", clazz)] +
								(method.parameters as List<Parameter>)
									.subList(1, method.parameters.length).collect { Parameter param ->
										new VariableExpression(param.name, param.type)
									} as List<Expression>
							))
						)

						// Copy of ClassNode is necessary to avoid error: "A transform used a generics containing
						// ClassNode..."
						returnType = ClassHelper.make(clazz.name)

						blockStatement.statements << new ReturnStatement(
							new VariableExpression("__oo_injection", returnType)
						)
					} else {
						returnType = method.returnType

						if (method.isVoidMethod()) {
							blockStatement.statements << new ExpressionStatement(
								new StaticMethodCallExpression(clazz, method.name, new ArgumentListExpression(
									method.parameters.collect { Parameter param ->
										new VariableExpression(param.name, param.type)
									} as List<Expression>
								))
							)
						} else {
							blockStatement.statements << new ExpressionStatement(
								new DeclarationExpression(
									new VariableExpression("__oo_injection", returnType),
									Token.newSymbol("=", -1, -1),
									new StaticMethodCallExpression(clazz, method.name, new ArgumentListExpression(
										method.parameters.collect { Parameter param ->
											new VariableExpression(param.name, param.type)
										} as List<Expression>
									))
								)
							)

							blockStatement.statements << new ReturnStatement(
								new VariableExpression("__oo_injection", returnType)
							)
						}
					}

					ooMethods << new MethodNode(
						"__oo_injection__" + method.name,
						MethodNode.ACC_PUBLIC | MethodNode.ACC_STATIC | MethodNode.ACC_SYNTHETIC,
						returnType,
						method.parameters,
						method.exceptions,
						blockStatement
					)
				}

				ooMethods.each { clazz.addMethod(it) }
			}
		}
	}

	private static <T extends Statement> T tweakStatement(T statement) {
		if (statement instanceof ExpressionStatement) {
			statement.expression = tweakExpression(statement.expression)
		} else if (statement instanceof IfStatement) {
			statement.booleanExpression = tweakExpression(statement.booleanExpression) as BooleanExpression
			statement.ifBlock = tweakStatement(statement.ifBlock)
			statement.elseBlock = tweakStatement(statement.elseBlock)
		} else if (statement instanceof SwitchStatement) {
			statement.expression = tweakExpression(statement.expression)
			for (ListIterator<? extends Statement> statements = statement.caseStatements.listIterator(); statements.hasNext(); ) {
				statements.set(tweakStatement(statements.next()))
			}
		} else if (statement instanceof CaseStatement) {
			statement.expression = tweakExpression(statement.expression)
			statement.code = tweakStatement(statement.code)
		} else if (statement instanceof WhileStatement) {
			statement.loopBlock = tweakStatement(statement.loopBlock)
		} else if (statement instanceof ForStatement) {
			statement.loopBlock = tweakStatement(statement.loopBlock)
		} else if (statement instanceof SynchronizedStatement) {
			statement.expression = tweakExpression(statement.expression)
		} else if (statement instanceof ReturnStatement) {
			statement.expression = tweakExpression(statement.expression)
		} else if (statement instanceof TryCatchStatement) {
			statement.tryStatement = tweakStatement(statement.tryStatement)
			for (ListIterator<? extends Statement> statements = statement.catchStatements.listIterator(); statements.hasNext(); ) {
				statements.set(tweakStatement(statements.next()))
			}
		} else if (statement instanceof CatchStatement) {
			statement.code = tweakStatement(statement.code)
		} else if (statement instanceof ThrowStatement) {
			statement.expression = tweakExpression(statement.expression)
		} else if (statement instanceof BlockStatement) {
			for (ListIterator<Statement> statements = statement.statements.listIterator(); statements.hasNext(); ) {
				statements.set(tweakStatement(statements.next()))
			}
		}

		return statement
	}

	private static Expression tweakExpression(Expression expr) {
		if (expr instanceof MethodCallExpression) {
			expr.arguments = tweakExpression(expr.arguments)

			if (expr.objectExpression instanceof VariableExpression) {
				VariableExpression objExpr = expr.objectExpression as VariableExpression
				if (objExpr.type.annotations.any { it.classNode.name == ObjectOriented.name }) {
					ArgumentListExpression argExpr = expr.arguments as ArgumentListExpression
					argExpr.expressions.add(0, objExpr)

					expr = new StaticMethodCallExpression(
						objExpr.type,
						"__oo_injection__" + expr.methodAsString,
						argExpr
					)
				}
			}
		} else if (expr instanceof BooleanExpression) {
			expr = new BooleanExpression(tweakExpression(expr.expression))
		} else if (expr instanceof BinaryExpression) {
			expr.leftExpression = tweakExpression(expr.leftExpression)
			expr.rightExpression = tweakExpression(expr.rightExpression)
		} else if (expr instanceof ConstructorCallExpression) {
			expr = new ConstructorCallExpression(expr.type, tweakExpression(expr.arguments))
		} else if (expr instanceof ListExpression) {
			for (ListIterator<? extends Expression> expressions = expr.expressions.listIterator(); expressions.hasNext(); ) {
				expressions.set(tweakExpression(expressions.next()))
			}
		} else if (expr instanceof TupleExpression) {
			for (ListIterator<? extends Expression> expressions = expr.expressions.listIterator(); expressions.hasNext(); ) {
				expressions.set(tweakExpression(expressions.next()))
			}
		}

		return expr
	}
}
