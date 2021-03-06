/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.ast.expressions;

import org.eclipse.dltk.utils.CorePrinter;


public class FloatNumericLiteral extends Literal {

	private double doubleValue;
	

	public FloatNumericLiteral(int start, int end, double value) {
		super(start, end);
		this.doubleValue = value;
	}
	
	public FloatNumericLiteral(int start, int end, String value) {
		super(start, end);
		this.doubleValue = Double.parseDouble(value);
	}
	
	public double getDoubleValue () {
		return doubleValue;
	}

	
	@Override
	public String getValue() {
		return String.valueOf(doubleValue);
	}

	/**
	 * Return kind.
	 */
	@Override
	public int getKind() {
		return NUMBER_LITERAL;
	}

	/**
	 * Testing purposes only. Used to print number.
	 */
	@Override
	public void printNode(CorePrinter output) {
		output.formatPrintLn(this.getValue());
	}

}
