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

import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.SyntaxException

class ReportError {
	private final SourceUnit source
	private final String message

	ReportError(SourceUnit source, String message) {
		this.source = source
		this.message = message
	}

	void atLocation(int lineNumber, int columnNumber) {
		source.errorCollector.addError(
			new SyntaxErrorMessage(
				new SyntaxException(message, lineNumber, columnNumber),
				source
			)
		)
	}
}
