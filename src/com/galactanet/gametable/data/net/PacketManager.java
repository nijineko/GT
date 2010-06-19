/*
 * PacketManager.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.data.net;

import java.awt.Rectangle;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import com.galactanet.gametable.data.MapElement;
import com.galactanet.gametable.data.MapElementInstance;
import com.galactanet.gametable.data.MapElementInstanceID;
import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.data.MapElement.Layer;
import com.galactanet.gametable.data.PogGroups.Action;
import com.galactanet.gametable.data.deck.Card;
import com.galactanet.gametable.data.deck.Deck;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.LineSegment;
import com.galactanet.gametable.ui.GametableCanvas.BackgroundColor;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.UtilityFunctions;



/**
 * #GT-COMMENT
 * 
 * 
 * @author sephalon
 * 
 * #GT-AUDIT PacketManager
 */
public class PacketManager
{
    // --- Constants -------------------------------------------------------------------------------------------------

    /**
     * Set of files already asked for. todo: Add some kind of timed retry feature.
     */
    private static Set<String>      g_requestedFiles          = new HashSet<String>();

    /**
     * A Map of sets of pending incoming requests that could not be fulfilled.
     */
    private static Map<String, Set<Connection>>     g_unfulfilledRequests     = new HashMap<String, Set<Connection>>();

    // Pog added
    public static final int PACKET_ADDPOG             = 5;

    // Packet sent by the host telling all the players in the game
    public static final int PACKET_CAST               = 1;

    // informs players that a deck is pulling all its cards
    // home. Either because the deck is being destroyed, or
    // because it's having a complete shuffling
    public static final int PACKET_DECK_CLEAR_DECK    = 25;

    // sent by players to tell everyone that they've discarded
    // one or more cards
    public static final int PACKET_DECK_DISCARD_CARDS = 26;

    // informs you of which decks exist
    public static final int PACKET_DECK_LIST          = 22;

    // sent TO players, giving them cards they requested
    // (in response to a PACKED_TECK_REQUEST_CARDS)
    public static final int PACKET_DECK_RECEIVE_CARDS = 24;

    // sent by players who are trying to draw cards
    public static final int PACKET_DECK_REQUEST_CARDS = 23;

    // Eraser used
    public static final int PACKET_ERASE              = 4;

    // png data transfer
    public static final int PACKET_FILE               = 12;

    // notification of a hex mode / grid mode change
    public static final int PACKET_HEX_MODE           = 14;

    // Lines being added
    public static final int PACKET_LINES              = 3;

    // Pog lock state changed
    public static final int PACKET_LOCKPOG            = 28;

    // notification that the host is done sending you the inital packets
    // you get when you log in
    public static final int PACKET_LOGIN_COMPLETE     = 15;

    // Pog moved
    public static final int PACKET_MOVEPOG            = 7;

    // host sends PING, client sends back PING
    public static final int PACKET_PING               = 16;

    // Packet sent by a new joiner as soon as he joins
    public static final int PACKET_PLAYER             = 0;

    // request for a png
    public static final int PACKET_PNGREQUEST         = 13;

    // pog reorder packet
    public static final int PACKET_POG_REORDER        = 21;

    // a pog size packet
    public static final int PACKET_POG_SIZE           = 19;

    // pog data change
    public static final int PACKET_POGDATA            = 9;

    // point state change
    public static final int PACKET_POINT              = 8;

    // private text packet
    public static final int PACKET_PRIVATE_TEXT       = 20;

    // recentering packet
    public static final int PACKET_RECENTER           = 10;

    // join rejected
    public static final int PACKET_REJECT             = 11;

    // Pog removed
    public static final int PACKET_REMOVEPOGS         = 6;

    // Pog rotated
    public static final int PACKET_ROTATEPOG          = 27;
    public static final int PACKET_FORCEGRIDSNAP      = 106;

    // Pog flipped
    public static final int PACKET_FLIPPOG            = 30;

    // Packet with text to go to the text log
    public static final int PACKET_TEXT               = 2;

    public static final int PACKET_BGCOL              = 101;

    // Locks all pogs on map
    public static final int PACKET_LOCKALLPOG         = 102;    

    public static final int PACKET_POGLAYER           = 103; // Changing the layer of the Pog

    // Sends a Machanics Packet    
    public static final int PACKET_MECHANICS          = 104;
    public static final int PACKET_PRIVMECH           = 105;
    
    public static final int PACKET_GROUP              = 107;
    public static final int PACKET_POG_TYPE           = 108;

    
    // --- Static Members --------------------------------------------------------------------------------------------

    // Player is typing
    public static final int PACKET_TYPING             = 29;

    // --- Static Methods --------------------------------------------------------------------------------------------

    private static void addUnfulfilledRequest(final String filename, final Connection connection)
    {
        Set<Connection> set = g_unfulfilledRequests.get(filename);
        if (set == null)
        {
            set = new HashSet<Connection>();
            g_unfulfilledRequests.put(filename, set);
        }

        set.add(connection);
    }

