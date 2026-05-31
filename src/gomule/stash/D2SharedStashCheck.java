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
 * gomule; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 *
 ******************************************************************************/
package gomule.stash;

import gomule.item.D2Item;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import randall.d2files.D2DataFiles;
import randall.d2files.D2TblFile;
import randall.d2files.D2TxtFile;

/**
 * Guarded headless validation for the PD2 shared-stash reader. Run via
 * {@code ant check-stash}. Reads a local sample stash (read-only) and asserts
 * the parsed item count matches the file's declared count, then prints a small
 * parsed-item table. Skips cleanly (exit 0) when the data dir or sample stash
 * is absent so CI stays green without shipping copyrighted data.
 */
public final class D2SharedStashCheck
{
	private D2SharedStashCheck()
	{
	}

	public static void main(String[] pArgs) throws Exception
	{
		String lDataDir = pArgs.length > 0 ? pArgs[0] : D2DataFiles.getConfiguredDataDir();
		String lSampleStash = pArgs.length > 1 ? pArgs[1] : "data/sample/pd2_shared.stash";

		File lDataDirFile = new File(lDataDir);
		if (!lDataDirFile.isDirectory())
		{
			System.out.println("Skipping PD2 shared stash check; missing directory: " + lDataDir);
			return;
		}

		File lStashFile = new File(lSampleStash);
		if (!lStashFile.isFile())
		{
			System.out.println("Skipping PD2 shared stash check; missing sample stash: " + lSampleStash);
			return;
		}

		System.setProperty(D2DataFiles.DATA_DIR_PROPERTY, lDataDir);
		D2TxtFile.constructTxtFiles(lDataDir);
		D2TblFile.readAllFiles(lDataDir);

		// D2SharedStash is read-only, so reading the sample in place is safe.
		D2SharedStash lStash = new D2SharedStash(lSampleStash);

		System.out.println("Parsed " + lSampleStash
			+ " | softcore=" + lStash.isSC() + " hardcore=" + lStash.isHC()
			+ " | version=" + lStash.getVersion()
			+ " | itemListStart=" + lStash.getItemListStart()
			+ " | declaredCount=" + lStash.getDeclaredItemCount()
			+ " | parsedItems=" + lStash.getNrItems());
		System.out.println();
		printItemTable(lStash.getItemList());

		if (lStash.getNrItems() <= 0)
		{
			throw new IllegalStateException("PD2 shared stash parsed no items: " + lSampleStash);
		}
		if (lStash.getNrItems() != lStash.getDeclaredItemCount())
		{
			throw new IllegalStateException("Parsed item count " + lStash.getNrItems()
				+ " != declared count " + lStash.getDeclaredItemCount() + " in " + lSampleStash);
		}

		System.out.println();
		System.out.println("PD2 shared stash check passed: " + lSampleStash
			+ " (" + lStash.getNrItems() + " items)");
	}

	private static void printItemTable(ArrayList pItems)
	{
		int lShow = Math.min(pItems.size(), 15);
		System.out.println("| # | Name | Type | Quality | Key stats |");
		System.out.println("|---|---|---|---|---|");
		for (int i = 0; i < lShow; i++)
		{
			D2Item lItem = (D2Item) pItems.get(i);
			System.out.println("| " + (i + 1) + " | " + cell(lItem.getItemName()) + " | "
				+ cell(type(lItem)) + " | " + cell(quality(lItem)) + " | " + cell(stats(lItem)) + " |");
		}
		if (pItems.size() > lShow)
		{
			System.out.println("| ... | (" + (pItems.size() - lShow) + " more) | | | |");
		}
	}

	private static String type(D2Item pItem)
	{
		if (pItem.isRune())
		{
			return "rune";
		}
		if (pItem.isGem())
		{
			return "gem";
		}
		if (pItem.isCharm())
		{
			return "charm";
		}
		if (pItem.isTypeWeapon())
		{
			return "weapon";
		}
		if (pItem.isTypeArmor())
		{
			return "armor";
		}
		return "misc";
	}

	private static String quality(D2Item pItem)
	{
		String lRarity = "normal";
		if (pItem.isUnique())
		{
			lRarity = "unique";
		}
		else if (pItem.isSet())
		{
			lRarity = "set";
		}
		else if (pItem.isRuneWord())
		{
			lRarity = "runeword";
		}
		else if (pItem.isCrafted())
		{
			lRarity = "crafted";
		}
		else if (pItem.isRare())
		{
			lRarity = "rare";
		}
		else if (pItem.isMagical())
		{
			lRarity = "magic";
		}

		String lTier = pItem.getItemQuality();
		if (lTier == null || lTier.equals("") || lTier.equals("none"))
		{
			return lRarity;
		}
		return lRarity + " " + lTier;
	}

	private static String stats(D2Item pItem)
	{
		String[] lLines = pItem.itemDump(true).replace('\r', '\n').split("\n");
		List lStats = new ArrayList();
		for (int i = 0; i < lLines.length; i++)
		{
			String lLine = lLines[i].trim();
			if (lLine.length() == 0 || skipStatLine(pItem, lLine))
			{
				continue;
			}
			lStats.add(lLine);
			if (lStats.size() == 3)
			{
				break;
			}
		}

		if (lStats.isEmpty())
		{
			return "no displayed stats";
		}
		return join(lStats, "; ");
	}

	private static boolean skipStatLine(D2Item pItem, String pLine)
	{
		return pLine.equals(pItem.getItemName())
			|| pLine.equals(pItem.getBaseItemName())
			|| pLine.equals("null")
			|| pLine.startsWith("Required ")
			|| pLine.startsWith("Fingerprint:")
			|| pLine.startsWith("GUID:")
			|| pLine.startsWith("Item Level:")
			|| pLine.startsWith("Version:");
	}

	private static String join(List pValues, String pSeparator)
	{
		StringBuffer lBuffer = new StringBuffer();
		for (int i = 0; i < pValues.size(); i++)
		{
			if (i > 0)
			{
				lBuffer.append(pSeparator);
			}
			lBuffer.append(pValues.get(i));
		}
		return lBuffer.toString();
	}

	private static String cell(String pValue)
	{
		if (pValue == null || pValue.trim().length() == 0)
		{
			return "-";
		}
		String lValue = stripColorCodes(pValue).replace('|', '/').replace('\r', ' ').replace('\n', ' ').trim();
		while (lValue.indexOf("  ") != -1)
		{
			lValue = lValue.replace("  ", " ");
		}
		if (lValue.length() > 120)
		{
			return lValue.substring(0, 117) + "...";
		}
		return lValue;
	}

	private static String stripColorCodes(String pValue)
	{
		StringBuffer lClean = new StringBuffer();
		for (int i = 0; i < pValue.length(); i++)
		{
			if (pValue.charAt(i) == 0xff && i + 2 < pValue.length() && Character.toLowerCase(pValue.charAt(i + 1)) == 'c')
			{
				i += 2;
			}
			else if (pValue.charAt(i) == 0xc3 && i + 3 < pValue.length() && pValue.charAt(i + 1) == 0xbf && Character.toLowerCase(pValue.charAt(i + 2)) == 'c')
			{
				i += 3;
			}
			else
			{
				lClean.append(pValue.charAt(i));
			}
		}
		return lClean.toString();
	}
}
