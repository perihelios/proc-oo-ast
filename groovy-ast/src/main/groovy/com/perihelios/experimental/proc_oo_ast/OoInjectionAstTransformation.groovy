/*
 Copyright 2017, Perihelios LLC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package com.perihelios.experimental.proc_oo_ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static CopyVariable.copy
import static com.perihelios.experimental.proc_oo_ast.AstUtil.returnVariable

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OoInjectionAstTransformation implements ASTTransformation {
	private static final String PREFIX = "__oo_injection__"
	private static final String COPY_VARIABLE = "__oo_injection"

	@Override
	void visit(ASTNode[] nodes, SourceUnit source) {
		use(AstCategory) {
			source.AST.classes.findAll { it.hasAnnotation(ObjectOriented) }.each { ClassNode type ->
				visitClass(type, source)
			}
		}
	}

	private static void visitClass(ClassNode type, SourceUnit source) {
		use(AstCategory) {
			type << type.proceduralMethods.collect { MethodNode oldMethod ->
				List<String> paramNames = oldMethod.parameters*.name
				MethodBuilder newMethod = oldMethod.buildCopyNamed(PREFIX + oldMethod.name)

				if (oldMethod.proceduralParameter.hasAnnotation(OoCopy)) {
					if (!type.hasCopyConstructor()) {
						AnnotationNode copyAnnotation = oldMethod.proceduralParameter.annotation(OoCopy)

						source.reportError(
							"Found @$OoCopy.simpleName on procedure parameter in class $type.name, " +
								"which has no copy constructor"
						).atLocation(copyAnnotation.lineNumber, copyAnnotation.columnNumber)
					}

					newMethod << copy(oldMethod.proceduralParameter.name)
						.toNewVariable(COPY_VARIABLE).ofType(type).usingCopyConstructor()
					paramNames[0] = COPY_VARIABLE
					newMethod << oldMethod.callStatic(paramNames)

					if (oldMethod.voidMethod) {
						newMethod.useReturnType(type)
						newMethod << returnVariable(COPY_VARIABLE)
					}
				} else {
					newMethod << oldMethod.callStatic(paramNames)
				}

				return newMethod
			}
		}
	}
}
