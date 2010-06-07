/*
 * ColorEraseTool.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui.tools;

/**
 * Eraser tool that erases only the currently selected color.
 * 
 * @author iffy
 * 
 * #GT-AUDIT ColorEraseTool
 */
public class ColorEraseTool extends EraseTool
{
    /**
     * Constructor that tells superclass to go into color erase mode.
     */
    public ColorEraseTool()
    {
        super(true);
    }
}
