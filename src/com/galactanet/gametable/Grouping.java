/*
 * Group.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.galactanet.gametable.net.PacketManager;
import com.galactanet.gametable.net.PacketSourceState;

/**
 * Manages the Grouping of Pogs for all occasions
 * Moving, Deleting when drawing and so forth
 */

public class Grouping
{
    private TreeMap groups = new TreeMap();
    
    final static int NEW    = 0;
    final static int DELETE = 1;
    final static int ADD    = 2;
    final static int REMOVE = 3;
        
    
    /* ********************************************************************************************** */
    
    public class Group {            
        private ArrayList pogs = new ArrayList();
        private String name = null;       
        
        /** ***********************************************************
         * 
         * @param pog
         * @param id
         */
        public Group(final String n) {            
            name = n;
        }
        
        /** ***********************************************************
         * 
         * @return
         */
        public String getGroup() {
            return toString();
        }  
        
        public String toString() {
            return name;
        }
       
        /** ***********************************************************
         * 
         * @param pog
         */
        public void add(final Pog pog) {     
            if(pog == null) return;            
            pogs.add(pog);
            pog.setGroup(name);
        }
        
        /** ***********************************************************
         * 
         * @return
         */
        public ArrayList getPogList() {
            return pogs;
        }
        
        /** ***********************************************************
         * 
         * @param pog
         */
        public void remove(final Pog pog) {
            pogs.remove(pog);
            pog.setGroup(null); // Clear the Group on the pog.            
        }
       
        /** ***********************************************************
         * 
         */
        public void clear() {
            for (final Iterator iterator = pogs.iterator(); iterator.hasNext();) {
                final Pog p = (Pog)iterator.next();
                p.setGroup(null);
            }
        }
        
        /** ***********************************************************
         * 
         * @param n
         */
        public void setName(final String n) {
            name = n; 
        }
        
        /** ***********************************************************
         * 
         * @return
         */
        public String getName() {
            return name;
        }
        
        /** ***********************************************************
         * 
         * @return
         */
        public int size() {
            return pogs.size();
        }
    }
       
    /* ********************************************************************************************** */
    
    /** *********************************************************************************************
     * 
     * @param group
     * @param newgroup
     */
    void rename(final String group, final String newgroup) {
        Group g = (Group)groups.get(group);
        g.setName(newgroup);
        groups.remove(group);
        groups.put(newgroup,g);
    }
    
    /** *********************************************************************************************
     * 
     * @param pogID
     * @return
     */
    private Group newGroup(final String group) {
        Group g = new Group(group);        
        groups.put(group, g);        
        //System.out.println("New Group = " + group);   
        return g;
    }
    
    /** *********************************************************************************************
     * 
     * @param group
     * @param pog
     */
    public void add(final String group, final Pog pog) {
        if(pog == null) return;
        Group g = (Group)groups.get(group);
        if(g == null) g = newGroup(group);        
        if(pog.isGrouped()) remove(pog);
        g.add(pog);
        send(ADD, group, pog.getId());
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
    public void add(final String group, final List pogs) {
        //System.out.println("Add (multipogs List) pogs size = " + pogs.size());
        for(int i = 0;i < pogs.size();++i) {            
            add(group,(Pog)pogs.get(i));            
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
        Group g = (Group)groups.get(group);
        g.clear();
        groups.remove(group);
        send(DELETE, group);
    }
    
    /** *********************************************************************************************
     * 
     */
    public void deleteall() {
        Iterator i = getGroups();
        while(i.hasNext()) {
            Group g = (Group)i.next();
            g.clear();
            send(DELETE, g.toString());
        }
        groups.clear();
    }
    
    /** *********************************************************************************************
     * 
     */
    public void deleteunused() {
        Iterator i = getGroups();
        while(i.hasNext()) {
            Group g = (Group)i.next();
            if(g.size() == 0) {                
                groups.remove(g);
                send(DELETE, g.toString());
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
        Group g = (Group)groups.get(pog.getGroup());
        //System.out.println("Remove Pog = " + pog.getId() + " Group = " + g.getGroup());
        g.remove(pog);
        if(send) send(REMOVE, "", pog.getId());
    }    
    
    public void remove(final Pog pog) {
        remove(pog, true);
    }
    
    /** *********************************************************************************************
     * 
     * @return
     */
    public Iterator getGroups() {
        return groups.values().iterator();
    }
    
    /** *********************************************************************************************
     * 
     * @return
     */
    public int size() {
        return groups.size();
    }
    
    /** *********************************************************************************************
     * 
     * @param group
     * @return
     */
    public ArrayList getGroup(final String group) {
        Group g = (Group)groups.get(group);        
        return g.getPogList();
    }
    
    /** *********************************************************************************************
     * 
     * @param pog
     * @param group
     * @param action
     * @param player
     */
    public void packetReceived(final int action, final String group, final int pog) {
        //System.out.println("Entered Grouping Packet Received: action = " + action + " Group = " + group + " Pog = " + pog);    
        GametableMap map = GametableFrame.getGametableFrame().getGametableCanvas().getPublicMap();
        final Pog p = map.getPogByID(pog);
        switch (action) {
            case ADD:       add(group,p);       break;
            case REMOVE:    remove(p);          break;
            case DELETE:    deleteGroup(group); break;
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
    private void send(final int action, final String group) {
        send(action, group, 0);
    }
   
    private void send(final int action, final String group, final int pog) {
        //System.out.println("Entered Grouping Send");
        if((GametableFrame.getGametableFrame().getGametableCanvas().getActiveMap() == 
            GametableFrame.getGametableFrame().getGametableCanvas().getPublicMap()) &&
        (!PacketSourceState.isNetPacketProcessing())) {
            final int player = GametableFrame.getGametableFrame().getMyPlayerId();
            GametableFrame.getGametableFrame().send(PacketManager.makeGroupPacket(action, group, pog, player));
        }
    }
}
