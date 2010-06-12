/*
 * GametableMap.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.data;

import java.awt.Point;
import java.util.*;
import java.util.Map.Entry;

import com.galactanet.gametable.data.net.PacketManager;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.LineSegment;
import com.galactanet.gametable.util.UtilityFunctions;



/**
 * This class stores the data related to a gametable map. This includes line, pogs, and underlays
 * 
 * @author sephalon
 * 
 * #GT-AUDIT GametableMap
 */
public class GametableMap
{
    /* ******************* CONSTANTS ************************ */

    public final static int MAX_UNDO_LEVELS = 20;

    /* ******************* CONSTRUCTION ************************ */

    protected boolean       m_bIsSharedMap;

    /** ************************** CLASS DATA ****************************** */
    // lines on the map
    protected List<LineSegment>          m_lines         = new ArrayList<LineSegment>();

    /* ***************** LINES MANAGEMENT********************* */

    protected SortedSet<Pog>     m_orderedPogs   = new TreeSet<Pog>();

    // pogs on the map
    protected List<Pog>          m_pogs          = new ArrayList<Pog>();
    
    // pogs on the map that are currently selected #grouping
    public List<Pog>          m_selectedPogs  = new ArrayList<Pog>();


    private int             m_redoIndex     = -1;

    // add to origin to get actual coordinates.
    // (Negative if inside image)
    private int             m_scrollX;

    // add to origin to get actual coordinates.
    // (Negative if inside image)
    private int             m_scrollY;

    /* ***************** POGS MANAGEMENT********************* */

    private List<MapState>            m_undoLevels    = new ArrayList<MapState>();

    // bIsSharedMap is true if this is the shared gametable map.
    // it is false if it's a private map (private layer).
    public GametableMap(final boolean bIsSharedMap)
    {
        m_bIsSharedMap = bIsSharedMap;

        // seed the undo stack with the "blank state"
        beginUndoableAction();
        endUndoableAction(-1, -1);
    }

    public void addLine(final LineSegment ls)
    {
        m_lines.add(ls);
    }

    public void addPog(final Pog pog)
    {
        m_pogs.add(pog);
        if (!pog.isUnderlay())
        {
            m_orderedPogs.add(pog);
        }
    }
    
    public void addOrderedPog(final Pog pog) {
        m_orderedPogs.add(pog);
    }
    
    /** **********************************************************************************************
     * #grouping
     * @param pog
     */
    public void addSelectedPog(final Pog pog) {
        m_selectedPogs.add(pog);
        pog.setSelected();
    }
    
    /** **********************************************************************************************
     * #grouping
     * @param pogs
     */
    public void addSelectedPogs(final List<Pog> pogs) 
    {
        final int size = pogs.size();
        
        System.out.println("addSelectedPogs: size = " + size);
        
        for (Pog pog : pogs)
        {
            System.out.println("Pog = " + pog.getId());
            addSelectedPog(pog);
        }
    }
    
    /** **********************************************************************************************
     * #grouping
     * @param pog
     */
    public void removeSelectedPog(final Pog pog) {
        m_selectedPogs.remove(pog);
        pog.unsetSelected();
    }


    private void adoptState(final MapState state)
    {
        // adopt the lines from the MapState
        m_lines = new ArrayList<LineSegment>();
        for (int i = 0; i < state.m_lineSegments.size(); i++)
        {
            final LineSegment ls = state.m_lineSegments.get(i);
            final LineSegment toAdd = new LineSegment(ls);
            m_lines.add(toAdd);
        }
    }

    // call before making changes you want to be undoable
    public void beginUndoableAction()
    {
        // nothing needed here yet
    }

    public boolean canRedo()
    {
        // you can't redo if there's no action to redo
        if (m_redoIndex < 0)
        {
            return false;
        }

        // you can redo if it's the private layer
        if (!m_bIsSharedMap)
        {
            return true;
        }

        // you can't redo if it's not an action you did
        final int myID = GametableFrame.getGametableFrame().getMyPlayerId();

        final MapState nextRedoable = m_undoLevels.get(m_redoIndex);
        if (nextRedoable.m_playerID == myID)
        {
            // you did the last redoable action. You can redo it
            return true;
        }
        return false;
    }

