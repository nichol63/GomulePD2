/*******************************************************************************
 * 
 * This file is part of gomule.
 * 
 * gomule is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * gomule is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * gomlue; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 *  
 ******************************************************************************/
package randall.d2files;

import java.io.File;

public final class D2DataLoadCheck
{
	private D2DataLoadCheck()
	{
	}

	public static void main(String[] pArgs)
	{
		String lDataDir = pArgs.length > 0 ? pArgs[0] : D2DataFiles.getConfiguredDataDir();
		File lDir = new File(lDataDir);
		if (!lDir.isDirectory())
		{
			System.out.println("Skipping PD2 data load check; missing directory: " + lDataDir);
			return;
		}

		D2TxtFile.constructTxtFiles(lDataDir);
		D2TblFile.readAllFiles(lDataDir);

		assertRows("ItemStatCost", D2TxtFile.ITEM_STAT_COST);
		assertRows("Weapons", D2TxtFile.WEAPONS);
		assertRows("Armor", D2TxtFile.ARMOR);
		assertRows("Misc", D2TxtFile.MISC);
		assertRows("UniqueItems", D2TxtFile.UNIQUES);
		assertRows("SetItems", D2TxtFile.SETITEMS);
		assertRows("Runes", D2TxtFile.RUNES);
		assertCount("patchstring.tbl", D2TblFile.getPatchStringCount());
		assertCount("expansionstring.tbl", D2TblFile.getExpansionStringCount());
		assertCount("string.tbl", D2TblFile.getStringCount());

		System.out.println("Data load check passed: " + lDataDir);
	}

	private static void assertRows(String pName, D2TxtFile pFile)
	{
		int lRows = pFile.getRowSize();
		if (lRows <= 0)
		{
			throw new IllegalStateException(pName + " did not load any rows");
		}
		System.out.println(pName + " rows: " + lRows);
	}

	private static void assertCount(String pName, int pCount)
	{
		if (pCount <= 0)
		{
			throw new IllegalStateException(pName + " did not load any strings");
		}
		System.out.println(pName + " strings: " + pCount);
	}
}
