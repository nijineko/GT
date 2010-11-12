/*
 * CardModule.java
 * 
 * @created 2010-07-15
 * 
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.plugins.cards;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.ChatEngineIF.MessageType;
import com.galactanet.gametable.data.net.NetAddMapElement;
import com.galactanet.gametable.module.Module;
import com.galactanet.gametable.net.NetworkConnectionIF;
import com.galactanet.gametable.net.NetworkEvent;
import com.galactanet.gametable.net.NetworkModuleIF;
import com.galactanet.gametable.net.NetworkStatus;
import com.galactanet.gametable.ui.chat.SlashCommands;
import com.galactanet.gametable.util.UtilityFunctions;
import com.maziade.messages.MessageDefinition;
import com.maziade.messages.MessageID;
import com.maziade.messages.MessageListener;
import com.maziade.messages.MessagePriority;
import com.maziade.tools.XMLUtils;

/**
 * Experimental module implementation fragment (noncommital enough?)
 * 
 * @author Eric Maziade
 */
public class CardModule extends Module implements MessageListener
{
	private static CardModule					g_module			= null;

	private static MessageDefinition	MSG_DISCARD;

	private static MessageID					MSGID_DISCARD;

	public static CardModule getModule()
	{
		if (g_module == null)
			g_module = new CardModule();

		return g_module;
	}

	// all the cards you have
	private final List<Card>					m_cards				= new ArrayList<Card>();

	// only valid if this client is the host
	private final List<Deck>					m_decks				= new ArrayList<Deck>();	// List of decks

	private Vector<Card>							m_discardPile	= new Vector<Card>();