    public boolean canUndo()
    {
        // if there's nothing to undo, then no, you can't undo it
        if (m_undoLevels.size() == 0)
        {
            return false;
        }

        if (!m_bIsSharedMap)
        {
            // we're the private layer. We can undo anything
            return true;
        }

        // the most recent action has to be yours for it to be undoable
        final int myID = GametableFrame.getGametableFrame().getMyPlayerId();

        final MapState lastUndoable = m_undoLevels.get(m_undoLevels.size() - 1);
        if (lastUndoable.m_playerID == myID)
        {
            // you did the last undoable action. You can undo
            return true;
        }
        return false;
    }

    public void clearLines()
    {
        m_lines = new ArrayList<LineSegment>();
    }

    public void clearPogs()
    {
        m_pogs.clear();
        m_orderedPogs.clear();
    }

    /** **********************************************************************************************
     * #grouping
     */
    public void clearSelectedPogs() {
        final int size = m_selectedPogs.size();
        Pog p;
        for(int i = 0; i < size; ++i) {
            p = m_selectedPogs.get(i);
            p.unsetSelected();
        }
        m_selectedPogs.clear();
    }

    /** ***************** UNDO MANAGEMENT********************* */
    public void clearUndos()
    {
        m_undoLevels = new ArrayList<MapState>();
        m_redoIndex = -1;

        // seed it
        beginUndoableAction();
        endUndoableAction(-1, -1);
    }

    // call after making the changes you want undoable
    public void endUndoableAction(final int playerID, final int stateID)
    {
        // adding an action means removing any actions from the undo tree beyond the current state
        if (m_redoIndex >= 0)
        {
            killStates(m_redoIndex);
            m_redoIndex = -1;
        }

        final MapState state = new MapState();
        state.setLines(m_lines);
        state.m_playerID = playerID;
        state.m_stateID = stateID;

        // add it to the undo stack
        m_undoLevels.add(state);

        // trim the stack if necessary
        if (m_undoLevels.size() > MAX_UNDO_LEVELS)
        {
            // dump the earliest one
            m_undoLevels.remove(0);
        }

        // System.out.println("Added undoable - state:"+stateID+", plr:"+playerID);
    }

    public LineSegment getLineAt(final int idx)
    {
        return m_lines.get(idx);
    }

    /* ***************** SCROLL MANAGEMENT********************* */

    public int getNumLines()
    {
        return m_lines.size();
    }
    
    public List<LineSegment> getLines() // @revise added this to optimize a call in publishTool and avoid getLineAt.  Try to add access to unmodifiable list instead
    {
        return m_lines;
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
                switch(pog.getLayer())
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
        
        if (envHit != null) return envHit;
        if (overlayHit != null) return overlayHit;

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
        return Collections.unmodifiableList(m_pogs);    // @comment {themaze75} The returned unmodifiableList remains in sync with the original list - we can retain only one instance here instead of creating a new one each call 
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
        return m_scrollX;
    }

    public int getScrollY()
    {
        return m_scrollY;
    }

    private int getUseableStackSize()
    {
        // youcan't look at undoables past the redoIndex
        int useableStackSize = m_undoLevels.size();
        if (m_redoIndex >= 0)
        {
            useableStackSize = m_redoIndex;
        }

        return useableStackSize;
    }

    private void killStates(final int startIdx)
    {
        while (m_undoLevels.size() > startIdx)
        {
            m_undoLevels.remove(startIdx);
        }
    }

    public void redo()
    {
        if (!canRedo())
        {
            return;
        }

        // fairly simple, actually. just adopt the state and advance the redo
        final MapState nextRedoable = m_undoLevels.get(m_redoIndex);
        adoptState(nextRedoable);
        m_redoIndex++;

        if (m_redoIndex >= m_undoLevels.size())
        {
            // we've redone up to the end of the undo stack.
            m_redoIndex = -1;
        }
    }

    public void redo(final int stateID)
    {
        // this function has no meaning on the private map
        if (!m_bIsSharedMap)
        {
            return;
        }

        // for redo, we don't care if it's you who did the undoable action or not.
        // it could have been sent in from another player. We just redo it.

        // first, find the action.
        int stateIdx = -1;
        for (int i = 0; i < m_undoLevels.size(); i++)
        {
            final MapState state = m_undoLevels.get(i);
            if (state.m_stateID == stateID)
            {
                // this is the action that we can redo.
                stateIdx = i;
            }
        }

        if (stateIdx < 0)
        {
            // Houston... we have a problem.
            // if we're here, it means someone managed to send an redo
            // command and we don't have the state to revert to. This
            // means we'll probably desynch with the rest of the players.
            // This shouldn't happen. But on the offchance that it does for
            // some unknown reason, we should defensively return, rather than
            // crash. Desynched is better than crashed.
            return;
        }

        // get the state
        final MapState redoTo = m_undoLevels.get(stateIdx);

        // adopt the state
        adoptState(redoTo);

        // now we have to trash all states beyond this undo state
        m_redoIndex = stateIdx + 1;

        if (m_redoIndex >= m_undoLevels.size())
        {
            // no worlds left to conquer
            m_redoIndex = -1;
        }
    }

