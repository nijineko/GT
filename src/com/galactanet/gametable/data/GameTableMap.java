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
 *         #GT-AUDIT GametableMap
 * 
 * @revise listeners and triggers for pogs, selection, lines
 */
public class GameTableMap
{
	/**
	 * Lines drawn on the map @revise should lines be an external graphical object?
	 */
	private final List<LineSegment>	m_lines;

	/**
	 * Unmodifiable version of m_lines
	 */
	private final List<LineSegment>	m_linesUnmodifiable;

	/**
	 * A copy of the pog list, kept in order, used by ActivePogsPanel. Excludes underlay pogs. The order is simply the one
	 * chosen by the user to show on ActivePogsPanel.
	 * 
	 * @revise this is redundant data. If ActivePogsPanel requires such a list for its display purposes, it should not be
	 *         stored in the map.
	 */
	private SortedSet<Pog>					m_orderedPogs	= new TreeSet<Pog>();

	/**
	 * List of pogs or all types to display on the map
	 * 
	 * @revise for layers
	 */
	private final List<Pog>					m_pogs;

	/**
	 * Unmodifiable list of pogs
	 */
	private final List<Pog>					m_pogsUnmodifiable;

	/**
	 * Whether this is the public of private version of the map
	 */
	private final boolean						m_publicMap;

	/**
	 * Current scroll coordinates, relative to scroll origin
	 */
	private Point										m_scrollPos		= new Point();

	/**
	 * Lists the currently selected pogs
	 */
	private final List<Pog>					m_selectedPogs;

	/**
	 * Unmodifiable version of the selected pogs to be returned to callers
	 */
	private final List<Pog>					m_selectedPogsUnmodifiable;

	// @revise #{@link javax.swing.undo.UndoableEdit}

	/**
	 * Constructor
	 * 
	 * @param publicMap true for public map (shared with other users). false for private map.
	 */
	public GameTableMap(boolean publicMap)
	{
		m_publicMap = publicMap;
		m_selectedPogs = new ArrayList<Pog>();
		m_selectedPogsUnmodifiable = Collections.unmodifiableList(m_selectedPogs);

		m_lines = new ArrayList<LineSegment>();
		m_linesUnmodifiable = Collections.unmodifiableList(m_lines);

		m_pogs = new ArrayList<Pog>();
		m_pogsUnmodifiable = Collections.unmodifiableList(m_pogs);
	}

	/**
	 * Adds a line segment to the map
	 * 
	 * @param ls line segment to add
	 */
	public void addLineSegment(LineSegment ls)
	{
		m_lines.add(ls);
		// @revise whichever process is building an undo stack should combine consecutive line segments from the same user
	}

	/**
	 * Adds a pog to the ordered list used by the ActivePogs panel @revise this should not be here.
	 * 
	 * @deprecated
	 * @param pog
	 */
	@Deprecated
	public void addOrderedPog(final Pog pog)
	{
		m_orderedPogs.add(pog);
	}

	/**
	 * Adds a pog to the map
	 * 
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
	 * 
	 * @return list of LineSegment (never null)
	 */
	public List<LineSegment> getLines()
	{
		return m_linesUnmodifiable;
	}

	/**
	 * Returns a list of pogs in some order
	 * 
	 * @revise should be within the UI that handles these things - nothing to do with map data
	 * @return
	 */
	public SortedSet<Pog> getOrderedPogs()
	{
		return Collections.unmodifiableSortedSet(m_orderedPogs);
	}

	/**
	 * Get Pog matching given position on the map
	 * 
	 * @param modelPosition Coordinates to test for
	 * @return Matching Pog or none
	 */
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

		for (Pog pog : m_pogs)
		{
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

		// Pogs is top layer
		if (pogHit != null)
			return pogHit;

		// Environment is 2nd layer
		if (envHit != null)
			return envHit;

		// Overlay is 3rd layer
		if (overlayHit != null)
			return overlayHit;

		// Underlay is fourth layer
		return underlayHit;
	}

	/**
	 * Get Pog by PogID
	 * 
	 * @param id ID of the pog we are looking for
	 * @return Matching Pog or null
	 */
	public Pog getPogByID(final int id)
	{
		for (Pog pog : m_pogs)
		{
			if (pog.getId() == id)
				return pog;
		}

		return null;
	}

	/**
	 * Get pog by name
	 * 
	 * @param pogName name of the pog we are looking for
	 * @return Pog or null
	 */
	public Pog getPogByName(final String pogName)
	{
		return getPogsByName(pogName, null);
	}

	/**
	 * Get list of pogs
	 * 
	 * @return unmodifiable list of pogs
	 */
	public List<Pog> getPogs()
	{
		return m_pogsUnmodifiable;
	}

