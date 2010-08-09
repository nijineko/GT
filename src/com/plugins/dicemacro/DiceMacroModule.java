/*
 * DiceMacroModule.java
 *
 * @created 2010-08-05
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

package com.plugins.dicemacro;

import com.galactanet.gametable.module.Module;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.PogWindow;

/**
 * todo: comment
 *
 * @author Eric Maziade
 * 
 * #GT-AUDIT DiceMacroModule
 */
public class DiceMacroModule  extends Module
{
	/**
	 * 
	 */
	public DiceMacroModule()
	{
	}
	
	/*
	 * @see com.galactanet.gametable.module.ModuleIF#getModuleName()
	 */
	@Override
	public String getModuleName()
	{
		return DiceMacroModule.class.getName();
	}
	
	/*
	 * @see com.galactanet.gametable.module.Module#onInitializeUI()
	 */
	@Override
	public void onInitializeUI()
	{
		// Todo #DiceMacro should be self-contained
		GametableFrame frame = GametableFrame.getGametableFrame();
		PogWindow panelBar = frame.getTabbedPane();
		panelBar.addTab(frame.m_macroPanel, frame.getLanguageResource().DICE_MACROS);    		
	}
	
	/*
	 * @see com.galactanet.gametable.module.ModuleIF#onToggleActiveMap(boolean)
	 */
	@Override
	public void onToggleActiveMap(boolean publicMap)
	{
	}
}
