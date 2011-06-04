package com.gametable.ui;

/**
 * TODO Extract to top level class in UI
 */
public enum BackgroundColor
{
	BLACK("Black"), BLUE("Blue"), BROWN("Brown"), DARK_BLUE("Dark Blue"), DARK_GREEN("Dark Green"), DARK_GREY("Dark Grey"), DEFAULT("Default"), GREEN(
			"Green"), GREY("Grey"), WHITE("White");

	/**
	 * Get BackgroundColor object from ordinal value
	 * 
	 * @param ordinal
	 * @return
	 */
	public static BackgroundColor fromOrdinal(int ordinal)
	{
		BackgroundColor values[] = BackgroundColor.values();
		if (ordinal < 0 || ordinal >= values.length)
			return null;
		
		return values[ordinal];
	}

	/**
	 * Private constructor
	 * 
	 * @param text
	 */
	private BackgroundColor(String text)
	{
		m_text = text;
	}

	/**
	 * Get a text representation for the color
	 * 
	 * @return string
	 */
	public String getText()
	{
		return m_text;
	}

	private final String	m_text;
}