	/**
	 * Find pogs matching a given name
	 * 
	 * @param pogName Name of the pog we are looking for
	 * @return List of matching pogs (never null)
	 */
	public List<Pog> getPogsByName(String pogName)
	{
		List<Pog> retVal = new ArrayList<Pog>();
		getPogsByName(pogName, retVal);

		return retVal;
	}

	/**
	 * Gets the X coordinate of the scroll position
	 * 
	 * @revise move to VIEW
	 * @return
	 */
	public int getScrollX()
	{
		return m_scrollPos.x;
	}

	/**
	 * Gets the Y coordinate of the scroll position
	 * 
	 * @revise move to VIEW
	 * @return
	 */
	public int getScrollY()
	{
		return m_scrollPos.y;
	}

	/**
	 * Gets selected pogs list
	 * 
	 * @revise move to VIEW?
	 * @return The list of currently selected pogs (unmodifiable). Never null.
	 * 
	 */
	public List<Pog> getSelectedPogs()
	{
		return m_selectedPogsUnmodifiable;
	}

	/**
	 * Checks if this is a private or public map
	 * 
	 * @revise Relevant?
	 * @return
	 */
	public boolean isPublicMap()
	{
		return m_publicMap;
	}

	/**
	 * Remove pogs linked to cards
	 * 
	 * @revise move to Card Module
	 * @param discards
	 */
	public void removeCardPogsForCards(final Card discards[])
	{
		final List<Pog> removeList = new ArrayList<Pog>();

		for (Pog pog : m_pogs)
		{
			if (pog.isCardPog())
			{
				final Card pogCard = pog.getCard();

				// this is a card pog. Is it out of the discards?
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
		removePogs(removeList);
	}

	/**
	 * Remove line segment from map
	 * 
	 * @param ls line segment to remove
	 */
	public void removeLineSegment(final LineSegment ls)
	{
		m_lines.remove(ls);
	}

	/**
	 * Remove ordered pog
	 * 
	 * @revise move to ActivePogPanel
	 * @param pog
	 */
	public void removeOrderedPog(final Pog pog)
	{
		m_orderedPogs.remove(pog);
	}

	/**
	 * Remove a given pog from the map
	 * 
	 * @param pog pog to remove
	 */
	public void removePog(final Pog pog)
	{
		if (pog.isSelected())
			unselectPog(pog);

		m_pogs.remove(pog);
		m_orderedPogs.remove(pog);

		// @revise trigger listeners
	}

	/**
	 * Remove multiple pogs from the map
	 * 
	 * @param pogs list of pogs to remove
	 */
	public void removePogs(List<Pog> pogs)
	{
		for (Pog pog : pogs)
			removePog(pog);

		// @revise trigger listeners
	}

	/**
	 * TODO @revise I don't get this.
	 * 
	 * @param changes
	 */
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

	/**
	 * Adds a pog to the selected pog list
	 * 
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
	 * 
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
	 * Set the scroll position
	 * 
	 * @revise move to VIEW
	 * @param x x coordinates of the scroll position
	 * @param y y coordinates of the scroll position
	 */
	public void setScrollPosition(final int x, final int y)
	{
		m_scrollPos.setLocation(x, y);
	}

	/**
	 * Set the sort order for a given pog
	 * 
	 * @param pogID pog ID to look for
	 * @param sortOrder sort order number
	 * @revise move to ActivePogPanel
	 */
	public void setSortOrder(int pogID, long sortOrder)
	{
		final Pog pog = getPogByID(pogID);
		if (pog == null)
			return;

		// @revise won't the OrderedSet resort itself when iterating through the list?
		m_orderedPogs.remove(pog);
		pog.setSortOrder(sortOrder);
		m_orderedPogs.add(pog);
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
	 * Remove a pog from the selection
	 * 
	 * @param pog Pog to remove
	 */
	public void unselectPog(final Pog pog)
	{
		m_selectedPogs.remove(pog);
		pog.setSelected(false);

		// @revise trigger listeners
	}

	/**
	 * Find pogs matching a given name
	 * 
	 * @param pogName Name of the pog we are looking for
	 * @param pogList if non-null, will be populated with all matching pogs
	 * @return If pogList is null, will return first matching Pog
	 */
	private Pog getPogsByName(String pogName, List<Pog> pogList)
	{
		if (pogName == null || pogName.equals(""))
			return null;

		final String normalizedName = UtilityFunctions.normalizeName(pogName);

		for (Pog pog : m_pogs)
		{
			if (pog.getNormalizedName().equals(normalizedName))
			{
				if (pogList != null)
					pogList.add(pog);
				else
					return pog;
			}
		}

		return null;
	}
}
