/*
 * CardModule.java
 *
 * @created 2010-07-15
 *
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.galactanet.gametable.data.deck;

import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.data.MapElementID;
import com.galactanet.gametable.data.XMLSerializeConverter;
import com.galactanet.gametable.module.ModuleIF;
import com.galactanet.gametable.module.ModuleSaveIF;
import com.galactanet.gametable.util.UtilityFunctions;
import com.maziade.tools.XMLUtils;

/**
 * Experimental module implementation fragment
 * (noncommital enough?)
 * 
 * @author Eric Maziade
 */
public class CardModule implements ModuleIF, ModuleSaveIF
{
	/**
	 * 
	 */
	public CardModule()
	{
	}
	
	/*
	 * @see com.galactanet.gametable.module.ModuleIF#getModuleName()
	 */
	@Override
	public String getModuleName()
	{
		return CardModule.class.getName();
	}
	
	/*
	 * @see com.galactanet.gametable.module.ModuleSaveIF#loadFromXML(org.w3c.dom.Element, com.galactanet.gametable.data.XMLSerializeConverter)
	 */
	@Override
	public void loadFromXML(Element node, XMLSerializeConverter converter)
	{
		Card.g_cardMap.clear();
		
		for (Element cardEl : XMLUtils.getChildElementsByTagName(node, "card"))
		{
			long saveID = UtilityFunctions.parseLong(cardEl.getAttribute("id"), 0);
			MapElementID id = converter.getMapElementID(saveID);
			if (id != null)
			{
				Card card = new Card();
				card.loadFromXML(cardEl);
				Card.g_cardMap.put(id, card);
			}
		}
	}
	
	/*
	 * @see com.galactanet.gametable.module.ModuleSaveIF#saveToXML(org.w3c.dom.Element)
	 */
	@Override
	public boolean saveToXML(Element node)
	{
		if (Card.g_cardMap.isEmpty())
			return false;
		
		Document doc = node.getOwnerDocument();
		
		for (Entry<MapElementID, Card> entry : Card.g_cardMap.entrySet())
		{
			Element cardEl = doc.createElement("card");
			cardEl.setAttribute("id", String.valueOf(entry.getKey().numeric()));
			entry.getValue().saveToXML(cardEl);
			node.appendChild(cardEl);
		}
		
		return true;
	}
}