    public void redoNextRecent()
    {
        final MapState nextRedoable = m_undoLevels.get(m_redoIndex);
        final GametableFrame frame = GametableFrame.getGametableFrame();
        final GametableCanvas canvas = frame.getGametableCanvas();
        if (m_bIsSharedMap)
        {
            if (canvas.isPublicMap())
            {
                frame.send(PacketManager.makeRedoPacket(nextRedoable.m_stateID));

                if (frame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
                {
                    redo(nextRedoable.m_stateID);
                }
            }
            else
            {
                redo(nextRedoable.m_stateID);
            }
        }
        else
        {
            // redoing on the private map doesn't work through IDs,
            // and causes no network activity
            redo();
        }
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
    
    public void removeOrderedPog(final Pog pog) {        
        m_orderedPogs.remove(pog);
    }

    public void removePog(final Pog pog) {
        removePog(pog,false);
    }
    
    public void removePog(final Pog pog, final boolean selected)
    {
        if(selected) removeSelectedPog(pog);
        m_pogs.remove(pog);
        m_orderedPogs.remove(pog);
    }

    public void reorderPogs(final Map<Integer, Long> changes)
    {
        if (changes == null)
        {
            return;
        }

        for(Entry<Integer, Long> entry : changes.entrySet())
        {
            setSortOrder(entry.getKey(), entry.getValue());
        }
    }

    public void setScroll(final int x, final int y)
    {
        m_scrollX = x;
        m_scrollY = y;
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

    public void undo(final int stateID)
    {
        // this function has no meaning on the private map
        if (!m_bIsSharedMap)
        {
            return;
        }

        // for undo, we don't care if it's you who did the undoable action or not.
        // it could have been sent in from another player. We just undo it.

        // first, find the action.
        final int useableStackSize = getUseableStackSize();
        int stateIdx = -1;
        for (int i = 0; i < useableStackSize; i++)
        {
            final MapState state = m_undoLevels.get(i);
            if (state.m_stateID == stateID)
            {
                // this is the action that we can undo.
                // so the state we need to set it to is the action
                // BEFORE this one.
                stateIdx = i - 1;
            }
        }

        if (stateIdx < 0)
        {
            // Houston... we have a problem.
            // if we're here, it means someone managed to send an undo
            // command and we don't have the state to revert to. This
            // means we'll probably desynch with the rest of the players.
            // This shouldn't happen. But on the offchance that it does for
            // some unknown reason, we should defensively return, rather than
            // crash. Desynched is better than crashed.
            return;
        }

        // get the state
        final MapState undoTo = m_undoLevels.get(stateIdx);
        // System.out.println("Undoing to ID:" + undoTo.m_stateID);

        // adopt the state
        adoptState(undoTo);

        // now we have to trash all states beyond this undo state
        m_redoIndex = stateIdx + 1;
    }

    public void undoMostRecent()
    {
        // safety check
        if (!canUndo())
        {
            return;
        }

        final int useableStackSize = getUseableStackSize();

        if (m_bIsSharedMap)
        {
            final MapState lastUndoable = m_undoLevels.get(useableStackSize - 1);
            final GametableFrame frame = GametableFrame.getGametableFrame();
            final GametableCanvas canvas = frame.getGametableCanvas();
            if (canvas.isPublicMap())
            {
                frame.send(PacketManager.makeUndoPacket(lastUndoable.m_stateID));

                if (frame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
                {
                    undo(lastUndoable.m_stateID);
                }
            }
            else
            {
                undo(lastUndoable.m_stateID);
            }
        }
        else
        {
            // undoing on the private map doesn't work through IDs,
            // and causes no network activity
            final int undoToIdx = useableStackSize - 2;
            if (undoToIdx < 0)
            {
                // nothing to undo
                return;
            }
            final MapState undoTo = m_undoLevels.get(undoToIdx);
            adoptState(undoTo);
            m_redoIndex = useableStackSize - 1;
        }
    }
}
