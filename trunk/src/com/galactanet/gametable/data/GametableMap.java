/*
 * GameTableMap.java
 * 
 * @created 2005-09-05
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
package com.galactanet.gametable.data;

import java.awt.Point;
import java.util.*;
import java.util.Map.Entry;

import com.galactanet.gametable.data.deck.Card;
import com.galactanet.gametable.ui.LineSegment;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * This class stores the data related to a gametable map. This includes line, pogs, and underlays
 * 
 * @author sephalon
 * 
 * #GT-AUDIT GametableMap
 * 
 * @revise listeners and triggers for pogs, selection, lines
 */
public class GametableMap
{
	/**
	 * Whether this is the public of private version of the map
	 */
	private final boolean				m_publicMap;

	/**
	 * Lines drawn on the map @revise should lines be an external graphical object?
	 */
	private final List<LineSegment>	m_lines;
	
	/**
	 * Unmodifiable version of m_lines 
	 */
	private final List<LineSegment>	m_linesUnmodifiable;

	/**
	 * A copy of the pog list, kept in order, used by ActivePogsPanel. Excludes underlay pogs.
	 * The order is simply the one chosen by the user to show on ActivePogsPanel.
	 * 
	 * @revise this is redundant data.  If ActivePogsPanel requires such a list for its display purposes, it should not be stored in the map.
	 */
	private SortedSet<Pog>		m_orderedPogs		= new TreeSet<Pog>();

	/**
	 * List of pogs or all types to display on the map
	 * @revise for layers
	 */
	private List<Pog>					m_pogs					= new ArrayList<Pog>();

	/**
	 * Lists the currently selected pogs
	 */
	private final List<Pog>						m_selectedPogs;
	
	/**
	 * Unmodifiable version of the selected pogs to be returned to callers
	 */
	private final List<Pog>						m_selectedPogsUnmodifiable;

	/**
	 * Current scroll coordinates, relative to scroll origin
	 */
	private Point	m_scrollPos = new Point();

	// @revise #{@link javax.swing.undo.UndoableEdit}

	/**
	 * Constructor
	 * 
	 * @param publicMap true for public map (shared with other users). false for private map.
	 */
	public GametableMap(boolean publicMap)
	{
		m_publicMap = publicMap;
		m_selectedPogs = new ArrayList<Pog>();
		m_selectedPogsUnmodifiable = Collections.unmodifiableList(m_selectedPogs);
		
		m_lines = new ArrayList<LineSegment>();
		m_linesUnmodifiable = Collections.unmodifiableList(m_lines);
	}

	/**
	 * Adds a line segment to the map
	 * @param ls line segment to add
	 */
	public void addLine(LineSegment ls)
	{
		m_lines.add(ls);
		// @revise whichever process is building an undo stack should combine consecutive line segments from the same user 
	}

	/**
	 * Adds a pog to the map
	 * @param pog
	 */
	public void addPog(Pog pog)
	{
		m_pogs.add(pog);
		
		if (!pog.isUnderlay())
		{
			m_orderedPogs.add(pog);
		}
	}

	/**
	 * Adds a pog to the ordered list used by the ActivePogs panel @revise this should not be here.
	 * @deprecated
	 * @param pog
	 */
	public void addOrderedPog(final Pog pog)
	{
		m_orderedPogs.add(pog);
	}

	/**
	 * Adds a pog to the selected pog list
	 * @param pog Pog to add to selection
	 */
	public void selectPog(Pog pog)
	{
		m_selectedPogs.add(pog);
		pog.setSelected(true);
		// @revise trigger listeners 
	}

	/**
	 * Add multiple pogs to the selection
	 * @param pogs List of pogs to add to the selection
	 */
	public void selectPogs(final List<Pog> pogs)
	{
		m_selectedPogs.addAll(pogs);
		
		for (Pog pog : pogs)
			pog.setSelected(true);
		
		// @revise trigger listeners
	}

	/**
	 * Remove a pog from the selection
	 * @param pog Pog to remove
	 */
	public void unselectPog(final Pog pog)
	{
		m_selectedPogs.remove(pog);
		pog.setSelected(false);
		
		// @revise trigger listeners
	}
	
	/**
	 * Remove all pogs from selection 
	 */
	public void unselectAllPogs()
	{
		for (Pog pog : m_selectedPogs)
			pog.setSelected(false);

		m_selectedPogs.clear();
		
		// @revise trigger listeners
	}

	/**
	 * Remove all lines from the map
	 */
	public void clearLines()
	{
		m_lines.clear();
		// @revise trigger listeners
	}

	/**
	 * Remove all pogs from the map
	 */
	public void clearPogs()
	{
		m_pogs.clear();
		m_orderedPogs.clear();
		// @revise trigger listeners
	}
	
	/**
	 * Get unmodifiable list of lines contained within GameTableMap
	 * @return list of LineSegment (never null)
	 */
	public List<LineSegment> getLines() 
	{
		return m_linesUnmodifiable;
	}

	
	
	
	
	
	public int getNumPogs()
	{
		return m_pogs.size();
	}

