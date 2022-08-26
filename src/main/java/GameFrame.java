import java.io.IOException;

import javax.swing.JFrame;

public class GameFrame extends JFrame {

	public GameFrame() throws IOException {
		this.add(new GamePanel(this));
		this.setTitle("Chess");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.setUndecorated(true);
		this.setAlwaysOnTop(true);
		this.pack();
		this.setVisible(true);
		this.setLocationRelativeTo(null);
	}

}