    public static String getPacketName(final byte[] packet)
    {
        try
        {
            final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet));
            return getPacketName(dis.readInt());
        }
        catch (final IOException ioe)
        {
            return "ERROR";
        }
    }

    //@revise this makes me feel that packet types might want to be classes instead...
    public static String getPacketName(final int type)
    {
        switch (type)
        {
            case PACKET_PLAYER:
                return "PACKET_PLAYER";
            case PACKET_REJECT:
                return "PACKET_REJECT";
            case PACKET_CAST:
                return "PACKET_CAST";
            case PACKET_TEXT:
                return "PACKET_TEXT";
            case PACKET_MECHANICS:
                return "PACKET_MECHANICS";
            case PACKET_PRIVMECH:
                return "PACKET_PRIVMECH";
            case PACKET_TYPING:
                return "PACKET_TYPING";
            case PACKET_LINES:
                return "PACKET_LINES";
            case PACKET_ERASE:
                return "PACKET_ERASE";
            case PACKET_ADDPOG:
                return "PACKET_ADDPOG";
            case PACKET_REMOVEPOGS:
                return "PACKET_REMOVEPOGS";
            case PACKET_MOVEPOG:
                return "PACKET_MOVEPOG";
            case PACKET_LOCKPOG:
                return "PACKET_LOCKPOG";
            case PACKET_POGLAYER:
                return "PACKET_POGLAYER"; 
            case PACKET_LOCKALLPOG:
                return "PACKET_LOCKALLPOG";
            case PACKET_POINT:
                return "PACKET_POINT";
            case PACKET_POGDATA:
                return "PACKET_POGDATA";
            case PACKET_RECENTER:
                return "PACKET_RECENTER";
            case PACKET_FILE:
                return "PACKET_FILE";
            case PACKET_PNGREQUEST:
                return "PACKET_PNGREQUEST";
            case PACKET_HEX_MODE:
                return "PACKET_HEX_MODE";
            case PACKET_LOGIN_COMPLETE:
                return "PACKET_LOGIN_COMPLETE";
            case PACKET_PING:
                return "PACKET_PING";
            case PACKET_POG_SIZE:
                return "PACKET_POG_SIZE";
            case PACKET_PRIVATE_TEXT:
                return "PACKET_PRIVATE_TEXT";
            case PACKET_POG_REORDER:
                return "PACKET_POG_REORDER";
            case PACKET_DECK_LIST:
                return "PACKET_DECK_LIST";
            case PACKET_DECK_REQUEST_CARDS:
                return "PACKET_DECK_REQUEST_CARDS";
            case PACKET_DECK_RECEIVE_CARDS:
                return "PACKET_DECK_RECEIVE_CARDS";
            case PACKET_DECK_CLEAR_DECK:
                return "PACKET_DECK_CLEAR_DECK";
            case PACKET_DECK_DISCARD_CARDS:
                return "PACKET_DECK_DISCARD_CARDS";
            case PACKET_BGCOL:
                return "PACKET_BGCOL";
            case PACKET_GROUP:
              return "PACKET_GROUP";
            case PACKET_POG_TYPE:
              return "PACKET_POG_TYPE";    
            default:
                return "PACKET_UNKNOWN";
        }
    }

    /* *********************** ADDPOG PACKET *********************************** */
    // calls for the pog to be added to the public layer
    public static byte[] makeAddPogPacket(final MapElementInstance pog)
    {
        return makeAddPogPacket(pog, true);
    }

    /* *********************** CAST PACKET *********************************** */

    public static byte[] makeAddPogPacket(final MapElementInstance pog, final boolean bPublicLayerPog)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_ADDPOG); // type
            dos.writeBoolean(bPublicLayerPog); // layer
            pog.writeToPacket(dos);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /** *******************************************************************************************
     * 
     * @param color
     * @return
     */
    public static byte[] makeBGColPacket(BackgroundColor color) {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_BGCOL); // type
            dos.writeBoolean(false);	// type:color
            dos.writeInt(color.ordinal());

            
            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        } 
    }
    
    public static byte[] makeBGColPacket(MapElementInstanceID elementID) {
      try
      {
          final ByteArrayOutputStream baos = new ByteArrayOutputStream();
          final DataOutputStream dos = new DataOutputStream(baos);

          dos.writeInt(PACKET_BGCOL); // type
          dos.writeBoolean(true);	// type:pog ID
          dos.writeLong(elementID.numeric());
          
          
          return baos.toByteArray();
      }
      catch (final IOException ex)
      {
          Log.log(Log.SYS, ex);
          return null;
      } 
  }
    
    // This is for future use........
    protected static byte[] makeBGColPacket(final MapElementInstance pog) {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_BGCOL); // type
            dos.writeBoolean(true); // type:pog
            dos.writeInt(-1);                        
            dos.writeUTF(pog.getPogType().getImageFilename());
            
            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        } 
    }
      
    
    public static byte[] makeCastPacket(final Player recipient)
    {
        try
        {
            // create a packet with all the players in it
            final GametableFrame frame = GametableFrame.getGametableFrame();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_CAST);

            final List<Player> players = frame.getPlayers();
            dos.writeInt(players.size());
            
            for (Player player : players)
            {
                dos.writeUTF(player.getCharacterName());
                dos.writeUTF(player.getPlayerName());
                dos.writeInt(player.getId());
                dos.writeBoolean(player.isHostPlayer());
            }

            // finally, tell the recipient which player he is
            final int whichPlayer = frame.getPlayerIndex(recipient);
            dos.writeInt(whichPlayer);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** PLAYER PACKET *********************************** */

    /* ********************* CLEAR DECK PACKET *********************************** */
    public static byte[] makeClearDeckPacket(final String deckName)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_DECK_CLEAR_DECK); // packet type
            dos.writeUTF(deckName); // the deck in question

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** DECK LIST PACKET *********************************** */
    public static byte[] makeDeckListPacket(final List<Deck> decks)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_DECK_LIST); // packet type
            dos.writeInt(decks.size()); // number of decks
            for (Deck d : decks)
            {
                dos.writeUTF(d.m_name); // the name of this deck
            }
            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** TEXT PACKET *********************************** */

    /* ********************* DISCARD CARDS PACKET *********************************** */
    public static byte[] makeDiscardCardsPacket(final String playerName, final Card cards[])
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_DECK_DISCARD_CARDS); // packet type
            dos.writeUTF(playerName); // the player doing the discarding

            dos.writeInt(cards.length); // how many cards
            // and now the cards
            for (int i = 0; i < cards.length; i++)
            {
                cards[i].write(dos);
            }

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeErasePacket(final Rectangle r, final boolean bColorSpecific, final int color,
        final int authorPlayerID, final int stateID)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_ERASE); // type
            dos.writeInt(authorPlayerID);
            dos.writeInt(stateID);
            dos.writeInt(r.x);
            dos.writeInt(r.y);
            dos.writeInt(r.width);
            dos.writeInt(r.height);
            dos.writeBoolean(bColorSpecific);
            dos.writeInt(color);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** TYPING PACKET *********************************** */

    public static byte[] makeGridModePacket(final int hexMode)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_HEX_MODE); // type
            dos.writeInt(hexMode); // type

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeGrmPacket(final byte[] grmData)
    {
        // grmData will be the contents of the file
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            // write the packet type
            dos.writeInt(PACKET_FILE);

            // write the mime type
            dos.writeUTF("application/x-gametable-grm");

            // now write the data length
            dos.writeInt(grmData.length);

            // and finally, the data itself
            dos.write(grmData);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** PRIVATE TEXT PACKET *************************** */
    
    /** *******************************************************************************************
     * #grouping 
     * @revise these make... packets are more clues that packet types should be moved to classes 
     * @param openLink
     * @param closeLink
     * @return
     */
    public static byte[] makeGroupPacket(Action action, final String group, final MapElementInstanceID pogID, final int player) {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(PACKET_GROUP);
            dos.writeInt(action.ordinal());
            if(group == null) dos.writeUTF("");
            else dos.writeUTF(group);           
            
            if (pogID == null)
            	dos.writeLong(0);
           	else
           		dos.writeLong(pogID.numeric());
            
            dos.writeInt(player);
            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
        
    }
    
    public static byte[] makePogTypePacket(final MapElementInstanceID id, final MapElementInstanceID type) {
      try
      {
          final ByteArrayOutputStream baos = new ByteArrayOutputStream();
          final DataOutputStream dos = new DataOutputStream(baos);

          dos.writeInt(PACKET_POG_TYPE);
          dos.writeLong(id.numeric());
          dos.writeLong(type.numeric());

          return baos.toByteArray();
      }
      catch (final IOException ex)
      {
          Log.log(Log.SYS, ex);
          return null;
      }
  }

    /** *************************************************************************************
     * 
     * @param dis
     */    
    public static void readGroupPacket(final DataInputStream dis) {
        try
        {
            final int actionOrd = dis.readInt();
            Action action = Action.fromOrdinal(actionOrd);
            
            final String group = dis.readUTF();
            
            long pogID = dis.readLong();
            MapElementInstanceID pog = MapElementInstanceID.fromNumeric(pogID);
            
            final int player = dis.readInt();
            GametableFrame.getGametableFrame().groupPacketReceived(action, group, pog, player);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }
    
    public static void readPogTypePacket(final DataInputStream dis)
    {
        try
        {
        	long pid = dis.readLong();
          MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
          
          pid = dis.readLong();
          MapElementInstanceID type = MapElementInstanceID.fromNumeric(pid);
          
            // tell the model
            GametableFrame.getGametableFrame().pogTypePacketReceived(id, type);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }



    


    public static byte[] makeLinesPacket(List<LineSegment> lines, final int authorPlayerID, final int stateID)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_LINES); // type
            dos.writeInt(authorPlayerID);
            dos.writeInt(stateID);
            dos.writeInt(lines.size());
            
            for (LineSegment line : lines)
            {
                line.writeToPacket(dos);
            }

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }
    
    public static byte[] makeLinesPacket(LineSegment line, final int authorPlayerID, final int stateID)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_LINES); // type
            dos.writeInt(authorPlayerID);
            dos.writeInt(stateID);
            dos.writeInt(1);
            
            line.writeToPacket(dos);
            
            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* ************************ LOCKPOG PACKET ********************************* */
    public static byte[] makeLockAllPogPacket(final boolean newLocked)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_LOCKALLPOG); // type           
            dos.writeBoolean(newLocked);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeLockPogPacket(final MapElementInstanceID id, final boolean newLocked)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_LOCKPOG); // type
            dos.writeLong(id.numeric());
            dos.writeBoolean(newLocked);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** LINES PACKET *********************************** */

    public static byte[] makeLoginCompletePacket()
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_LOGIN_COMPLETE); // type
            // there's actually no additional data. Just the info that the login is complete

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeMovePogPacket(final MapElementInstanceID id, final int newX, final int newY)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_MOVEPOG); // type
            dos.writeLong(id.numeric());
            dos.writeInt(newX);
            dos.writeInt(newY);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** ERASE PACKET *********************************** */

    public static byte[] makePingPacket()
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_PING); // type
            // there's actually no additional data. Just the info that the login is complete

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makePlayerPacket(final Player plr, final String password)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_PLAYER);
            dos.writeInt(GametableFrame.COMM_VERSION);
            dos.writeUTF(password);
            dos.writeUTF(plr.getCharacterName());
            dos.writeUTF(plr.getPlayerName());
            dos.writeBoolean(plr.isHostPlayer());

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makePngPacket(final String filename)
    {
        // load the entire png file
        final byte[] pngFileData = UtilityFunctions.loadFileToArray(filename);

        if (pngFileData == null)
        {
            return null;
        }

        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            // write the packet type
            dos.writeInt(PACKET_FILE);

            // write the mime type
            dos.writeUTF("image/png");

            // write the filename
            dos.writeUTF(UtilityFunctions.getUniversalPath(filename));

            // now write the data length
            dos.writeInt(pngFileData.length);

            // and finally, the data itself
            dos.write(pngFileData);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makePngRequestPacket(final String filename)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_PNGREQUEST); // type
            dos.writeUTF(UtilityFunctions.getUniversalPath(filename));

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makePogDataPacket(final MapElementInstanceID id, final String s)
    {
        return makePogDataPacket(id, s, null, null);
    }

    public static byte[] makePogDataPacket(final MapElementInstanceID id, final String s, final Map<String, String> toAdd, final Set<String> toDelete) 
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_POGDATA);
            dos.writeLong(id.numeric());
            if (s != null)
            {
                dos.writeBoolean(true);
                dos.writeUTF(s);
            }
            else
            {
                dos.writeBoolean(false);
            }

            // removing
            if (toDelete == null)
            {
                dos.writeInt(0);
            }
            else
            {
                dos.writeInt(toDelete.size());
                for (String key : toDelete)
                {
                    dos.writeUTF(key);
                }
            }

            // adding
            if (toAdd == null)
            {
                dos.writeInt(0);
            }
            else
            {
                dos.writeInt(toAdd.size());
                for (Entry<String, String> entry : toAdd.entrySet())
                {
                    dos.writeUTF(entry.getKey());
                    dos.writeUTF(entry.getValue());
                }
            }

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /** *******************************************************************************************
     * 
     * @param id
     * @param layer
     * @return
     */
    public static byte[] makePogLayerPacket(final MapElementInstanceID id, final Layer layer)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_POGLAYER);
            dos.writeLong(id.numeric());
            dos.writeInt(layer.ordinal());

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }
    
    /* *********************** REMOVEPOG PACKET *********************************** */

    public static byte[] makePogReorderPacket(final Map<MapElementInstanceID, Long> changes)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_POG_REORDER);
            dos.writeInt(changes.size());
            for (Entry<MapElementInstanceID, Long> entry : changes.entrySet())
            {
                dos.writeLong(entry.getKey().numeric());
                dos.writeLong(entry.getValue());
            }

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makePogSizePacket(final MapElementInstanceID id, final float size)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_POG_SIZE);
            dos.writeLong(id.numeric());
            dos.writeFloat(size);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** MOVEPOG PACKET *********************************** */

    public static byte[] makePointPacket(final int plrIdx, final int x, final int y, final boolean bPointing)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_POINT); // type
            dos.writeInt(plrIdx);
            dos.writeInt(x);
            dos.writeInt(y);
            dos.writeBoolean(bPointing);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makePrivMechanicsPacket(final String toName, final String text)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_PRIVMECH); // type
            dos.writeUTF(toName);
            dos.writeUTF(text);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }
    
    public static byte[] makePrivateTextPacket(final String fromName, final String toName, final String text)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_PRIVATE_TEXT); // type
            dos.writeUTF(fromName);
            dos.writeUTF(toName);
            dos.writeUTF(text);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* ********************* RERECEIVE CARDS PACKET *********************************** */
    public static byte[] makeReceiveCardsPacket(final Card cards[])
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_DECK_RECEIVE_CARDS); // packet type
            dos.writeInt(cards.length); // how many cards

            // and now the cards
            for (int i = 0; i < cards.length; i++)
            {
                cards[i].write(dos);
            }

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeRecenterPacket(final int x, final int y, final int zoom)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_RECENTER); // type
            dos.writeInt(x);
            dos.writeInt(y);
            dos.writeInt(zoom);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeRejectPacket(final int reason)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_REJECT); // type
            dos.writeInt(reason); // type

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** POINT PACKET *********************************** */

    public static byte[] makeRemovePogsPacket(final MapElementInstanceID ids[])
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_REMOVEPOGS); // type

            // the number of pogs to be removed is first
            dos.writeInt(ids.length);

            // then the IDs of the pogs.
            for (MapElementInstanceID id : ids)
            {
                dos.writeLong(id.numeric());
            }

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }
    
    public static byte[] makeRemovePogsPacket(List<MapElementInstance> pogs)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_REMOVEPOGS); // type

            // the number of pogs to be removed is first
            dos.writeInt(pogs.size());

            // then the IDs of the pogs.
            for (MapElementInstance pog : pogs)
            {
                dos.writeLong(pog.getId().numeric());
            }

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* ********************* REQUEST CARDS PACKET *********************************** */
    public static byte[] makeRequestCardsPacket(final String deckName, final int numCards)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_DECK_REQUEST_CARDS); // packet type
            dos.writeUTF(deckName); // the deck
            dos.writeInt(numCards); // how many cards

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** POGDATA PACKET *********************************** */

    /* *********************** ROTATEPOG PACKET ********************************* */
    public static byte[] makeRotatePogPacket(final MapElementInstanceID id, final double newAngle)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_ROTATEPOG); // type
            dos.writeLong(id.numeric());
            dos.writeDouble(newAngle);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeForceSnapPogPacket(final MapElementInstanceID id, final boolean forceGridSnap)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_FORCEGRIDSNAP); // type
            dos.writeLong(id.numeric());
            dos.writeBoolean(forceGridSnap);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /* *********************** FLIPPOG PACKET ********************************* */
    public static byte[] makeFlipPogPacket(final MapElementInstanceID id, final boolean left, final boolean right)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_FLIPPOG); // type
            dos.writeLong(id.numeric());
            dos.writeBoolean(left);
            dos.writeBoolean(right);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeTextPacket(final String text)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_TEXT); // type
            dos.writeUTF(text);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }
    
    public static byte[] makeMechanicsPacket(final String text)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_MECHANICS); // type
            dos.writeUTF(text);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] makeTypingPacket(final String playerName, final boolean typing)
    {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(PACKET_TYPING); // type
            dos.writeUTF(playerName);
            dos.writeBoolean(typing);

            return baos.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static void readAddPogPacket(final Connection conn, final DataInputStream dis)
    {
        try
        {
            final boolean bPublicLayerPog = dis.readBoolean(); // layer. true = public. false = private
            //System.out.println(bPublicLayerPog);
            
            final MapElementInstance pog = new MapElementInstance(dis);
            if (pog.isCorrupted())
            {
                // for one reason or another, this pog is corrupt and should
                // be ignored
                return;
            }

            if (!pog.getPogType().isLoaded())
            {
                // we need this image
                requestPogImage(conn, pog);
            }

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.addPogPacketReceived(pog, bPublicLayerPog);
        }
        catch (final IOException ex)
        {
            Log.log(Log.NET, ex);
        }
    }
    /** *******************************************************************************************
     * 
     * @param dis
     */

    public static void readBGColPacket(final DataInputStream dis)
    {
        try
        {   
            final boolean pogMode = dis.readBoolean();
            
            if (pogMode)
            {
            	long pogID = dis.readLong();
            	MapElementInstanceID id = MapElementInstanceID.fromNumeric(pogID);
            	GametableFrame.getGametableFrame().changeBGPacketRec(id);
            }
            else
            {
            	int colorID = dis.readInt();
            	BackgroundColor color = BackgroundColor.fromOrdinal(colorID);
            	
            	GametableFrame.getGametableFrame().changeBGPacketRec(color);
            }
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }
    
    public static void readCastPacket(final DataInputStream dis)
    {
        try
        {
            final int numPlayers = dis.readInt();
            final Player[] players = new Player[numPlayers];
            for (int i = 0; i < numPlayers; i++)
            {
                final String charName = dis.readUTF();
                final String playerName = dis.readUTF();
                final int playerID = dis.readInt();
                players[i] = new Player(playerName, charName, playerID);
                players[i].setHostPlayer(dis.readBoolean());
            }

            // get which index we are
            final int ourIdx = dis.readInt();

            // this is only ever received by players
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.updateCast(players, ourIdx);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readClearDeckPacket(final DataInputStream dis)
    {
        try
        {
            // which deck?
            final String deckName = dis.readUTF();

            // tell the model
            GametableFrame.getGametableFrame().clearDeck(deckName);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** REDO PACKET *********************************** */

    public static void readDeckListPacket(final DataInputStream dis)
    {
        try
        {
            final int numDecks = dis.readInt();
            final String[] deckNames = new String[numDecks];

            for (int i = 0; i < deckNames.length; i++)
            {
                deckNames[i] = dis.readUTF();
            }

            // tell the model
            GametableFrame.getGametableFrame().deckListPacketReceived(deckNames);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readDiscardCardsPacket(final DataInputStream dis)
    {
        try
        {
            // who is discarding?
            final String playerName = dis.readUTF();

            // how many cards are there?
            final int numCards = dis.readInt();

            // make the array
            final Card cards[] = new Card[numCards];

            // read in all the cards
            for (int i = 0; i < cards.length; i++)
            {
                cards[i] = new Card();
                cards[i].read(dis);
            }

            // tell the model
            GametableFrame.getGametableFrame().doDiscardCards(playerName, cards);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** REJECT PACKET *********************************** */

    public static void readErasePacket(final DataInputStream dis)
    {

        try
        {
            final int authorID = dis.readInt();
            final int stateID = dis.readInt();

            final Rectangle r = new Rectangle();
            r.x = dis.readInt();
            r.y = dis.readInt();
            r.width = dis.readInt();
            r.height = dis.readInt();

            final boolean bColorSpecific = dis.readBoolean();
            final int color = dis.readInt();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.erasePacketReceived(r, bColorSpecific, color, authorID, stateID);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readFilePacket(final DataInputStream dis)
    {
        // get the mime type of the file
        try
        {
            // get the mime type
            final String mimeType = dis.readUTF();

            if (mimeType.equals("image/png"))
            {
                // this is a png file
                readPngPacket(dis);
            }
            else if (mimeType.equals("application/x-gametable-grm"))
            {
                // this is a png file
                readGrmPacket(dis);
            }
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** HEX MODE PACKET *********************************** */

    public static void readGridModePacket(final DataInputStream dis)
    {

        try
        {
            final int gridMode = dis.readInt();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.gridModePacketReceived(gridMode);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readGrmPacket(final DataInputStream dis)
    {
        try
        {
            // read the length of the png file data
            final int len = dis.readInt();

            // the file itself
            final byte[] grmFile = new byte[len];
            dis.read(grmFile);

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.grmPacketReceived(grmFile);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** FILE PACKET *********************************** */

    public static void readLinesPacket(final DataInputStream dis)
    {
        try
        {
            final int authorID = dis.readInt();
            final int stateID = dis.readInt();
            final int numLines = dis.readInt();
            
            List<LineSegment> lines = new ArrayList<LineSegment>(numLines);
            
            for (int i = 0; i < numLines; i++)
            {
            	lines.add(new LineSegment(dis));
            }

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.linesPacketReceived(lines, authorID, stateID);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** PNG PACKET *********************************** */

    public static void readLockAllPogPacket(final DataInputStream dis)
    {
        try
        {           
            final boolean newLocked = dis.readBoolean();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.lockAllPogPacketReceived(newLocked);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readLockPogPacket(final DataInputStream dis)
    {
        try
        {
        	long pid = dis.readLong();
          MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
            final boolean newLocked = dis.readBoolean();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.lockPogPacketReceived(id, newLocked);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readLoginCompletePacket(final DataInputStream dis)
    {
        // there's no data in a login_complete packet.

        // tell the model
        final GametableFrame gtFrame = GametableFrame.getGametableFrame();
        gtFrame.loginCompletePacketReceived();
    }


    public static void readMechanicsPacket(final DataInputStream dis)
    {
        try
        {
            final String text = dis.readUTF();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.mechanicsPacketReceived(text);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }
    
    public static void readMovePogPacket(final DataInputStream dis)
    {
        try
        {
        	long pid = dis.readLong();
          MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
            final int newX = dis.readInt();
            final int newY = dis.readInt();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.movePogPacketReceived(id, newX, newY);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readPacket(final Connection conn, final byte[] packet)
    {
        try
        {
            final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet));
            final int type = dis.readInt();

            Log.log(Log.NET, "Received: " + getPacketName(type) + ", length = " + packet.length);
            // find the player responsible for this

            PacketSourceState.beginNetPacketProcessing();
            switch (type)
            {
                case PACKET_PLAYER:
                {
                    readPlayerPacket(conn, dis);
                }
                break;

                case PACKET_REJECT:
                {
                    readRejectPacket(dis);
                }
                break;
                case PACKET_BGCOL:
                    readBGColPacket(dis);
                    break;
                case PACKET_CAST:
                {
                    readCastPacket(dis);
                }
                break;

                case PACKET_TEXT:
                {
                    readTextPacket(dis);
                }
                break;
                case PACKET_MECHANICS:
                {
                    readMechanicsPacket(dis);
                }
                break;
                case PACKET_PRIVMECH:
                {
                    readPrivMechanicsPacket(dis);
                }
                break;
                case PACKET_TYPING:
                {
                    readTypingPacket(dis);
                }
                break;

                case PACKET_LINES:
                {
                    readLinesPacket(dis);
                }
                break;

                case PACKET_ERASE:
                {
                    readErasePacket(dis);
                }
                break;

                case PACKET_ADDPOG:
                {
                    readAddPogPacket(conn, dis);
                }
                break;

                case PACKET_REMOVEPOGS:
                {
                    readRemovePogsPacket(dis);
                }
                break;
                case PACKET_POGLAYER:
                    readPogLayer(dis);
                    break;
                case PACKET_MOVEPOG:
                {
                    readMovePogPacket(dis);
                }
                break;

                case PACKET_ROTATEPOG:
                {
                    readRotatePogPacket(dis);
                }
                break;

                case PACKET_FLIPPOG:
                {
                    readFlipPogPacket(dis);
                }
                break;

                case PACKET_LOCKPOG:
                {
                    readLockPogPacket(dis);
                }
                break;

                case PACKET_LOCKALLPOG:
                {
                    readLockAllPogPacket(dis);
                }
                break;

                case PACKET_POINT:
                {
                    readPointPacket(dis);
                }
                break;

                case PACKET_POGDATA:
                {
                    readPogDataPacket(dis);
                }
                break;

                case PACKET_RECENTER:
                {
                    readRecenterPacket(dis);
                }
                break;

                case PACKET_FILE:
                {
                    readFilePacket(dis);
                }
                break;

                case PACKET_PNGREQUEST:
                {
                    readPngRequestPacket(conn, dis);
                }
                break;

                case PACKET_HEX_MODE:
                {
                    readGridModePacket(dis);
                }
                break;

                case PACKET_LOGIN_COMPLETE:
                {
                    readLoginCompletePacket(dis);
                }
                break;

                case PACKET_PING:
                {
                    readPingPacket(dis);
                }
                break;

                case PACKET_POG_SIZE:
                {
                    readPogSizePacket(dis);
                }
                break;

                case PACKET_PRIVATE_TEXT:
                {
                    readPrivateTextPacket(dis);
                }
                break;

                case PACKET_POG_REORDER:
                {
                    readPogReorderPacket(dis);
                }
                break;

                case PACKET_DECK_LIST:
                {
                    readDeckListPacket(dis);
                }
                break;

                case PACKET_DECK_REQUEST_CARDS:
                {
                    readRequestCardsPacket(conn, dis);
                }
                break;

                case PACKET_DECK_RECEIVE_CARDS:
                {
                    readReceiveCardsPacket(dis);
                }
                break;

                case PACKET_DECK_DISCARD_CARDS:
                {
                    readDiscardCardsPacket(dis);
                }
                break;

                case PACKET_DECK_CLEAR_DECK:
                {
                    readClearDeckPacket(dis);
                }
                break;
                
                case PACKET_GROUP:
                  readGroupPacket(dis);
                  break;
              case PACKET_POG_TYPE:
                  readPogTypePacket(dis);
                  break;


                default:
                {
                    throw new IllegalArgumentException("Unknown packet");
                }
            }
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }

        PacketSourceState.endNetPacketProcessing();
    }

    /* *********************** PNG REQUEST PACKET *********************************** */

    public static void readPingPacket(final DataInputStream dis)
    {
        // there's no data in a login_complete packet.

        // tell the model
        final GametableFrame gtFrame = GametableFrame.getGametableFrame();
        gtFrame.pingPacketReceived();
    }

    public static void readPlayerPacket(final Connection conn, final DataInputStream dis)
    {

        try
        {
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();

            final int commVersion = dis.readInt();
            if (commVersion != GametableFrame.COMM_VERSION)
            {
                // cut them off right there.
                gtFrame.kick(conn, GametableFrame.REJECT_VERSION_MISMATCH);
                return;
            }

            final String password = dis.readUTF();
            final String characterName = dis.readUTF();
            final String playerName = dis.readUTF();
            final Player newPlayer = new Player(playerName, characterName, -1);
            newPlayer.setHostPlayer(dis.readBoolean());

            // this is only ever received by the host
            gtFrame.playerJoined(conn, newPlayer, password);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** TEXT PACKET *********************************** */

    public static void readPngPacket(final DataInputStream dis)
    {
        try
        {
            // the file name
            final String filename = UtilityFunctions.getLocalPath(dis.readUTF());

            // read the length of the png file data
            final int len = dis.readInt();

            // the file itself
            final byte[] pngFile = new byte[len];
            dis.read(pngFile);

            // validate PNG file
            if (!UtilityFunctions.isPngData(pngFile))
            {
                GametableFrame.getGametableFrame().getChatPanel().logAlertMessage(
                    "Illegal pog data: \"" + filename + "\", aborting transfer.");
                return;
            }

            // validate file location
            final File here = new File("").getAbsoluteFile();
            File target = new File(filename).getAbsoluteFile();
            if (!UtilityFunctions.isAncestorFile(here, target))
            {
                GametableFrame.getGametableFrame().getChatPanel().logAlertMessage("Malicious pog path? \"" + filename + "\"");
                final String temp = filename.toLowerCase();
                if (temp.contains("underlay"))
                {
                    target = new File("underlays" + File.separator + target.getName());
                }
                else if (temp.contains("pog"))
                {
                    target = new File("pogs" + File.separator + target.getName());
                }
                else
                {
                    GametableFrame.getGametableFrame().getChatPanel().logAlertMessage(
                        "Illegal pog path: \"" + filename + "\", aborting transfer.");
                    return;
                }
            }

            final File parentDir = target.getParentFile();
            if (!parentDir.exists())
            {
                parentDir.mkdirs();
            }

            // now save out the png file
            final OutputStream os = new BufferedOutputStream(new FileOutputStream(target));
            os.write(pngFile);
            os.flush();
            os.close();

            final MapElement pogType = GametableFrame.getGametableFrame().getPogLibrary().getPog(filename);
            pogType.load();

            // tell the pog panels to check for the new image
            GametableFrame.getGametableFrame().refreshPogList();

            // Ok, now send the file out to any previously unfulfilled requests.
            final File providedFile = new File(filename).getCanonicalFile();
            final Iterator<String> iterator = g_unfulfilledRequests.keySet().iterator();
            byte[] packet = null;
            while (iterator.hasNext())
            {
                final String requestedFilename = iterator.next();
                final Set<Connection> connections = g_unfulfilledRequests.get(requestedFilename);
                
                if (connections.isEmpty())
                {
                    iterator.remove();
                    continue;
                }

                final File requestedFile = new File(requestedFilename).getCanonicalFile();
                if (requestedFile.equals(providedFile))
                {
                    if (packet == null)
                    {
                        packet = makePngPacket(filename);
                        if (packet == null)
                        {
                            // Still can't make packet
                            // TODO: echo failure message to peoples?
                            break;
                        }
                    }

                    // send to everyone asking for this file
                    for (Connection connection : connections)                    
                    {
                        connection.sendPacket(packet);
                    }
                }
            }
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readPngRequestPacket(final Connection conn, final DataInputStream dis)
    {
        try
        {
            // someone wants a png file from us.
            final String filename = UtilityFunctions.getLocalPath(dis.readUTF());

            // make a png packet and send it back
            final byte[] packet = makePngPacket(filename);
            if (packet != null)
            {
                conn.sendPacket(packet);
            }
            else
            {
                addUnfulfilledRequest(filename, conn);
            }
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** PING PACKET *********************************** */

    public static void readPogDataPacket(final DataInputStream dis)
    {
        try
        {
        	long pid = dis.readLong();
          MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
            String name = null;
            if (dis.readBoolean())
            {
                name = dis.readUTF();
            }

            final Set<String> toDelete = new HashSet<String>();
            final int numToDelete = dis.readInt();
            for (int i = 0; i < numToDelete; ++i)
            {
                toDelete.add(dis.readUTF());
            }
            final Map<String, String> toAdd = new HashMap<String, String>();
            final int numToAdd = dis.readInt();
            for (int i = 0; i < numToAdd; ++i)
            {
                final String key = dis.readUTF();
                final String value = dis.readUTF();
                toAdd.put(key, value);
            }

            // tell the model
            GametableFrame.getGametableFrame().pogDataPacketReceived(id, name, toAdd, toDelete);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /** ********************************************************************************************
     * Reads the layer of the pog from the packet.
     * @param dis
     */
    public static void readPogLayer(final DataInputStream dis) {
        try {
        	long pid = dis.readLong();
          MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
            final Layer layer = Layer.fromOrdinal(dis.readInt());
            GametableFrame.getGametableFrame().pogLayerPacketReceived(id, layer);
            
        } catch (final IOException ex) {
            Log.log(Log.SYS, ex);
        }
        
    }
    
    public static void readPogReorderPacket(final DataInputStream dis)
    {
        try
        {
            final int numChanges = dis.readInt();
            final Map<MapElementInstanceID, Long> changes = new HashMap<MapElementInstanceID, Long>();
            for (int i = 0; i < numChanges; ++i)
            {
            	long pid = dis.readLong();
              MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
              
                // id, order
                changes.put(id, dis.readLong());
            }

            // tell the model
            GametableFrame.getGametableFrame().pogReorderPacketReceived(changes);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** POG_SIZE PACKET *********************************** */

    public static void readPogSizePacket(final DataInputStream dis)
    {
        try
        {
        	long pid = dis.readLong();
          MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
            final float size = dis.readFloat();

            // tell the model
            GametableFrame.getGametableFrame().pogSizePacketReceived(id, size);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readPointPacket(final DataInputStream dis)
    {

        try
        {
            final int plrIdx = dis.readInt();
            final int x = dis.readInt();
            final int y = dis.readInt();
            final boolean bPointing = dis.readBoolean();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.pointPacketReceived(plrIdx, x, y, bPointing);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readPrivMechanicsPacket(final DataInputStream dis)
    {
        try
        {
            final String toName = dis.readUTF();
            final String text = dis.readUTF();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.privMechanicsPacketReceived(toName, text);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }
    
    public static void readPrivateTextPacket(final DataInputStream dis)
    {
        try
        {
            final String fromName = dis.readUTF();
            final String toName = dis.readUTF();
            final String text = dis.readUTF();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.privateTextPacketReceived(fromName, toName, text);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readReceiveCardsPacket(final DataInputStream dis)
    {
        try
        {
            // how many cards are there?
            final int numCards = dis.readInt();

            // make the array
            final Card cards[] = new Card[numCards];

            // read in all the cards
            for (int i = 0; i < cards.length; i++)
            {
                cards[i] = new Card();
                cards[i].read(dis);
            }

            // tell the model
            GametableFrame.getGametableFrame().receiveCards(cards);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readRecenterPacket(final DataInputStream dis)
    {
        try
        {
            final int x = dis.readInt();
            final int y = dis.readInt();
            final int zoom = dis.readInt();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.recenterPacketReceived(x, y, zoom);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readRejectPacket(final DataInputStream dis)
    {

        try
        {
            final int reason = dis.readInt();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.rejectPacketReceived(reason);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readRemovePogsPacket(final DataInputStream dis)
    {
        try
        {
            // the number of pogs to be removed is first
            final MapElementInstanceID[] ids = new MapElementInstanceID[dis.readInt()];

            // then the IDs of the pogs.
            for (int i = 0; i < ids.length; i++)
            {
            	long pid = dis.readLong();
              MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
              
                ids[i] = id;
            }

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.removePogsPacketReceived(ids);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readRequestCardsPacket(final Connection conn, final DataInputStream dis)
    {
        try
        {
            // note the deck we're after
            final String deckName = dis.readUTF();

            // note how many cards have been requested
            final int numCards = dis.readInt();

            // tell the model
            GametableFrame.getGametableFrame().requestCardsPacketReceived(conn, deckName, numCards);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readRotatePogPacket(final DataInputStream dis)
    {
        try
        {
            long pid = dis.readLong();
            MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
            
            final double newAngle = dis.readDouble();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.rotatePogPacketReceived(id, newAngle);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readFlipPogPacket(final DataInputStream dis)
    {
        try
        {
            final int pid = dis.readInt();            
            MapElementInstanceID id = MapElementInstanceID.fromNumeric(pid);
            
            final boolean flipH = dis.readBoolean();
            final boolean flipV = dis.readBoolean();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.flipPogPacketReceived(id, flipH, flipV);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readTextPacket(final DataInputStream dis)
    {
        try
        {
            final String text = dis.readUTF();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.textPacketReceived(text);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    public static void readTypingPacket(final DataInputStream dis)
    {

        try
        {
            final String playerName = dis.readUTF();
            final boolean typing = dis.readBoolean();

            // tell the model
            final GametableFrame gtFrame = GametableFrame.getGametableFrame();
            gtFrame.typingPacketReceived(playerName, typing);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }
    }

    /* *********************** POG_SIZE PACKET *********************************** */

    public static void requestPogImage(final Connection conn, final MapElementInstance pog)
    {
        final String desiredFile = pog.getPogType().getImageFilename();

        if (g_requestedFiles.contains(desiredFile))
        {
            return;
        }

        // add it to the list of pogs that need art
        g_requestedFiles.add(desiredFile);

        // there are no pending requests for this file. Send one
        // if this somehow came from a null connection, return
        if (conn == null)
        {
            return;
        }

        conn.sendPacket(makePngRequestPacket(desiredFile));
    }

    // --- Constructors ----------------------------------------------------------------------------------------------

    // prevent instantiation
    private PacketManager()
    {
        throw new RuntimeException("PacketManager should not be instantiated!");
    }

}