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

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.MapElementID;
import com.galactanet.gametable.data.XMLSerializeConverter;
import com.galactanet.gametable.module.Module;
import com.galactanet.gametable.net.NetworkEvent;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.PogWindow;

/**
 * todo: comment
 *
 * @author Eric Maziade
 * 
 * #GT-AUDIT ActivePogsModule
 */
public class ActivePogsModule  extends Module 
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
	 * @see com.galactanet.gametable.module.Module#onInitializeCore(com.galactanet.gametable.data.GametableCore)
	 */
	@Override
	public void onInitializeCore(GameTableCore core)
	{
		core.getNetworkModule().registerMessageType(NetSetMapElementOrder.getMessageType());
	}
	
	/*
	 * @see com.galactanet.gametable.module.Module#onInitializeUI()
	 */
	@Override
	public void onInitializeUI(GametableFrame frame)
	{
		PogWindow panelBar = frame.getTabbedPane();
		
		g_activePogsPanel = new ActivePogsPanel(frame);
    panelBar.addTab(g_activePogsPanel, "Active Pogs");    
	}
	
	/*
	 * @see com.galactanet.gametable.module.Module#loadFromXML(org.w3c.dom.Element, com.galactanet.gametable.data.XMLSerializeConverter, com.galactanet.gametable.net.NetworkEvent)
	 */
	@Override
	public void loadFromXML(Element node, XMLSerializeConverter converter, NetworkEvent netEvent)
	{		
		if (g_activePogsPanel == null)
			return;
		
		g_activePogsPanel.loadFromXML(node, converter, netEvent);		
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
  protected void pogReorderPacketReceived(final Map<MapElementID, Long> changes)
  {
  	g_activePogsPanel.reorderElements(changes, false);
  }
  
  /*
   * @see com.galactanet.gametable.module.ModuleIF#onToggleActiveMap(boolean)
   */
  @Override
  public void onActiveMapChange(boolean publicMap)
  {
  	g_activePogsPanel.showPublicMap(publicMap);
  }

  private static ActivePogsPanel g_activePogsPanel;
	private static ActivePogsModule g_module = null;
}
