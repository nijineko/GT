/*
 * PNGDecoder.java
 * 
 * @created 2010-07-12
 * 
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.galactanet.gametable.util;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.sun.imageio.plugins.png.PNGMetadata;

/**
 * http://forums.sun.com/thread.jspa?threadID=5346727&messageID=10545201#10545201
 * 
 * I ended up doing this a somewhat different way to avoid iterating over all of the pixels in the resultant BufferedImage. 
 * What did was write a very spartan PNG decoder, which only decodes the PNG chunks. 
 * When I try to read a PNG, I send the stream to this PNG decoder, which reads the IHDR chunk, and then continues on until 
 * we find a tRNS or IDAT chunk. (tRNS, if present, must occur before the first IDAT chunk, so if we come to an IDAT chunk 
 * without finding a tRNS chunk, we know there isn't one.) We pull the bit depth and color type from the IHDR chunk, and if 
 * we have an 8-bit PNG of color type 2 which has a tRNS chunk, then we know we need to use Toolkit.createImage() to read 
 * the BufferedImage.
 *
 * 
 * @author uckelman
 */
class PNGUtils
{
	/**
	 * PNG Chunkk
	 */
	private static class Chunk
	{
		public final byte[]	data;
		public final int		type;

		private Chunk(int ctype, byte[] cdata)
		{
			this.type = ctype;
			this.data = cdata;
		}
	}
	// ancillary chunks
//	private static final int	bKGD	= 0x624b4744;
//	private static final int	cHRM	= 0x6348524d;
//	private static final int	gAMA	= 0x67414d41;

//	private static final int	hIST	= 0x68495354;
//	private static final int	iCCP	= 0x69434350;
	private static final int	IDAT	= 0x49444154;
//	private static final int	IEND	= 0x49454e44;
	// critical chunks
	private static final int	IHDR	= 0x49484452;
//	private static final int	iTXt	= 0x69545874;
//	private static final int	pHYs	= 0x70485973;
//	private static final int	PLTE	= 0x504c5445;
//	private static final int	sBIT	= 0x73424954;
//	private static final int	sPLT	= 0x73504c54;
//	private static final int	sRGB	= 0x73524742;
//	private static final int	tEXt	= 0x74455874;
//	private static final int	tIME	= 0x74494d45;

	private static final int	tRNS	= 0x74524e53;

//	private static final int	zTXt	= 0x7a545874;

	/**
	 * Decode PNG chunk
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static Chunk decodeChunk(DataInputStream in) throws IOException
	{
		// 5.3
		final int length = in.readInt();
		if (length < 0)
			throw new IOException("chunk length out of range");

		final byte[] type = new byte[4];
		in.readFully(type);

		final byte[] data = new byte[length];
		in.readFully(data);

		// final long crc = in.readInt() & 0x0000000ffffffffL;

		final int t = (type[0] << 24) | (type[1] << 16) | (type[2] << 8) | type[3];
		return new Chunk(t, data);
	}

	/**
	 * Decode Signature
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static boolean decodeSignature(DataInputStream in) throws IOException
	{
		// 5.2
		return in.readLong() == 0x89504e470d0a1a0aL;
	}

	/**
	 * Utility method to detect if an 8 BIT RGB PNG file has an alpha mask 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static boolean isMasked8BitRGBPNG(InputStream in) throws IOException
	{
		final DataInputStream din = new DataInputStream(in);

		// Bail immediately if this stream is not a PNG.
		if (!PNGUtils.decodeSignature(din))
			return false;

		// IHDR is required to be first, and tRNS is required to appear before
		// the first IDAT chunk; therefore, if we find an IDAT we're done.
		PNGUtils.Chunk ch;

		ch = PNGUtils.decodeChunk(din);
		if (ch.type != PNGUtils.IHDR)
			return false;
		if (ch.data[8] != 8 || ch.data[9] != 2)
			return false;

		while (true)
		{
			ch = PNGUtils.decodeChunk(din);

			// Numbers here refer to sections in the PNG standard, found
			// at http://www.w3.org/TR/PNG/
			switch (ch.type)
			{
			case PNGUtils.tRNS: // 11.3.2
				return true;
			case PNGUtils.IDAT: // 12.2.4
				return false;
			default:
			}
		}
	}
	
	/**
	 * Check if image stream is PNG
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	public static boolean isPNG(ImageInputStream stream) throws IOException
	{
		Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);

		while (iter.hasNext())
		{
			ImageReader reader = iter.next();
			if (reader.getFormatName().equalsIgnoreCase("png"))
				return true;			
		}
		
		return false;
	}

	
	/**
	 * Loads a PNG image.  Fixes alpha layer if necessary
	 * @param stream
	 * @return
	 * @throws IOException
	 * @author Maxideon  
	 */
	public static BufferedImage loadPNGImage(ImageInputStream stream) throws IOException
	{		
		ImageReader r = ImageIO.getImageReadersByFormatName("png").next();
		r.setInput(stream,true,false);
		PNGMetadata metadata = (PNGMetadata) r.getImageMetadata(0);
		BufferedImage i = r.read(0);

		if(!i.getColorModel().hasAlpha() && metadata.tRNS_present) 
		{
			int alphaPix = (metadata.tRNS_red<<16)|(metadata.tRNS_green<<8)|(metadata.tRNS_blue);
			BufferedImage tmp = new BufferedImage(i.getWidth(),i.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for(int x = 0; x < i.getWidth(); x++) 
			{
        for(int y = 0; y < i.getHeight(); y++) 
        {
            int rgb = i.getRGB(x, y);
            rgb = (rgb&0xFFFFFF)==alphaPix?alphaPix:rgb;
            tmp.setRGB(x, y, rgb);
        }
			}
			
			i = tmp;
		}
		
		return i;
	}
}
