/*
 * AbstractTool.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui.tools;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

import com.galactanet.gametable.data.MapCoordinates;
import com.galactanet.gametable.data.prefs.PreferenceDescriptor;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.ToolIF;



/**
 * A basic non-functional implementation of Tool that other tools can subclass.
 * 
 * @author iffy
 * 
 * #GT-AUDIT NullTool
 */
public class NullTool implements ToolIF
{
    /**
     * Constructor.
     */
    public NullTool()
    {
    }

    // --- Tool Implementation ---

    /*
     * @see com.galactanet.gametable.Tool#activate(com.galactanet.gametable.GametableCanvas)
     */
    public void activate(final GametableCanvas canvas)
    {
    }

    /*
     * @see com.galactanet.gametable.Tool#deactivate()
     */
    public void deactivate()
    {
    }

    public void endAction()
    {
    }

    /*
     * @see com.galactanet.gametable.Tool#getPreferences()
     */
    public List<PreferenceDescriptor> getPreferences()
    {
        return Collections.emptyList();
    }

    /*
     * @see com.galactanet.gametable.Tool#isBeingUsed()
     */
    public boolean isBeingUsed()
    {
        return false;
    }

    /*
     * @see com.galactanet.gametable.Tool#mouseButtonPressed(int, int)
     */
    public void mouseButtonPressed(MapCoordinates modelPos, final int modifierMask)
    {
    }

    /*
     * @see com.galactanet.gametable.Tool#mouseButtonReleased(int, int)
     */
    public void mouseButtonReleased(MapCoordinates modelPos, final int modifierMask)
    {
    }

    /*
     * @see com.galactanet.gametable.Tool#mouseMoved(int, int)
     */
    public void mouseMoved(MapCoordinates modelPos, final int modifierMask)
    {
    }

    /*
     * @see com.galactanet.gametable.Tool#paint(java.awt.Graphics)
     */
    public void paint(final Graphics g)
    {
    }

		
		static Rectangle createRectangle(final Point a, final Point b)
		{
		    final int x = Math.min(a.x, b.x);
		    final int y = Math.min(a.y, b.y);
		    final int width = Math.abs(b.x - a.x) + 1;
		    final int height = Math.abs(b.y - a.y) + 1;
		
		    return new Rectangle(x, y, width, height);
		}

}
