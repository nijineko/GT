/*
 * UIModeAction.java
 *
 * @created 2011-04-10
 *
 * Copyright (C) 1999-2011 Open Source Game Table Project
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

package com.galactanet.gametable.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.util.ImageCache;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class UIModeAction extends AbstractAction
{
	/**
	 * Default constructor
	 * @param uiMode UI Mode linked to this action
	 * @param name Name for the action.  Could be used for button or menu text.
	 * @param description Description for the action.  Used for tool tip text.
	 * @param acceleratorKey Keystroke to be used as accelerator key (see KeyEvent constants)
	 * @param iconFileName File name of the icon for this action.  The file will be read from the icons folder.
	 */
	public UIModeAction(UIMode uiMode, String name, String description, int keyEventAccelerator, String iconFileName)
	{
		m_uiMode = uiMode;
		
		if (keyEventAccelerator > 0)
		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyEventAccelerator, 
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}
				
		putValue(Action.SHORT_DESCRIPTION, description);
		putValue(Action.NAME, name);
		putValue(Action.SMALL_ICON, new ImageIcon(ImageCache.getImage(new File(GametableFrame.PATH_ICONS + iconFileName))));
		
		/* todo #NB - Cross-platform method of getting menu keystroke:
		 * not : KeyEvent.CTRL_MASK, but
		 * Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
		 */
	}

	/*
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		GametableApp.getUserInterface().setUIMode(m_uiMode);
	}

	private final UIMode m_uiMode;
}
