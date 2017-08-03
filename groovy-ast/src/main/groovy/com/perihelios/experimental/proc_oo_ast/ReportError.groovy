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