	/**
	 * 
	 */
	private CardModule()
	{
		MSGID_DISCARD = MessageID.acquire(CardModule.class.getCanonicalName() + ".DISCARD");
		MSG_DISCARD = new MessageDefinition(MSGID_DISCARD, MessagePriority.LOW);
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
	 * clear all cards of a given deck
	 * 
	 * @param deckName name of the deck whose cards will be deleted
	 */
	public void clearDeck(final String deckName)
	{
		for (int i = 0; i < m_cards.size(); i++) // for each card
		{
			final Card card = m_cards.get(i);
			if (card.getDeckName().equals(deckName)) // if it belongs to the deck to erase
			{
				// this card has to go.
				m_cards.remove(i); // remove the card
				i--; // to keep up with the changed list
			}
		}
	}

	/**
	 * handles the reception of a list of decks
	 * 
	 * @param deckNames array of string with the names of decks received
	 */
	public void deckListPacketReceived(final String[] deckNames)
	{
		GameTableCore core = GametableApp.getCore();

		// if we're the host, this is a packet we should never get
		if (core.getNetworkStatus() == NetworkStatus.HOSTING)
		{
			throw new IllegalStateException("Host received deckListPacket.");
		}

		// set up out bogus decks to have the appropriate names
		m_decks.clear();

		for (int i = 0; i < deckNames.length; i++)
		{
			final Deck bogusDeck = new Deck();
			bogusDeck.initPlaceholderDeck(deckNames[i]);
			m_decks.add(bogusDeck);
		}
	}

	/**
	 * discards cards from a deck
	 * 
	 * @param playerName who is discarding the cards
	 * @param discards array of cards to discard
	 */
	public void doDiscardCards(final String playerName, final Card discards[])
	{
		if (discards.length == 0) // nothing to discard
		{
			// this shouldn't happen, but let's not freak out.
			return;
		}

		GameTableCore core = GametableApp.getCore();

		// only the host should get this
		if (core.getNetworkStatus() != NetworkStatus.HOSTING)
		{
			throw new IllegalStateException("doDiscardCards should only be done by the host.");
		}

		// tell the decks about the discarded cards
		for (int i = 0; i < discards.length; i++)
		{
			final String deckName = discards[i].getDeckName();
			final Deck deck = getDeck(deckName);
			if (deck == null)
			{
				// don't panic. Just ignore it. It probably means
				// a player discarded a card right as the host deleted the deck
			}
			else
			{
				deck.discard(discards[i]);
			}
		}

		// finally, remove any card pogs that are hanging around based on these cards
		removeCardPogsForCards(discards);

		// tell everyone about the cards that got discarded
		if (discards.length == 1)
		{
			core.sendMessageBroadcast(MessageType.SYSTEM, playerName + " " + "discards" + ": " + discards[0].getCardName());
		}
		else
		{
			core.sendMessageBroadcast(MessageType.SYSTEM, playerName + " " + "discard" + " " + discards.length + " "
					+ "cards");
			for (int i = 0; i < discards.length; i++)
			{
				core.sendMessageBroadcast(MessageType.SYSTEM, "---" + discards[i].getCardName());
			}
		}
	}

	/*
	 * @see com.maziade.messages.MessageListener#executeMessage(com.maziade.messages.MessageID,
	 * com.maziade.messages.MessagePriority, java.lang.Object, java.lang.String)
	 */
	@Override
	public void executeMessage(MessageID msgID, MessagePriority proprity, Object param, String debug)
	{
		if (msgID == MSGID_DISCARD)
			doDiscard(m_discardPile);
	}

	/**
	 * get a number of cards from a deck
	 * 
	 * @param deckName name of the deck to draw from
	 * @param num number of cards to draw
	 * @return array with the cards drawn
	 */
	public Card[] getCards(final String deckName, final int num)
	{
		GameTableCore core = GametableApp.getCore();

		int numCards = num;

		// get the deck
		final Deck deck = getDeck(deckName);
		if (deck == null)
		{
			// the deck doesn't exist. There are various ways this could happen,
			// mostly due to split-second race conditions where the host deletes the deck while
			// a card request was incoming. We just return null in this edge case.
			return null;
		}

		if (numCards <= 0)
		{
			// invalid
			throw new IllegalArgumentException("drawCards: " + numCards);
		}

		// We can't draw more cards than there are
		final int remain = deck.cardsRemaining();
		if (numCards > remain)
		{
			numCards = remain;
		}

		// make the return value
		final Card ret[] = new Card[numCards];

		// draw the cards
		for (int i = 0; i < numCards; i++)
		{
			ret[i] = deck.drawCard();
		}

		// now that the cards are drawn, check the deck status
		if (deck.cardsRemaining() == 0)
		{
			// no more cards in the deck, alert them
			core.sendMessageBroadcast(MessageType.SYSTEM, deckName + " " + "is out of cards.");
		}

		return ret;
	}

	/**
	 * gets a deck by name or null if it doesn't exist
	 * 
	 * @param name name of the deck to get
	 * @return the requested deck or null if it doesn't exist
	 */
	public Deck getDeck(final String name)
	{
		final int idx = getDeckIdx(name);
		if (idx == -1)
		{
			return null;
		}
		// Doesn't get here if idx == -1; no need for else
		// else
		{
			final Deck d = m_decks.get(idx);
			return d;
		}
	}

	/**
	 * gets the position in the list of decks of a deck with a given name or -1 if not found
	 * 
	 * @param name name of the deck to locate
	 * @return the position of the deck in the list or -1 if not found
	 */
	public int getDeckIdx(final String name)
	{
		for (int i = 0; i < m_decks.size(); i++)
		{
			final Deck d = m_decks.get(i);
			if (d.m_name.equals(name))
			{
				return i;
			}
		}
		return -1;
	}

	/*
	 * @see com.galactanet.gametable.module.ModuleIF#getModuleName()
	 */
	@Override
	public String getModuleName()
	{
		return CardModule.class.getName();
	}

	/*
	 * @see com.galactanet.gametable.module.Module#loadFromXML(org.w3c.dom.Element, com.galactanet.gametable.data.XMLSerializeConverter, com.galactanet.gametable.net.NetworkEvent)
	 */
	@Override
	public void loadFromXML(Element node, XMLSerializeConverter converter, NetworkEvent netEvent)
	{
		Card.g_cardMap.clear();

		for (Element cardEl : XMLUtils.getChildElementsByTagName(node, "card"))
		{
			long saveID = UtilityFunctions.parseLong(cardEl.getAttribute("id"), 0);
			MapElementID id = converter.getMapElementID(saveID);
			if (id != null)
			{
				Card card = new Card();
				card.loadFromXML(cardEl);
				Card.g_cardMap.put(id, card);
			}
		}
	}
	
	/*
	 * @see com.galactanet.gametable.module.Module#onInitializeCore(com.galactanet.gametable.data.GametableCore)
	 */
	@Override
	public void onInitializeCore(GameTableCore core)
	{
		NetworkModuleIF network = core.getNetworkModule();
		
		network.registerMessageType(new NetClearDeck());
		network.registerMessageType(new NetDiscardCards());
		network.registerMessageType(new NetReceiveCards());
		network.registerMessageType(new NetRequestCards());
		network.registerMessageType(new NetSendDeckList());

		SlashCommands.registerChatCommand(new DeckCommand());

		GameTableMap publicMap = core.getMap(GameTableCore.MapType.PUBLIC);
		GameTableMap privateMap = core.getMap(GameTableCore.MapType.PRIVATE);

		GameTableMapListenerIF listener = new GameTableMapAdapter() {
			/*
			 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstanceRemoved(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElement, boolean, com.galactanet.gametable.net.NetworkEvent)
			 */
			@Override
			public void onMapElementRemoved(GameTableMap map, MapElement mapElement, boolean batch, NetworkEvent netEvent)
			{
				discard(mapElement);
			}
		};

		publicMap.addListener(listener);
		privateMap.addListener(listener);

		GameTableCoreListenerIF frameListener = new GameTableCoreAdapter() {
			/*
			 * @see com.galactanet.gametable.ui.GameTableFrameAdapter#onHostingStarted()
			 */
			@Override
			public void onHostingStarted()
			{
				super.onHostingStarted();

				m_decks.clear();
				m_cards.clear();
			}

			/*
			 * @see com.galactanet.gametable.ui.GameTableFrameAdapter#onPlayerJoined(com.galactanet.gametable.data.Player)
			 */
			@Override
			public void onPlayerJoined(Player player)
			{
				super.onPlayerJoined(player);

				// send deck list to new player
				sendDeckList();
			}
		};

		core.addListener(frameListener);
	}

	public void receiveCards(final Card cards[])
	{
		GameTableCore core = GametableApp.getCore();

		if (cards.length == 0)
		{
			// drew 0 cards. Ignore.
			return;
		}

		// all of these cards get added to your hand
		for (int i = 0; i < cards.length; i++)
		{
			m_cards.add(cards[i]);
			final String toPost = "You drew:" + " " + cards[i].getCardName() + " (" + cards[i].getDeckName() + ")";
			core.sendMessageLocal(MessageType.SYSTEM, toPost);
		}

		// make sure we're on the private layer
		if (core.isActiveMapPublic())
		{
			// we call toggleLayer rather than setActiveMap because
			// toggleLayer cleanly deals with drags in action and other
			// interrupted actions.
			core.setActiveMap(core.isActiveMapPublic() ? GameTableCore.MapType.PRIVATE : GameTableCore.MapType.PUBLIC);
		}

		// tell everyone that you drew some cards
		if (cards.length == 1)
		{
			core.sendMessageBroadcast(MessageType.SYSTEM, core.getPlayer().getPlayerName() + " " + "draws from the" + " " + cards[0].getDeckName()
					+ " " + "deck");
		}
		else
		{
			core.sendMessageBroadcast(MessageType.SYSTEM, core.getPlayer().getPlayerName() + " " + "draws" + " " + cards.length + " "
					+ "cards from the" + " " + cards[0].getDeckName() + " " +  "deck" + ".");
		}

	}

	public void requestCardsPacketReceived(final NetworkConnectionIF conn, final String deckName, final int numCards)
	{
		GameTableCore core = GametableApp.getCore();

		if (core.getNetworkStatus() != NetworkStatus.HOSTING)
		{
			// this shouldn't happen
			throw new IllegalStateException("Non-host had a call to requestCardsPacketReceived");
		}
		// the player at conn wants some cards
		final Card cards[] = getCards(deckName, numCards);

		if (cards == null)
		{
			// there was a problem. Probably a race-condition thaty caused a
			// card request to get in after a deck was deleted. Just ignore this
			// packet.
			return;
		}

		// send that player his cards
		core.send(NetReceiveCards.makePacket(cards), conn);

		// also, we need to send that player the pogs for each of those cards
		for (int i = 0; i < cards.length; i++)
		{
			final MapElement newPog = makeCardPog(cards[i]);

			if (newPog != null)
			{
				// make a pog packet, saying this pog should go to the PRIVATE LAYER,
				// then send it to that player. Note that we don't add it to
				// our own layer.
				core.send(NetAddMapElement.makePacket(newPog, false), conn);
			}
		}
	}

	/*
	 * @see com.galactanet.gametable.module.ModuleSaveIF#saveToXML(org.w3c.dom.Element)
	 */
	@Override
	public boolean saveToXML(Element node)
	{
		if (Card.g_cardMap.isEmpty())
			return false;

		Document doc = node.getOwnerDocument();

		for (Entry<MapElementID, Card> entry : Card.g_cardMap.entrySet())
		{
			Element cardEl = doc.createElement("card");
			cardEl.setAttribute("id", String.valueOf(entry.getKey().numeric()));
			entry.getValue().saveToXML(cardEl);
			node.appendChild(cardEl);
		}

		return true;
	}

	/**
	 * interprets and execute the deck commands
	 * 
	 * @param words array of words in the deck command
	 */
	protected void deckCommand(final String[] words)
	{
		GameTableCore core = GametableApp.getCore();
		
		// we need to be in a network game to issue deck commands
		// otherwise log the error and exit
		if (core.getNetworkStatus() == NetworkStatus.DISCONNECTED)
		{
			core.sendMessageLocal(MessageType.ALERT, "You must be in a session to use /deck commands.");
			return;
		}

		// words[0] will be "/deck". IF it weren't we wouldn't be here.
		if (words.length < 2)
		{
			// they just said "/deck". give them the help text and return
			showDeckUsage();
			return;
		}

		final String command = words[1]; // since words[0] is the word "deck" we are interested in the
		// next word, that's why we take words[1]

		if (command.equals("create")) // create a new deck
		{
			if (core.getNetworkStatus() != NetworkStatus.HOSTING) // verify that we are the host of the network game
			{
				core.sendMessageLocal(MessageType.ALERT, "Only the host can create a deck.");
				return;
			}

			// create a new deck.
			if (words.length < 3) // we were expecting the deck name
			{
				// not enough parameters
				showDeckUsage();
				return;
			}

			final String deckFileName = words[2];
			String deckName;
			if (words.length == 3)
			{
				// they specified the deck, but not a name. So we
				// name it after the type
				deckName = deckFileName;
			}
			else
			{
				// they specified a name
				deckName = words[3];
			}

			// if the name is already in use, puke out an error
			if (getDeck(deckName) != null)
			{
				core.sendMessageLocal(MessageType.ALERT, "Error - There is already a deck named" + " '" + deckName + "'.");
				return;
			}

			// create the deck stored in an xml file
			final DeckData dd = new DeckData();
			final File deckFile = new File("decks" + File.separator + deckFileName + ".xml");
			boolean result = dd.init(deckFile);

			if (!result)
			{
				core.sendMessageLocal(MessageType.ALERT, "Could not create the deck.");
				return;
			}

			// create a deck and add it
			final Deck deck = new Deck();
			deck.init(dd, 0, deckName);
			m_decks.add(deck);

			// alert all players that this deck has been created
			sendDeckList();
			core.sendMessageBroadcast(MessageType.SYSTEM, core.getPlayer().getPlayerName() + " " + "creates a new" + " " + deckFileName
					+ " " + "deck named" + " " + deckName);

		}
		else if (command.equals("destroy")) // remove a deck
		{
			if (core.getNetworkStatus() != NetworkStatus.HOSTING)
			{
				core.sendMessageLocal(MessageType.ALERT, "Only the host can destroy a deck.");
				return;
			}

			if (words.length < 3)
			{
				// they didn't specify a deck
				showDeckUsage();
				return;
			}

			// remove the deck named words[2]
			final String deckName = words[2];
			final int toRemoveIdx = getDeckIdx(deckName); // get the position of the deck in the deck list

			if (toRemoveIdx != -1) // if we found the deck
			{
				// we can successfully destroy the deck
				m_decks.remove(toRemoveIdx);

				// tell the players
				clearDeck(deckName);
				sendDeckList();
				core.sendMessageBroadcast(MessageType.SYSTEM, core.getPlayer().getPlayerName() + "destroys the deck named");
			}
			else
			{
				// we couldn't find a deck with that name
				core.sendMessageLocal(MessageType.ALERT, "There is no deck named" + " '" + deckName + "'.");
			}
		}
		else if (command.equals("shuffle")) // shuffle the deck
		{
			if (core.getNetworkStatus() != NetworkStatus.HOSTING) // only if you are the host
			{
				core.sendMessageLocal(MessageType.ALERT, "Only the host can shuffle a deck.");
				return;
			}

			if (words.length < 4)
			{
				// not enough parameters
				showDeckUsage();
				return;
			}

			final String deckName = words[2];
			final String operation = words[3];

			// first get the deck
			final Deck deck = getDeck(deckName);
			if (deck == null)
			{
				// and report the error if not found
				core.sendMessageLocal(MessageType.ALERT, "There is no deck named" + " '" + deckName + "'.");
				return;
			}

			if (operation.equals("all"))
			{
				// collect and shuffle all the cards in the deck.
				clearDeck(deckName); // let the other players know about the demise of those cards
				deck.shuffleAll();
				core.sendMessageBroadcast(MessageType.SYSTEM, core.getPlayer().getPlayerName() + "collects all the cards from the " + deckName
						+ " deck from all players and shuffles them.");
				core.sendMessageBroadcast(MessageType.SYSTEM,deckName + " " + "has" + " " + deck.cardsRemaining() + " "
						+ "cards" + ".");
			}
			else if (operation.equals("discards"))
			{
				// shuffle only the cards in the discard pile.
				deck.shuffle();
				core.sendMessageBroadcast(MessageType.SYSTEM,core.getPlayer().getPlayerName() + "shuffles the discards back into the " + deckName + " "
						+ "deck" + ".");
				core.sendMessageBroadcast(MessageType.SYSTEM,deckName + " has " + deck.cardsRemaining() + " "
						+ "cards" + ".");
			}
			else
			{
				// the shuffle operation is illegal
				core.sendMessageLocal(MessageType.ALERT,"'" + operation + "' " + "is not a valid type of shuffle. This parameter must be either 'all' or 'discards'.");
				return;
			}
		}
		else if (command.equals("draw")) // draw a card from the deck
		{
			// before chesking net status we check to see if the draw command was
			// legally done
			if (words.length < 3)
			{
				// not enough parameters
				showDeckUsage();
				return;
			}

			// ensure that desired deck exists -- this will work even if we're not the
			// host. Because we'll have "dummy" decks in place to track the names
			final String deckName = words[2];
			final Deck deck = getDeck(deckName);
			if (deck == null)
			{
				// that deck doesn't exist
				core.sendMessageLocal(MessageType.ALERT, "There is no deck named" + " '" + deckName + "'.");
				return;
			}

			int numToDraw = 1;
			// they optionally can specify a number of cards to draw
			if (words.length >= 4)
			{
				// numToDrawStr is never used.
				// String numToDrawStr = words[3];

				// note the number of cards to draw
				try
				{
					numToDraw = Integer.parseInt(words[3]);
					if (numToDraw <= 0)
					{
						// not allowed
						throw new Exception();
					}
				}
				catch (final Exception e)
				{
					// it's ok not to specify a number of cards to draw. It's not
					// ok to put garbage in that field
					core.sendMessageLocal(MessageType.ALERT,"'" + words[3] + "' " + "is not a valid number of cards to draw");
				}
			}

			drawCards(deckName, numToDraw);
		}
		else if (command.equals("hand")) // this shows the cards in our hand
		{
			if (m_cards.size() == 0)
			{
				core.sendMessageLocal(MessageType.SYSTEM, "You have no cards");
				return;
			}

			core.sendMessageLocal(MessageType.SYSTEM, "You have" + " " + m_cards.size() + " " + "cards"
					+ ":");

			for (int i = 0; i < m_cards.size(); i++) // for each card
			{
				final int cardIdx = i + 1;
				final Card card = m_cards.get(i); // get the card
				// craft a message
				final String toPost = "" + cardIdx + ": " + card.getCardName() + " (" + card.getDeckName() + ")";
				// log the message
				core.sendMessageLocal(MessageType.SYSTEM, toPost);
			}
		}
		else if (command.equals("discard")) // discard a card
		{
			// discard the nth card from your hand
			// 1-indexed
			if (words.length < 3)
			{
				// note enough parameters
				showDeckUsage();
				return;
			}

			final String param = words[2];

			// the parameter can be "all" or a number
			Card discards[];
			if (param.equals("all"))
			{
				// discard all cards
				discards = new Card[m_cards.size()];

				for (int i = 0; i < discards.length; i++)
				{
					discards[i] = m_cards.get(i);
				}
			}
			else
			{
				// discard the specified card
				int idx = -1;
				try
				{
					idx = Integer.parseInt(param);
					idx--; // make it 0-indexed

					if (idx < 0) // we can't discard a card with a negative index
					{
						throw new Exception();
					}

					if (idx >= m_cards.size()) // we can't discard a card higher than what we have
					{
						throw new Exception();
					}
				}
				catch (final Exception e)
				{
					// they put in some illegal value for the param
					core.sendMessageLocal(MessageType.ALERT, "There is no card named" + " '" + param + "'.");
					return;
				}
				discards = new Card[1];
				discards[0] = m_cards.get(idx);
			}

			// now we have the discards[] filled with the cards to be
			// removed
			discardCards(discards);
		}
		else if (command.equals("decklist"))
		{
			// list off the decks
			// we keep "dummy" decks for joiners,
			// so either a host of a joiner is safe to use this code:
			if (m_decks.size() == 0)
			{
				core.sendMessageLocal(MessageType.SYSTEM, "There are no decks");
				return;
			}

			core.sendMessageLocal(MessageType.SYSTEM, "There are" + " " + m_decks.size() + " " + "decks");
			for (int i = 0; i < m_decks.size(); i++)
			{
				final Deck deck = m_decks.get(i);
				core.sendMessageLocal(MessageType.SYSTEM, "---" + deck.m_name);
			}
		}
		else
		{
			// they selected a deck command that doesn't exist
			showDeckUsage();
		}
	}

	/**
	 * Discard a card
	 * 
	 * @param element
	 */
	private void discard(MapElement element)
	{
		Card card = Card.getCard(element);
		if (card != null)
		{
			m_discardPile.add(card);

			// Queue a discard message
			MSG_DISCARD.addMessage(this, "discard");
		}
	}

	/**
	 * remove cards from our deck
	 * 
	 * @param discards array of cards to discard
	 */
	private void discardCards(final Card discards[])
	{
		GameTableCore core = GametableApp.getCore();

		if (core.getNetworkStatus() == NetworkStatus.CONNECTED)
		{
			// if we are not the host we have bogus decks, so we send a package to
			// notify of the discards. It will be processed by the host
			core.sendBroadcast(NetDiscardCards.makePacket(core.getPlayer().getPlayerName(), discards));
		}
		else if (core.getNetworkStatus() == NetworkStatus.HOSTING)
		{
			// we are the host, so we can process the discard of the cards
			doDiscardCards(core.getPlayer().getPlayerName(), discards);
		}

		// and in either case, we remove the cards from ourselves.
		for (int i = 0; i < m_cards.size(); i++) // for each card we have
		{
			final Card handCard = m_cards.get(i);
			for (int j = 0; j < discards.length; j++) // compare with each card to discard
			{
				if (handCard.equals(discards[j])) // if they are the same
				{
					// we need to dump this card
					m_cards.remove(i);
					i--; // to keep up with the iteration
					break;
				}
			}
		}
	}

	/**
	 * Discard list
	 * 
	 * @param cardsList
	 */
	private void doDiscard(Collection<Card> cardsList)
	{
		// Remove the offending cards
		if (cardsList.size() > 0)
		{
			Card cards[];

			synchronized (cardsList)
			{
				cards = cardsList.toArray(new Card[0]);
				cardsList.clear();
			}

			discardCards(cards);
		}
	}

	/**
	 * draw cards from a deck. Non-host players request it from the host. The host is the one that actually draws from the
	 * deck
	 * 
	 * @param deckName deck to draw cards from
	 * @param numToDraw how many cards are requested
	 */
	private void drawCards(final String deckName, final int numToDraw)
	{
		GameTableCore core = GametableApp.getCore();

		if (core.getNetworkStatus() == NetworkStatus.CONNECTED)
		{
			// joiners send a request for cards
			core.sendBroadcast(NetRequestCards.makePacket(deckName, numToDraw));
			return;
		}

		// if we're here, we're the host. So we simply draw the cards
		// and give it to ourselves.
		final Card drawnCards[] = getCards(deckName, numToDraw);
		if (drawnCards != null)
		{
			receiveCards(drawnCards);
			
			GameTableMap privateMap = core.getMap(GameTableCore.MapType.PRIVATE);

			// also, we need to add the desired cards to our own private layer
			for (int i = 0; i < drawnCards.length; i++)
			{
				final MapElement cardPog = makeCardPog(drawnCards[i]);
				if (cardPog != null)
				{
					privateMap.addMapElement(cardPog);						
				}
			}
		}
	}

	// makes a card pog out of the sent in card
	private MapElement makeCardPog(final Card card)
	{
		GameTableCore core = GametableApp.getCore();

		// there might not be a pog associated with this card
		if (card.getCardFile().length() == 0)
		{
			return null;
		}

		final MapElementTypeIF newPogType = core.getMapElementTypeLibrary().getMapElementType("pogs" + File.separator + card.getCardFile());

		if (newPogType == null)
		{
			return null;
		}

		final MapElement newPog = new MapElement(newPogType);

		// make it a card pog
		Card.setCard(newPog, card);
		return newPog;
	}
	private void removeCardPogsForCards(final Card discards[])
	{
		GameTableCore core = GametableApp.getCore();

		GameTableMap publicMap = core.getMap(GameTableCore.MapType.PUBLIC);
		GameTableMap privateMap = core.getMap(GameTableCore.MapType.PRIVATE);

		// distribute this to each layer
		removeCardPogsForCards(privateMap, discards);
		removeCardPogsForCards(publicMap, discards);
	}

	/**
	 * Remove pogs linked to cards
	 * @param discards
	 */
	private void removeCardPogsForCards(GameTableMap map, final Card discards[])
	{
		final List<MapElement> removeList = new ArrayList<MapElement>();

		for (MapElement pog : map.getMapElements())
		{
			final Card pogCard = Card.getCard(pog);
			if (pogCard != null)
			{
				// this is a card pog. Is it out of the discards?
				for (int j = 0; j < discards.length; j++)
				{
					if (pogCard.equals(discards[j]))
					{
						// it's the pog for this card
						removeList.add(pog);
					}
				}
			}
		}

		// remove any offending pogs
		map.removeMapElements(removeList);
	}

	private void showDeckUsage()
	{
		GameTableCore core = GametableApp.getCore();
		
		core.sendMessageLocal(MessageType.SYSTEM, "/deck usage: ");
		core.sendMessageLocal(MessageType.SYSTEM, "---/deck create [decktype] [deckname]: create a new deck. [decktype] is the name of a deck in the decks directory. It will be named [deckname]");
		core.sendMessageLocal(MessageType.SYSTEM, "---/deck destroy [deckname]: remove the specified deck from the session.");
		core.sendMessageLocal(MessageType.SYSTEM, "---/deck shuffle [deckname] ['all' or 'discards']: shuffle cards back in to the deck.");
		core.sendMessageLocal(MessageType.SYSTEM, "---/deck draw [deckname] [number]: draw [number] cards from the specified deck.");
		core.sendMessageLocal(MessageType.SYSTEM, "---/deck hand [deckname]: List off the cards (and their ids) you have from the specified deck.");
		core.sendMessageLocal(MessageType.SYSTEM, "---/deck /discard [cardID]: Discard a card. A card's ID can be seen by using /hand.");
		core.sendMessageLocal(MessageType.SYSTEM, "---/deck /discard all: Discard all cards that you have.");
		core.sendMessageLocal(MessageType.SYSTEM, "---/deck decklist: Lists all the decks in play.");
	}
	
	void sendDeckList()
	{
		GameTableCore core = GametableApp.getCore();
		core.sendBroadcast(NetSendDeckList.makePacket(m_decks));
	}

}
