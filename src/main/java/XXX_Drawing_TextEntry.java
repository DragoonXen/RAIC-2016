import java.awt.*;

/**
 * Created by by.dragoon on 10/31/16.
 */
public class XXX_Drawing_TextEntry {

	private String text;
	private Color color;

	public XXX_Drawing_TextEntry(String text) {
		this(text, Color.black);
	}

	public XXX_Drawing_TextEntry(String text, Color color) {
		this.text = text;
		this.color = color;
	}

	public String getText() {
		return text;
	}

	public Color getColor() {
		return color;
	}
}
