/*
 * Group.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.data;

import java.util.*;

import com.galactanet.gametable.data.net.PacketManager;
import com.galactanet.gametable.data.net.PacketSourceState;
import com.galactanet.gametable.ui.GametableFrame;

/**
 * Manages the Grouping of Pogs for all occasions
 * Moving, Deleting when drawing and so forth
 */

public class Grouping
{
    private Map<String, Group> m_groups = new HashMap<String, Group>();

    public enum ActionType {
    	NEW, DELETE, ADD, REMOVE;
    	
    	/**
    	 * Get ActionType from ordinal value
    	 * @param ord
    	 * @return
    	 */
    	public static ActionType fromOrdinal(int ord)
    	{
    		for (ActionType t : ActionType.values())
    		{
    			if (t.ordinal() == ord)
    				return t;
    		}
    		
    		return null;
    	}
    }
    
    /* ********************************************************************************************** */
    
    // @revise should this really be an internal class?
    public class Group {            
        private List<Pog> m_pogs = new ArrayList<Pog>();
        private String m_name = null;       
        
        /** ***********************************************************
         * 
         * @param pog
         * @param id
         */
        public Group(final String n) {            
            m_name = n;
        }
        
        /** ***********************************************************
         * 
         * @return
         */
        public String getGroup() {
            return toString();
        }  
        
        public String toString() {
            return m_name;
        }
       
        /** ***********************************************************
         * 
         * @param pog
         */
        public void add(final Pog pog) {     
            if(pog == null) return;            
            m_pogs.add(pog);
            pog.setGroup(m_name);
        }
        
        /** ***********************************************************
         * 
         * @return
         */
        public List<Pog> getPogList() {
            return m_pogs;
        }
        
        /** ***********************************************************
         * 
         * @param pog
         */
        public void remove(final Pog pog) 
        {
            m_pogs.remove(pog);
            pog.setGroup(null); // Clear the Group on the pog.            
        }
       
        /** ***********************************************************
         * 
         */
        public void clear() 
        {
        	for (Pog p : m_pogs)
        		p.setGroup(null);
        }
        
        /** ***********************************************************
         * 
         * @param n
         */
        public void setName(final String n) {
            m_name = n; 
        }
        
        /** ***********************************************************
         * 
         * @return
         */
        public String getName() {
            return m_name;
        }
        
        /** ***********************************************************
         * 
         * @return
         */
        public int size() {
            return m_pogs.size();
        }
    }
       
    /* ********************************************************************************************** */
    
    /** *********************************************************************************************
     * 
     * @param groupName
     * @param newgroup
     */
    void rename(final String groupName, final String newgroup) 
    {
        Group g = m_groups.get(groupName);
        g.setName(newgroup);
        m_groups.remove(groupName);
        m_groups.put(newgroup,g);
    }
    
    /** *********************************************************************************************
     * 
     * @param pogID
     * @return
     */
    private Group newGroup(final String group) {
        Group g = new Group(group);        
        m_groups.put(group, g);        
        //System.out.println("New Group = " + group);   
        return g;
    }
    
    /** *********************************************************************************************
     * 
     * @param groupName
     * @param pog
     */
    public void add(final String groupName, final Pog pog) 
    {
        if(pog == null) return;
        Group g = m_groups.get(groupName);
        if(g == null) g = newGroup(groupName);        
        if(pog.isGrouped()) remove(pog);
        g.add(pog);
        send(ActionType.ADD, groupName, pog.getId());
       //System.out.println("Add to Group = " + g.getName() + "(" + g.toString() + 
       //     "/" + g.pogs.size() + ") Added pog " + pog.getId());        
    }   
    
    /** *********************************************************************************************
     * 
     * @param pogs
     * @return
     */
    public void add(final int pogs[], final String group) {  
        GametableMap map =  GametableFrame.getGametableFrame().getGametableCanvas().getActiveMap();        
        //System.out.println("Add (multipogs int) pogs size = " + pogs.length);        
        for(int i = 0;i < pogs.length;++i) {
            add(group, map.getPogByID(pogs[i]));
        }
    }
   
