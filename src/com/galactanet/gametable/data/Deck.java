package com.galactanet.gametable.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;



/**
 * This is a deck, managing the cards and distributing them as needed.
 * For the functionality that manages loading a deck from disk, see DeckData
 * 
 * #GT-AUDIT Deck
 */
public class Deck
{
    /** 
     *  a static Random number generator
     */
    public final static Random g_random   = new Random();

    /** 
     * Cards in the deck.  
     * This lists is filled with Integers referring to indices that can be sent in to DeckData.getCard.
     */
    private List<Integer>               m_deck     = new ArrayList<Integer>();

    private DeckData           m_deckData;
    
    // @comment {themaze75} I made all these public, but I strongly recommend going through accessor methods.
    
    /** 
     * Cards in the discards
     * This lists is filled with Integers referring to indices that can be sent in to DeckData.getCard.
     */
    public List<Integer>                       m_discards = new ArrayList<Integer>();

    /**
     * Deck ID
     */
    public int                        m_id;

    /**
     * Deck Name
     */
    public String                     m_name;

    /**
     * Constructor
     */
    public Deck()
    {
    }

    /** 
     * @return the number of cards that are neither in the deck nor in the discards
     */
    public int cardsOutstanding()
    {
        return m_deckData.getNumCards() - m_deck.size() - m_discards.size();
    }

    /**
     * @return the number of cards remaining in the deck
     */
    public int cardsRemaining()
    {
        return m_deck.size();
    }

    /**
     * Discards a given card
     * @param card card to discard
     */
    public void discard(final Card card)
    {
        if (!card.m_deckName.equals(m_name))
        {
            // this is not our card. ignore it
            return;
        }

        // note the id of the discarded card
        final Integer cardNum = new Integer(card.m_cardId);

        // sanity check: make sure a given card only gets added if
        // it's not already somewhere else.
        for (int i = 0; i < m_discards.size(); i++)
        {
            final int checkCardId = (m_discards.get(i)).intValue();
            if (checkCardId == card.m_cardId)
            {
                // we already have this in the discards
                // don't panic, just mention it
                System.out.println("discarded card already in discards.");
                return;
            }
        }

        for (int i = 0; i < m_deck.size(); i++)
        {
            final int checkCardId = (m_deck.get(i)).intValue();
            if (checkCardId == card.m_cardId)
            {
                // we already have this in the discards
                // don't panic, just mention it
                System.out.println("discarded card already in the deck.");
                return;
            }
        }

        // add it to the discards
        m_discards.add(cardNum);
    }

    /**
     * Draw a card from the deck
     * @return Card drawn card
     */
    public Card drawCard()
    {
        // get the next card in the deck
        final Integer cardNum = m_deck.get(0);
        m_deck.remove(0);
        // System.out.println(""+cardNum.intValue());
        final Card card = m_deckData.getCard(cardNum.intValue());

        // note the id. We'll need that later when it gets discarded
        card.m_cardId = cardNum.intValue();
        card.m_deckName = m_name;
        return card;
    }

    /**
     * Initialize and shuffle the deck
     * @param deckData deck data to use (cards used to populate the deck)
     * @param id deck ID
     * @param name deck Name
     */
    public void init(final DeckData deckData, final int id, final String name)
    {
        m_id = id;
        m_name = name;
        m_deckData = deckData;

        // generate a number for each of the cards in the deck
        for (int i = 0; i < m_deckData.getNumCards(); i++)
        {
            final Integer toAdd = new Integer(i);
            m_deck.add(toAdd);
        }
        shuffle();
    }

    /**
     * Initializes the deck as a place holder. Meaning an empty deck with no data or ID.
     * @param name deck Name
     */
    public void initPlaceholderDeck(final String name)
    {       
        m_name = name;
        m_id = -1;
        m_deckData = null; // this being null ensures that functional calls will throw exceptions
    }


    /**
     * Randomizer helper function 
     */
    private int rand()
    {
        return Math.abs(g_random.nextInt());
    }
 
    /**
     * Shuffles the cards and discards together
     */
    public void shuffle()
    {
        // put all the discards into the deck
        for (int i = 0; i < m_discards.size(); i++)
        {
            final Integer cardNum = m_discards.get(i);
            m_deck.add(cardNum);
        }
        m_discards.clear();

        // if the deck has 0 or 1 cards, there's no point in going on
        if (m_deck.size() < 2)
        {
            return;
        }

        // shuffle the deck by randomly pulling cards within
        // it and putting them at the back
        // we do a total of 2*(deck size) rearrangements. This way, each card
        // will have been moved an average of 2 times. That's a fair
        // bit of shuffling
        for (int i = 0; i < m_deckData.getNumCards() * 2; i++)
        {
            // yes we could end up swapping a card with itself. That's ok
            final int idx = rand() % m_deck.size();
            final Integer cardNum = m_deck.get(idx);

            // pull it out of the deck
            m_deck.remove(idx);

            // move it to the end
            m_deck.add(cardNum);
        }
    }

    /**
     * Re-shuffles ALL cards in the deck, even those not in the discards
     */
    public void shuffleAll()
    {
        // clear out the deck and discards lists
        m_deck.clear();
        m_discards.clear();

        // fill up the deck array again
        for (int i = 0; i < m_deckData.getNumCards(); i++)
        {
            final Integer toAdd = new Integer(i);
            m_deck.add(toAdd);
        }

        // shuffle
        shuffle();
    }
}
