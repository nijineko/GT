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

package com.plugins.activepogs;

import java.util.Map;

import org.w3c.dom.Element;

import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.net.PacketManager;
import com.galactanet.gametable.module.Module;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.PogWindow;
import com.galactanet.gametable.ui.GametableFrame.NetStatus;

/**
 * todo: comment
 *
 * @author Eric Maziade
 * 
 * #GT-AUDIT ActivePogsModule
 */
public class ActivePogsModule  extends Module implements GameTableMapListenerIF
{
	/**
	 * 
	 */
	private ActivePogsModule() 
	{		
	}
	
	public static ActivePogsModule getModule()
	{
		if (g_module == null)
			g_module = new ActivePogsModule();
		
		return g_module;		
	}
	
	/*
	 * @see com.galactanet.gametable.module.ModuleIF#getModuleName()
	 */
	@Override
	public String getModuleName()
	{
		return ActivePogsModule.class.getName();
	}
	
	/*
	 * @see com.galactanet.gametable.module.Module#onInitializeUI()
	 */
	@Override
	public void onInitializeUI()
	{
		GametableFrame frame = GametableFrame.getGametableFrame();		
		GametableCanvas canvas = frame.getGametableCanvas();
		PogWindow panelBar = frame.getTabbedPane();
		
		GameTableMap map = canvas.getPublicMap();
		map.addListener(this);
		
		map = canvas.getPrivateMap();
		map.addListener(this);
		
		g_activePogsPanel = new ActivePogsPanel();
    panelBar.addTab(g_activePogsPanel, frame.getLanguageResource().POG_ACTIVE);    
	}
	
	/*
	* @see com.galactanet.gametable.module.ModuleSaveIF#loadFromXML(org.w3c.dom.Element)
	*/
	@Override
	public void loadFromXML(Element node, XMLSerializeConverter converter)
	{		
		if (g_activePogsPanel == null)
			return;
		
		g_activePogsPanel.loadFromXML(node, converter);		
	}
	
	/*
	* @see com.galactanet.gametable.module.ModuleSaveIF#saveToXML(org.w3c.dom.Element)
	*/
	@Override
	public boolean saveToXML(Element node)
	{
		if (g_activePogsPanel == null)
			return false;
		
		return g_activePogsPanel.saveToXML(node);
	}
	
	/*
	 * @see com.galactanet.gametable.module.Module#canSaveToXML()
	 */
	@Override
	public boolean canSaveToXML()
	{
		return true;
	}
	
	/**
	 * @param changes
	 */
  public void pogReorderPacketReceived(final Map<MapElementID, Long> changes)
  {
  	g_activePogsPanel.reorderElements(changes, false);
  	GametableFrame frame = GametableFrame.getGametableFrame();
  	
  	if (frame.getNetStatus() == NetStatus.HOSTING)
    {
  		frame.send(PacketManager.makePogReorderPacket(changes));
    }
  }
  
  /*
   * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstanceAdded(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElement)
   */
  @Override
  public void onMapElementInstanceAdded(GameTableMap map, MapElement mapElement)
  {
  }
  
  /*
   * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstanceRemoved(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElement)
   */
  @Override
  public void onMapElementInstanceRemoved(GameTableMap map, MapElement mapElement, boolean clearingMap)
  {
  }
  
  /*
   * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstancesCleared(com.galactanet.gametable.data.GameTableMap)
   */
  @Override
  public void onMapElementInstancesCleared(GameTableMap map)
  {
  }
  	
  
  /*
   * @see com.galactanet.gametable.module.ModuleIF#onToggleActiveMap(boolean)
   */
  @Override
  public void onToggleActiveMap(boolean publicMap)
  {
  	g_activePogsPanel.showPublicMap(publicMap);
  }

	private static ActivePogsPanel g_activePogsPanel;
	private static ActivePogsModule g_module = null;
}