    /** *********************************************************************************************
     * 
     * @param group
     * @param pog
     */
    public void add(final String group, final List<Pog> pogs) {
        //System.out.println("Add (multipogs List) pogs size = " + pogs.size());
        for(int i = 0;i < pogs.size();++i) {            
            add(group, pogs.get(i));            
        }
    }
    
    /** *********************************************************************************************
     * 
     * @param group
     */
    public void delete(final String group) {
        deleteGroup(group);
    }
    public void deleteGroup(final String group) {
        Group g = m_groups.get(group);
        g.clear();
        m_groups.remove(group);
        send(ActionType.DELETE, group);
    }
    
    /** *********************************************************************************************
     * 
     */
    public void deleteall() {
    	
    	for (Group g : getGroups())
    	{
    		g.clear();
        send(ActionType.DELETE, g.toString());
      }
    	
      m_groups.clear();
    }
    
    /** *********************************************************************************************
     *  
     */
    
    public void deleteunused() {
    	for (Group g : getGroups())
    	{      
            if(g.size() == 0) {                
                m_groups.remove(g);  // @revise this is prone to break due to concurrent modification
                send(ActionType.DELETE, g.toString());
            }
        }
    }
    
    /** *********************************************************************************************
     * 
     * @param group
     * @param pog
     */
    public void remove(final Pog pog, final boolean send) {
        if(pog == null) return;
        if(!pog.isGrouped()) return;
        Group g = m_groups.get(pog.getGroup());
        //System.out.println("Remove Pog = " + pog.getId() + " Group = " + g.getGroup());
        g.remove(pog);
        if(send) send(ActionType.REMOVE, "", pog.getId());
    }    
    
    public void remove(final Pog pog) {
        remove(pog, true);
    }
    
    /** *********************************************************************************************
     * 
     * @return
     */
    public Collection<Group> getGroups() {
        return m_groups.values();
    }
    
    /** *********************************************************************************************
     * 
     * @return
     */
    public int size() {
        return m_groups.size();
    }
    
    /** *********************************************************************************************
     * 
     * @param group
     * @return
     */
    public List<Pog> getGroup(final String group) {
        Group g = m_groups.get(group);
        
        return g.getPogList();
    }
    
    /** *********************************************************************************************
     * 
     * @param pog
     * @param group
     * @param action
     * @param player
     */
    public void packetReceived(ActionType action, final String group, final int pog) {
        //System.out.println("Entered Grouping Packet Received: action = " + action + " Group = " + group + " Pog = " + pog);    
        GametableMap map = GametableFrame.getGametableFrame().getGametableCanvas().getPublicMap();
        final Pog p = map.getPogByID(pog);
        switch (action) {
            case ADD:       add(group,p);       break;
            case REMOVE:    remove(p);          break;
            case DELETE:    deleteGroup(group); break;
            
            case NEW: 
            	// do nothing 
            	break;
        }
    }
    
    /** *********************************************************************************************
     * Doubler Check myself - If Im not alreayd processing the packet, send it. If I am, I recieved it!
     * And if this is the private map, who cares atm! This will be done when the pog is processed once 
     * its moved to the public map 
     * @param pog
     * @param group
     * @param action
     */    
    private void send(ActionType action, final String group) {
        send(action, group, 0);
    }
   
    private void send(ActionType action, final String groupName, final int pog) {
        //System.out.println("Entered Grouping Send");
        if((GametableFrame.getGametableFrame().getGametableCanvas().getActiveMap() == 
            GametableFrame.getGametableFrame().getGametableCanvas().getPublicMap()) &&
        (!PacketSourceState.isNetPacketProcessing())) {
            final int player = GametableFrame.getGametableFrame().getMyPlayerId();
            GametableFrame.getGametableFrame().send(PacketManager.makeGroupPacket(action, groupName, pog, player));
        }
    }
}
