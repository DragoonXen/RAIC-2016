import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Created by by.dragoon on 10/31/16.
 */
public class YYY_Drawing_MainFrame extends JFrame {

	private final double MAX_X;
	private final double MAX_Y;

	private YYY_Drawing_DrawPanel drawPanel;

	private JButton prevBtn;
	private JButton nextBtn;
	private JButton runBtn;

	private JSlider slider;

	private YYY_Drawing_TextInfoPanel textInfoPanel;

	public YYY_Drawing_MainFrame(double maxX, double maxY) throws HeadlessException {
		this.MAX_X = maxX;
		this.MAX_Y = maxY;

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setVisible(true);
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
		setMinimumSize(new Dimension(800, 400));

		setFocusTraversalKeysEnabled(false);
		setFocusable(true);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1;
		c.weightx = 1;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.BOTH;
		add(drawPanel = new YYY_Drawing_DrawPanel(MAX_X, MAX_Y), c);

		c.gridx = 4;
		c.gridy = 0;
		c.weighty = 0;
		c.weightx = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.VERTICAL;
		add(textInfoPanel = new YYY_Drawing_TextInfoPanel(), c);
		textInfoPanel.setPreferredSize(new Dimension(300, 10));

		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		add(prevBtn = new JButton("<"), c);
		prevBtn.setPreferredSize(new Dimension(100, 20));
		prevBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				movePrev();
			}
		});

		c.gridx = 1;
		c.gridy = 1;
		add(nextBtn = new JButton(">"), c);
		nextBtn.setPreferredSize(new Dimension(100, 20));
		nextBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				moveNext();
			}
		});

		c.gridx = 2;
		c.gridy = 1;
		add(runBtn = new JButton("Run"), c);
		runBtn.setPreferredSize(new Dimension(100, 20));
		runBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				run();
			}
		});

		c.gridwidth = 5;
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(slider = new JSlider(0, 0, 0), c);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				repaintSlider();
			}
		});

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int count = 1;
				switch (e.getModifiers()) {
					case 2:
						count = 10;
						break;
					case 1:
						count = 100;
						break;
				}
				switch (e.getKeyCode()) {
					case 39:
						moveNext(count);
						break;
					case 37:
						movePrev(count);
						break;
					case 10:
						run();
						break;
				}
			}
		});
		slider.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int count = 0;
				switch (e.getModifiers()) {
					case 2:
						count = 10;
						break;
					case 1:
						count = 100;
						break;
				}
				switch (e.getKeyCode()) {
					case 39:
						moveNext(count);
						break;
					case 37:
						movePrev(count);
						break;
					case 10:
						run();
						break;
				}
			}
		});
		slider.setFocusTraversalKeysEnabled(false);
		slider.setFocusable(true);
	}

	private void moveNext() {
		moveNext(1);
	}

	private void moveNext(int cnt) {
		slider.setValue(slider.getValue() + cnt);
		run();
	}

	private void movePrev() {
		movePrev(1);
	}

	private void movePrev(int cnt) {
		slider.setValue(slider.getValue() - cnt);
		run();
	}

	private void run() {
		YYY_Drawing_DrawingStrategy.getInstance().move(slider.getValue());
	}

	private void repaintSlider() {
		textInfoPanel.putText(String.format("%d ticks / %d total",
											slider.getValue(),
											slider.getMaximum()),
							  0);
		textInfoPanel.repaint();
	}

	public YYY_Drawing_DrawPanel getDrawPanel() {
		return drawPanel;
	}

	public YYY_Drawing_TextInfoPanel getTextInfoPanel() {
		return textInfoPanel;
	}

	public JSlider getSlider() {
		return slider;
	}
}
