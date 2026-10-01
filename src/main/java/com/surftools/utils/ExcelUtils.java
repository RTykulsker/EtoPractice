/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/
package com.surftools.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelUtils {
	private static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

	public static String getStringValue(Row row, int columnIndex) {
		Cell cell = row.getCell(columnIndex);
		if (cell == null) {
			return "";
		}

		switch (cell.getCellType()) {
		case BLANK:
			return "";

		case BOOLEAN:
			return Boolean.toString(cell.getBooleanCellValue());

		case FORMULA: {
			CellType cachedCellType = cell.getCachedFormulaResultType();
			if (cachedCellType == CellType.STRING) {
				return cell.getStringCellValue().strip();
			} else if (cachedCellType == CellType.NUMERIC) {
				return Double.toString(cell.getNumericCellValue());
			} else if (cachedCellType == CellType.BOOLEAN) {
				return Boolean.toString(cell.getBooleanCellValue());
			}
		}

		case NUMERIC:
			return Double.toString(cell.getNumericCellValue());

		case STRING:
			return cell.getStringCellValue().strip();

		default:
			logger.error("Unsupported type: " + cell.getCellType().name() + " on row: " + row.getRowNum() + ", col: "
					+ columnIndex);
			return "";
		}
	}
}