	public SortedSet<Pog> getOrderedPogs()
	{
		return Collections.unmodifiableSortedSet(m_orderedPogs);
	}

	public Pog getPog(final int idx)
	{
		return m_pogs.get(idx);
	}

	public Pog getPogAt(final Point modelPosition)
	{
		if (modelPosition == null)
		{
			return null;
		}

		Pog pogHit = null;
		Pog envHit = null;
		Pog overlayHit = null;
		Pog underlayHit = null;

		for (int i = 0; i < getNumPogs(); i++)
		{
			final Pog pog = getPog(i);

			if (pog.testHit(modelPosition))
			{
				// they clicked this pog
				switch (pog.getLayer())
				{
				case UNDERLAY:
					underlayHit = pog;
					break;
				case OVERLAY:
					overlayHit = pog;
					break;
				case ENVIRONMENT:
					envHit = pog;
					break;
				case POG:
					pogHit = pog;
					break;
				}
			}
		}

		// pogs take priority over underlays
		if (pogHit != null)
		{
			return pogHit;
		}

		if (envHit != null)
			return envHit;
		if (overlayHit != null)
			return overlayHit;

		return underlayHit;
	}

	public Pog getPogByID(final int id)
	{
		for (int i = 0, size = getNumPogs(); i < size; ++i)
		{
			final Pog pog = getPog(i);
			if (pog.getId() == id)
			{
				return pog;
			}
		}

		return null;
	}

	public Pog getPogNamed(final String pogName)
	{
		final List<Pog> pogs = getPogsNamed(pogName);
		if (pogs.isEmpty())
		{
			return null;
		}

		return pogs.get(0);
	}

	public List<Pog> getPogs()
	{
		return Collections.unmodifiableList(m_pogs); // @comment {themaze75} The returned unmodifiableList remains in sync
																									// with the original list - we can retain only one instance here
																									// instead of creating a new one each call
	}

	public List<Pog> getPogsNamed(final String pogName)
	{
		final String normalizedName = UtilityFunctions.normalizeName(pogName);
		final List<Pog> retVal = new ArrayList<Pog>();
		for (int i = 0, size = getNumPogs(); i < size; ++i)
		{
			final Pog pog = getPog(i);
			if (UtilityFunctions.normalizeName(pog.getText()).equals(normalizedName))
			{
				retVal.add(pog);
			}
		}

		return retVal;
	}

	public int getScrollX()
	{
		return m_scrollPos.x;
	}

	public int getScrollY()
	{
		return m_scrollPos.y;
	}
	
	/**
	 * Gets selected pogs list
	 * @return The list of currently selected pogs (unmodifiable).  Never null.
	 */
	public List<Pog> getSelectedPogs()
	{
		return m_selectedPogsUnmodifiable;
	}
	
	/**
	 * Checks if this is a private or public map
	 * @return
	 */
	public boolean isPublicMap()
	{
		return m_publicMap;
	}

	public void removeCardPogsForCards(final Card discards[])
	{
		final List<Pog> removeList = new ArrayList<Pog>();

		for (int i = 0; i < m_pogs.size(); i++)
		{
			final Pog pog = m_pogs.get(i);
			if (pog.isCardPog())
			{
				final Card pogCard = pog.getCard();
				// this is a card pog. Is it oue of the discards?
				for (int j = 0; j < discards.length; j++)
				{
					if (pogCard.equals(discards[j]))
					{
						// it's the pog for this card
						removeList.add(pog);
					}
				}
			}
		}

		// remove any offending pogs
		if (removeList.size() > 0)
		{
			for (int i = 0; i < removeList.size(); i++)
			{
				removePog(removeList.get(i));
			}
		}
	}

	public void removeLine(final LineSegment ls)
	{
		m_lines.remove(ls);
	}

	public void removeOrderedPog(final Pog pog)
	{
		m_orderedPogs.remove(pog);
	}

	public void removePog(final Pog pog)
	{
		removePog(pog, false);
	}

	public void removePog(final Pog pog, final boolean selected)
	{
		if (selected)
			unselectPog(pog);
		m_pogs.remove(pog);
		m_orderedPogs.remove(pog);
	}

	public void reorderPogs(final Map<Integer, Long> changes)
	{
		if (changes == null)
		{
			return;
		}

		for (Entry<Integer, Long> entry : changes.entrySet())
		{
			setSortOrder(entry.getKey(), entry.getValue());
		}
	}

	public void setScroll(final int x, final int y)
	{
		m_scrollPos.setLocation(x, y);
	}

	public void setSortOrder(final int id, final long order)
	{
		final Pog pog = getPogByID(id);
		if (pog == null)
		{
			return;
		}

		m_orderedPogs.remove(pog);
		pog.setSortOrder(order);
		m_orderedPogs.add(pog);
	}
}
