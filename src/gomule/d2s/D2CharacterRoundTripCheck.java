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
package gomule.d2s;

import gomule.item.D2Item;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import randall.d2files.D2DataFiles;
import randall.d2files.D2TblFile;
import randall.d2files.D2TxtFile;

public final class D2CharacterRoundTripCheck
{
	private D2CharacterRoundTripCheck()
	{
	}

	public static void main(String[] pArgs) throws Exception
	{
		String lDataDir = pArgs.length > 0 ? pArgs[0] : D2DataFiles.getConfiguredDataDir();
		String lSampleSave = pArgs.length > 1 ? pArgs[1] : "data/sample/RyHCs.d2s";

		File lDataDirFile = new File(lDataDir);
		if (!lDataDirFile.isDirectory())
		{
			System.out.println("Skipping PD2 character round-trip check; missing directory: " + lDataDir);
			return;
		}

		Path lSampleSavePath = Paths.get(lSampleSave);
		if (!Files.isRegularFile(lSampleSavePath))
		{
			System.out.println("Skipping PD2 character round-trip check; missing sample save: " + lSampleSave);
			return;
		}

		System.setProperty(D2DataFiles.DATA_DIR_PROPERTY, lDataDir);
		D2TxtFile.constructTxtFiles(lDataDir);
		D2TblFile.readAllFiles(lDataDir);

		Path lTempDir = Files.createTempDirectory("gomule-pd2-character-");
		Path lTempSave = lTempDir.resolve(lSampleSavePath.getFileName());
		try
		{
			Files.copy(lSampleSavePath, lTempSave, StandardCopyOption.REPLACE_EXISTING);
			runCheck(lTempSave);
		}
		finally
		{
			deleteIfExists(lTempSave);
			deleteIfExists(lTempDir);
		}
	}

	private static void runCheck(Path pTempSave) throws Exception
	{
		byte[] lOriginalSaveBytes = Files.readAllBytes(pTempSave);
		String lOriginalChecksum = checksum(lOriginalSaveBytes);

		D2Character lCharacter = new D2Character(pTempSave.toString());
		ArrayList lOriginalItemBytes = itemBytes(lCharacter);

		System.out.println("Parsed character: " + lCharacter.getCharName() + " | " + lCharacter.getCharClass() + " | level " + lCharacter.getCharLevel());
		System.out.println("Parsed items: " + lCharacter.getNrItems());
		System.out.println();
		printItemTable(lCharacter);

		lCharacter.saveWithoutBackupForRoundTripCheck();
		byte[] lSavedBytes = Files.readAllBytes(pTempSave);
		String lSavedChecksum = checksum(lSavedBytes);

		if (!lOriginalChecksum.equals(lSavedChecksum))
		{
			throw new IllegalStateException("File checksum changed after unmodified save: " + lOriginalChecksum + " -> " + lSavedChecksum);
		}

		D2Character lReloadedCharacter = new D2Character(pTempSave.toString());
		ArrayList lReloadedItemBytes = itemBytes(lReloadedCharacter);
		assertItemBytesStable(lOriginalItemBytes, lReloadedItemBytes);

		System.out.println();
		System.out.println("Round-trip check passed: item bytes stable; checksum " + lSavedChecksum);
	}

	private static ArrayList itemBytes(D2Character pCharacter)
	{
		ArrayList lBytes = new ArrayList();
		ArrayList lItems = pCharacter.getItemList();
		for (int i = 0; i < lItems.size(); i++)
		{
			lBytes.add(((byte[]) ((D2Item) lItems.get(i)).get_bytes().clone()));
		}
		return lBytes;
	}

	private static void assertItemBytesStable(ArrayList pOriginalItemBytes, ArrayList pReloadedItemBytes)
	{
		if (pOriginalItemBytes.size() != pReloadedItemBytes.size())
		{
			throw new IllegalStateException("Item count changed after round-trip: " + pOriginalItemBytes.size() + " -> " + pReloadedItemBytes.size());
		}

		for (int i = 0; i < pOriginalItemBytes.size(); i++)
		{
			byte[] lOriginal = (byte[]) pOriginalItemBytes.get(i);
			byte[] lReloaded = (byte[]) pReloadedItemBytes.get(i);
			if (!Arrays.equals(lOriginal, lReloaded))
			{
				throw new IllegalStateException("Item bytes changed after round-trip at item #" + (i + 1));
			}
		}
	}

	private static void printItemTable(D2Character pCharacter)
	{
		ArrayList lItems = pCharacter.getItemList();
		System.out.println("| # | Name | Type | Quality | Key stats |");
		System.out.println("|---|---|---|---|---|");
		for (int i = 0; i < lItems.size(); i++)
		{
			D2Item lItem = (D2Item) lItems.get(i);
			System.out.println("| " + (i + 1) + " | " + cell(lItem.getItemName()) + " | " + cell(type(lItem)) + " | " + cell(quality(lItem)) + " | " + cell(stats(lItem)) + " |");
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

	private static String checksum(byte[] pBytes)
	{
		if (pBytes.length < 16)
		{
			throw new IllegalStateException("Save is too short to contain a checksum");
		}
		long lChecksum = ((long) pBytes[12] & 0xff)
			| (((long) pBytes[13] & 0xff) << 8)
			| (((long) pBytes[14] & 0xff) << 16)
			| (((long) pBytes[15] & 0xff) << 24);
		return String.format("0x%08x", Long.valueOf(lChecksum));
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

	private static void deleteIfExists(Path pPath)
	{
		try
		{
			Files.deleteIfExists(pPath);
		}
		catch (IOException pEx)
		{
			System.err.println("Warning: could not delete temp path " + pPath + ": " + pEx.getMessage());
		}
	}
}
