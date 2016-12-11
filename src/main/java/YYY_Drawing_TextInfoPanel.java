import javax.swing.*;
import java.awt.*;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by by.dragoon on 10/31/16.
 */
public class YYY_Drawing_TextInfoPanel extends JPanel {

	private List<YYY_Drawing_TextEntry> textEntryList;

	private Map<TextAttribute, Object> attributesMap;

	public YYY_Drawing_TextInfoPanel() {
		textEntryList = new LinkedList<YYY_Drawing_TextEntry>();
		attributesMap = new HashMap<TextAttribute, Object>();
		attributesMap.put(TextAttribute.FAMILY, "Serif");
		attributesMap.put(TextAttribute.SIZE, new Float(18.0));
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Graphics2D gr2D = (Graphics2D) g;
		int posY = 0;
		synchronized (this) {
			for (YYY_Drawing_TextEntry textEntry : textEntryList) {
				if (textEntry.getText().isEmpty()) {
					continue;
				}
				attributesMap.put(TextAttribute.FOREGROUND, textEntry.getColor());
				AttributedString stringToDraw = new AttributedString(textEntry.getText(), attributesMap);
				LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(stringToDraw.getIterator(),
																	   gr2D.getFontRenderContext());

				while (lineMeasurer.getPosition() != textEntry.getText().length()) {
					float breakWidth = (float) this.getWidth();
					TextLayout layout = lineMeasurer.nextLayout(breakWidth);

					posY += layout.getAscent();
					layout.draw(gr2D, 0, posY);
					posY += layout.getDescent() + layout.getLeading();
				}
			}
		}
	}

	public void clear() {
		synchronized (this) {
			textEntryList.clear();
		}
	}

	public void putText(String text, int position) {
		synchronized (this) {
			while (textEntryList.size() <= position) {
				textEntryList.add(new YYY_Drawing_TextEntry(""));
			}
			Color color = Color.black;
			if (position < textEntryList.size()) {
				YYY_Drawing_TextEntry remove = textEntryList.remove(position);
				color = remove.getColor();
			}
			textEntryList.add(position, new YYY_Drawing_TextEntry(text, color));
		}
	}
}
