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

import gomule.gui.*;
import gomule.item.*;
import gomule.util.*;

import java.io.*;
import java.util.*;

/**
 * Reader for the Project Diablo 2 shared-stash file format (the *.stash files,
 * e.g. pd2_shared.stash / pd2_hc_shared.stash). This is a PD2-custom container,
 * NOT the PlugY .sss format.
 *
 * Layout (little-endian):
 *   - 16 byte header: magic 0xBB55BB55 (u32) + version (u32) + fileSize (u32) + checksum (u32)
 *   - opaque metadata preamble (starts with ASCII "st"); not decoded here
 *   - item list: ASCII "JM" + u16 itemCount, then itemCount items each starting
 *     with "JM", in the standard Diablo II item format read by {@link D2Item}.
 *
 * READ-ONLY: writing (and therefore muling into a shared stash) is intentionally
 * disabled until M3, where the preamble is preserved verbatim and the size +
 * checksum recomputed.
 */
public class D2SharedStash extends D2ItemListAdapter
{
	public static final long MAGIC = 0xBB55BB55L;
	public static final int HEADER_SIZE = 16;

	private static final String READ_ONLY_MESSAGE =
		"PD2 shared stash is read-only in GoMulePD2 (writing/muling is planned for M3)";

	private ArrayList iItems;
	private D2BitReader iBR;
	private boolean iHC;
	private boolean iSC;

	private long iVersion;
	private long iStoredFileSize;
	private long iStoredChecksum;
	private int iItemListStart;
	private int iDeclaredItemCount;

	private final int iCharLvl = 75; // default char lvl for property display

	private File lFile;

	public D2SharedStash(String pFileName) throws Exception
	{
		super(pFileName);
		if (iFileName == null)
		{
			throw new Exception("Missing PD2 shared stash file name");
		}
		iItems = new ArrayList();
		lFile = new File(iFileName);

		String lLowerName = lFile.getName().toLowerCase();
		iHC = lLowerName.indexOf("hc") != -1;
		iSC = lLowerName.indexOf("sc") != -1 || !iHC;

		iBR = new D2BitReader(iFileName);
		if (iBR.isNewFile())
		{
			throw new Exception("PD2 shared stash file is empty or missing: " + iFileName);
		}
		if (iBR.get_length() < HEADER_SIZE)
		{
			throw new Exception("PD2 shared stash file is too small: " + iFileName);
		}

		iBR.set_byte_pos(0);
		long lMagic = iBR.read(32);
		if (lMagic != MAGIC)
		{
			throw new Exception("Not a PD2 shared stash (bad magic 0x"
				+ Long.toHexString(lMagic) + "): " + iFileName);
		}
		iVersion = iBR.read(32);        // byte 4
		iStoredFileSize = iBR.read(32); // byte 8
		iStoredChecksum = iBR.read(32); // byte 12

		if (iStoredFileSize != iBR.get_length())
		{
			System.err.println("Warning: PD2 shared stash declared size " + iStoredFileSize
				+ " != actual file length " + iBR.get_length() + " for " + iFileName);
		}

		readItemList();

		// freshly read from disk; not modified
		setModified(false);
	}

	private void readItemList() throws Exception
	{
		int lListStart = iBR.findNextFlag("JM", HEADER_SIZE);
		if (lListStart < 0)
		{
			throw new Exception("PD2 shared stash item list not found: " + iFileName);
		}
		iItemListStart = lListStart;

		// item count is the u16 immediately after the "JM" list header
		iBR.set_byte_pos(lListStart + 2);
		iDeclaredItemCount = (int) iBR.read(16);

		// parse exactly iDeclaredItemCount items; do NOT trust raw "JM" scanning
		// for the count, since 0x4A4D can occur inside bit-packed item data.
		int lLastItemEnd = lListStart + 4; // skip "JM" + u16 count
		for (int i = 0; i < iDeclaredItemCount; i++)
		{
			int lItemStart = iBR.findNextFlag("JM", lLastItemEnd);
			if (lItemStart < 0)
			{
				throw new Exception("PD2 shared stash: expected " + iDeclaredItemCount
					+ " items but only found " + i + " in " + iFileName);
			}
			D2Item lItem = new D2Item(iFileName, iBR, lItemStart, iCharLvl);
			lLastItemEnd = lItemStart + lItem.getItemLength();
			iItems.add(lItem);
		}
	}

	/**
	 * Magic-byte sniff so dispatch can be robust regardless of file extension.
	 */
	public static boolean hasSharedStashMagic(String pFileName)
	{
		FileInputStream lIn = null;
		try
		{
			File lF = new File(pFileName);
			if (!lF.isFile() || lF.length() < 4)
			{
				return false;
			}
			lIn = new FileInputStream(lF);
			int b0 = lIn.read();
			int b1 = lIn.read();
			int b2 = lIn.read();
			int b3 = lIn.read();
			long lMagicLE = (((long) (b3 & 0xff)) << 24) | ((b2 & 0xff) << 16)
				| ((b1 & 0xff) << 8) | (b0 & 0xff);
			return lMagicLE == MAGIC;
		}
		catch (Exception pEx)
		{
			return false;
		}
		finally
		{
			if (lIn != null)
			{
				try { lIn.close(); } catch (IOException pIgnore) { }
			}
		}
	}

	public static boolean isSharedStashFileName(String pFileName)
	{
		return pFileName != null && pFileName.toLowerCase().endsWith(".stash");
	}

	public long getVersion()
	{
		return iVersion;
	}

	public long getStoredChecksum()
	{
		return iStoredChecksum;
	}

	public int getDeclaredItemCount()
	{
		return iDeclaredItemCount;
	}

	public int getItemListStart()
	{
		return iItemListStart;
	}

	public String getFilename()
	{
		return iFileName;
	}

	public String getFileNameEnd()
	{
		return lFile.getName();
	}

	public boolean isHC()
	{
		return iHC;
	}

	public boolean isSC()
	{
		return iSC;
	}

	public boolean isReadOnly()
	{
		return true;
	}

	public ArrayList getItemList()
	{
		return iItems;
	}

	public int getNrItems()
	{
		return iItems.size();
	}

	public boolean containsItem(D2Item pItem)
	{
		return iItems.contains(pItem);
	}

	public void addItem(D2Item pItem)
	{
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	public void removeItem(D2Item pItem)
	{
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	protected void saveInternal(D2Project pProject)
	{
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE + ": " + iFileName);
	}

	public void fullDump(PrintWriter pWriter)
	{
		pWriter.println(iFileName);
		pWriter.println();
		if (iItems != null)
		{
			for (int i = 0; i < iItems.size(); i++)
			{
				((D2Item) iItems.get(i)).toWriter(pWriter);
			}
		}
		pWriter.println("Finished: " + iFileName);
		pWriter.println();
	}
}
