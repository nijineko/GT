package com.galactanet.gametable.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 * This class is used by the Deck class to manage a random deck of cards. 
 * 
 * #GT-AUDIT DeckData
 */
public class DeckData
{
    /**
     * List of Card objects found within this DeckData
     */
    private final List<Card> m_cards = new ArrayList<Card>();

    /** 
     * Number of cards in this "DeckData"
     */
    private int        m_numCards;

    /**
     * Constructor
     */
    public DeckData()
    {
    }

    /**
     * Add a new Card to the DeckData
     * (used by the SAX parser) 
     */
    public void addCard(final Card newCardType)
    {
        m_cards.add(newCardType);
    }
 
    /**
     * Called when we're done adding CardTypes.
     * Calculates deck information (like how many cards there are)
     */
    public void deckComplete()
    {
        // work out how many cards are in this deck
        m_numCards = 0;
        for (int i = 0; i < m_cards.size(); i++)
        {
            final Card cardType = m_cards.get(i);
            m_numCards += cardType.m_quantityInDeck;
        }
    }
    
    /**
     * Get a card at the specified position in the DeckData, taking into account
     * the number of copies of a card.
     * 
     * Imagine that the deck is organized by card type, in the order
     * that the card types are in in m_cardTypes. This will return the
     * cardNum'th card in that order. A given number will always get
     * the same result. This allows the Deck class to track cards merely
     * by number. 
     * 
     * @param cardNum a number from 0 to getNumCards()-1
     * @return a COPY of the cardData.
     */
    public Card getCard(final int cardNum)
    {
        int cardIndex = cardNum;
        
        // @comment {themaze75} this is a list... it should be navigated by its iterator. 
        // (In this case its an ArrayList, so its fine but we shouldn't assume when using the "List" interface)
        
        for (int i = 0; i < m_cards.size(); i++)
        {
            final Card cardType = m_cards.get(i);
            cardIndex -= cardType.m_quantityInDeck;

            if (cardIndex < 0)
            {
                // the type we have here is the type that this card is
                final Card ret = new Card();
                ret.copy(cardType);
                return ret;
            }
        }

        // it should be impossible to get here. If we're here they sent
        // in a card that's out of range.
        throw new IllegalArgumentException("DeckData.getCardType():" + cardNum);
    }

    /**
     * @return the number of cards in this DeckData
     */
    public int getNumCards()
    {
        return m_numCards;
    }

    /**
     * Initialize DeckData
     * @param file input XML file containing deck data
     * @return true if loaded DeckData successfully
     */
    public boolean init(final File file)
    {
        try
        {
            // make a SAX parser and use our DeckData handler to
            // parse out the deck info
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            final DeckSaxHandler handler = new DeckSaxHandler();
            handler.init(this);
            parser.parse(file, handler);

            deckComplete();
            return true;
        }
        catch (final Exception e)
        {
            // error case
            e.printStackTrace();
            return false;
        }
    }
}
