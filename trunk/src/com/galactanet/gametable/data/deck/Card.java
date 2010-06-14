

package com.galactanet.gametable.data.deck;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;



/**
 * This class represents a card. There should be only one instance of a particular card at any time.
 * 
 * #GT-AUDIT Card
 */
public class Card
{
    /**
     * Card description. ex: "This is the Jack of Clubs!"
     */
    protected String m_cardDesc = "";

    /**
     * Card image file. ex: "j_club.png"
     */
    protected String m_cardFile = "";

    /**
     * Card identifier number
     */
    protected int    m_cardId = -1;

    /**
     * Card name. ex: "Jack of Clubs"
     */
    protected String m_cardName = "";

    /**
     * Name of the deck this card is issued from
     */
    protected String m_deckName = "";

    /**
     * the number of instances of this card found within this deck.
     */
    protected int    m_quantityInDeck = 0;

    /**
     * Constructor (blank card)
     */
    public Card()
    {
    }

    /*
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        return makeCopy();
    }

    /**
     * Copy a second card's data into this instance
     * @param in card to copy from
     */
    public void copy(final Card in)
    {
        m_cardName = in.m_cardName;
        m_cardFile = in.m_cardFile;
        m_cardDesc = in.m_cardDesc;
        m_quantityInDeck = in.m_quantityInDeck;
        m_cardId = in.m_cardId;
        m_deckName = in.m_deckName;
    }

    /**
     * Compares this card with another one
     * 
     * @param in card to compare to
     * @return true if both cards are equal
     */
    public boolean equals(final Card in)
    {
        // we are "equal" to another card if we came from the
        // same deck and have the same id

        if (!in.m_cardName.equals(m_cardName))
        {
            return false;
        }

        if (in.m_cardId != m_cardId)
        {
            return false;
        }
        return true;
    }

    /**
     * @return Card Description
     */
    public String getCardDesc()
    {
        return m_cardDesc;
    }

    /**
     * Get the card's picture file name
     * @return
     */
    public String getCardFile()
    {
        return m_cardFile;
    }

    /**
     * @return Card ID within DeckData
     */
    public int getCardId()
    {
        return m_cardId;
    }

    /** 
     * @return Card Name
     */
    public String getCardName()
    {
        return m_cardName;
    }

    /**
     * @return Deck Name
     */
    public String getDeckName()
    {
        return m_deckName;
    }

    /**
     * @return number of instance of this card within a deck
     */
    public int getQuantityInDeck()
    {
        return m_quantityInDeck;
    }

    /**
     * @return Card instance
     */
    public Card makeCopy()
    {
        final Card ret = new Card();
        ret.copy(this);
        return ret;
    }

    /**
     * Read this card's data from stream
     * 
     * @param dis data input stream
     * @throws IOException
     */
    public void read(final DataInputStream dis) throws IOException
    {
        m_cardName = dis.readUTF();
        m_cardFile = dis.readUTF();
        m_cardDesc = dis.readUTF();
        m_deckName = dis.readUTF();
        m_quantityInDeck = dis.readInt();
        m_cardId = dis.readInt();
    }

    /**
     * Set the card's textual description
     * @param cardDesc
     */
    public void setCardDesc(String cardDesc)
    {
        m_cardDesc = cardDesc;
    }

    /**
     * Set the card's picture file name
     * @param cardFile
     */
    public void setCardFile(String cardFile)
    {
        m_cardFile = cardFile;
    }

    /**
     * Set the card's ID within DeckData
     * @param cardId
     */
    public void setCardId(int cardId)
    {
        m_cardId = cardId;
    }

    /**
     * Set the card's name
     * @param cardName
     */
    public void setCardName(String cardName)
    {
        m_cardName = cardName;
    }

    /**
     * Set the name of the deck that this card is part of
     * @param deckName
     */
    public void setDeckName(String deckName)
    {
        m_deckName = deckName;
    }

    /**
     * Sets the number of instances of this card that we can find in the deck
     * @param quantityInDeck
     */
    public void setQuantityInDeck(int quantityInDeck)
    {
        m_quantityInDeck = quantityInDeck;
    }

    /**
     * Write this card's data to stream
     * 
     * @param dos data output stream
     * @throws IOException
     */
    public void write(final DataOutputStream dos) throws IOException
    {
        dos.writeUTF(m_cardName);
        dos.writeUTF(m_cardFile);
        dos.writeUTF(m_cardDesc);
        dos.writeUTF(m_deckName);
        dos.writeInt(m_quantityInDeck);
        dos.writeInt(m_cardId);
    }

}
