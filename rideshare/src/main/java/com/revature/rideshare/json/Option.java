package com.revature.rideshare.json;

import com.revature.rideshare.util.EquivalenceUtilities;

/**
 * This class is used to create a Option.
 * <p>
 * Primarily used by {@link Action}
 * <p>
 * <b>Notable Fields:</b><br>
 * {@link #text}<br>
 * {@link #value}<br>
 * <p>
 * <b>Primary Constructor:</b><br>
 * {@link #Option(String, String) Option(String text, String value)}
 */
public class Option {

	/**
	 * A short, user-facing string to label this option to users. Use a maximum
	 * of 30 characters.
	 */
	private String text;

	/**
	 * A short string that identifies this particular option to your
	 * application. It will be sent to your Action URL when this option is
	 * selected.
	 */
	private String value;

	/**
	 * no-arg constructor
	 */
	public Option() {
	}

	/**
	 * Constructor used to create an Option
	 * 
	 * @param text
	 * @param value
	 */
	public Option(String text, String value) {
		super();
		this.text = text;
		this.value = value;
	}

	/**
	 * Get the text of the Option
	 * 
	 * @return the text of the Option
	 */
	public String getText() {
		return text;
	}

	/**
	 * Set the text of the Option
	 * 
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Get the value of the Option
	 * 
	 * @return the value of the Option
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the value of the Option
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * String representation of the Option
	 */
	@Override
	public String toString() {
		return "Option [text=" + text + ", value=" + value + "]";
	}

	@Override
	public int hashCode() {
		return new String(text + value).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != this.getClass()) {
			return false;
		}
		Option otherOption = (Option) obj;
		
		if (!EquivalenceUtilities.SafeCompare(getText(), otherOption.getText())) {
			return false;
		}
		if (!EquivalenceUtilities.SafeCompare(getValue(), otherOption.getValue())) {
			return false;
		}
		return true;
	}
}
