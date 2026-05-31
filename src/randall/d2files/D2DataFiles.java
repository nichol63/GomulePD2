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

public final class D2DataFiles
{
	public static final String DEFAULT_DATA_DIR = "d2111";
	public static final String DATA_DIR_ENV = "GOMULE_DATA_DIR";
	public static final String DATA_DIR_PROPERTY = "gomule.data.dir";

	private D2DataFiles()
	{
	}

	public static String getConfiguredDataDir()
	{
		String lDataDir = clean(System.getenv(DATA_DIR_ENV));
		if (lDataDir != null)
		{
			return lDataDir;
		}

		lDataDir = clean(System.getProperty(DATA_DIR_PROPERTY));
		if (lDataDir != null)
		{
			return lDataDir;
		}

		return DEFAULT_DATA_DIR;
	}

	public static File resolveCaseInsensitive(String pDataDir, String pFileName)
	{
		File lDataDir = new File(pDataDir);
		File lFile = new File(lDataDir, pFileName);
		if (lFile.isFile())
		{
			return lFile;
		}

		File[] lFiles = lDataDir.listFiles();
		if (lFiles != null)
		{
			for (int i = 0; i < lFiles.length; i++)
			{
				if (lFiles[i].isFile() && lFiles[i].getName().equalsIgnoreCase(pFileName))
				{
					return lFiles[i];
				}
			}
		}

		System.err.println("Warning: missing data file: " + lFile.getPath());
		return lFile;
	}

	private static String clean(String pValue)
	{
		if (pValue == null)
		{
			return null;
		}

		pValue = pValue.trim();
		if (pValue.length() == 0)
		{
			return null;
		}

		return pValue;
	}
}
